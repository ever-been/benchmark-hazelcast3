package cz.cuni.mff.d3s.been.benchmark.hazelcast3.common;

import com.hazelcast.core.HazelcastInstance;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.FileResult;
import cz.cuni.mff.d3s.been.core.persistence.EntityID;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.persistence.Query;
import cz.cuni.mff.d3s.been.persistence.QueryBuilder;
import cz.cuni.mff.d3s.been.persistence.ResultQueryBuilder;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.NoSuchElementException;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.ENTITY_GROUP;
import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.ENTITY_KIND;

/**
 * @author Martin Sixta
 */
public abstract class SkeletalTask extends Task {

	private static final Logger log = LoggerFactory.getLogger(SkeletalTask.class);


	protected final TaskPropertyReader props;
	protected final String GROUP;


	public SkeletalTask() {
		props = new TaskPropertyReader(createPropertyReader());
		GROUP = props.getString(ENTITY_GROUP);
	}


	final protected Path retrieveJar(String name) throws TaskException {

		Query query = new ResultQueryBuilder().on(GROUP).with("contextId", getContextId()).with("name", name).fetch();

		try {
			final Collection<FileResult> fileResults = results.query(query, FileResult.class);


			// take the first one (should be just one)
			final FileResult fileResult = fileResults.iterator().next();


			log.debug("Received file result {} has md5Hex {}", fileResult.getName(), fileResult.getMd5Hex());
			checkFileContent(fileResult);

			final Path jarPath = Paths.get("files", fileResult.getName());


			Files.write(jarPath, fileResult.getContent());

			return jarPath;


		} catch (NoSuchElementException e) {
			throw new TaskException("Query did not return any results!");
		} catch (DAOException e) {
			throw new TaskException("Error while retrieving a jar file :(", e);
		} catch (IOException e) {
			throw new TaskException("Cannot write result to a file", e);
		}
	}

	private void checkFileContent(final FileResult result) throws TaskException {
		String receivedMd5 = DigestUtils.md5Hex(result.getContent());


		if (!receivedMd5.equals(result.getMd5Hex())) {
			String msg = String.format("Checksum does not match! expected='%s', actual='%s'", result.getMd5Hex(), receivedMd5);
			throw new TaskException(msg);
		}

	}


	protected final void loadJar(final Path file) throws TaskException {
		try {
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
			method.setAccessible(true);
			method.invoke(ClassLoader.getSystemClassLoader(), file.toUri().toURL());
		} catch (Exception e) {
			throw new TaskException(String.format("Cannot load %s", file.toString()), e);
		}
	}

	protected final void closeHazelcastInstance(HazelcastInstance instance) {
		if (instance == null) {
			return;
		}

		try {
			instance.getLifecycleService().terminate();
		} catch (Exception e) {
			System.err.printf("Cannot properly shutdown Hazelcast instance\n");
		}
	}


}
