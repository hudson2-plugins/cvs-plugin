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
 * Anton Kozak, Nikita Levyankov
 *
 *******************************************************************************/
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
