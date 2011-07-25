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

import hudson.model.Descriptor;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet.CVSChangeLog;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet.File;
import org.eclipse.hudson.scm.cvs.CVSChangeLogSet.Revision;
import org.eclipse.hudson.scm.cvs.CVSRepositoryBrowser;
import hudson.scm.RepositoryBrowser;
import hudson.Extension;
import hudson.scm.browsers.QueryBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link RepositoryBrowser} for CVS.
 * @author Kohsuke Kawaguchi
 */
// See http://viewvc.tigris.org/source/browse/*checkout*/viewvc/trunk/docs/url-reference.html
public final class ViewCVS extends CVSRepositoryBrowser {
    /**
     * The URL of the top of the site.
     *
     * Normalized to ends with '/', like <tt>http://isscvs.cern.ch/cgi-bin/viewcvs-all.cgi/</tt>
     * It may contain a query parameter like <tt>?cvsroot=foobar</tt>, so relative URL
     * construction needs to be done with care.
     */
    public final URL url;

    @DataBoundConstructor
    public ViewCVS(URL url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(url);
    }

    public URL getFileLink(File file) throws IOException {
        return new URL(url,trimHeadSlash(file.getFullName())+param());
    }

    public URL getDiffLink(File file) throws IOException {
        Revision r = new Revision(file.getRevision());
        Revision p = r.getPrevious();
        if(p==null) return null;

        return new URL(getFileLink(file), file.getSimpleName()+".diff"+param().add("r1="+p).add("r2="+r));
    }

    /**
     * No changeset support in ViewCVS.
     */
    public URL getChangeSetLink(CVSChangeLog changeSet) throws IOException {
        return null;
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "ViewCVS";
        }
    }

    private static final long serialVersionUID = 1L;
}
