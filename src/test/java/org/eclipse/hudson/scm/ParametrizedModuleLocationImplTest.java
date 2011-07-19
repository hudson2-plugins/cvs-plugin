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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link ParametrizedModuleLocationImpl}
 */
public class ParametrizedModuleLocationImplTest {
    @Test
    public void testGet() {
        String cvsRoot = ":pserver:${user}:${password}@${host}:/var/cvsroot";
        String modules = "${modules}";
        String branch = "${branch}";
        String dir = "${dir}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("user", "anonymous");
        params.put("password", "password");
        params.put("host", "localhost");
        params.put("dir", "local");
        params.put("branch", "test");
        params.put("modules", "doc src");
        ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(
            new ModuleLocationImpl(cvsRoot, modules, branch, false, dir), params);
        assertEquals(parametrizedLocation.getCvsroot(), ":pserver:anonymous:password@localhost:/var/cvsroot");
        assertEquals(parametrizedLocation.getModule(), "doc src");
        assertEquals(parametrizedLocation.getBranch(), "test");
        assertEquals(parametrizedLocation.getLocalDir(), "local");

    }
}
