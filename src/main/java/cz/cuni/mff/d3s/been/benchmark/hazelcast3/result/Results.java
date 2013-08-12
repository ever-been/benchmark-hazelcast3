package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.TaskPropertyReader;

/**
 * @author Martin Sixta
 */
public class Results {

	public static ClientResult createClientResult(final TaskPropertyReader props, long time, long sent) {
		String clientId = props.getString(CLIENT_ID);

		ClientResult result = new ClientResult(sent);

		withProps(props, result).withTime(time).withId(clientId).withType("CLIENT");

		return result;
	}

	public static NodeResult createNodeResult(final TaskPropertyReader props, long time, long received) {
		String nodeId = props.getString(NODE_ID);

		NodeResult result = new NodeResult(received);
		withProps(props, result).withTime(time).withId(nodeId).withType("NODE");

		return result;
	}

	public static FileResult createFromFile(Path pathToFile) throws IOException {
		byte[] tmp = Files.readAllBytes(pathToFile);
		return new FileResult(pathToFile.getFileName().toString(), tmp);
	}

	private static HazelcastResult withProps(final TaskPropertyReader props, final HazelcastResult result) {
		String commit = props.getString(COMMIT_CURRENT);
		int run = props.getInteger(RUN);
		int nodes = props.getInteger(NODE_COUNT);
		int clients = props.getInteger(CLIENT_COUNT);
		int messages = props.getInteger(HAZELCAST_CLIENT_MSG_COUNT);
		int msgSize = props.getInteger(HAZELCAST_MSG_SIZE);

		return result.withCommit(commit).withRun(run).withClients(clients).withNodes(nodes).withMessages(messages).withMessageSize(
				msgSize);

	}
}
