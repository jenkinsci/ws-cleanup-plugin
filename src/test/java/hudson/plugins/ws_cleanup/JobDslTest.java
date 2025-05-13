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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import java.io.File;
import java.util.Collections;
import java.util.List;
import javaposse.jobdsl.dsl.DslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JobDslTest {

    private static JenkinsRule j;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void emptyCleanWs() throws Exception {
        applyScript("job('emptyCleanWs') { publishers { cleanWs() } }");

        WsCleanup wsc = getJob("emptyCleanWs").getPublishersList().get(WsCleanup.class);

        assertThat(wsc.getPatterns(), emptyIterable());
        assertFalse(wsc.getDeleteDirs());
        assertEquals("", wsc.getExternalDelete());
        assertFalse(wsc.isDisableDeferredWipeout());
        assertTrue(wsc.isCleanWhenAborted());
        assertTrue(wsc.isCleanWhenFailure());
        assertTrue(wsc.isCleanWhenNotBuilt());
        assertTrue(wsc.isCleanWhenSuccess());
        assertTrue(wsc.isCleanWhenUnstable());
        assertFalse(wsc.getNotFailBuild());
    }

    @Test
    void fullCleanWs() throws Exception {
        applyScript(
                """
                job('fullCleanWs') {publishers {
                    cleanWs {
                        cleanWhenAborted(true)
                        cleanWhenFailure(true)
                        cleanWhenNotBuilt(false)
                        cleanWhenSuccess(true)
                        cleanWhenUnstable(true)
                        deleteDirs(true)
                        notFailBuild(true)
                        disableDeferredWipeout(true)
                        patterns {
                            pattern {
                                type('EXCLUDE')
                                pattern('.propsfile')
                            }
                            pattern {
                                type('INCLUDE')
                                pattern('.gitignore')
                            }
                        }
                    }
                }
                }""");

        WsCleanup wsc = getJob("fullCleanWs").getPublishersList().get(WsCleanup.class);

        assertTrue(wsc.getDeleteDirs());
        assertEquals("", wsc.getExternalDelete());
        assertTrue(wsc.isDisableDeferredWipeout());
        assertTrue(wsc.isCleanWhenAborted());
        assertTrue(wsc.isCleanWhenFailure());
        assertFalse(wsc.isCleanWhenNotBuilt());
        assertTrue(wsc.isCleanWhenSuccess());
        assertTrue(wsc.isCleanWhenUnstable());
        assertTrue(wsc.getNotFailBuild());

        List<Pattern> patterns = wsc.getPatterns();
        assertThat(patterns, iterableWithSize(2));
        Pattern ex = patterns.get(0);
        assertEquals(Pattern.PatternType.EXCLUDE, ex.getType());
        assertEquals(".propsfile", ex.getPattern());
        Pattern in = patterns.get(1);
        assertEquals(Pattern.PatternType.INCLUDE, in.getType());
        assertEquals(".gitignore", in.getPattern());
    }

    @Test
    void emptyPreBuildCleanup() throws Exception {
        applyScript("job('emptyPreBuildCleanup') { wrappers { preBuildCleanup() } }");

        PreBuildCleanup pbc =
                getJob("emptyPreBuildCleanup").getBuildWrappersList().get(PreBuildCleanup.class);
        assertEquals("", pbc.getCleanupParameter());
        assertFalse(pbc.getDeleteDirs());
        assertFalse(pbc.getDisableDeferredWipeout());
        assertEquals("", pbc.getExternalDelete());
        assertThat(pbc.getPatterns(), emptyIterable());
    }

    @Test
    void examplePreBuildCleanup() throws Exception {
        applyScript(
                """
                job('examplePreBuildCleanup') {
                    wrappers {
                        preBuildCleanup {
                            includePattern('**/target/**')
                            deleteDirectories()
                            cleanupParameter('CLEANUP')
                        }
                    }
                }""");

        PreBuildCleanup pbc =
                getJob("examplePreBuildCleanup").getBuildWrappersList().get(PreBuildCleanup.class);
        assertEquals("CLEANUP", pbc.getCleanupParameter());
        assertTrue(pbc.getDeleteDirs());
        assertFalse(pbc.getDisableDeferredWipeout());
        assertEquals("", pbc.getExternalDelete());

        List<Pattern> patterns = pbc.getPatterns();
        assertThat(patterns, iterableWithSize(1));
        Pattern ex = patterns.get(0);
        assertEquals(Pattern.PatternType.INCLUDE, ex.getType());
        assertEquals("**/target/**", ex.getPattern());
    }

    private static void applyScript(String s) throws java.io.IOException {
        JenkinsJobManagement jjm = new JenkinsJobManagement(System.out, Collections.emptyMap(), new File("."));
        new DslScriptLoader(jjm).runScript(s);
    }

    private static FreeStyleProject getJob(String emptyCleanWs) {
        return j.jenkins.getItem(emptyCleanWs, j.jenkins, FreeStyleProject.class);
    }
}
