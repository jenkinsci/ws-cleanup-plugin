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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
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
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class CleanupTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    // "IllegalArgumentException: Illegal group reference" observed when filename contained '$';
    @Test
    public void doNotTreatFilenameAsRegexReplaceWhenUsingCustomCommand() throws Exception {
        final String filename = "\\s! Dozen for $5 only!";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, filename);

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.<Pattern>emptyList(), false, null, "rm %s"));
        j.buildAndAssertSuccess(p);
    }

    @Test @Issue("JENKINS-20056")
    public void wipeOutWholeWorkspaceBeforeBuild() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.<Pattern>emptyList(), false, null, null));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
    }

    @Test @Issue("JENKINS-20056")
    public void wipeOutWholeWorkspaceAfterBuild() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.getBuildersList().add(new Shell("touch content.txt"));

        p.getPublishersList().add(wipeoutPublisher());
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(b);
    }

    @Test @Issue("JENKINS-20056")
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

    @Test @Issue("JENKINS-20056")
    public void workspaceShouldNotBeManipulated() throws Exception {
        final int ITERATIONS = 50;

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("RAND", "")));
        p.setConcurrentBuild(true);
        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.<Pattern>emptyList(), false, null, null));
        p.getPublishersList().add(wipeoutPublisher());
        p.getBuildersList().add(new Shell(
                "echo =$BUILD_NUMBER= > marker;" +
                // Something hopefully expensive to delete
                "mkdir -p a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/a/b/c/d/e/f/g/h/j/k/l/m/n/o/p/q/r/s//u/v/w/x/y/z/" +
                "sleep $(($BUILD_NUMBER%5));" +
                "grep =$BUILD_NUMBER= marker"
        ));

        final List<Future<FreeStyleBuild>> futureBuilds = new ArrayList<Future<FreeStyleBuild>>(ITERATIONS);

        for (int i = 0; i < ITERATIONS; i++) {
            futureBuilds.add(p.scheduleBuild2(0, null, new ParametersAction(
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
        p.getBuildersList().add(new Shell("touch a¶‱ﻷ.txt"));

        p.getPublishersList().add(wipeoutPublisher());

        FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertWorkspaceCleanedUp(build);
    }

    @Test @Issue("JENKINS-26250")
    public void doNotFailToWipeoutWhenRenameFails() throws Exception {
        assumeTrue(!Functions.isWindows()); // chmod does not work here

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        populateWorkspace(p, "content.txt");
        p.getPublishersList().add(wipeoutPublisher());

        FilePath workspace = p.getLastBuild().getWorkspace();
        workspace.getParent().chmod(0555); // Remove write for parent dir so rename will fail

        workspace.renameTo(workspace.withSuffix("2"));
        assertTrue("Rename operation should fail", workspace.exists());

        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        assertWorkspaceCleanedUp(build);
    }

    @Test
    public void reportCleanupCommandFailure() throws Exception {
        String command = "mkdir %s";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        FilePath ws = j.buildAndAssertSuccess(p).getWorkspace();
        FilePath pre = ws.child("pre-build");
        pre.touch(0);
        FilePath post = ws.child("post-build");
        post.touch(0);

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.<Pattern>emptyList(), false, null, command));
        p.getPublishersList().add(new WsCleanup(
                Collections.<Pattern>emptyList(), false, true, true, true, true, true, true, true, command
        ));

        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        String log = build.getLog();

        assertThat(log, containsString("ERROR: Cleanup command 'mkdir " + pre.getRemote() + "' failed with code 1"));
        assertThat(log, containsString("ERROR: Cleanup command 'mkdir " + post.getRemote() + "' failed with code 1"));
        assertThat(log, containsString("mkdir: cannot create directory"));
        assertThat(log, containsString("File exists"));
    }

    private WsCleanup wipeoutPublisher() {
        return new WsCleanup(Collections.<Pattern>emptyList(), false,
                true, true, true, true, true, true, true, // run always
        null);
    }

    private void populateWorkspace(FreeStyleProject p, String filename) throws Exception {
        p.getBuildersList().add(new Shell("touch '" + filename + "'"));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        p.getBuildersList().clear();
        assertFalse("Workspace populated", b.getWorkspace().list().isEmpty());
    }

    private void assertWorkspaceCleanedUp(AbstractBuild<?, ?> b) throws Exception {
        final FilePath workspace = b.getWorkspace();
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
}
