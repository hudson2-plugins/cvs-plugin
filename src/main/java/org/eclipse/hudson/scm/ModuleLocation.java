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
package org.eclipse.hudson.scm;

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
