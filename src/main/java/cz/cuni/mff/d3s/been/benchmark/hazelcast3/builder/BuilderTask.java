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
package cz.cuni.mff.d3s.been.benchmark.hazelcast3.builder;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.ProcessUtils;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.SkeletalTask;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.Results;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.results.Result;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.TaskException;

/**
 * @author Martin Sixta
 */
public class BuilderTask extends SkeletalTask {

	/**
	 * logging
	 */
	private static final Logger log = LoggerFactory.getLogger(BuilderTask.class);

	/**
	 * GitHub url template - needs owner and repository
	 */
	private static final String GITHUB_URL_TEMPLATE = "https://github.com/%s/%s.git";

	@Override
	public void run(String[] strings) throws TaskException, DAOException, MessagingException {
		final String repository = getGitHubUrl();
		final String branch = props.getString(GITHUB_BRANCH);
		final String hazelcastJar = props.getString(JAR_NODE);
		final String hazelcastClientJar = props.getString(JAR_CLIENT);

		final String commit = props.getString(COMMIT_CURRENT);

		final String gitCmdFormat = getProperty("cmd.git");
		final String mvnCmd = getProperty("cmd.mvn");
		final String hazelcastDir = getProperty("hazelcast.dir");

		log.debug("Going to build commit {}", commit);

		// --------------------------------------------------------------------
		// GIT
		// --------------------------------------------------------------------
		String gitCmd = String.format(gitCmdFormat, branch, repository);

		log.debug("Cloning with command: {}", gitCmd);

		int gitExitValue = ProcessUtils.run(gitCmd, ".", false, false);

		if (gitExitValue != 0) {
			String msg = String.format("Cannot clone branch '%s' from repository  '%s'", branch, repository);
			throw new TaskException(msg);
		}

		String gitCheckoutCmd = String.format("git checkout %s", commit);

		log.debug("Checking out with command: {}", gitCheckoutCmd);

		int gitCheckoutExitValue = ProcessUtils.run(gitCheckoutCmd, hazelcastDir, false, false);

		if (gitCheckoutExitValue != 0) {
			String msg = String.format("Cannot checkout commit '%s'", commit);
			throw new TaskException(msg);
		}

		// --------------------------------------------------------------------
		// MVN
		// --------------------------------------------------------------------
		log.info("Building with command: {}", mvnCmd);
		int mvnExitValue = ProcessUtils.run(mvnCmd, hazelcastDir, false, false);

		if (mvnExitValue != 0) {
			String msg = String.format("Error running '%s'", mvnCmd);
			throw new TaskException(msg);
		}

		// --------------------------------------------------------------------
		// RESULTS
		// --------------------------------------------------------------------
		log.info("Storing jar as result");

		Path nodeJarPath = Paths.get(hazelcastDir, "hazelcast", "target", hazelcastJar);
		Path clientJarPath = Paths.get(hazelcastDir, "hazelcast-client", "target", hazelcastClientJar);

		uploadJar(nodeJarPath);
		uploadJar(clientJarPath);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

		}

		int nodes = props.getInteger(NODE_COUNT);

		try (CheckpointController checkpoint = CheckpointController.create()) {
			checkpoint.latchSet("benchmark.node.latch", nodes);
		}

		log.debug("Builder task is finished");

	}

	private void uploadJar(Path path) throws TaskException {
		try {
			Result jarResult = Results.createFromFile(path);
			store(jarResult, KIND, GROUP);
		} catch (IOException e) {
			throw new TaskException("Cannot read built jar file", e);
		} catch (DAOException e) {
			throw new TaskException("Cannot store result", e);
		}

	}

	private String getGitHubUrl() {
		String owner = props.getString(GITHUB_OWNER);
		String repository = props.getString(GITHUB_REPOSITORY);
		return String.format(GITHUB_URL_TEMPLATE, owner, repository);

	}

}
