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
 * Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.cvs.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test for {@link org.eclipse.hudson.scm.cvs.util.ParamUtils}
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
