package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

/**
 * @author Martin Sixta
 */
public class ClientResult extends HazelcastResult {
	private long sent;

	public ClientResult() {
		// makes Jackson happy
	}

	public ClientResult(long sent) {

		this.sent = sent;
	}

	public long getSent() {
		return sent;
	}
}
