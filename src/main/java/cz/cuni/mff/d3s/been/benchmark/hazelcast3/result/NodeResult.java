package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

/**
 * @author Martin Sixta
 */
public class NodeResult extends HazelcastResult {
	private long received;

	public NodeResult(String id, String commit, int run, long time, long received) {
		super("NODE", commit, run, id, time);
		this.received = received;
	}

	public long getReceived() {
		return received;
	}


	public NodeResult() {
		// makes Jackson happy
	}
}
