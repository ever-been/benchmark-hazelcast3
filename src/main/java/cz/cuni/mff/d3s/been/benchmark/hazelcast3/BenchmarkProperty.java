/**
 *
 * Copyright 2013 Martin Sixta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package cz.cuni.mff.d3s.been.benchmark.hazelcast3;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.PropertyProvider;

/**
 * @author Martin Sixta
 */
public enum BenchmarkProperty implements PropertyProvider {
	@Deprecated
	ENTITY_KIND("entity.kind", "result", true),
	ENTITY_GROUP("entity.group", "hazelcast3", true),

	JAR_NODE("hazelcast.node.jar", "hazelcast-3.0-SNAPSHOT.jar", true),
	JAR_CLIENT("hazelcast.client.jar", "hazelcast-client-3.0-SNAPSHOT.jar", true),
	HAZELCAST_GROUP("hazelcast.group", "been", true),
	HAZELCAST_PASSWORD("hazelcast.password", "been-pass", true),
	HAZELCAST_QUEUE_NAME("hazelcast.queue.name", "benchmark-queue", true),

	HAZELCAST_NODE_CONFIG("hazelcast.node.config", "", true),
	HAZELCAST_NODE_CONFIG_URL("hazelcast.node.config.url", "", true),

	HAZELCAST_CLIENT_CONFIG("hazelcast.client.config", "", true),
	HAZELCAST_MSG_SIZE("hazelcast.msg.size", 64, true),
	HAZELCAST_CLIENT_MSG_COUNT("hazelcast.client.msg.count", 10000, true),

	SELECTOR_NODE("selector.node", "", false),
	SELECTOR_CLIENT("selector.client", "", false),
	SELECTOR_BUILDER("selector.builder", "", false),
	SELECTOR_EVALUATOR("selector.evaluator", "", false),

	NODE_ID("node.id", "", false),
	CLIENT_ID("client.id", "", false),
	RUN("run", -1, false),

	GITHUB_BRANCH("github.branch", "3.0", true),
	GITHUB_OWNER("github.owner", "hazelcast", true),
	GITHUB_REPOSITORY("github.owner", "hazelcast", true),

	GITHUB_OAUTH_TOKEN("github.oauth.token", "", true),

	COMMIT_LIST_URL("git.commit.list.url", "", false),
	COMMIT_START("git.commit.start", "", false),
	COMMIT_END("git.commit.end", "", false),
	COMMIT_COUNT("git.commit.count", 10, false),
	COMMIT_CURRENT("git.commit.current", "", true),

	CLIENT_COUNT("hazelcast.client.count", 2, true),
	NODE_COUNT("hazelcast.node.count", 2, true),
	NODE_LATCH("benchmark.node.latch", 0, false),

	NODE_CHECK_MESSAGE("node.check.msg", false, false),

	EVALUATOR_BENCHMARK_ID("evaluator.benchmark.id", "", false),
	EVALUATOR_RUN_AFTER("evaluator.run.after", 10, false),
	EVALUATOR_WIDTH_SIZE("evaluator.graph.size.width", 800, true),
	EVALUATOR_HEIGHT_SIZE("evaluator.graph.size.height", 600, true),

	EXCLUSIVITY_CLIENTS("exclusivity.clients", true, true),
	EXCLUSIVITY_NODES("exclusivity.nodes", true, true),
	BENCHMARK_TYPE("benchmark.type", "QUEUE", false),
	LOG_LEVEL("task.log.level", "DEBUG", true);

	private final String propertyName;
	private final boolean isContextProperty;
	private final Object defaultValue;

	BenchmarkProperty(String propertyName, Object defaultValue, boolean isContextProperty) {
		this.propertyName = propertyName;
		this.isContextProperty = isContextProperty;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	boolean isContextProperty() {
		return isContextProperty;
	}

}
