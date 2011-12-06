package hudson.plugins.ws_cleanup;

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
			BuildListener listener) {
		listener.getLogger().append("\nDeleting project workspace... ");
		FilePath ws = build.getWorkspace();
		if (ws != null) {
			try {
				if (ws == null || !ws.exists())
		            return;
                if (patterns == null || patterns.isEmpty()) {
				    ws.deleteContents();
                } else {
                    build.getWorkspace().act(new Cleanup(patterns,deleteDirs));
                }

				listener.getLogger().append("done\n\n");
			} catch (IOException  e) {
				Logger.getLogger(PreBuildCleanup.class.getName()).log(Level.SEVERE, null, e);
				e.printStackTrace();
			}catch(InterruptedException e){
				Logger.getLogger(PreBuildCleanup.class.getName()).log(Level.SEVERE, null, e);
				e.printStackTrace();
			}
		}
	}

	@Extension(ordinal=99999)
	public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

		public String getDisplayName() {
			return Messages.PreBuildCleanup_Delete_workspace();
		}

	}

	class NoopEnv extends Environment{
	}
	 

}
