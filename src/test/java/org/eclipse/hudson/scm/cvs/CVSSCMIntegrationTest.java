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

import hudson.model.FreeStyleProject;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class CVSSCMIntegrationTest extends HudsonTestCase {

    //TODO remove this test and enable others when refactored cvs plugin version will be bundled into hudson
    public void testFake(){
        assertTrue(true);
    }

    /**
     * Verifies that there's no data loss.
     */
    public void ignore_testConfigRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        // verify values
        CVSSCM scm1 = new CVSSCM("cvsroot", "module", "branch", "cvsRsh", true, true, true, "excludedRegions");
        p.setScm(scm1);
        roundtrip(p);
        assertEquals(scm1, (CVSSCM)p.getScm());

        // all boolean fields need to be tried with two values
        scm1 = new CVSSCM("x", "y", "z", "w", false, false, false, "t");
        p.setScm(scm1);

        roundtrip(p);
        assertEquals(scm1, (CVSSCM)p.getScm());
    }

    @Bug(4456)
    public void ignore_testGlobalConfigRoundtrip() throws Exception {
        CVSSCM.DescriptorImpl d = hudson.getDescriptorByType(CVSSCM.DescriptorImpl.class);
        d.setCvspassFile("a");
        d.setCvsExe("b");

        submit(createWebClient().goTo("configure").getFormByName("config"));
        assertEquals("a", d.getCvspassFile());
        assertEquals("b",d.getCvsExe());
    }

    @Email("https://hudson.dev.java.net/servlets/BrowseList?list=users&by=thread&from=2222483")
    @Bug(4760)
    public void  ignore_testProjectExport() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        CVSSCM scm = new CVSSCM(":pserver:nowhere.net/cvs/foo", ".", null, null, true, true, false, null);
        p.setScm(scm);
        Field repositoryBrowser = scm.getClass().getDeclaredField("repositoryBrowser");
        repositoryBrowser.setAccessible(true);
        repositoryBrowser.set(scm, new org.eclipse.hudson.scm.cvs.browsers.ViewCVS(new URL("http://nowhere.net/viewcvs/")));
        new WebClient().goTo(p.getUrl()+"api/xml", "application/xml");
        new WebClient().goTo(p.getUrl() + "api/xml?depth=999", "application/xml");
    }

    private void roundtrip(FreeStyleProject p) throws Exception {
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
    }

    private void assertEquals(CVSSCM scm1, CVSSCM scm2) {
        assertTrue(Arrays.equals(scm1.getModuleLocations(), scm2.getModuleLocations()));
    }
}
