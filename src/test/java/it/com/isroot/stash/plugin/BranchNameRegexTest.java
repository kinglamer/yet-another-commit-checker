package it.com.isroot.stash.plugin;

import com.google.common.collect.ImmutableMap;
import it.com.isroot.stash.plugin.util.YaccRule;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sean Ford
 * @since 2017-08-20
 */
public class BranchNameRegexTest {
    @Rule
    public YaccRule gitRepoRule = new YaccRule();

    @Test
    public void testYaccDisabled_newBranchesAllowed() throws Exception {
        Git git = gitRepoRule.getGitRepo().getGit();

        git.branchCreate()
                .setName("mybranch")
                .call();

        gitRepoRule.getGitRepo().push("mybranch");
    }

    @Test
    public void testYaccEnabled_blocksBranchCreationIfNameIsInvalid() throws Exception {
        Git git = gitRepoRule.getGitRepo().getGit();

        gitRepoRule.enableYaccRepoHook();
        gitRepoRule.configureYaccRepoHook(ImmutableMap
                .of("branchNameRegex", "master|feature/.*"));

        git.branchCreate()
                .setName("invalid-name")
                .call();

        PushResult pushResult = gitRepoRule.getGitRepo().push("invalid-name");

        for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
            assertThat(update.getStatus())
                    .isEqualTo(RemoteRefUpdate.Status.REJECTED_OTHER_REASON);
        }

        assertThat(pushResult.getMessages())
                .contains("refs/heads/invalid-name: Invalid branch name. 'invalid-name' " +
                        "does not match regex 'master|feature/.*'");
    }

    @Test
    public void testYaccEnabled_allowsPushIfBranchNameIsValid() throws Exception {
        Git git = gitRepoRule.getGitRepo().getGit();

        gitRepoRule.enableYaccRepoHook();
        gitRepoRule.configureYaccRepoHook(ImmutableMap
                .of("branchNameRegex", "master|feature/.*"));

        git.branchCreate()
                .setName("feature/correct-branch-name")
                .call();

        PushResult pushResult = gitRepoRule.getGitRepo().push("feature/correct-branch-name");

        for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
            assertThat(update.getStatus())
                    .isEqualTo(RemoteRefUpdate.Status.OK);
        }

        assertThat(pushResult.getMessages())
                .contains("Create pull request");
    }

    @Test
    public void testYaccEnabled_alreadyExistingBranchesAreCanStillBePushedTo() throws Exception {
        Git git = gitRepoRule.getGitRepo().getGit();

        gitRepoRule.configureYaccRepoHook(ImmutableMap
                .of("branchNameRegex", "master|feature/.*"));

        git.branchCreate()
                .setName("invalid-name")
                .call();

        PushResult pushResult = gitRepoRule.getGitRepo().push("invalid-name");

        for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
            assertThat(update.getStatus())
                    .isEqualTo(RemoteRefUpdate.Status.OK);
        }

        gitRepoRule.enableYaccRepoHook();

        gitRepoRule.getGitRepo().commitFile("newfile", "commit will be allowed");
        gitRepoRule.getGitRepo().push("invalid-name");

        for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
            assertThat(update.getStatus())
                    .isEqualTo(RemoteRefUpdate.Status.OK);
        }
    }
}
