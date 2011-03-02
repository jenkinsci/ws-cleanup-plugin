package hudson.plugins.ws_cleanup;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author dvrzalik
 */
public class WsCleanup extends Notifier {

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        listener.getLogger().append("\nDeleting project workspace... ");
        try {
            build.getProject().getWorkspace().deleteRecursive();
            listener.getLogger().append("done\n\n");
        } catch (InterruptedException ex) {
            Logger.getLogger(WsCleanup.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService(){
    	return BuildStepMonitor.STEP;
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor {

        public DescriptorImpl() {
            super(WsCleanup.class);
        }

        @Override
        public String getDisplayName() {
            return "Delete workspace when build is done";
        }

        @Override
        public Notifier newInstance(StaplerRequest req) throws FormException {
            return new WsCleanup();
        }

        @Override
        public boolean isApplicable(Class clazz) {
            return true;
        }
    }
    

}
