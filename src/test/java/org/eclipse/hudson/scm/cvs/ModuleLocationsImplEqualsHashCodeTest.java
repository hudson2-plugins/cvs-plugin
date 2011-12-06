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

import java.util.Arrays;
import java.util.Collection;
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
public class ModuleLocationsImplEqualsHashCodeTest {

    private ModuleLocationImpl defaultLocation = new ModuleLocationImpl("cvsroot", "module", "branch", false, "localDir");
    private ModuleLocationImpl location;
    private boolean expectedResult;

    public ModuleLocationsImplEqualsHashCodeTest(ModuleLocationImpl location, boolean expectedResult) {
        this.location = location;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][] {
            {new ModuleLocationImpl("cvsroot", "module", "branch", false, "localDir"), true},
            {new ModuleLocationImpl("cvsroot1", "module", "branch", false, "localDir"), false},
            {new ModuleLocationImpl(null, "module", "branch", false, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", "module1", "branch", false, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", null, "branch", false, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", "module", "branch1", false, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", "module", null, false, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", "module", "branch", true, "localDir"), false},
            {new ModuleLocationImpl("cvsroot", "module", "branch", false, "localDir1"), false},
            {new ModuleLocationImpl("cvsroot", "module", "branch", false, null), false}
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, defaultLocation.equals(location));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, defaultLocation.hashCode() == location.hashCode());
    }

}
