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
