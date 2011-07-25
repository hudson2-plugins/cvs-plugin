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
 * Kohsuke Kawaguchi, Jene Jasper, Stephen Connolly, Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.cvs;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.scm.SCM;
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