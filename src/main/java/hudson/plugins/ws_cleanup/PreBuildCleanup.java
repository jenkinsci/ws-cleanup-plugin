package hudson.plugins.ws_cleanup;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
/**
 * 
 * @author vjuranek
 *
 */
public class PreBuildCleanup extends BuildWrapper {

	private static final Logger LOGGER = Logger.getLogger(PreBuildCleanup.class.getName());

	private final List<Pattern> patterns;
	private final boolean deleteDirs;
	private final String cleanupParameter;
        private final String externalDelete;

	@DataBoundConstructor
	public PreBuildCleanup(List<Pattern> patterns, boolean deleteDirs, String cleanupParameter, String externalDelete) {
		this.patterns = patterns;
		this.deleteDirs = deleteDirs;
		this.cleanupParameter = cleanupParameter;
                this.externalDelete = externalDelete;
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
        
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new NoopEnv();
	}

	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws AbortException {

		// Check if a cleanupParameter has been setup and skip cleaning workspace if set to false
		if (cleanupParameter != null && !cleanupParameter.isEmpty()) {
			Boolean doCleanup = Boolean.valueOf((String) build.getBuildVariables().get(this.cleanupParameter));
			if (!doCleanup) {
				listener.getLogger().println(WsCleanup.LOG_PREFIX + "Clean-up disabled, skipping workspace deletion.");
				return;
			}
		}

		FilePath ws = build.getWorkspace();
               
		if (ws != null) {
		    listener.getLogger().println(WsCleanup.LOG_PREFIX + "Deleting project workspace...");
		    RemoteCleaner cleaner = RemoteCleaner.get(patterns, deleteDirs, externalDelete, listener, build);
			try {
				// Retry the operation a couple of times,
				int retry = 3;
				while (true) {
					try {
						if (!ws.exists()) {
							return;
						}

						cleaner.perform(ws);
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
			} catch(InterruptedException e) {
				LOGGER.log(Level.SEVERE, "Cleanup interrupted", e);
			}
		}
	}

	@Extension(ordinal=9999)
	public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

		@Override
		public String getDisplayName() {
			return Messages.PreBuildCleanup_Delete_workspace();
		}

	}

	class NoopEnv extends Environment{
	}
}
