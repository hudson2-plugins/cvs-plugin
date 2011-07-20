/*
 * The MIT License
 *
* Copyright (c) 2004-2011, Oracle Corporation, Anton Kozak
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
package org.eclipse.hudson.scm.config;

import hudson.model.FreeStyleProject;
import org.eclipse.hudson.scm.CVSSCM;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test to verify backward compatibility with Hudson 2.1.0 configuration
 */
public class Hudson201ConfigTest extends BaseLegacyConverterTest {

    @Override
    protected String getResourceName() {
        return "config-2.0.1.xml";
    }

    @Test
    public void testLegacyUnmarshall() throws Exception {
        FreeStyleProject project = (FreeStyleProject) getSourceConfigFile(XSTREAM).read();
        CVSSCM scm = (CVSSCM) project.getScm();
        assertNotNull(scm);
        assertEquals(scm.getModuleLocations().length, 1);
        assertEquals(scm.getModuleLocations()[0].getCvsroot(), ":pserver:anonymous:password@10.4.0.50:/var/cvsroot");
        assertEquals(scm.getModuleLocations()[0].getBranch(), "tag");
        assertEquals(scm.getModuleLocations()[0].getModule(), "test_cvs doc");
        assertEquals(scm.getModuleLocations()[0].getLocalDir(), ".");
        assertTrue(scm.getModuleLocations()[0].isTag());
        assertTrue(scm.getCanUseUpdate());
        assertTrue(scm.isLegacy());
        assertFalse(scm.isFlatten());
        assertEquals(scm.getExcludedRegions(), "");
    }

    @Test
    public void testMarshall() throws Exception {
        //read object from config
        Object item = getSourceConfigFile(XSTREAM).read();
        //save to new config file
        getTargetConfigFile(XSTREAM).write(item);
        getTargetConfigFile(XSTREAM).read();
    }

}