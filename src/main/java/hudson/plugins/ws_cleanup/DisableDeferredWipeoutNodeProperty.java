/*
 * The MIT License
 *
 * Copyright (c) 2018 Red Hat, Inc.
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

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Node property for disabling deferred wipeout
 *
 *  @author pjanouse
 */
public class DisableDeferredWipeoutNodeProperty extends NodeProperty<Node> {

    @DataBoundConstructor
    public DisableDeferredWipeoutNodeProperty() {
    }

    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class NodePropertyDescriptorImpl extends NodePropertyDescriptor {

        public NodePropertyDescriptorImpl(){
            super(DisableDeferredWipeoutNodeProperty.class);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @NonNull
        public String getDisplayName() {
            return Messages.DisableDeferredWipeoutNodeProperty_Name();
        }
    }
}
