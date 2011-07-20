
/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Anton Kozak
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
