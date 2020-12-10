/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ws_cleanup;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import java.io.IOException;
import java.util.List;

/**
 * Local abstraction to orchestrate remote cleaning.
 *
 * @author ogondza
 */
/*package*/ abstract class RemoteCleaner {

    /*package*/ static RemoteCleaner get(
            List<Pattern> patterns,
            boolean deleteDirs,
            String externalDelete,
            TaskListener listener,
            Run<?, ?> build,
            boolean disableDeferredWipeout
    ) {
        boolean wipeout = (patterns == null || patterns.isEmpty())
                && (externalDelete == null || externalDelete.trim().isEmpty())
                && !disableDeferredWipeout
        ;

        Node node = null;
        if (build instanceof AbstractBuild) {
            node = ((AbstractBuild<?, ?>) build).getBuiltOn();
        }

        if (disableDeferredWipeout) {
            listener.getLogger().append(
                    WsCleanup.LOG_PREFIX + "Deferred wipeout is disabled by the job configuration...\n");
        }

        if (wipeout && node != null) {
            wipeout = node.getNodeProperties().get(DisableDeferredWipeoutNodeProperty.class) == null;
            if (!wipeout) {
                listener.getLogger().append(
                        WsCleanup.LOG_PREFIX + "Deferred wipeout is disabled by the node property...\n");
            }
        }

        if (wipeout) {
            listener.getLogger().append(
                    WsCleanup.LOG_PREFIX + "Deferred wipeout is used...\n");
            return Wipeout.getInstance();
        }

        EnvironmentVariablesNodeProperty properties = null;
        if (node != null) {
            properties = node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        }

        return new Cleanup(patterns, deleteDirs, properties, externalDelete, listener);
    }

    /**
     * Perform the cleanup.
     *
     * Once this method completes, workspace is ready to be used.
     */
    protected abstract void perform(FilePath workspace) throws IOException, InterruptedException;
}
