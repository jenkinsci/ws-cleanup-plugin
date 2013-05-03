package hudson.plugins.ws_cleanup;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author dvrzalik
 */
public class WsCleanup extends Notifier implements MatrixAggregatable {

    private final List<Pattern> patterns;
    private final boolean deleteDirs;

    private final boolean cleanWhenSuccess;
    private final boolean cleanWhenUnstable;
    private final boolean cleanWhenFailure;
    private final boolean cleanWhenNotBuilt;
    private final boolean cleanWhenAborted;

    private final boolean notFailBuild;
    private final boolean cleanupMatrixParent;

    @DataBoundConstructor
    // FIXME can't get repeteable to work with a List<String>
    public WsCleanup(List<Pattern> patterns, boolean deleteDirs, final boolean cleanWhenSuccess, final boolean cleanWhenUnstable, final boolean cleanWhenFailure,
                     final boolean cleanWhenNotBuilt, final boolean cleanWhenAborted, final boolean notFailBuild, final boolean cleanupMatrixParent) {
        this.patterns = patterns;
        this.deleteDirs = deleteDirs;
        this.notFailBuild = notFailBuild;
        this.cleanupMatrixParent = cleanupMatrixParent;
        this.cleanWhenSuccess = cleanWhenSuccess;
        this.cleanWhenUnstable = cleanWhenUnstable;
        this.cleanWhenFailure = cleanWhenFailure;
        this.cleanWhenNotBuilt = cleanWhenNotBuilt;
        this.cleanWhenAborted = cleanWhenAborted;
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
    
    public boolean getCleanupMatrixParent() {
    	return cleanupMatrixParent;
    }

    private boolean shouldCleanBuildBasedOnState(Result result) {
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().append("\nDeleting project workspace... \n");
        FilePath workspace = build.getWorkspace();
        try {
        	if (workspace == null || !workspace.exists()) 
                return true;
        	if(!shouldCleanBuildBasedOnState(build.getResult())) {
        		listener.getLogger().append("Skipped based on build state " + build.getResult() + "\n\n");
        		return true;
        	}
            if (patterns == null || patterns.isEmpty()) {
                workspace.deleteRecursive();
            } else {
                workspace.act(new Cleanup(patterns,deleteDirs));
            }
            listener.getLogger().append("done\n\n");
        } catch (Exception ex) {
            Logger.getLogger(WsCleanup.class.getName()).log(Level.SEVERE, null, ex);
            if(notFailBuild) {
            	listener.getLogger().append("Cannot delete workspace: " + ex.getCause() + "\n");
            	listener.getLogger().append("Option not to fail the build is turned on, so let's continue\n");
            	return true;
            }
            return false;
        }
        return true;
    }

	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		if(cleanupMatrixParent)
			return new WsCleanupMatrixAggregator(build, launcher, listener, patterns, deleteDirs, cleanWhenSuccess, cleanWhenUnstable, cleanWhenFailure,
                cleanWhenNotBuilt, cleanWhenAborted, notFailBuild);
		return null;
	}
	
    public BuildStepMonitor getRequiredMonitorService(){
    	return BuildStepMonitor.STEP;
    }
    
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
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
