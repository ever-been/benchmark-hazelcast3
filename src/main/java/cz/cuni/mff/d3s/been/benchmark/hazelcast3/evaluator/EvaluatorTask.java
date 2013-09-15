package cz.cuni.mff.d3s.been.benchmark.hazelcast3.evaluator;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.TaskPropertyReader;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.NodeResult;
import cz.cuni.mff.d3s.been.core.persistence.EntityID;
import cz.cuni.mff.d3s.been.evaluators.EvaluatorResult;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.persistence.Query;
import cz.cuni.mff.d3s.been.persistence.QueryBuilder;
import cz.cuni.mff.d3s.been.taskapi.Evaluator;
import cz.cuni.mff.d3s.been.taskapi.TaskException;

/**
 * @author Martin Sixta
 */
public class EvaluatorTask extends Evaluator {
	private static final Logger log = LoggerFactory.getLogger(EvaluatorTask.class);

	@Override
	public EvaluatorResult evaluate() throws DAOException, TaskException {
		TaskPropertyReader props = new TaskPropertyReader(createPropertyReader());

		final String kind = props.getString(BenchmarkProperty.ENTITY_KIND);
		final String group = props.getString(BenchmarkProperty.ENTITY_GROUP);

		final EntityID eid = new EntityID().withKind(kind).withGroup(group);

		try {

			String benchmarkId = getTaskProperty("evaluator.benchmark.id");

			if (benchmarkId == null || benchmarkId.isEmpty()) {
				log.debug("Using current benchmarkId to fetch results");
				benchmarkId = getBenchmarkId();
			}

			Query query = new QueryBuilder().on(eid).with("benchmarkId", benchmarkId).with("type", "NODE").fetch();
			final Collection<NodeResult> nodeResults = results.query(query, NodeResult.class);

			Map<String, List<Double>> grouped = new HashMap<>();
			Map<Integer, String> runToCommit = new TreeMap<>();

			for (NodeResult nodeResult : nodeResults) {
				final String commit = nodeResult.getCommit();
				if (!grouped.containsKey(commit)) {
					grouped.put(commit, new ArrayList<Double>(10));
				}

				double ms = TimeUnit.MILLISECONDS.convert(nodeResult.getTime(), TimeUnit.NANOSECONDS);
				grouped.get(commit).add(ms);
				runToCommit.put(nodeResult.getRun(), commit);
			}

			String yCaption = "time (ms)";

			for (NodeResult r : nodeResults) {
				yCaption = String.format(
						"Nodes %d, Clients %s, Msgs/client: %s, Msg Size %d",
						r.getNodes(),
						r.getClients(),
						r.getMessages(),
						r.getMsgSize());
				break;
			}

			int width = props.getInteger(BenchmarkProperty.EVALUATOR_WIDTH_SIZE);
			int height = props.getInteger(BenchmarkProperty.EVALUATOR_HEIGHT_SIZE);

			BufferedImage image = generateChart(grouped, runToCommit, yCaption, width, height);

			ImageIO.write(image, "png", new File("out.png"));

			EvaluatorResult er = new EvaluatorResult();
			er.setBenchmarkId(getBenchmarkId());
			er.setFilename("hazelcast.png");
			er.setTimestamp(System.currentTimeMillis());
			er.setMimeType(EvaluatorResult.MIME_TYPE_IMAGE_PNG);
			er.setData(Files.readAllBytes(Paths.get("out.png")));
			er.setId(getId());

			return er;

		} catch (IllegalStateException e) {
			throw new TaskException("Cannot build a query to fetch results", e);
		} catch (IOException e) {
			throw new TaskException("Cannot generate results plot!", e);
		}

	}

	public BufferedImage generateChart(Map<String, List<Double>> data, Map<Integer, String> runToCommit, String yCaption,
			int width, int height) {
		// create dataset
		YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		YIntervalSeries s1 = new YIntervalSeries(yCaption);

		int i = 0;

		for (Integer run : runToCommit.keySet()) {
			String commit = runToCommit.get(run);

			DescriptiveStatistics stats = getStats(data.get(commit));

			double mean = stats.getMean();
			double std = stats.getStandardDeviation();

			s1.add(i++, mean, mean - 1.96 * std, mean + 1.96 * std);

		}

		dataset.addSeries(s1);

		// create chart
		//NumberAxis xAxis = new NumberAxis("Revision number");

		Set<String> strings = data.keySet();
		final String[] stringsArray = new String[strings.size()];

		int k = 0;
		for (String str : strings) {
			stringsArray[k++] = StringUtils.abbreviate(str, 10);
		}
		ValueAxis xAxis = new SymbolAxis("Commits", stringsArray);

		NumberAxis yAxis = new NumberAxis("Cluster processing time (ms)");
		yAxis.setAutoRangeStickyZero(false);
		XYErrorRenderer renderer = new XYErrorRenderer();
		renderer.setBaseLinesVisible(true);
		renderer.setSeriesStroke(0, new BasicStroke(3.0f));
		renderer.setBaseShapesVisible(false);
		renderer.setErrorPaint(Color.blue);
		renderer.setErrorStroke(new BasicStroke(1.0f));
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		JFreeChart chart = new JFreeChart("Average time to process queued work", plot);
		chart.setBackgroundPaint(Color.white);

		// create output image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setBackground(Color.white);
		graphics.clearRect(0, 0, width, height);

		// render
		chart.draw(graphics, new Rectangle2D.Double(0, 0, width, height));

		return image;
	}

	private double[] toPrimitiveArray(List<Double> list) {
		// Stupid Java
		double[] values = new double[list.size()];

		for (int j = 0; j < list.size(); ++j) {
			values[j] = list.get(j);
		}

		return values;
	}

	private DescriptiveStatistics getStats(List<Double> data) {

		double[] values = toPrimitiveArray(data);

		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = new DescriptiveStatistics();

		// Add the data from the array
		for (double value : values) {
			stats.addValue(value);
		}
		return stats;

	}
}
