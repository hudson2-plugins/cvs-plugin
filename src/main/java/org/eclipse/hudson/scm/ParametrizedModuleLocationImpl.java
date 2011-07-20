package org.eclipse.hudson.scm;

import org.eclipse.hudson.scm.util.ParamUtils;
import java.util.Map;

public class ParametrizedModuleLocationImpl implements ModuleLocation {

    /**
     * {@link ModuleLocation} with populated parameters in the properties.
     */
    ModuleLocation location;

    /**
     * Constructs ParametrizedModule based on location module and map of parameters.
     *
     * @param location module location.
     * @param params map with parameters.
     */
    public ParametrizedModuleLocationImpl(ModuleLocation location, Map<String, String> params) {
        this.location = new ModuleLocationImpl(ParamUtils.populateParamValues(location.getCvsroot(), params),
            ParamUtils.populateParamValues(location.getModule(), params),
            ParamUtils.populateParamValues(location.getBranch(), params), location.isTag(),
            ParamUtils.populateParamValues(location.getLocalDir(), params));
    }

    /**
     * @inheritDoc
     */
    public String getCvsroot() {
        return location != null ? location.getCvsroot() : null;
    }

    /**
     * @inheritDoc
     */
    public String getModule() {
        return location != null ? location.getModule() : null;
    }

    /**
     * @inheritDoc
     */
    public String getBranch() {
        return location != null ? location.getBranch() : null;
    }

    /**
     * @inheritDoc
     */
    public boolean isTag() {
        return location != null && location.isTag();
    }

    /**
     * @inheritDoc
     */
    public String getLocalDir() {
        return location != null ? location.getLocalDir() : null;
    }

    /**
     * @inheritDoc
     */
    public String[] getNormalizedModules() {
        return ParamUtils.getNormalizedModules(getModule());
    }
}
