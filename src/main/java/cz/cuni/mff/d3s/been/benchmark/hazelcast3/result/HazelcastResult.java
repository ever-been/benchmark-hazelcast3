package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

import cz.cuni.mff.d3s.been.results.Result;

/**
 * @author Martin Sixta
 */
public class HazelcastResult extends Result {
	private int run;
	private String id;
	private String type;
	private String commit;
	private long time;

	public HazelcastResult() {
		// makes Jackson happy
	}

	public HazelcastResult(String type, String commit, int run, String id, long time) {
		this.type = type;
		this.commit = commit;
		this.run = run;
		this.id = id;
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public String getCommit() {
		return commit;
	}

	public String getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	public int getRun() {
		return run;
	}
}
