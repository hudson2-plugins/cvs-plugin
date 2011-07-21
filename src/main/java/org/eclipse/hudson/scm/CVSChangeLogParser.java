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
package org.eclipse.hudson.scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * {@link hudson.scm.ChangeLogParser} for CVS.
 * @author Kohsuke Kawaguchi
 */
public class CVSChangeLogParser extends ChangeLogParser {
    public CVSChangeLogSet parse(AbstractBuild build, File changelogFile) throws IOException, SAXException {
        return CVSChangeLogSet.parse(build,changelogFile);
    }
}
