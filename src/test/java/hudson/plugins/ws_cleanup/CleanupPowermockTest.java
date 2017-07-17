/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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
import hudson.Util;
import hudson.model.FreeStyleProject;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.resourcedisposer.AsyncResourceDisposer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*"}) // http://stackoverflow.com/questions/12914814/java-security-class-cast-exception
@PrepareForTest(FilePath.class)
public class CleanupPowermockTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void retryAsyncDirDeletion() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "sut");

        p.getPublishersList().add(CleanupTest.wipeoutPublisher());

        Wipeout.INSTANCE = spy(Wipeout.INSTANCE);

        final Answer[] answer = new Answer[1];
        PowerMockito.doAnswer(new Answer<Void>() { // Plug actual answers dynamically
            @Override public Void answer(InvocationOnMock invocation) throws Throwable {
                answer[0].answer(invocation);
                return null;
            }
        }).when(Wipeout.INSTANCE).performDelete(any(FilePath.class));

        answer[0] = new Answer<Void>() { // Throw exception
            @Override public Void answer(InvocationOnMock invocation) throws Throwable {
                throw new IOException("BOOM!");
            }
        };

        System.out.println(j.buildAndAssertSuccess(p).getLog());

        Thread.sleep(100);

        AsyncResourceDisposer disposer = AsyncResourceDisposer.get();
        Set<AsyncResourceDisposer.WorkItem> backlog = disposer.getBacklog();
        assertThat(backlog, Matchers.<AsyncResourceDisposer.WorkItem>iterableWithSize(1));
        AsyncResourceDisposer.WorkItem entry = backlog.iterator().next();
        assertThat(entry.getDisposable().getDisplayName(), startsWith("Workspace master:"));

        assertEquals("BOOM!", entry.getLastState().getDisplayName());

        answer[0] = new Answer<Void>() { // Correct deletion
            @Override public Void answer(InvocationOnMock invocation) throws Throwable {
                ((FilePath) invocation.getArguments()[0]).deleteRecursive();
                return null;
            }
        };

        //noinspection deprecation
        disposer.reschedule();

        Thread.sleep(100);

        assertThat(disposer.getBacklog(), emptyCollectionOf(AsyncResourceDisposer.WorkItem.class));
    }
}
