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
package org.eclipse.hudson.scm.util;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.hudson.scm.util.ParamUtils;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test for {@link ParamUtils}
 */
public class ParamUtilsTest {

    @Test
    public void testPopulateParamValues() {
        String input = ":pserver:${user}:${password}@${host}:/var/cvsroot";
        Map<String, String> params = new HashMap<String, String>();
        params.put("user", "anonymous");
        params.put("password", "password");
        params.put("host", "localhost");
        String result = ParamUtils.populateParamValues(input, params);
        assertEquals(result, ":pserver:anonymous:password@localhost:/var/cvsroot");
    }

    @Test
    public void testPopulateWithEmptyParams() {
        String input = ":pserver:user:password@host:/var/cvsroot";
        Map<String, String> params = new HashMap<String, String>();
        String result = ParamUtils.populateParamValues(input, params);
        assertEquals(result, input);
    }

    @Test
    public void testPopulateWithNullParams() {
        String input = ":pserver:user:password@host:/var/cvsroot";
        String result = ParamUtils.populateParamValues(input, null);
        assertEquals(result, input);
    }

    @Test
    public void testPopulateWithEmptyText() {
        String input = "";
        Map<String, String> params = new HashMap<String, String>();
        params.put("user", "anonymous");
        String result = ParamUtils.populateParamValues(input, params);
        assertEquals(result, input);
    }

    @Test
    public void testPopulateWithNullText() {
        String input = null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("user", "anonymous");
        String result = ParamUtils.populateParamValues(input, params);
        assertEquals(result, input);
    }

}
