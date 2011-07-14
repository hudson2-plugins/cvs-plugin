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
package hudson.scm;

import java.io.Serializable;

/**
 * Interface describes module cvs location.
 * <p/>
 * Date: 6/22/11
 *
 * @author Anton Kozak
 */
public interface ModuleLocation extends Serializable {
    /**
     * Returns cvs root.
     *
     * @return cvs root.
     */
    String getCvsroot();

    /**
     * Returns module.
     *
     * @return module.
     */
    String getModule();

    /**
     * Returns branch.
     *
     * @return branch.
     */
    String getBranch();

    /**
     * Returns true if {@link #getBranch()} represents a tag.
     * <p/>
     * This causes Hudson to stop using "-D" option while check out and update.
     *
     * @return true if {@link #getBranch()} represents a tag.
     */
    boolean isTag();

    /**
     * Returns local dir to checkout.
     *
     * @return local dir.
     */
    String getLocalDir();

    /**
     * List up all modules to check out.
     *
     * @return array of split modules from modules string.
     */
    String[] getNormalizedModules();
}
