/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Oracle Corporation, Anton Kozak, Nikita Levyankov
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
package org.eclipse.hudson.scm.cvs;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.hudson.scm.cvs.browsers.FishEyeCVS;
import org.eclipse.hudson.scm.cvs.browsers.ViewCVS;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Verifies equals and hashcCode of CVSRepositoryBrowser objects.
 * </p>
 * Date: 10/7/2011
 * @author Anton Kozak, Nikita Levyankov
 */
public class CVSRepositoryBrowserEqualsHashCodeTest {

    private CVSRepositoryBrowser fishEyeCVS;
    private CVSRepositoryBrowser viewCVS;

    @Before
    public void setUp() throws MalformedURLException {
        fishEyeCVS = new FishEyeCVS(new URL("http://deadlock.netbeans.org/fisheye/browse/netbeans/"));
        viewCVS = new ViewCVS(new URL("http://isscvs.cern.ch/cgi-bin/viewcvs-all.cgi"));
    }

    @Test
    public void testEquals() throws MalformedURLException {
        assertEquals(fishEyeCVS, new FishEyeCVS(new URL("http://deadlock.netbeans.org/fisheye/browse/netbeans/")));
        assertFalse(fishEyeCVS.equals(new FishEyeCVS(new URL("http://deadlock.netbeans.org/fisheye/browse/"))));
        assertEquals(viewCVS, new ViewCVS(new URL("http://isscvs.cern.ch/cgi-bin/viewcvs-all.cgi")));
        assertFalse(viewCVS.equals(new ViewCVS(new URL("http://isscvs.cern.ch/cgi-bin/viewcvs.cgi"))));
        assertFalse(fishEyeCVS.equals(viewCVS));
    }

    @Test
    public void testHashCode() throws MalformedURLException {
        assertEquals(fishEyeCVS.hashCode(), new FishEyeCVS(new URL("http://deadlock.netbeans.org/fisheye/browse/netbeans/")).hashCode());
        assertFalse(fishEyeCVS.hashCode() == new FishEyeCVS(new URL("http://deadlock.netbeans.org/fisheye/browse/")).hashCode());
        assertEquals(viewCVS.hashCode(), new ViewCVS(new URL("http://isscvs.cern.ch/cgi-bin/viewcvs-all.cgi")).hashCode());
        assertFalse(viewCVS.hashCode() == new ViewCVS(new URL("http://isscvs.cern.ch/cgi-bin/viewcvs.cgi")).hashCode());
        assertFalse(fishEyeCVS.hashCode() == viewCVS.hashCode());
    }
}
