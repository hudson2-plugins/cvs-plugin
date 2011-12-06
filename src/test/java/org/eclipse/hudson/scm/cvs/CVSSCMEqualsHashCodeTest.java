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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

/**
 * Verifies equals and hashcCode of CVSRepositoryBrowser objects.
 * </p>
 * Date: 10/7/2011
 * @author Anton Kozak, Nikita Levyankov
 */
@RunWith(Parameterized.class)
public class CVSSCMEqualsHashCodeTest {

    private CVSSCM defaultCvsscm;
    private CVSSCM cvsscm;
    private boolean expectedResult;

    @Before
    public void setUp() {
        List<ModuleLocationImpl> locations = new ArrayList<ModuleLocationImpl>();
        locations.add(new ModuleLocationImpl("cvsroot", "module", "branch", false, "subdir"));
        defaultCvsscm = new CVSSCM(locations, "cvsRsh", true, false, "regions", true);
    }

    public CVSSCMEqualsHashCodeTest(CVSSCM cvsscm, boolean expectedResult) {
        this.cvsscm = cvsscm;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        List<ModuleLocationImpl> locations = new ArrayList<ModuleLocationImpl>();
        List<ModuleLocationImpl> locations1 = new ArrayList<ModuleLocationImpl>();
        locations.add(new ModuleLocationImpl("cvsroot", "module", "branch", false, "subdir"));
        locations1.add(new ModuleLocationImpl("cvsroot", "module2", "branch", false, "subdir"));

        return Arrays.asList(new Object[][] {
            {new CVSSCM(locations, "cvsRsh", true, false, "regions", true), true},
            {new CVSSCM(locations1, "cvsRsh", true, false, "regions", true), false},
            {new CVSSCM(locations, "cvsRsh1", true, false, "regions", true), false},
            {new CVSSCM(locations, null, true, false, "regions", true), false},
            {new CVSSCM(locations, "cvsRsh", false, false, "regions", true), false},
            {new CVSSCM(locations, "cvsRsh", true, true, "regions", true), false},
            {new CVSSCM(locations, "cvsRsh", true, false, "regions1", true), false},
            {new CVSSCM(locations, "cvsRsh", true, false, null, true), false},
            {new CVSSCM(locations, "cvsRsh", true, false, "regions", false), false},
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, defaultCvsscm.equals(cvsscm));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, defaultCvsscm.hashCode() == cvsscm.hashCode());
    }
}
