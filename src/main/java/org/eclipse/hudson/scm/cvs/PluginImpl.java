/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
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

import org.eclipse.hudson.scm.cvs.CVSSCM.DescriptorImpl;
import hudson.Plugin;
import hudson.XmlFile;
import hudson.model.Items;
import java.io.IOException;

/**
 * Plugin entry point.
 *
 * @author Anton Kozak
 */
public class PluginImpl extends Plugin {

    @Override
    public void start() throws IOException {
        setXtreamAliasForBackwardCompatibility();
    }
    
    /**
     * Register XStream aliases for backward compatibility - should be removed eventually
     */
    public static void setXtreamAliasForBackwardCompatibility(){
        Items.XSTREAM.alias("hudson.scm.CVSSCM", CVSSCM.class);
        Items.XSTREAM.alias("hudson.scm.ModuleLocationImpl", ModuleLocationImpl.class);

        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.CVSSCM$DescriptorImpl", DescriptorImpl.class);
    }
}
