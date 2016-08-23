package hudson.plugins.ws_cleanup;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dvrzalik
 */
public class WsCleanup extends Notifier implements MatrixAggregatable, SimpleBuildStep {

    public static final String LOG_PREFIX = "[WS-CLEANUP] ";

    private List<Pattern> patterns = Collections.emptyList();
    private boolean deleteDirs = false;

    @Deprecated
    private boolean skipWhenFailed = false; // keep it for backward compatibility
    private boolean cleanWhenSuccess = true;
    private boolean cleanWhenUnstable = true;
    private boolean cleanWhenFailure = true;
    private boolean cleanWhenNotBuilt = true;
    private boolean cleanWhenAborted = true;

    private boolean notFailBuild = false;
    private boolean cleanupMatrixParent = false;
    private String externalDelete = StringUtils.EMPTY;

    @DataBoundConstructor
    public WsCleanup() {}

    @DataBoundSetter
    // FIXME can't get repeteable to work with a List<String>
    public void setPatterns(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    @DataBoundSetter
    public void setDeleteDirs(boolean deleteDirs) {
        this.deleteDirs = deleteDirs;
    }

    @Deprecated
    @DataBoundSetter
    public void setSkipWhenFailed(boolean skipWhenFailed) {
        this.skipWhenFailed = skipWhenFailed;
    }

    @DataBoundSetter
    public void setCleanWhenSuccess(boolean cleanWhenSuccess) {
        this.cleanWhenSuccess = cleanWhenSuccess;
    }

    @DataBoundSetter
    public void setCleanWhenUnstable(boolean cleanWhenUnstable) {
        this.cleanWhenUnstable = cleanWhenUnstable;
    }

    @DataBoundSetter
    public void setCleanWhenFailure(boolean cleanWhenFailure) {
        this.cleanWhenFailure = cleanWhenFailure;
    }

    @DataBoundSetter
    public void setCleanWhenNotBuilt(boolean cleanWhenNotBuilt) {
        this.cleanWhenNotBuilt = cleanWhenNotBuilt;
    }

    @DataBoundSetter
    public void setCleanWhenAborted(boolean cleanWhenAborted) {
        this.cleanWhenAborted = cleanWhenAborted;
    }

    @DataBoundSetter
    public void setNotFailBuild(boolean notFailBuild) {
        this.notFailBuild = notFailBuild;
    }

    @DataBoundSetter
    public void setCleanupMatrixParent(boolean cleanupMatrixParent) {
        this.cleanupMatrixParent = cleanupMatrixParent;
    }

    @DataBoundSetter
    public void setExternalDelete(String externalDelete) {
        this.externalDelete = externalDelete;
    }

    public Object readResolve(){
        // backward compatibility issues, see JENKINS-17930 and JENKINS-17940
        // if workspace cleanup is turn on, but for all results it's turned off, it doesn't make sense,
        // so assuming we hit backward compatibility issue and set all to true, so ws gets cleanup after every build
        if(cleanWhenSuccess == false && 
           cleanWhenUnstable == false && 
           cleanWhenFailure == false && 
           cleanWhenNotBuilt == false &&
           cleanWhenAborted == false
        ) {
            cleanWhenSuccess = true;
            cleanWhenUnstable = true;
            cleanWhenFailure = true;
            cleanWhenNotBuilt = true;
            cleanWhenAborted = true;
        }
        
        if(skipWhenFailed) { // convert deprecated option to choice per result
            skipWhenFailed = false; // set to false, so that we will skip this in the future 
            cleanWhenSuccess = true;
            cleanWhenUnstable = true;
            cleanWhenFailure = false;
            cleanWhenNotBuilt = false;
            cleanWhenAborted = false;
        }
        
        return this;
    }
    
    public List<Pattern> getPatterns(){
		return patterns;
	}
    
    public boolean getDeleteDirs(){
    	return deleteDirs;
    }

    public boolean getNotFailBuild() {
    	return notFailBuild;
    }

    @Deprecated
    public boolean isSkipWhenFailed() {
        return skipWhenFailed;
    }

    public boolean isCleanWhenSuccess() {
		return cleanWhenSuccess;
	}

	public boolean isCleanWhenUnstable() {
		return cleanWhenUnstable;
	}

	public boolean isCleanWhenFailure() {
		return cleanWhenFailure;
	}

	public boolean isCleanWhenNotBuilt() {
		return cleanWhenNotBuilt;
	}

	public boolean isCleanWhenAborted() {
		return cleanWhenAborted;
	}

	public boolean getCleanupMatrixParent() {
    	return cleanupMatrixParent;
    }
        
    public String getExternalDelete() {
        return this.externalDelete;
    }

    private boolean shouldCleanBuildBasedOnState(@CheckForNull Result result) {
        if (result == null) {
            // in case of Pipeline, the result may be null
            return true;
        }
        if(result.equals(Result.SUCCESS))
            return this.cleanWhenSuccess;
        if(result.equals(Result.UNSTABLE))
            return this.cleanWhenUnstable;
        if(result.equals(Result.FAILURE))
            return this.cleanWhenFailure;
        if(result.equals(Result.NOT_BUILT))
            return this.cleanWhenNotBuilt;
        if(result.equals(Result.ABORTED))
            return this.cleanWhenAborted;

        return true;
    }
        
	@Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        try {
            if (workspace == null || !workspace.exists())
                return;
            listener.getLogger().append(WsCleanup.LOG_PREFIX + "Deleting project workspace...");
            if(!shouldCleanBuildBasedOnState(build.getResult())) {
                listener.getLogger().println(WsCleanup.LOG_PREFIX + "Skipped based on build state " + build.getResult());
                return;
            }
            RemoteCleaner cleaner = RemoteCleaner.get(patterns, deleteDirs, externalDelete, listener, build);
            cleaner.perform(workspace);
            listener.getLogger().println(WsCleanup.LOG_PREFIX + "done");
        } catch (Exception ex) {
            Logger.getLogger(WsCleanup.class.getName()).log(Level.SEVERE, null, ex);
            if(notFailBuild) {
            	listener.getLogger().append("Cannot delete workspace: " + ex.getCause() + "\n");
            	listener.getLogger().append("Option not to fail the build is turned on, so let's continue\n");
            	return;
            }
            listener.getLogger().append("Cannot delete workspace :" + ex.getMessage() + "\n");
            throw new AbortException("Cannot delete workspace: " + ex.getMessage());
        }
    }

	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		if(cleanupMatrixParent)
			return new WsCleanupMatrixAggregator(build, launcher, listener, patterns, deleteDirs, cleanWhenSuccess, cleanWhenUnstable, cleanWhenFailure,
                cleanWhenNotBuilt, cleanWhenAborted, notFailBuild, this.externalDelete);
		return null;
	}
	
    public BuildStepMonitor getRequiredMonitorService(){
    	return BuildStepMonitor.NONE;
    }
    
    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }
    
    //TODO remove if https://github.com/jenkinsci/jenkins/pull/834 is accepted
    public boolean isMatrixProject(Object o) {
        return o instanceof MatrixProject;
    }
    
    @Extension(ordinal=-9999)
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(WsCleanup.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.WsCleanup_Delete_workspace();
        }

        @Override
        public boolean isApplicable(Class clazz) {
            return true;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }


}
