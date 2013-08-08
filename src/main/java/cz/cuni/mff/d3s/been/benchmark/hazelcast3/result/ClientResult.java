package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

/**
 * @author Martin Sixta
 */
public class ClientResult extends HazelcastResult {
	private long sent;

	public ClientResult() {
		// makes Jackson happy
	}

	public ClientResult(String id, String commit, int run, long time, long sent) {
		super("CLIENT", commit, run, id, time);
		this.sent = sent;
	}

	public long getSent() {
		return sent;
	}
}
