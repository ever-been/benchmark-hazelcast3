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


import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.TaskPropertyReader;
import cz.cuni.mff.d3s.been.benchmarkapi.Benchmark;
import cz.cuni.mff.d3s.been.benchmarkapi.BenchmarkException;
import cz.cuni.mff.d3s.been.core.task.TaskContextDescriptor;
import cz.cuni.mff.d3s.been.core.task.TaskContextState;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

public class HazelcastBenchmark extends Benchmark {

	private static final Logger log = LoggerFactory.getLogger(HazelcastBenchmark.class);

	private HashMap<Integer, RepositoryCommit> commitMap;

	private final TaskPropertyReader props = new TaskPropertyReader(createPropertyReader());


	private static final String STORAGE_CURRENT_RUN = "current.run";
	private static final String STORAGE_EVALUATOR_RUN = "evaluator.run";

	@Override
	public TaskContextDescriptor generateTaskContext() throws BenchmarkException {

		final int currentRun = storageGetInt(STORAGE_CURRENT_RUN, 0);


		final int runAfter = props.getInteger(EVALUATOR_RUN_AFTER);

		boolean runEvaluator = (currentRun > 0) && (currentRun % runAfter == 0);
		if (runEvaluator) {
			int lastEvaluatorRun = storageGetInt(STORAGE_EVALUATOR_RUN, 0);

			if (lastEvaluatorRun != currentRun) {
				return generateEvaluatorContext(currentRun);
			}
		}

		final RepositoryCommit commit;

		try {
			commit = getCommit(currentRun);

			if (commit == null) {
				log.info("No more commits");
				return null;
			}

		} catch (IOException e) {
			log.error("Cannot fetch commit list", e);
			return null;
		}


		TaskContextDescriptor contextDescriptor = Contexts.create(currentRun, commit.getSha(), props);


		storageSet(STORAGE_CURRENT_RUN, Integer.toString(currentRun + 1));
		storageSet("current.commit.sha", commit.getSha());

		log.debug("Generated context for commit {}", commit.getSha());

		return contextDescriptor;


	}

	private TaskContextDescriptor generateEvaluatorContext(int run) throws BenchmarkException {
		storageSet(STORAGE_EVALUATOR_RUN, Integer.toString(run));
		return Contexts.createEvaluatorContext(props);
	}

	@Override
	public void onResubmit() {
		// nothing
	}

	@Override
	public void onTaskContextFinished(String s, TaskContextState taskContextState) {
		// nothing
	}

	private RepositoryCommit getCommit(int run) throws IOException {
		if (commitMap == null) {
			commitMap = buildCommits();
		}

		if (commitMap.containsKey(run)) {
			return commitMap.get(run);
		} else {
			return null;
		}

	}

	private HashMap<Integer, RepositoryCommit> buildCommits() throws IOException {

		String urlList = props.getString(COMMIT_LIST_URL);

		if (urlList != null && !urlList.isEmpty()) {
			log.debug("Using {} as a source of commits", urlList);
			return fetchFromUrl(urlList);
		} else {
			log.debug("Using GitHub as a source of commits");
			return fetchFromGitHub();

		}

	}

	private HashMap<Integer, RepositoryCommit> fetchFromGitHub() throws IOException {
		HashMap<Integer, RepositoryCommit> map = new HashMap<>();
		int numberOfRuns = props.getInteger(COMMIT_COUNT);
		log.debug("Looking for last {} commits on github", numberOfRuns);


		final String oAuth2Token = props.getString(GITHUB_OAUTH_TOKEN);
		final String branch = props.getString(GITHUB_BRANCH);
		final String gitHubOwner = props.getString(GITHUB_OWNER);
		final String gitHubRepo = props.getString(GITHUB_REPOSITORY);

		final List<RepositoryCommit> commits = getCommits(oAuth2Token, gitHubOwner, gitHubRepo, branch);


		for (RepositoryCommit commit : commits) {
			if (numberOfRuns <= 0) {
				break;
			}

			map.put(--numberOfRuns, commit);
		}

		return map;
	}


	private HashMap<Integer, RepositoryCommit> fetchFromUrl(String url) throws IOException {

		int numberOfRuns = props.getInteger(COMMIT_COUNT);
		String inputLine;
		HashMap<Integer, RepositoryCommit> map = new HashMap<>();

		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(new URL(url).openStream()))) {

			while ((inputLine = in.readLine()) != null) {

				if (numberOfRuns <= 0) {
					break;
				}

				String line = inputLine.trim();

				if (line.isEmpty()) {
					continue;
				}

				map.put(--numberOfRuns, new RepositoryCommit().setSha(line));
			}
		}

		return map;

	}


	private List<RepositoryCommit> getCommits(final String auth, final String owner, final String name, final String branch) throws IOException {
		GitHubClient client = new GitHubClient();

		if (auth != null && !auth.isEmpty()) {
			client.setOAuth2Token(auth);
		} else {
			log.warn("OAuth2Token not set");
		}

		CommitService cs = new CommitService(client);

		RepositoryService rs = new RepositoryService(client);
		final Repository repository = rs.getRepository(owner, name);

		return cs.getCommits(repository, branch, null);

	}


	private int storageGetInt(final String key, final int defaultValue) {

		String storageValue = storageGet(STORAGE_CURRENT_RUN, Integer.toString(defaultValue));

		return Integer.parseInt(storageValue);


	}


}
