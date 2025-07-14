/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.ws_cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.plugins.ws_cleanup.Pattern.PatternType;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author lucinka
 */
@WithJenkins
class PreBuildCleanupTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @LocalData
    void testCleanAllWorkspace() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByDeleteCommandIncludeDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(
                new ArrayList<>(), true, null, Functions.isWindows() ? "cmd /c rd /s /q %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "delete-me").exists(), "File delete-me in workspace should not exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByDeleteCommandInNodePropertyIncludeDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        Slave slave = j.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties()
                .add(new EnvironmentVariablesNodeProperty(
                        new Entry("delete", Functions.isWindows() ? "cmd /c rd /s /q %s" : "rm -rf %s")));
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), true, null, "delete", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "delete-me").exists(), "File delete-me in workspace should not exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByDeleteCommandWithoutDirectory() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(
                new ArrayList<>(), false, null, Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should not exists.");
        assertFalse(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should not exists.");
        assertTrue(new File(workspace, "not-delete-me").exists(), "File not-delete-me in workspace should exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByDeleteCommandInNodePropertyWithoutDirectory() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        Slave slave = j.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties()
                .add(new EnvironmentVariablesNodeProperty(
                        new Entry("delete", Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s")));
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), false, null, "delete", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should not exists.");
        assertFalse(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should not exists.");
        assertTrue(new File(workspace, "not-delete-me").exists(), "File not-delete-me in workspace should exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByDeleteCommandWithPattern() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("delete-me/file*.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup =
                new PreBuildCleanup(patterns, true, null, Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(
                new File(workspace.getAbsolutePath() + "/delete-me/file1.txt").exists(),
                "File file1.txt in workspace should not exists.");
        assertFalse(
                new File(workspace.getAbsolutePath() + "/delete-me/file2.txt").exists(),
                "File file2.txt in workspace should exists.");
        assertTrue(
                new File(workspace.getAbsolutePath() + "/delete-me/exclude1.txt").exists(),
                "File exclude1.txt in workspace should exists.");
        assertTrue(
                new File(workspace.getAbsolutePath() + "/delete-me/exclude2.txt").exists(),
                "File exclude2.txt in workspace should exists.");
        assertTrue(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should exists.");
        assertTrue(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should exists.");
        assertTrue(new File(workspace, "exclude1.txt").exists(), "File exclude1.txt in workspace should exists.");
        assertTrue(new File(workspace, "exclude2.txt").exists(), "File exclude2.txt in workspace should exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByPatternIncludeWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("file1.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should not exists.");
        assertTrue(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should exists.");
        assertTrue(new File(workspace, "not-delete-me").exists(), "File not-delete-me in workspace should exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByPatternExcludeWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("file1.txt", PatternType.EXCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertTrue(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should exists.");
        assertFalse(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should not exists.");
        assertTrue(new File(workspace, "not-delete-me").exists(), "File not-delete-me in workspace should exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByPatternWithDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, true, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should exists.");
        assertFalse(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should exists.");
        assertFalse(new File(workspace, "delete-me").exists(), "File delete-me in workspace should not exists.");
    }

    @Test
    @LocalData
    void testCleanWorkspaceByPatternWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse(new File(workspace, "file1.txt").exists(), "File file1.txt in workspace should not exists.");
        assertFalse(new File(workspace, "file2.txt").exists(), "File file2.txt in workspace should not exists.");
        assertTrue(new File(workspace, "delete-me").exists(), "File delete-me in workspace should exists.");
    }

    @Test
    @LocalData
    void disableDeferredWipeout() throws Exception {
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(), "workspace");

        // Deferred wipeout enabled
        FreeStyleProject project = j.createFreeStyleProject("project1");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, true));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", build);

        // Deferred wipeout disabled
        project = j.createFreeStyleProject("project2");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is used...", build);

        // Deferred wipeout default setting
        project = j.createFreeStyleProject("project3");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is used...", build);

        // Attach a DisableDeferredWipeout node property to the master node
        j.jenkins.getComputer("").getNode().getNodeProperties().add(new DisableDeferredWipeoutNodeProperty());

        // Deferred wipeout enabled
        project = j.createFreeStyleProject("project4");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, true));
        build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", build);

        // Deferred wipeout disabled
        project = j.createFreeStyleProject("project5");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is disabled by the node property...", build);

        // Deferred wipeout default setting
        project = j.createFreeStyleProject("project6");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(new PreBuildCleanup(new ArrayList<>(), false, null, null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals(0, workspace.listFiles().length, "Workspace should not contains any file.");
        j.assertLogContains("Deferred wipeout is disabled by the node property...", build);
    }
}
