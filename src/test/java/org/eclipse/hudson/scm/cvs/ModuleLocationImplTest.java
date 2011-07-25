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

import org.eclipse.hudson.scm.cvs.ModuleLocation;
import org.eclipse.hudson.scm.cvs.ModuleLocationImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link ModuleLocationImpl}
 */
public class ModuleLocationImplTest {

    private static final String CVSROOT = ":pserver:anonymous:password@10.4.0.50:/var/cvsroot";
    private static final String MODULES = "module1 module2 module\\ name";
    private static final String BRANCH = "branch";
    private static final String LOCAL_DIR = "localDir";

    @Test
    public void testConstructor(){
        ModuleLocation moduleLocation = new ModuleLocationImpl(CVSROOT, MODULES, BRANCH, false, LOCAL_DIR);
        assertEquals(moduleLocation.getCvsroot(), CVSROOT);
        assertEquals(moduleLocation.getModule(), MODULES);
        assertEquals(moduleLocation.getBranch(), BRANCH);
        assertFalse(moduleLocation.isTag());
        assertEquals(moduleLocation.getLocalDir(), LOCAL_DIR);
    }

    @Test
    public void testConstructorEmptyArgs(){
        ModuleLocation moduleLocation = new ModuleLocationImpl("", "", "", false, "");
        assertNull(moduleLocation.getCvsroot());
        assertEquals(moduleLocation.getModule(), "");
        assertNull(moduleLocation.getBranch());
        assertFalse(moduleLocation.isTag());
        assertEquals(moduleLocation.getLocalDir(), ModuleLocationImpl.DEFAULT_LOCAL_DIR);
    }

    @Test
    public void testConstructorNullArgs(){
        ModuleLocation moduleLocation = new ModuleLocationImpl(null, null, null, false, null);
        assertNull(moduleLocation.getCvsroot());
        assertNull(moduleLocation.getModule());
        assertNull(moduleLocation.getBranch());
        assertFalse(moduleLocation.isTag());
        assertEquals(moduleLocation.getLocalDir(), ModuleLocationImpl.DEFAULT_LOCAL_DIR);
    }

    @Test
    public void testConstructorHeadBranch(){
        ModuleLocation moduleLocation = new ModuleLocationImpl(null, null, ModuleLocationImpl.HEAD_BRANCH, false, null);
        assertNull(moduleLocation.getBranch());
    }

    @Test
    public void testGetNormalizedModules(){
        ModuleLocation moduleLocation = new ModuleLocationImpl(null, MODULES, null, false, null);
        String[] normalizedModules = moduleLocation.getNormalizedModules();
        assertEquals(normalizedModules.length, 3);
        assertEquals(normalizedModules[0], "module1");
        assertEquals(normalizedModules[1], "module2");
        assertEquals(normalizedModules[2], "module name");
    }
}
