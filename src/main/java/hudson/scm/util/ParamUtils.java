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
package hudson.scm.util;

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
