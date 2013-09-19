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


/**
 * The Hazelcast3 benchmark task - main entry.
 * <p/>
 * <p/>
 * Drives the benchmark.
 */
public final class HazelcastBenchmark extends Benchmark {

	/**
	 * Logging
	 */
	private static final Logger log = LoggerFactory.getLogger(HazelcastBenchmark.class);

	/**
	 * Map of commits to benchmark.
	 */
	private HashMap<Integer, RepositoryCommit> commitMap;

	/**
	 * The task's property reader.
	 */
	private final TaskPropertyReader props = new TaskPropertyReader(createPropertyReader());


	/**
	 * Current run preserved among runs of the benchmark.
	 */
	private static final String STORAGE_CURRENT_RUN = "current.run";

	/**
	 * Evaluator run - to keep track when to run evaluator.
	 */
	private static final String STORAGE_EVALUATOR_RUN = "evaluator.run";

	@Override
	public TaskContextDescriptor generateTaskContext() throws BenchmarkException {

		final int currentRun = storageGetInt(STORAGE_CURRENT_RUN, 0);

		log.debug("Current run is {}", currentRun);

		// Check whether to run the evaluator task
		if (isEvaluatorRun(currentRun)) {
			log.debug("Will run evaluator");

			// save state
			storageSet(STORAGE_EVALUATOR_RUN, Integer.toString(currentRun));
			return Contexts.createEvaluatorContext(props);
		} else {
			log.debug("Will not run evaluator");
		}


		// Figure out which commit to benchmark
		final RepositoryCommit commit;

		try {
			commit = getCommit(currentRun);

			if (commit == null) {
				log.debug("No more commits");
				return null;
			}

		} catch (IOException e) {
			log.error("Cannot fetch commit list", e);
			return null;
		}


		// Create the context of the benchmark run
		TaskContextDescriptor contextDescriptor = Contexts.create(currentRun, commit.getSha(), props);

		// save state
		storageSet(STORAGE_CURRENT_RUN, Integer.toString(currentRun + 1));
		storageSet("current.commit.sha", commit.getSha());

		log.debug("Generated context for commit {}", commit.getSha());


		// submit
		return contextDescriptor;

	}


	@Override
	public void onResubmit() {
		log.warn("onResubmit not implemented");
	}

	@Override
	public void onTaskContextFinished(String s, TaskContextState taskContextState) {
		log.debug("onTaskContextFinished not implemented");
	}

	/**
	 * Checks whether to run an evaluator in this iteration.
	 *
	 * @param currentRun current run
	 * @return true if an evaluator should be run, false otherwise
	 */
	private boolean isEvaluatorRun(int currentRun) {
		final int runAfter = props.getInteger(EVALUATOR_RUN_AFTER);
		int lastEvaluatorRun = storageGetInt(STORAGE_EVALUATOR_RUN, 0);

		if (lastEvaluatorRun == currentRun) {
			return false;
		}

		// run as the last context
		if (runAfter <= 0) {
			int numberOfRuns = props.getInteger(COMMIT_COUNT);

			return (currentRun == numberOfRuns);
		}

		return ((currentRun > 0) && (currentRun % runAfter == 0));

	}


	/**
	 * Returns commit for the current run
	 *
	 * @param run the current run of the benchmark
	 * @return commit for the current run
	 * @throws IOException when a commit cannot be obtained
	 */
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

	/**
	 * Builds list of commits to benchmark
	 *
	 * @return run to commit map
	 * @throws IOException when the map cannot be created
	 */
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

	/**
	 * Builds commits from GitHub.
	 *
	 * @return run to commit map
	 * @throws IOException when the map cannot be created
	 */
	private HashMap<Integer, RepositoryCommit> fetchFromGitHub() throws IOException {
		HashMap<Integer, RepositoryCommit> map = new HashMap<>();
		int numberOfRuns = props.getInteger(COMMIT_COUNT);
		log.debug("Looking for last {} commits on github", numberOfRuns);

		final String oAuth2Token = props.getString(GITHUB_OAUTH_TOKEN);
		final String branch = props.getString(GITHUB_BRANCH);
		final String gitHubOwner = props.getString(GITHUB_OWNER);
		final String gitHubRepo = props.getString(GITHUB_REPOSITORY);

		final List<RepositoryCommit> commits = getCommitsFromGitHub(oAuth2Token, gitHubOwner, gitHubRepo, branch);

		for (RepositoryCommit commit : commits) {
			if (numberOfRuns <= 0) {
				break;
			}

			map.put(--numberOfRuns, commit);
		}

		return map;
	}

	/**
	 * Builds commits from a url.
	 *
	 * @param url where to fetch the list from
	 * @return run to commit map
	 * @throws IOException when the map cannot be created
	 */
	private HashMap<Integer, RepositoryCommit> fetchFromUrl(String url) throws IOException {

		int numberOfRuns = props.getInteger(COMMIT_COUNT);
		String inputLine;
		HashMap<Integer, RepositoryCommit> map = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {

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

	/**
	 * Fetches list of commits form GitHub repository.
	 *
	 * @param auth
	 * @param owner
	 * @param name
	 * @param branch
	 * @return list of commits
	 * @throws IOException
	 */
	private List<RepositoryCommit> getCommitsFromGitHub(final String auth, final String owner, final String name,
														final String branch) throws IOException {
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

		String storageValue = storageGet(key, Integer.toString(defaultValue));

		return Integer.parseInt(storageValue);

	}

}
