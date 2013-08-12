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

	private int nodes;
	private int clients;
	private int messages;
	private int msgSize;

	protected HazelcastResult() {
		// makes Jackson happy
	}

	protected HazelcastResult withRun(int run) {
		this.run = run;
		return this;
	}

	protected HazelcastResult withId(String id) {
		this.id = id;
		return this;
	}

	protected HazelcastResult withType(String type) {
		this.type = type;
		return this;
	}

	protected HazelcastResult withCommit(String commit) {
		this.commit = commit;
		return this;
	}

	protected HazelcastResult withTime(long time) {
		this.time = time;
		return this;
	}
	protected HazelcastResult withNodes(int nodes) {
		this.nodes = nodes;
		return this;
	}
	protected HazelcastResult withClients(int clients) {
		this.clients = clients;
		return this;
	}

	protected HazelcastResult withMessages(int messages) {
		this.messages = messages;
		return this;
	}

	protected HazelcastResult withMessageSize(int msgSize) {
		this.msgSize = msgSize;
		return this;
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

	public int getNodes() {
		return nodes;
	}

	public int getClients() {
		return clients;
	}

	public int getMessages() {
		return messages;
	}

	public int getMsgSize() {
		return msgSize;
	}
}
