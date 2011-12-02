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
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.cvs.browsers;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet.File;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet.Revision;
import org.eclipse.hudson.scm.cvs.CVSRepositoryBrowser;
import hudson.scm.RepositoryBrowser;
import hudson.scm.browsers.QueryBuilder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Repository browser for CVS in a FishEye server.
 */
public final class FishEyeCVS extends CVSRepositoryBrowser {

    /**
     * The URL of the FishEye repository, e.g.
     * <tt>http://deadlock.netbeans.org/fisheye/browse/netbeans/</tt>
     */
    public final URL url;

    @DataBoundConstructor
    public FishEyeCVS(URL url) {
        this.url = RepositoryBrowser.normalizeToEndWithSlash(url);
    }

    @Override
    public URL getDiffLink(File file) throws IOException {
        Revision r = new Revision(file.getRevision());
        Revision p = r.getPrevious();
        if (p == null) {
            return null;
        }
        return new URL(url, RepositoryBrowser.trimHeadSlash(file.getFullName()) + new QueryBuilder(url.getQuery()).add("r1=" + p).add("r2=" + r));
    }

    @Override
    public URL getFileLink(File file) throws IOException {
        return new URL(url, RepositoryBrowser.trimHeadSlash(file.getFullName()) + new QueryBuilder(url.getQuery()));
    }

    @Override
    public URL getChangeSetLink(CVSChangeLogSet.CVSChangeLog changeSet) throws IOException {
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        @Override
        public String getDisplayName() {
            return "FishEye";
        }

        public FormValidation doCheckUrl(@QueryParameter String value) throws IOException, ServletException {
            value = Util.fixEmpty(value);
            if (value == null) return FormValidation.ok();
            if (!value.endsWith("/"))   value += '/';
            if (!URL_PATTERN.matcher(value).matches())
                return FormValidation.errorWithMarkup("The URL should end like <tt>.../browse/foobar/</tt>");

            // Connect to URL and check content only if we have admin permission
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER))
                return FormValidation.ok();
            
            final String finalValue = value;
            return new FormValidation.URLCheck() {
                @Override
                protected FormValidation check() throws IOException, ServletException {
                    try {
                        if (findText(open(new URL(finalValue)), "FishEye")) {
                            return FormValidation.ok();
                        } else {
                            return FormValidation.error("This is a valid URL but it doesn't look like FishEye");
                        }
                    } catch (IOException e) {
                        return handleIOException(finalValue, e);
                    }
                }
            }.check();
        }

        private static final Pattern URL_PATTERN = Pattern.compile(".+/browse/[^/]+/");

    }

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FishEyeCVS that = (FishEyeCVS) o;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
