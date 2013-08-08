import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.List;


/**
 * @author Martin Sixta
 */
public class GitHubTest {

	public static void main(String[] args) throws IOException {

		GitHubClient client = new GitHubClient();

		if (args.length == 1) {
			client.setOAuth2Token(args[0]);

		}


		CommitService cs = new CommitService(client);

		RepositoryService rs = new RepositoryService(client);
		final Repository repository = rs.getRepository("hazelcast", "hazelcast");

		final List<RepositoryCommit> commits = cs.getCommits(repository, "3.0", null);

		System.out.println(commits.size());
		for (RepositoryCommit commit : commits) {
			System.out.println(commit.getSha());
		}


	}
}
