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
package org.eclipse.hudson.scm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Class contains utilities and helper methods to process parameters.
 * <p/>
 * Date: 6/22/11
 *
 * @author Anton Kozak
 */
public final class ParamUtils {
    private static final String PARAM_FORMAT = "${%s}";
    private static final String MODULES_REGEX = "(?<!\\\\)[ \\r\\n]+";

    private ParamUtils() {
    }

    /**
     * Populates text with parameter values.
     *
     * @param text input text to process
     * @param parameters map with parameters.
     * @return populated text.
     */
    public static String populateParamValues(String text, Map<String, String> parameters) {
        if(MapUtils.isEmpty(parameters) || StringUtils.isEmpty(text)){
            return text;
        }
        List<String> searchList = new ArrayList<String>(parameters.keySet().size());
        List<String> replacementList = new ArrayList<String>(parameters.keySet().size());
        for (String key : parameters.keySet()) {
            searchList.add(String.format(PARAM_FORMAT, key));
            replacementList.add(parameters.get(key));
        }
        return org.apache.commons.lang.StringUtils.replaceEach(text, searchList.toArray(new String[searchList.size()]),
            replacementList.toArray(new String[replacementList.size()]));
    }

    /**
     * @inheritDoc
     */
    public static String[] getNormalizedModules(String modules) {
        // split by whitespace, except "\ "
        String[] r = modules.split(MODULES_REGEX);
        // now replace "\ " to " ".
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].replaceAll("\\\\ ", " ");
        }
        return r;
    }


}
