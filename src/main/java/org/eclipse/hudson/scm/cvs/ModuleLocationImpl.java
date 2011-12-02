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

package org.eclipse.hudson.scm.cvs;

import org.eclipse.hudson.scm.cvs.util.ParamUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Class stores cvs repository url, modules, branches, etc.
 * <p/>
 * Date: 6/22/11
 *
 * @author Anton Kozak
 */
@ExportedBean
public final class ModuleLocationImpl implements ModuleLocation {

    private static final long serialVersionUID = 1L;

    public static final String HEAD_BRANCH = "HEAD";
    static final String DEFAULT_LOCAL_DIR = ".";

    static final String TAGGING_SUBDIR = "TAGGING_SUBDIR";

    /**
     * CVSSCM connection string, like ":pserver:me@host:/cvs"
     */
    private String cvsroot;

    /**
     * Module names.
     * This could be a whitespace/NL-separated list of multiple modules. Modules could be either directories or
     * files. "\ " is used to escape" ", which is needed for modules with whitespace in it.
     */
    private String module;

    /**
     * Branch name.
     */
    private String branch;

    /**
     * Specifies whether this is tag.
     */
    private boolean isTag;

    /**
     * Specifies local dir.
     */
    private String localDir;

    @DataBoundConstructor
    public ModuleLocationImpl(String cvsroot, String module, String branch, boolean isTag, String localDir) {
        if (HEAD_BRANCH.equals(branch)) {
            branch = null;
        }
        this.cvsroot = StringUtils.trim(StringUtils.defaultIfEmpty(cvsroot, null));
        this.module = StringUtils.trim(module);
        this.branch = StringUtils.trim(StringUtils.defaultIfEmpty(branch, null));
        this.isTag = isTag;
        this.localDir = StringUtils.trim(StringUtils.defaultIfEmpty(localDir, DEFAULT_LOCAL_DIR));
    }

    /**
     * @inheritDoc
     */
    @Exported
    public String getCvsroot() {
        return cvsroot;
    }

    /**
     * @inheritDoc
     */
    @Exported
    public String getModule() {
        return module;
    }

    /**
     * @inheritDoc
     */
    @Exported
    public String getBranch() {
        return branch;
    }

    /**
     * @inheritDoc
     */
    @Exported
    public boolean isTag() {
        return isTag;
    }

    /**
     * @inheritDoc
     */
    @Exported
    public String getLocalDir() {
        return localDir;
    }

    /**
     * @inheritDoc
     */
    public String[] getNormalizedModules() {
        return ParamUtils.getNormalizedModules(module);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModuleLocation that = (ModuleLocation) o;

        if (isTag != that.isTag()) {
            return false;
        }
        if (branch != null ? !branch.equals(that.getBranch()) : that.getBranch() != null) {
            return false;
        }
        if (cvsroot != null ? !cvsroot.equals(that.getCvsroot()) : that.getCvsroot() != null) {
            return false;
        }
        if (localDir != null ? !localDir.equals(that.getLocalDir()) : that.getLocalDir() != null) {
            return false;
        }
        if (module != null ? !module.equals(that.getModule()) : that.getModule() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = cvsroot != null ? cvsroot.hashCode() : 0;
        result = 31 * result + (module != null ? module.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (isTag ? 1 : 0);
        result = 31 * result + (localDir != null ? localDir.hashCode() : 0);
        return result;
    }
}
