package hudson.scm;

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

import java.io.Serializable;
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
public final class ModuleLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String HEAD_BRANCH = "HEAD";
    private static final String MODULES_REGEX = "(?<!\\\\)[ \\r\\n]+";

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
    public ModuleLocation(String cvsroot, String module, String branch, boolean isTag, String localDir) {
        if (HEAD_BRANCH.equals(branch)) {
            branch = null;
        }
        this.cvsroot = StringUtils.trim(StringUtils.defaultIfEmpty(cvsroot, null));
        this.module = StringUtils.trim(module);
        this.branch = StringUtils.trim(StringUtils.defaultIfEmpty(branch, null));
        this.isTag = isTag;
        this.localDir = StringUtils.trim(StringUtils.defaultIfEmpty(localDir, "./"));
    }

    /**
     * Returns cvs root.
     *
     * @return cvs root.
     */
    @Exported
    public String getCvsroot() {
        return cvsroot;
    }

    /**
     * Returns module.
     *
     * @return module.
     */
    @Exported
    public String getModule() {
        return module;
    }

    /**
     * Returns branch.
     *
     * @return branch.
     */
    @Exported
    public String getBranch() {
        return branch;
    }

    /**
     * Returns true if {@link #getBranch()} represents a tag.
     * <p/>
     * This causes Hudson to stop using "-D" option while check out and update.
     * @return true if {@link #getBranch()} represents a tag.
     */
    @Exported
    public boolean isTag() {
        return isTag;
    }

    /**
     * Returns local dir to checkout.
     *
     * @return local dir.
     */
    @Exported
    public String getLocalDir() {
        return localDir;
    }

    /**
     * List up all modules to check out.
     */
    public String[] getNormalizedModules() {
        // split by whitespace, except "\ "
        String[] r = module.split(MODULES_REGEX);
        // now replace "\ " to " ".
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].replaceAll("\\\\ ", " ");
        }
        return r;
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

        if (isTag != that.isTag) {
            return false;
        }
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        if (cvsroot != null ? !cvsroot.equals(that.cvsroot) : that.cvsroot != null) {
            return false;
        }
        if (localDir != null ? !localDir.equals(that.localDir) : that.localDir != null) {
            return false;
        }
        if (module != null ? !module.equals(that.module) : that.module != null) {
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
