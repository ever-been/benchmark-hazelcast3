package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.TaskPropertyReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

/**
 * @author Martin Sixta
 */
public class Results {

	public static ClientResult createClientResult(final TaskPropertyReader props, long time, long sent) {
		String clientId = props.getString(CLIENT_ID);
		String commit = props.getString(COMMIT_CURRENT);
		int run = props.getInteger(RUN);


		return new ClientResult(clientId, commit, run, time, sent);
	}

	public static NodeResult createNodeResult(final TaskPropertyReader props, long time, long received) {
		String nodeId = props.getString(NODE_ID);
		String commit = props.getString(COMMIT_CURRENT);
		int run = props.getInteger(RUN);

		return new NodeResult(nodeId, commit, run, time, received);
	}

	public static FileResult createFromFile(Path pathToFile) throws IOException {
		byte[] tmp = Files.readAllBytes(pathToFile);
		return new FileResult(pathToFile.getFileName().toString(), tmp);
	}
}
