/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ws_cleanup;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.hamcrest.Matchers;
import org.jenkinsci.plugins.resourcedisposer.AsyncResourceDisposer;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

public class CleanupTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder ws = new EnhancedTemporaryFolder();

    // "IllegalArgumentException: Illegal group reference" observed when filename contained '$';
    @Test
    public void doNotTreatFilenameAsRegexReplaceWhenUsingCustomCommand() throws Exception {
        String filename = "\\s! Dozen for $5 only!";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, filename);

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.emptyList(), false,
                null, Functions.isWindows() ? "cmd /c del %s" : "rm %s", false));
        j.buildAndAssertSuccess(p);
    }

    @Test
    @Issue("JENKINS-20056")
    public void wipeOutWholeWorkspaceBeforeBuild() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.emptyList(), false, null, null, false));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
    }

    @Test
    @Issue("JENKINS-20056")
    public void wipeOutWholeWorkspaceAfterBuild() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.getBuildersList().add(getTouchBuilder("content.txt"));

        p.getPublishersList().add(wipeoutPublisher());
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
    }

    @Test
    @Issue("JENKINS-20056")
    public void wipeOutWholeWorkspaceAfterBuildMatrix() throws Exception {
        MatrixProject p = j.jenkins.createProject(MatrixProject.class, "sut");
        p.setAxes(new AxisList(new TextAxis("name", "a b")));
        p.getBuildWrappersList().add(new MatrixWsPopulator());
        p.getPublishersList().add(wipeoutPublisher());
        MatrixBuild b = j.buildAndAssertSuccess(p);

        assertWorkspaceCleanedUp(b);

        assertWorkspaceCleanedUp(p.getItem("name=a").getLastBuild());
        assertWorkspaceCleanedUp(p.getItem("name=b").getLastBuild());
    }

    @Test
    @Issue("JENKINS-20056")
    public void workspaceShouldNotBeManipulated() throws Exception {
        int ITERATIONS = 50;

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("RAND", "")));
        p.setConcurrentBuild(true);
        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.emptyList(), false, null, null, false));
        p.getPublishersList().add(wipeoutPublisher());
        p.getBuildersList().add(
                Functions.isWindows() ?
                    new BatchFile("echo =$BUILD_NUMBER= > marker;" +
                            // Something hopefully expensive to delete
                        "cmd /x /c mkdir a\\b\\c\\d\\e\\f\\g\\h\\i\\j\\k\\l\\m\\n\\o\\p\\q\\r\\s\\t\\u\\v\\w\\x\\y\\z\\a\\b\\c\\d\\e\\f\\g\\h\\j\\k\\l\\m\\n\\o\\p\\q\\r\\s\\u\\v\\w\\x\\y\\z;" +
                        "sleep $(($BUILD_NUMBER%5));" +
                        "grep =$BUILD_NUMBER= marker") :
                    new Shell("echo =$BUILD_NUMBER= > marker;" +
                        // Something hopefully expensive to delete
                        "mkdir -p a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/a/b/c/d/e/f/g/h/j/k/l/m/n/o/p/q/r/s//u/v/w/x/y/z/;" +
                        "sleep $(($BUILD_NUMBER%5));" +
                        "grep =$BUILD_NUMBER= marker"
        ));

        List<Future<FreeStyleBuild>> futureBuilds = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < ITERATIONS; i++) {
            futureBuilds.add(p.scheduleBuild2(0, (Cause) null, new ParametersAction(
                    new StringParameterValue("RAND", Integer.toString(i))
            )));
        }

        for (Future<FreeStyleBuild> fb: futureBuilds) {
            j.assertBuildStatusSuccess(fb.get());
        }
    }

    @Test
    public void deleteWorkspaceWithNonAsciiCharacters() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.getBuildersList().add(getTouchBuilder("a¶‱ﻷ.txt"));

        p.getPublishersList().add(wipeoutPublisher());

        FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertWorkspaceCleanedUp(build);
    }

    @Test
    @Issue("JENKINS-26250")
    public void doNotFailToWipeoutWhenRenameFails() throws Exception {
        assumeTrue(!Functions.isWindows()); // In MSFT we can't disable renaming a folder without enable to delete it

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");
        p.getPublishersList().add(wipeoutPublisher());

        FilePath workspace = p.getLastBuild().getWorkspace();
        workspace.getParent().chmod(0555); // Remove write for parent dir so rename will fail

        try {
            workspace.renameTo(workspace.withSuffix("2"));
            assumeTrue("Rename operation should fail", workspace.exists());
        } catch (java.nio.file.AccessDeniedException ade) {
            // expected on Java 9 +
        }

        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(build);
    }

    @Test
    public void reportCleanupCommandFailure() throws Exception {
        String command = Functions.isWindows() ? "cmd /c md %s" : "mkdir %s";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        FilePath ws = j.buildAndAssertSuccess(p).getWorkspace();
        FilePath pre = ws.child("pre-build");
        pre.touch(0);
        FilePath post = ws.child("post-build");
        post.touch(0);

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.emptyList(), false, null, command, false));
        WsCleanup wsCleanup = new WsCleanup();
        wsCleanup.setNotFailBuild(true);
        wsCleanup.setCleanupMatrixParent(true);
        wsCleanup.setExternalDelete(command);
        p.getPublishersList().add(wsCleanup);

        FreeStyleBuild build = j.buildAndAssertSuccess(p);

        if (Functions.isWindows()) {
            j.assertLogContains("ERROR: Cleanup command 'cmd /c md " + pre.getRemote() + "' failed with code 1", build);
            j.assertLogContains("ERROR: Cleanup command 'cmd /c md " + post.getRemote() + "' failed with code 1", build);
            j.assertLogContains("A subdirectory or file " + pre.getRemote() + " already exists.", build);
            j.assertLogContains("A subdirectory or file " + post.getRemote() + " already exists.", build);
        } else {
            j.assertLogContains("ERROR: Cleanup command 'mkdir " + pre.getRemote() + "' failed with code 1", build);
            j.assertLogContains("ERROR: Cleanup command 'mkdir " + post.getRemote() + "' failed with code 1", build);
            j.assertLogContains("mkdir: cannot create directory", build);
            j.assertLogContains("File exists", build);
        }
    }

    @Test
    @Issue("JENKINS-28454")
    public void pipelineWorkspaceCleanup() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "') { \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'foobar' \n" +
                "       } finally { \n" +
                "           step([$class: 'WsCleanup']) \n" +
                "       } \n" +
                "  } \n" +
                "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", run);
        j.assertLogContains("[WS-CLEANUP] done", run);

        assertThat(ws.getRoot().listFiles(), nullValue());
    }

    @Test
    @Issue("JENKINS-28454")
    public void pipelineWorkspaceCleanupUsingPattern() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "') { \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'first file' \n" +
                "           writeFile file: 'bar.txt', text: 'second file' \n" +
                "       } finally { \n" +
                "           step([$class: 'WsCleanup', patterns: [[pattern: 'bar.*', type: 'INCLUDE']]]) \n" +
                "       } \n" +
                "   } \n" +
                "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", run);
        j.assertLogContains("[WS-CLEANUP] done", run);

        verifyFileExists("foo.txt");
    }

    @Test
    @Issue("JENKINS-28454")
    public void pipelineWorkspaceCleanupUnlessBuildFails() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "'){ \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'foobar' \n" +
                "			throw new Exception() \n" +
                "		} catch (err) { \n" +
                "			currentBuild.result = 'FAILURE' \n" +
                "       } finally { \n" +
                "			step ([$class: 'WsCleanup', cleanWhenFailure: false]) \n" +
                "       } \n" +
                "   } \n" +
                "}", true));
        WorkflowRun build = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", build);
        j.assertLogContains("[WS-CLEANUP] Skipped based on build state FAILURE", build);

        verifyFileExists("foo.txt");
    }

    @Test
    @Issue("JENKINS-37054")
    public void symbolAnnotationWorkspaceCleanup() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "') { \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'foobar' \n" +
                "       } finally { \n" +
                "           cleanWs() \n" +
                "       } \n" +
                "  } \n" +
                "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", run);
        j.assertLogContains("[WS-CLEANUP] done", run);

        assertThat(ws.getRoot().listFiles(), nullValue());
    }

    @Test
    @Issue("JENKINS-37054")
    public void symbolWorkspaceCleanupAnnotationUsingPattern() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "') { \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'first file' \n" +
                "           writeFile file: 'bar.txt', text: 'second file' \n" +
                "       } finally { \n" +
                "           cleanWs patterns: [[pattern: 'bar.*', type: 'INCLUDE']] \n" +
                "       } \n" +
                "   } \n" +
                "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", run);
        j.assertLogContains("[WS-CLEANUP] done", run);

        verifyFileExists("foo.txt");
    }

    @Test
    @Issue("JENKINS-37054")
    public void symbolAnnotationWorkspaceCleanupUnlessBuildFails() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("" +
                "node { \n" +
                "   ws ('" + ws.getRoot() + "'){ \n" +
                "       try { \n" +
                "           writeFile file: 'foo.txt', text: 'foobar' \n" +
                "           error 'error'\n" +
                "       } catch (err) { \n" +
                "           currentBuild.result = 'FAILURE' \n" +
                "       } finally { \n" +
                "           cleanWs cleanWhenFailure: false \n" +
                "       } \n" +
                "   } \n" +
                "}", true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, run);
        j.assertLogContains("[WS-CLEANUP] Deleting project workspace...", run);
        j.assertLogContains("[WS-CLEANUP] Skipped based on build state FAILURE", run);

        verifyFileExists("foo.txt");
    }

    @Test
    public void doNotRunExternalCommandWhenNull() throws Exception {
        String command = null;

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");

        p.getBuildWrappersList().add(
                new PreBuildCleanup(
                        Collections.emptyList(),
                        false,
                        null,
                        command,
                        false));

        FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertWorkspaceCleanedUp(build);
    }

    @Test
    public void doNotRunExternalCommandWhenWhitespace() throws Exception {
        String command = "  \n  ";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");

        p.getBuildWrappersList().add(
                new PreBuildCleanup(
                        Collections.emptyList(),
                        false,
                        null,
                        command,
                        false));

        FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertWorkspaceCleanedUp(build);
    }

    @Test
    public void retryAsyncDirDeletion() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");

        p.getPublishersList().add(CleanupTest.wipeoutPublisher());

        Wipeout.INSTANCE = new Wipeout() {
            @Override void performDelete(FilePath workspace) throws IOException {
                throw new IOException("BOOM!");
            }
        };

        j.buildAndAssertSuccess(p);

        Thread.sleep(100);

        AsyncResourceDisposer disposer = AsyncResourceDisposer.get();
        Set<AsyncResourceDisposer.WorkItem> backlog = disposer.getBacklog();
        assertThat(backlog, Matchers.iterableWithSize(1));
        AsyncResourceDisposer.WorkItem entry = backlog.iterator().next();
        assertThat(entry.getDisposable().getDisplayName(), startsWith("Workspace master:"));

        assertEquals("BOOM!", entry.getLastState().getDisplayName());

        Wipeout.INSTANCE = new Wipeout();

        //noinspection deprecation
        disposer.reschedule();

        Thread.sleep(100);

        assertThat(disposer.getBacklog(), emptyCollectionOf(AsyncResourceDisposer.WorkItem.class));
    }

    @Test
    public void deferredWipeoutAfterBuild() throws Exception {
        // Deferred wipeout enabled
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut1");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        WsCleanup wsCleanup = new WsCleanup();
        wsCleanup.setDisableDeferredWipeout(true);
        p.getPublishersList().add(wsCleanup);
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", b);

        // Deferred wipeout disabled
         p = j.jenkins.createProject(FreeStyleProject.class, "sut2");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        wsCleanup = new WsCleanup();
        wsCleanup.setDisableDeferredWipeout(false);
        p.getPublishersList().add(wsCleanup);
        b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is used...", b);

        // Deferred wipeout default setting
        p = j.jenkins.createProject(FreeStyleProject.class, "sut3");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        wsCleanup = new WsCleanup();
        p.getPublishersList().add(wsCleanup);
        b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is used...", b);

        // Attach a DisableDeferredWipeout node property to the master node
        j.jenkins.getComputer("").getNode().getNodeProperties().add(new DisableDeferredWipeoutNodeProperty());

        // Deferred wipeout enabled
        p = j.jenkins.createProject(FreeStyleProject.class, "sut4");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        wsCleanup = new WsCleanup();
        wsCleanup.setDisableDeferredWipeout(true);
        p.getPublishersList().add(wsCleanup);
        b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", b);

        // Deferred wipeout disabled
        p = j.jenkins.createProject(FreeStyleProject.class, "sut5");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        wsCleanup = new WsCleanup();
        wsCleanup.setDisableDeferredWipeout(false);
        p.getPublishersList().add(wsCleanup);
        b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is disabled by the node property...", b);

        // Deferred wipeout default setting
        p = j.jenkins.createProject(FreeStyleProject.class, "sut6");
        p.getBuildersList().add(getTouchBuilder("content.txt"));
        wsCleanup = new WsCleanup();
        p.getPublishersList().add(wsCleanup);
        b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
        j.assertLogContains("Deferred wipeout is disabled by the node property...", b);
    }

    private void verifyFileExists(String fileName) {
        File[] files = ws.getRoot().listFiles();
        assertThat(files, notNullValue());
        assertThat(files, arrayWithSize(1));
        assertThat(files[0].getName(), is(fileName));
    }

    public static WsCleanup wipeoutPublisher() {
        WsCleanup wsCleanup = new WsCleanup();
        wsCleanup.setNotFailBuild(true);
        wsCleanup.setCleanupMatrixParent(true);

        return wsCleanup;
    }

    private TestBuilder getTouchBuilder(String filename) {
        return new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                                   BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child(filename).touch(0);
                return true;
            }
        };
    }

    private void populateWorkspace(FreeStyleProject p, String filename) throws Exception {
        p.getBuildersList().add(getTouchBuilder(filename));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        p.getBuildersList().clear();
        assertFalse("Workspace populated", b.getWorkspace().list().isEmpty());
    }

    private void assertWorkspaceCleanedUp(AbstractBuild<?, ?> b) throws Exception {
        FilePath workspace = b.getWorkspace();
        if (workspace == null) return; // removed

        List<FilePath> files = workspace.list();
        if (files == null) return; // removed

        assertTrue("Workspace contains: " + files, files.isEmpty());
    }

    /**
     * Create content in workspace of both master and child builds.
     *
     * @author ogondza
     */
    private static final class MatrixWsPopulator extends BuildWrapper {
        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            build.getWorkspace().child("content.txt").touch(0);
            if (build instanceof MatrixRun) {
                MatrixBuild mb = ((MatrixRun) build).getParentBuild();
                mb.getWorkspace().child("content.txt").touch(0);
            }

            return new Environment() {};
        }

        @Override
        public Descriptor getDescriptor() {
            return new Descriptor();
        }

        private static final class Descriptor extends hudson.model.Descriptor<BuildWrapper> {
            @Override
            public String getDisplayName() {
                return "Matrix workspace populator";
            }
        }
    }

    private static final class EnhancedTemporaryFolder extends TemporaryFolder {
        @Override
        public EnhancedFile getRoot() {
            return new EnhancedFile(super.getRoot());
        }

        private static final class EnhancedFile extends File {
            public EnhancedFile(File f) {
                super(f.getPath());
            }

            @Override
            public String toString() {
                String org = super.getPath();
                return Functions.isWindows() ? org.replaceAll("\\\\", "/") : org;
            }
        }
    }
}
