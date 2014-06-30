/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.ws_cleanup;

import hudson.plugins.ws_cleanup.Pattern.PatternType;
import java.util.List;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.model.Slave;
import java.io.File;
import java.util.ArrayList;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lucinka
 */
public class PreBuildCleanupTest extends HudsonTestCase{
    
    @Test
    @LocalData
    public void testCleanAllWorkspace() throws Exception{
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<Pattern>(), false, null, null);
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertEquals("Workspace should not contains any file.", 0, workspace.listFiles().length);
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandIncludeDirectories() throws Exception{
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<Pattern>(), true, null, "rm -rf %s");
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandInNodePropertyIncludeDirectories() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        Slave slave = this.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new Entry("delete", "rm -rf %s")));
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<Pattern>(), true, null, "delete");
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandWithoutDirectory() throws Exception{
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<Pattern>(), false, null, "rm -rf %s");
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandInNodePropertyWithoutDirectory() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        Slave slave = this.createOnlineSlave();
        project.setAssignedLabel(slave.getSelfLabel());
        slave.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new Entry("delete", "rm -rf %s")));
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        PreBuildCleanup cleanup = new PreBuildCleanup(new ArrayList<Pattern>(), false, null, "delete");
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByDeleteCommandWithPattern()throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("delete-me/file*.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, true, null, "rm -rf %s");
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
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
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("file1.txt", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null);
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertTrue("File file2.txt in workspace should exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }
    
    @LocalData
    public void testCleanWorkspaceByPatternExcludeWithoutDirectories() throws Exception{
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("file1.txt", PatternType.EXCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null);
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertTrue("File file1.txt in workspace should exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File not-delete-me in workspace should exists.", new File(workspace,"not-delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByPatternWithDirectories() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, true, null, null);
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should exists.", new File(workspace,"file2.txt").exists());
        assertFalse("File delete-me in workspace should not exists.", new File(workspace,"delete-me").exists());
    }
    
    @Test
    @LocalData
    public void testCleanWorkspaceByPatternWithoutDirectories() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        File workspace = new File(jenkins.getRootDir().getAbsolutePath(),"workspace");
        project.setCustomWorkspace(workspace.getAbsolutePath());
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("*", PatternType.INCLUDE));
        PreBuildCleanup cleanup = new PreBuildCleanup(patterns, false, null, null);
        project.getBuildWrappersList().add(cleanup);
        buildAndAssertSuccess(project);
        assertFalse("File file1.txt in workspace should not exists.", new File(workspace,"file1.txt").exists());
        assertFalse("File file2.txt in workspace should not exists.", new File(workspace,"file2.txt").exists());
        assertTrue("File delete-me in workspace should exists.", new File(workspace,"delete-me").exists());
    }
}
