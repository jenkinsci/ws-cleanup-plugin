package hudson.plugins.ws_cleanup;

import hudson.Plugin;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrappers;

/**
 * @plugin
 * 
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() {
        BuildStep.PUBLISHERS.addNotifier(WsCleanup.DESCRIPTOR);
    }
}
