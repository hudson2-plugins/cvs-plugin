package hudson.scm;

/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Oracle Corporation, Kohsuke Kawaguchi, Jene Jasper, Stephen Connolly, Anton Kozak
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

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.MailAddressResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@link MailAddressResolver} implementation for major CVS hosting sites.
 * @author Kohsuke Kawaguchi
 */
@Extension
public class MailAddressResolverImpl extends MailAddressResolver {
    public String findMailAddressFor(User u) {
        for (AbstractProject<?,?> p : u.getProjects()) {
            SCM scm = p.getScm();
            if (scm instanceof CVSSCM) {
                String s = findMailAddressFor(u,(CVSSCM) scm);
                if(s!=null) return s;
            }
        }

        // didn't hit any known rules
        return null;
    }

    /**
     *
     * @param scm scm.
     */
    protected String findMailAddressFor(User u, CVSSCM scm) {
        for (Map.Entry<Pattern, String> e : RULE_TABLE.entrySet()){
            for (ModuleLocation moduleLocation : scm.getModuleLocations()) {
                if(e.getKey().matcher(moduleLocation.getCvsroot()).matches()){
                    return u.getId()+e.getValue();
                }
            }
        }
        return null;
    }

    private static final Map<Pattern,String/*suffix*/> RULE_TABLE = new HashMap<Pattern, String>();

    static {
        {// java.net
            String username = "([A-Za-z0-9_\\-])+";
            String host = "(.*.dev.java.net|kohsuke.sfbay.*)";
            Pattern cvsUrl = Pattern.compile(":pserver:"+username+"@"+host+":/cvs");

            RULE_TABLE.put(cvsUrl,"@dev.java.net");
        }

        {// source forge
            Pattern cvsUrl = Pattern.compile(":(pserver|ext):([^@]+)@([^.]+).cvs.(sourceforge|sf).net:.+");

            RULE_TABLE.put(cvsUrl,"@users.sourceforge.net");
        }

        // TODO: read some file under $HUDSON_HOME?
    }
}