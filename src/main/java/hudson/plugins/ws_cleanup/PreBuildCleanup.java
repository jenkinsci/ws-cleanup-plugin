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

	private final List<Pattern> patterns;
	private final boolean deleteDirs;

	@DataBoundConstructor
	public PreBuildCleanup(List<Pattern> patterns, boolean deleteDirs) {
		this.patterns = patterns;
		this.deleteDirs = deleteDirs;
	}

	public List<Pattern> getPatterns(){
		return patterns;
	}
	
	public boolean getDeleteDirs(){
		return deleteDirs;
	}
	
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	
	 @Override 
	 public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException{ 
		 return new NoopEnv();
	 }

	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws AbortException {
		listener.getLogger().append("\nDeleting project workspace... \n");
		FilePath ws = build.getWorkspace();
		if (ws != null) {
			try {
				// Retry the operation a couple of times,
				int retry = 3;
				while (true) {
					try {
						if (ws == null || !ws.exists())
							return;
						if (patterns == null || patterns.isEmpty()) {
							ws.deleteRecursive();
						} else {
							build.getWorkspace().act(new Cleanup(patterns,deleteDirs));
						}

						listener.getLogger().append("done\n\n");
						break;
					} catch (IOException e) {
						retry -= 1;
						if (retry > 0) {
							// Swallow the I/O exception and retry in a few seconds.
							Thread.sleep(3000);
						} else {
							listener.getLogger().append("Cannot delete workspace: " + e.getCause() + "\n");
							Logger.getLogger(PreBuildCleanup.class.getName()).log(Level.SEVERE, null, e);
							e.printStackTrace();
							throw new AbortException("Cannot delete workspace: " + e.getCause());
						}
					}
				}
			} catch(InterruptedException e) {
				Logger.getLogger(PreBuildCleanup.class.getName()).log(Level.SEVERE, null, e);
				e.printStackTrace();
			}
		}
	}

	@Extension(ordinal=9999)
	public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

		public String getDisplayName() {
			return Messages.PreBuildCleanup_Delete_workspace();
		}

	}

	class NoopEnv extends Environment{
	}
	 

}
