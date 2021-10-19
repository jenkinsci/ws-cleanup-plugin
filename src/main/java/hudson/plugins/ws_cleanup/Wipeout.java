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

import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.FilePath;
import hudson.model.Computer;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.resourcedisposer.AsyncResourceDisposer;
import org.jenkinsci.plugins.resourcedisposer.Disposable;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cleanup workspace wiping it out completely.
 *
 * This implementation renames workspace directory and let Jenkins to create
 * new one. Old workspace is removed asynchronously.
 *
 * @author ogondza
 */
/*package final*/ class Wipeout extends RemoteCleaner {

    /*private final*/ static Wipeout INSTANCE = new Wipeout();

    /*package*/ static Wipeout getInstance() {
        return INSTANCE;
    }

    @Override
    protected void perform(FilePath workspace) throws IOException, InterruptedException {
        FilePath deleteMe = workspace.withSuffix("_ws-cleanup_" + System.currentTimeMillis());
        Computer computer =  workspace.toComputer();
        if (computer == null) {
            performDelete(workspace);
            return;
        }

        String errmsg = "Cleaning workspace synchronously. Failed to rename " + workspace.getRemote() + " to " + deleteMe.getName() + ".";
        try {
            workspace.renameTo(deleteMe);
        } catch (FileSystemException expected) {
            errmsg += " " + expected;
        }

        if (deleteMe.exists()) {
            AsyncResourceDisposer.get().dispose(new DisposableImpl(deleteMe, computer.getName()));
            return;
        }

        LOGGER.log(Level.WARNING, errmsg);
        performDelete(workspace);
    }

    /*package for testing*/ void performDelete(FilePath workspace) throws IOException, InterruptedException {
        workspace.deleteRecursive();
    }

    private static final class DisposableImpl implements Disposable {
        private static final long serialVersionUID = 1L;

        // TODO node can get renamed which should be reflected here
        private final String node;
        private final String path;

        private DisposableImpl(FilePath ws, String computer) {
            this.node = computer;
            this.path = ws.getRemote();
        }

        @NonNull public State dispose() throws Throwable {
            Jenkins j = Jenkins.getInstanceOrNull();
            if (j == null) return State.TO_DISPOSE; // Going down?

            // We grab the computer and file path here each time.  Caching the file path is
            // dangerous because a FilePath contains a Channel, implying
            // the Channel is still around. In cases where you have a disconnect/reconnect,
            // machine that was altogether discarded, etc. this method would simply throw
            // and retry over and over again.  In certain cloud heavy installations,
            // this might mean we leak Channel objects for discarded machines
            // over time, eventually leading to an OOM.
            Computer computer = j.getComputer(node);
            if (computer == null) return State.PURGED;
            
            FilePath ws = new FilePath(computer.getChannel(), path);
            
            try {
                Wipeout.INSTANCE.performDelete(ws); // Use instance method for easy mocking of the behaviour
            } catch (IOException ex) {
                Throwable cause = ex.getCause();
                if (cause != null && ex.getMessage().startsWith("remote file operation failed:")) {
                    throw cause;
                }
                throw ex;
            }

            return ws.exists()
                ? State.TO_DISPOSE // Failed to delete silently
                : State.PURGED
            ;
        }

        @NonNull public String getDisplayName() {
            return "Workspace " + (node.isEmpty() ? "master" : node) + ':' + path;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Wipeout.class.getName());
}
