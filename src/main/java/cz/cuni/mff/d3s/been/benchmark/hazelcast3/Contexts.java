package cz.cuni.mff.d3s.been.benchmark.hazelcast3;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.TaskPropertyReader;
import cz.cuni.mff.d3s.been.benchmarkapi.BenchmarkException;
import cz.cuni.mff.d3s.been.benchmarkapi.ContextBuilder;
import cz.cuni.mff.d3s.been.core.task.Property;
import cz.cuni.mff.d3s.been.core.task.Task;
import cz.cuni.mff.d3s.been.core.task.TaskContextDescriptor;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

/**
 * @author Martin Sixta
 */
final class Contexts {
	private static final String NODE_TEMPLATE = "node";
	private static final String CLIENT_TEMPLATE = "client";
	private static final String EVALUATOR_TEMPLATE = "client";
	private static final String BUILDER_TEMPLATE = "builder";

	private static final String CONTEXT_TEMPLATE_RESOURCE = "hazelcast3-template-queue.tcd.xml";

	private static final String BUILDER_TASK_NAME = "builder";
	private static final String EVALUATOR_TASK_NAME = "evaluator";
	private static final String NODE_TASK_NAME_TEMPLATE = "node%02d";
	private static final String CLIENT_TASK_NAME_TEMPLATE = "client%02d";


	static TaskContextDescriptor create(int run, String commitSha, TaskPropertyReader props) throws BenchmarkException {
		final int clientCnt = props.getInteger(CLIENT_COUNT);
		final int nodeCnt = props.getInteger(NODE_COUNT);
		final String contextName = String.format("Hazelcast benchmark [run: %d, commit: %s]", run, commitSha);


		ContextBuilder builder = ContextBuilder.createFromResource(Contexts.class, CONTEXT_TEMPLATE_RESOURCE);
		builder.setName(contextName);


		// set properties
		for (BenchmarkProperty benchmarkProperty : BenchmarkProperty.values()) {
			if (!benchmarkProperty.isContextProperty()) {
				continue;
			}

			builder.setProperty(benchmarkProperty.getPropertyName(), props.getString(benchmarkProperty));
		}

		builder.setProperty(RUN.getPropertyName(), Integer.toString(run));
		builder.setProperty(COMMIT_CURRENT.getPropertyName(), commitSha);

		builder.setSelector(BUILDER_TEMPLATE, props.getString(SELECTOR_BUILDER));
		builder.setSelector(NODE_TEMPLATE, props.getString(SELECTOR_NODE));
		builder.setSelector(CLIENT_TEMPLATE, props.getString(SELECTOR_CLIENT));


		// add builder task
		builder.addTask(BUILDER_TASK_NAME, BUILDER_TEMPLATE);

		// create appropriate number of clients
		for (int i = 1; i <= clientCnt; ++i) {
			String clientId = String.format(CLIENT_TASK_NAME_TEMPLATE, i);
			Task task = builder.addTask(clientId, CLIENT_TEMPLATE).withRunAfterTask(BUILDER_TASK_NAME);
			task.getProperties().getProperty().add(new Property().withName(CLIENT_ID.getPropertyName()).withValue(clientId));
		}

		// create appropriate number of clients
		for (int i = 1; i <= nodeCnt; ++i) {
			String nodeId = String.format(NODE_TASK_NAME_TEMPLATE, i);

			Task task = builder.addTask(nodeId, NODE_TEMPLATE).withRunAfterTask(BUILDER_TASK_NAME);
			task.getProperties().getProperty().add(new Property().withName(NODE_ID.getPropertyName()).withValue(nodeId));

		}


		return builder.build();
	}


	static TaskContextDescriptor createEvaluatorContext(TaskPropertyReader props) throws BenchmarkException {

		ContextBuilder builder = ContextBuilder.createFromResource(Contexts.class, CONTEXT_TEMPLATE_RESOURCE);
		builder.setName("Evaluating Hazelcast results");
		builder.addTask(EVALUATOR_TASK_NAME, EVALUATOR_TEMPLATE);

		builder.setSelector(EVALUATOR_TEMPLATE, props.getString(SELECTOR_EVALUATOR));

		return builder.build();
	}


}
