/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.ws_cleanup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.plugins.ws_cleanup.Pattern.PatternType;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lucinka
 */
public class PreBuildCleanupTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testCleanAllWorkspace() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandIncludeDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), true, null,
                Functions.isWindows() ? "cmd /c rd /s /q %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandInNodePropertyIncludeDirectories() throws Exception {
        Assume.assumeFalse(
                "TODO Windows ACI agents do not have enough memory to run this test",
                Functions.isWindows());

        FreeStyleProject project = j.createFreeStyleProject("project1");
        Slave slave = j.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new Entry("delete",
                Functions.isWindows() ? "cmd /c rd /s /q %s" : "rm -rf %s")));
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), true, null, "delete", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandWithoutDirectory() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), false, null,
                Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandInNodePropertyWithoutDirectory() throws Exception {
        Assume.assumeFalse(
                "TODO Windows ACI agents do not have enough memory to run this test",
                Functions.isWindows());

        FreeStyleProject project = j.createFreeStyleProject("project1");
        Slave slave = j.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new Entry("delete",
                Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s")));
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<>(), false, null, "delete", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandWithPattern() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("delete-me/file*.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, true, null,
                Functions.isWindows() ? "cmd /c del %s" : "rm -rf %s", false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace.getAbsolutePath() + "/delete-me/file1.txt").exists());
        assertFalse("File file2.txt in workspace should exists.", new File(workspace.getAbsolutePath() + "/delete-me/file2.txt").exists());
        assertTrue("File exclude1.txt in workspace should exists.", new File(workspace.getAbsolutePath() + "/delete-me/exclude1.txt").exists());
        assertTrue("File exclude2.txt in workspace should exists.", new File(workspace.getAbsolutePath() + "/delete-me/exclude2.txt").exists());
        assertTrue("File file1.txt in workspace should exists.", new File(workspace,"file1.txt").exists());
        assertTrue("File file2.txt in workspace should exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File exclude1.txt in workspace should exists.", new File(workspace,"exclude1.txt").exists());
        assertTrue("File exclude2.txt in workspace should exists.", new File(workspace,"exclude2.txt").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByPatternIncludeWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("file1.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertTrue("File file2.txt in workspace should exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByPatternExcludeWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("file1.txt", PatternType.EXCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertTrue("File file1.txt in workspace should exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByPatternWithDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, true, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should exists.", new File(workspace,"file2.txt").exists());
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }

    @Test
    @LocalData
    public void testCleanWorkspaceByPatternWithoutDirectories() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project1");
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null, false);
        project.getBuildWrappersList().add(cleanup);
        j.buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File delete-me in workspace should exists.", new File(workspace,"delete-me").exists());
    }

    @Test
    @LocalData
    public void disableDeferredWipeout() throws Exception {
        File workspace = new File(j.jenkins.getRootDir().getAbsolutePath(),"workspace");

        // Deferred wipeout enabled
        FreeStyleProject project = j.createFreeStyleProject("project1");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,
                null, true));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", build);

        // Deferred wipeout disabled
        project = j.createFreeStyleProject("project2");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,
                null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is used...", build);

        // Deferred wipeout default setting
        project = j.createFreeStyleProject("project3");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is used...", build);

        // Attach a DisableDeferredWipeout node property to the master node
        j.jenkins.getComputer("").getNode().getNodeProperties().add(new DisableDeferredWipeoutNodeProperty());

        // Deferred wipeout enabled
        project = j.createFreeStyleProject("project4");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,
                        null, true));
        build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is disabled by the job configuration...", build);

        // Deferred wipeout disabled
        project = j.createFreeStyleProject("project5");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,
                        null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is disabled by the node property...", build);

        // Deferred wipeout default setting
        project = j.createFreeStyleProject("project6");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        project.getBuildWrappersList().add(
                new PreBuildCleanup(new ArrayList<>(), false, null,null, false));
        build = j.buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
        j.assertLogContains("Deferred wipeout is disabled by the node property...", build);
    }
}
