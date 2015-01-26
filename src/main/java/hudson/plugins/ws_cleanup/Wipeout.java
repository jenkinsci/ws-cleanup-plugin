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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.remoting.VirtualChannel;

/**
 * Cleanup workspace wiping it out completely.
 *
 * This implementation renames workspace directory and let Jenkins to create
 * new one. Old workspace is removed asynchronously.
 *
 * @author ogondza
 */
/*package*/ final class Wipeout extends RemoteCleaner {

    private final static Wipeout INSTANCE = new Wipeout();

    /*package*/ static Wipeout getInstance() {
        return INSTANCE;
    }

    @Override
    protected void perform(FilePath workspace) throws IOException, InterruptedException {
        final FilePath deleteMe = workspace.withSuffix("_ws-cleanup_" + System.currentTimeMillis());
        workspace.renameTo(deleteMe);

        if (!deleteMe.exists()) {
            LOGGER.log(
                    Level.WARNING,
                    "Cleaning workspace synchronously. Failed to rename {0} to {1}.",
                    new Object[] { workspace.getRemote(), deleteMe.getName() }
            );
            workspace.deleteRecursive();
        }

        deleteMe.actAsync(COMMAND);
    }

    private final static Command COMMAND = new Command();
    private final static class Command implements FileCallable<Void> {
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            try {
                Util.deleteRecursive(f);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Unable to delete workspace", ex);
            }
            if (f.exists()) {
                LOGGER.log(Level.SEVERE, "Workspace not deleted successfully: " + f.getAbsolutePath());
            }
            return null;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Wipeout.class.getName());
}
