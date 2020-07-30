package hudson.plugins.ws_cleanup;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author vjuranek
 */
public class PreBuildCleanup extends SimpleBuildWrapper {

	private static final Logger LOGGER = Logger.getLogger(PreBuildCleanup.class.getName());

	private final List<Pattern> patterns;
	private final boolean deleteDirs;
	private final String cleanupParameter;
	private final String externalDelete;
	private final boolean disableDeferredWipeout;

	@Deprecated
	public PreBuildCleanup(List<Pattern> patterns, boolean deleteDirs, String cleanupParameter, String externalDelete) {
		this(patterns, deleteDirs, cleanupParameter, externalDelete, false);
	}

	@DataBoundConstructor
    public PreBuildCleanup(List<Pattern> patterns, boolean deleteDirs, String cleanupParameter,
                           String externalDelete, boolean disableDeferredWipeout) {
        this.patterns = patterns;
        this.deleteDirs = deleteDirs;
        this.cleanupParameter = cleanupParameter;
        this.externalDelete = externalDelete;
        this.disableDeferredWipeout = disableDeferredWipeout;
    }

	public List<Pattern> getPatterns(){
		return patterns;
	}
	
	public boolean getDeleteDirs(){
		return deleteDirs;
	}

	public String getCleanupParameter() {
		return this.cleanupParameter;
	}
	
	public String getExternalDelete() {
            return this.externalDelete;
        }

    public boolean getDisableDeferredWipeout() {
	    return this.disableDeferredWipeout;
    }
        
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	protected boolean runPreCheckout() {
		return true;
	}

	@Override
	public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
		// Check if a cleanupParameter has been setup and skip cleaning workspace if set to false
		if (cleanupParameter != null && !cleanupParameter.isEmpty()) {
			boolean doCleanup = Boolean.parseBoolean(initialEnvironment.get(this.cleanupParameter));
			if (!doCleanup) {
				listener.getLogger().println(WsCleanup.LOG_PREFIX + "Clean-up disabled, skipping workspace deletion.");
				return;
			}
		}

		if (workspace != null) {
		    listener.getLogger().println(WsCleanup.LOG_PREFIX + "Deleting project workspace...");
		    RemoteCleaner cleaner = RemoteCleaner.get(patterns, deleteDirs, externalDelete, listener,
					build, disableDeferredWipeout);
			try {
				// Retry the operation a couple of times,
				int retry = 3;
				while (true) {
					try {
						if (!workspace.exists()) {
							return;
						}

						cleaner.perform(workspace);
						listener.getLogger().println(WsCleanup.LOG_PREFIX + "Done");
						break;
					} catch (IOException e) {
						retry -= 1;
						if (retry > 0) {
							// Swallow the I/O exception and retry in a few seconds.
							Thread.sleep(3000);
						} else {
							listener.error(WsCleanup.LOG_PREFIX + "Cannot delete workspace: " + e.getMessage());
							LOGGER.log(Level.SEVERE, "Cannot delete workspace", e);
							throw new AbortException("Cannot delete workspace: " + e.getMessage());
						}
					}
				}
			} catch (InterruptedException e) {
				LOGGER.log(Level.SEVERE, "Cleanup interrupted", e);
				Thread.currentThread().interrupt();
			}
		}
	}

	@Extension(ordinal=9999)
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return Messages.PreBuildCleanup_Delete_workspace();
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}
	}

	class NoopEnv extends Environment {}
}
