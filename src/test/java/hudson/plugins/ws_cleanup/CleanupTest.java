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

import static org.junit.Assert.assertTrue;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CleanupTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    // "IllegalArgumentException: Illegal group reference" observed when filename contained '$';
    @Test
    public void doNotTreatFilenameAsRegexReplaceWhenUsingCustomCommand() throws Exception {
        final String filename = "\\s! Dozen for 5$ only!";

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");
        p.getBuildersList().add(new Shell("touch '" + filename + "'"));
        j.buildAndAssertSuccess(p); // Populate workspace

        p.getBuildWrappersList().add(new PreBuildCleanup(Collections.<Pattern>emptyList(), false, null, "rm %s"));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);

        final String log = b.getLog();
        assertTrue(log, log.contains(
                "Using command: rm " + b.getWorkspace().getRemote() + "/" + filename
        ));
    }
}
