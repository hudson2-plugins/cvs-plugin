/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.cvs;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.google.common.collect.Lists.newArrayList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.easymock.PowerMock.*;
import static org.powermock.api.easymock.PowerMock.verify;

/**
 * @author Anton Kozak
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CVSSCM.class, AbstractBuild.class, BuildListener.class, FilePath.class})
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

    /**
     * Tests that SCM performs checkout after failed updating.
     *
     * @throws Exception if any.
     */
    @Test
    public void testCheckoutIfUpdateFailed() throws Exception {
        //Prepare data for testing.
        CVSSCM scm = createPartialMock(CVSSCM.class,
            new String[]{"isUpdatable", "update", "cleanCheckout", "archiveWorkspace", "calcChangeLog"},
            CVSROOT, MODULES, BRANCH, null, true, false, false, null);
        AbstractBuild build = createMock(AbstractBuild.class);
        expect(build.getBuildVariables()).andReturn(new HashMap<String, String>());
        expect(build.getTimestamp()).andReturn(Calendar.getInstance()).times(2);
        expect(build.getActions()).andReturn(new ArrayList<Action>());
        BuildListener listener = new StreamBuildListener(System.out, Charset.defaultCharset());
        Launcher launcher = new Launcher.LocalLauncher(listener);
        expect(scm.isUpdatable(EasyMock.<ModuleLocation>anyObject(), EasyMock.<FilePath>anyObject())).andReturn(null)
            .once();
        //Expect that updating failed.
        expect(scm.update(EasyMock.<ModuleLocation>anyObject(), EasyMock.anyBoolean(), EasyMock.<Launcher>anyObject(),
            EasyMock.<FilePath>anyObject(), EasyMock.<TaskListener>anyObject(), EasyMock.<Date>anyObject())).andReturn(
            null).once();
        //Expect that checkout performs if update fails.
        expectPrivate(scm, "cleanCheckout", EasyMock.<ModuleLocation>anyObject(), EasyMock.<Launcher>anyObject(),
            EasyMock.<FilePath>anyObject(), EasyMock.<TaskListener>anyObject(), EasyMock.<Date>anyObject()).andReturn(
            true).once();
        expectPrivate(scm, "archiveWorkspace", EasyMock.<AbstractBuild>anyObject(), EasyMock.<FilePath>anyObject());
        expectPrivate(scm, "calcChangeLog", EasyMock.<AbstractBuild>anyObject(), EasyMock.<FilePath>anyObject(),
            EasyMock.<List<String>>anyObject(), EasyMock.<File>anyObject(),
            EasyMock.<BuildListener>anyObject()).andReturn(true);

        replay(scm, build);
        scm.checkout(build, launcher, null, listener, null);
        verify(scm, build);
    }

    @Test
    public void testCleanCheckout() throws Exception {
        CVSSCM scm = new CVSSCM(CVSROOT, MODULES, BRANCH, null, true, false, false, null);
        FilePath path = Mockito.mock(FilePath.class);
        doReturn(path).when(path).child(".");
        doThrow(new IOException()).when(path).deleteContents();
        assertFalse(scm.cleanCheckout(scm.getModuleLocations()[0], null, path, null, null));
        doThrow(new InterruptedException()).when(path).deleteContents();
        assertFalse(scm.cleanCheckout(scm.getModuleLocations()[0], null, path, null, null));
    }
}
