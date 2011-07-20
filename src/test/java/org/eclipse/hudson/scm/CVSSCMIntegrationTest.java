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

    /**
     * Verifies that there's no data loss.
     */
    public void testConfigRoundtrip() throws Exception {
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
    public void testGlobalConfigRoundtrip() throws Exception {
        CVSSCM.DescriptorImpl d = hudson.getDescriptorByType(CVSSCM.DescriptorImpl.class);
        d.setCvspassFile("a");
        d.setCvsExe("b");

        submit(createWebClient().goTo("configure").getFormByName("config"));
        assertEquals("a", d.getCvspassFile());
        assertEquals("b",d.getCvsExe());
    }

    private void roundtrip(FreeStyleProject p) throws Exception {
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
    }

    private void assertEquals(CVSSCM scm1, CVSSCM scm2) {
        assertTrue(Arrays.equals(scm1.getModuleLocations(), scm2.getModuleLocations()));
    }

    @Email("https://hudson.dev.java.net/servlets/BrowseList?list=users&by=thread&from=2222483")
    @Bug(4760)
    public void testProjectExport() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        CVSSCM scm = new CVSSCM(":pserver:nowhere.net/cvs/foo", ".", null, null, true, true, false, null);
        p.setScm(scm);
        Field repositoryBrowser = scm.getClass().getDeclaredField("repositoryBrowser");
        repositoryBrowser.setAccessible(true);
        repositoryBrowser.set(scm, new org.eclipse.hudson.scm.browsers.ViewCVS(new URL("http://nowhere.net/viewcvs/")));
        new WebClient().goTo(p.getUrl()+"api/xml", "application/xml");
        new WebClient().goTo(p.getUrl() + "api/xml?depth=999", "application/xml");
    }
}
