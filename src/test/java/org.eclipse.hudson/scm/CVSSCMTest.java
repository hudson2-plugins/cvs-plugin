/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Oracle Corporation, Kohsuke Kawaguchi, Anton Kozak
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

package org.eclipse.hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import org.eclipse.hudson.scm.CVSSCM;
import org.eclipse.hudson.scm.ModuleLocation;
import org.eclipse.hudson.scm.ModuleLocationImpl;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Kozak
 */
public class CVSSCMTest {
    private static final String CVSROOT = ":pserver:anonymous:password@10.4.0.50:/var/cvsroot";
    private static final String MODULES = "module1 module2 module\\ name";
    private static final String BRANCH = "branch";
    private static final String LOCAL_DIR = "localDir";

    @Test
    public void testGetModuleLocations() {
        CVSSCM scm = new CVSSCM(Arrays.asList(
            new ModuleLocationImpl(CVSROOT, MODULES, BRANCH, false, LOCAL_DIR)),
            null, true, false, null, false);
        //there are 3 modules
        assertFalse(scm.isFlatten());
        assertTrue(scm.isLegacy());
        assertEquals(scm.getModuleLocations().length, 1);
        assertEquals(scm.getAllModules().length, 3);
    }

    @Test
    public void testLegacyGetModuleLocations() {
        CVSSCM scm = new CVSSCM(CVSROOT, MODULES, BRANCH, null, true, false, false, null);
        //there are 3 modules
        assertFalse(scm.isFlatten());
        assertTrue(scm.isLegacy());
        assertEquals(scm.getModuleLocations().length, 1);
    }

    @Test
    public void testFlatten() {
        CVSSCM scm = new CVSSCM(CVSROOT, "module", BRANCH, null, true, false, false, null);
        //there are 1 modules
        assertTrue(scm.isFlatten());
        assertFalse(scm.isLegacy());
        assertEquals(scm.getModuleLocations().length, 1);
    }

    @Test
    public void testLegacy() {
        CVSSCM scm = new CVSSCM(CVSROOT, "module", BRANCH, null, true, true, false, null);
        //there are 1 modules, but enabled legacy mode
        assertFalse(scm.isFlatten());
        assertTrue(scm.isLegacy());
        assertEquals(scm.getModuleLocations().length, 1);
    }

    @Test
    public void testRemoveInvalidEntries() {
        CVSSCM scm = new CVSSCM(null, MODULES, BRANCH, null, true, false, false, null);
        assertEquals(scm.getModuleLocations().length, 0);
    }

    @Test
    public void testCompareRemoteRevisionWith() throws IOException, InterruptedException {
        CVSSCM scm = new CVSSCM(CVSROOT, "module", BRANCH, null, true, true, false, "src/main/web/.*\\.html"){
            @Override
            List<String> update(ModuleLocation moduleLocation, boolean dryRun, Launcher launcher, FilePath workspace,
                                TaskListener listener, Date date) throws IOException, InterruptedException {
                return newArrayList("src/main/web/pom.xml", "src/main/web/Test2.html");
            }

            @Override
            String isUpdatable(ModuleLocation location, FilePath dir) throws IOException, InterruptedException {
                return null;
            }
        };
        PollingResult result = scm.compareRemoteRevisionWith(null, null, null, null, null);
        assertEquals(result, PollingResult.BUILD_NOW);
    }

    @Test
    public void testCompareRemoteRevisionWithAllExcluded() throws IOException, InterruptedException {
        CVSSCM scm = new CVSSCM(CVSROOT, "module", BRANCH, null, true, true, false, "src/main/web/.*\\.java\nsrc/main/web/.*\\.xml"){
            @Override
            List<String> update(ModuleLocation moduleLocation, boolean dryRun, Launcher launcher, FilePath workspace,
                                TaskListener listener, Date date) throws IOException, InterruptedException {
                return newArrayList("src/main/web/pom.xml", "src/main/web/Test2.java");
            }

            @Override
            String isUpdatable(ModuleLocation location, FilePath dir) throws IOException, InterruptedException {
                return null;
            }
        };
        PollingResult result = scm.compareRemoteRevisionWith(null, null, null, null, null);
        assertEquals(result, PollingResult.NO_CHANGES);
    }


    @Test
    public void testDoCheckCvsrootWithEmptyPassword() throws IOException {
        String cvsroot = ":pserver:anonymous:@tortoisecvs.cvs.sourceforge.net:/cvsroot/tortoisecvs";

        CVSSCM.DescriptorImpl descriptor = new CVSSCM.DescriptorImpl(false);
        Matcher matcher = CVSSCM.DescriptorImpl.CVSROOT_PSERVER_PATTERN.matcher(cvsroot);
        assertTrue(descriptor.isCvsrootValid(cvsroot, matcher));
    }
    @Test
    public void testDoCheckCvsrootWithoutPassword() throws IOException {
        String cvsroot = ":pserver:anonymous@tortoisecvs.cvs.sourceforge.net:/cvsroot/tortoisecvs";

        CVSSCM.DescriptorImpl descriptor = new CVSSCM.DescriptorImpl(false);
        Matcher matcher = CVSSCM.DescriptorImpl.CVSROOT_PSERVER_PATTERN.matcher(cvsroot);
        assertTrue(descriptor.isCvsrootValid(cvsroot, matcher));

    }

    @Test
    public void testDoCheckCvsrootWithPassword() throws IOException {
        String cvsroot = ":pserver:anonymous:password@tortoisecvs.cvs.sourceforge.net:/cvsroot/tortoisecvs";

        CVSSCM.DescriptorImpl descriptor = new CVSSCM.DescriptorImpl(false);
        Matcher matcher = CVSSCM.DescriptorImpl.CVSROOT_PSERVER_PATTERN.matcher(cvsroot);
        assertTrue(descriptor.isCvsrootValid(cvsroot, matcher));

    }

    @Test
    public void testDoCheckCvsrootInvalid() throws IOException {
        String cvsroot = ":pserver@tortoisecvs.cvs.sourceforge.net:/cvsroot/tortoisecvs";

        CVSSCM.DescriptorImpl descriptor = new CVSSCM.DescriptorImpl(false);
        Matcher matcher = CVSSCM.DescriptorImpl.CVSROOT_PSERVER_PATTERN.matcher(cvsroot);
        assertFalse(descriptor.isCvsrootValid(cvsroot, matcher));
    }

}
