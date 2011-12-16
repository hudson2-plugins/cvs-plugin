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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.JobProperty;
import hudson.model.ModelObject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.remoting.Future;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.RepositoryBrowsers;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.ArgumentListBuilder;
import hudson.util.AtomicFileWriter;
import hudson.util.ForkOutputStream;
import hudson.util.FormValidation;
import hudson.util.IOException2;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.eclipse.hudson.taskdefs.cvslib.ChangeLogTask;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.framework.io.ByteBuffer;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;
import static java.util.logging.Level.INFO;

/**
 * CVS.
 * <p/>
 * <p/>
 * I couldn't call this class "CVS" because that would cause the view folder name
 * to collide with CVS control files.
 * <p/>
 * <p/>
 * This object gets shipped to the remote machine to perform some of the work,
 * so it implements {@link Serializable}.
 *
 * @author Kohsuke Kawaguchi
 */
public class CVSSCM extends SCM implements Serializable {
    /**
     * Temporary workaround for assisting trouble-shooting.
     * <p/>
     * <p/>
     * Setting this property to true would cause <tt>cvs log</tt> to dump a lot of messages.
     */
    public static boolean debug = Boolean.getBoolean(CVSSCM.class.getName() + ".debug");

    // probe to figure out the CVS hang problem
    public static boolean noQuiet = Boolean.getBoolean(CVSSCM.class.getName() + ".noQuiet");

    private static final long serialVersionUID = 1L;

    /**
     * True to avoid computing the changelog. Useful with ancient versions of CVS that doesn't support
     * the -d option in the log command. See #1346.
     */
    public static boolean skipChangeLog = Boolean.getBoolean(CVSSCM.class.getName() + ".skipChangeLog");

    private static final Logger LOGGER = Logger.getLogger(CVSSCM.class.getName());

    // see http://www.network-theory.co.uk/docs/cvsmanual/cvs_153.html for the output format.
    // we don't care '?' because that's not in the repository
    private static final Pattern UPDATE_LINE = Pattern.compile("[UPARMC] (.+)");

    private static final Pattern REMOVAL_LINE = Pattern.compile(
        "cvs (server|update): `?(.+?)'? is no longer in the repository");

    /**
     * Looks for CVSROOT that includes password, like ":pserver:uid:pwd@server:/path".
     * <p/>
     * <p/>
     * Some CVS client (likely CVSNT?) appears to add the password despite the fact that CVSROOT Hudson is setting
     * doesn't include one. So when we compare CVSROOT, we need to remove the password.
     * <p/>
     * <p/>
     * Since the password equivalence shouldn't really affect the {@link #checkContents(File, String)}, we use
     * this pattern to ignore password from both cvsroot and the string found in <tt>path/CVS/Root</tt>
     * and then compare.
     * <p/>
     * See http://www.nabble.com/Problem-with-polling-CVS%2C-from-version-1.181-tt15799926.html for the user report.
     */
    private static final Pattern PSERVER_CVSROOT_WITH_PASSWORD = Pattern.compile("(:pserver:[^@:]+):[^@:]+(@.+)");
    
    private static final String CVS_SCM_GLOBAL_CONFIG_FILE = "cvs-scm-global-config.xml";

    private String cvsRsh;

    private boolean canUseUpdate;

    /**
     * True to avoid creating a sub-directory inside the workspace.
     * (Works only when there's just one module.)
     */
    private boolean flatten;

    private CVSRepositoryBrowser repositoryBrowser;

    private String excludedRegions;

    /**
     * Option is required for WinCVS and TortoiseCVS clients
     */
    private boolean preventLineEndingConversion;

    // No longer used but left for serialization compatibility
    @Deprecated
    private String cvsroot;

    @Deprecated
    private String module;

    @Deprecated
    private String branch;

    @Deprecated
    private boolean isTag;

    /**
     * Is used to store all configured SVS locations (with their local and remote part).
     *
     * @since 2.1.0
     */
    private ModuleLocation[] moduleLocations = new ModuleLocation[0];

    /**
     * @deprecated as of 2.1.0
     */
    public CVSSCM(String cvsRoot, String allModules, String branch, String cvsRsh, boolean canUseUpdate, boolean legacy,
                  boolean isTag, String excludedRegions) {
        this(Arrays.asList(new ModuleLocationImpl(cvsRoot, allModules, branch, isTag, null)),
            cvsRsh, canUseUpdate, legacy, excludedRegions, false);
    }

    @DataBoundConstructor
    public CVSSCM(List<ModuleLocationImpl> moduleLocations, String cvsRsh, boolean canUseUpdate, boolean legacy,
                  String excludedRegions, boolean preventLineEndingConversion) {
        moduleLocations = removeInvalidEntries(moduleLocations);
        this.moduleLocations = moduleLocations.toArray(new ModuleLocation[moduleLocations.size()]);
        this.cvsRsh = nullify(cvsRsh);
        this.canUseUpdate = canUseUpdate;
        this.flatten = !legacy && moduleLocations.size() == 1
            && moduleLocations.get(0).getNormalizedModules().length == 1;
        this.excludedRegions = excludedRegions;
        this.preventLineEndingConversion = preventLineEndingConversion;
    }

    @Override
    public CVSRepositoryBrowser getBrowser() {
        return repositoryBrowser;
    }

    /**
     * If there are multiple modules, return the module directory of the first one.
     *
     * @param workspace
     */
    public FilePath getModuleRoot(FilePath workspace) {
        if (flatten) {
            return workspace;
        }
        if (getModuleLocations().length > 0 && getModuleLocations()[0].getNormalizedModules().length > 0) {
            return workspace.child(getModuleLocations()[0].getNormalizedModules()[0]);
        } else {
            throw new IllegalArgumentException("Job doesn't contain remote repository configuration");
        }
    }

    @Override
    public FilePath[] getModuleRoots(FilePath workspace) {
        if (!flatten) {
            final ModuleLocation[] moduleLocations = getModuleLocations();
            if (moduleLocations.length > 0) {
                final String[] modules = getAllModules();
                FilePath[] moduleRoots = new FilePath[modules.length];
                for (int i = 0; i < modules.length; i++) {
                    moduleRoots[i] = workspace.child(modules[i]);
                }
                return moduleRoots;
            }
        }
        return new FilePath[]{getModuleRoot(workspace)};
    }

    public ChangeLogParser createChangeLogParser() {
        return new CVSChangeLogParser();
    }

    @Exported
    public String getExcludedRegions() {
        return excludedRegions;
    }

    public String[] getExcludedRegionsNormalized() {
        return excludedRegions == null ? null : excludedRegions.split("[\\r\\n]+");
    }

    @Exported
    public String getCvsRsh() {
        return cvsRsh;
    }

    @Exported
    public boolean getCanUseUpdate() {
        return canUseUpdate;
    }

    @Exported
    public boolean isFlatten() {
        return flatten;
    }

    @Exported
    public boolean isLegacy() {
        return !flatten;
    }

    @Exported
    public boolean isPreventLineEndingConversion() {
        return preventLineEndingConversion;
    }

    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath ws, BuildListener listener,
                            File changelogFile) throws IOException, InterruptedException {
        List<String> changedFiles = null; // files that were affected by update. null this is a check out
        for (ModuleLocation moduleLocation : getModuleLocations()) {
            @SuppressWarnings("unchecked")
            ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(moduleLocation,
                build.getBuildVariables());
            if (canUseUpdate && isUpdatable(parametrizedLocation, ws) == null) {
                changedFiles = update(parametrizedLocation, false, launcher, ws, listener,
                    build.getTimestamp().getTime());
                if (changedFiles == null
                    && !cleanCheckout(parametrizedLocation, launcher, ws, listener, build.getTimestamp().getTime())) {
                    return false;   // failed
                }
            } else {
                if (!checkout(parametrizedLocation, launcher, ws, listener, build.getTimestamp().getTime())) {
                    return false;
                }
            }
        }
        archiveWorkspace(build, ws);


        // contribute the tag action
        build.getActions().add(new TagAction(build));
        return calcChangeLog(build, ws, changedFiles, changelogFile, listener);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> abstractBuild, Launcher launcher,
                                                   TaskListener taskListener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    /**
     * Returns module locations.
     *
     * @return module locations.
     */
    @Exported
    public ModuleLocation[] getModuleLocations() {
        // to support backward compatibility
        if (ArrayUtils.isEmpty(moduleLocations) && cvsroot != null) {
            return new ModuleLocation[]{
                new ModuleLocationImpl(cvsroot, module, branch, isTag, null)
            };
        }
        return moduleLocations;
    }

    /**
     * Returns all described modules.
     *
     * @return modules.
     */
    @Exported
    public String[] getAllModules() {
        List<String> modules = new ArrayList<String>();
        for (ModuleLocation moduleLocation : getModuleLocations()) {
            modules.addAll(Arrays.asList(moduleLocation.getNormalizedModules()));
        }
        return modules.toArray(new String[modules.size()]);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        if (cvsRsh != null) {
            env.put("CVS_RSH", cvsRsh);
        }
        ModuleLocation[] locations = getModuleLocations();
        if (!ArrayUtils.isEmpty(locations) && locations.length == 1
            && locations[0] != null && locations[0].getBranch() != null) {
            env.put("CVS_BRANCH", locations[0].getBranch());
        }
        String cvspass = getDescriptor().getCvspassFile();
        if (cvspass.length() != 0) {
            env.put("CVS_PASSFILE", cvspass);
        }
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                                      FilePath workspace, final TaskListener listener,
                                                      SCMRevisionState _baseline)
        throws IOException, InterruptedException {
        for (ModuleLocation moduleLocation : getModuleLocations()) {
            @SuppressWarnings("unchecked")
            ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(moduleLocation,
                getBuildVariables(project));

            String why = isUpdatable(parametrizedLocation, workspace);
            if (why != null) {
                listener.getLogger().println(Messages.CVSSCM_WorkspaceInconsistent(why));
                return PollingResult.BUILD_NOW;
            }

            List<String> changedFiles = update(parametrizedLocation, true, launcher, workspace, listener, new Date());

            if (changedFiles != null && !changedFiles.isEmpty()) {
                Pattern[] patterns = getExcludedRegionsPatterns();
                if (patterns != null) {
                    boolean areThereChanges = false;
                    for (String changedFile : changedFiles) {
                        boolean patternMatched = false;
                        for (Pattern pattern : patterns) {
                            if (pattern.matcher(changedFile).matches()) {
                                patternMatched = true;
                                break;
                            }
                        }
                        if (!patternMatched) {
                            areThereChanges = true;
                            break;
                        }
                    }
                    if (areThereChanges) {
                        return PollingResult.BUILD_NOW;
                    }
                } else {
                    return PollingResult.BUILD_NOW;
                }
            }
        }
        return PollingResult.NO_CHANGES;
    }

    /**
     * Returns null if we can use "cvs update" instead of "cvs checkout"
     *
     * @return If update is impossible, return the text explaining why.
     */
    String isUpdatable(final ModuleLocation location, FilePath dir) throws IOException, InterruptedException {
        return dir.act(new FileCallable<String>() {
            public String invoke(File dir, VirtualChannel channel) throws IOException {
                if (flatten) {
                    return isUpdatableModule(dir, location);
                } else {
                    for (String m : location.getNormalizedModules()) {
                        File module;
                        if (StringUtils.isNotEmpty(location.getLocalDir())) {
                            module = new File(new File(dir, location.getLocalDir()), m);
                        } else {
                            module = new File(dir, m);
                        }
                        String reason = isUpdatableModule(module, location);
                        if (reason != null) {
                            return reason;
                        }
                    }
                    return null;
                }
            }

            private String isUpdatableModule(File module, final ModuleLocation location) {
                try {
                    // module is a file, like "foo/bar.txt". Then CVS information is "foo/CVS".
                    if (!module.isDirectory()) {
                        module = module.getParentFile();
                    }

                    File cvs = new File(module, "CVS");
                    if (!cvs.exists()) {
                        return "No CVS dir in " + module;
                    }

                    // check cvsroot
                    File cvsRootFile = new File(cvs, "Root");
                    if (!checkContents(cvsRootFile, location.getCvsroot())) {
                        return cvs + "/Root content mismatch: expected " + location.getCvsroot() + " but found "
                            + FileUtils.readFileToString(cvsRootFile);
                    }
                    if (location.getBranch() != null) {
                        if (!checkContents(new File(cvs, "Tag"),
                            (location.isTag() ? 'N' : 'T') + location.getBranch())) {
                            return cvs + " branch mismatch";
                        }
                    } else {
                        File tag = new File(cvs, "Tag");
                        if (tag.exists()) {
                            BufferedReader r = new BufferedReader(new FileReader(tag));
                            try {
                                String s = r.readLine();
                                if (s != null && s.startsWith("D")) {
                                    return null;    // OK
                                }
                                return "Workspace is on branch " + s;
                            } finally {
                                r.close();
                            }
                        }
                    }

                    return null;
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
        });
    }

    /**
     * Updates the workspace as well as locate changes.
     *
     * @return List of affected file names, relative to the workspace directory.
     *         Null if the operation failed.
     */
    List<String> update(ModuleLocation moduleLocation, boolean dryRun, Launcher launcher, FilePath workspace,
                        TaskListener listener, Date date) throws IOException, InterruptedException {
        List<String> changedFileNames = new ArrayList<String>();    // file names relative to the workspace
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(getDescriptor().getCvsExeOrDefault(), debug ? "-t" : "-q",
            compression(moduleLocation.getCvsroot()));

        if (preventLineEndingConversion) {
            cmd.add("--lf");
        }

        if (dryRun) {
            cmd.add("-n");
        }
        cmd.add("update", "-PdC");
        if (moduleLocation.getBranch() != null) {
            cmd.add("-r", moduleLocation.getBranch());
        }
        configureDate(moduleLocation, cmd, date);

        if (flatten) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if (!run(launcher, cmd, listener, workspace,
                new ForkOutputStream(baos, listener.getLogger()))) {
                return null;
            }

            // asynchronously start cleaning up the sticky tag while we work on parsing the result
            Future<Void> task = workspace.actAsync(new StickyDateCleanUpTask());
            parseUpdateOutput("", baos, changedFileNames);
            join(task);
        } else {
            if (performModuleUpdate(launcher, listener, changedFileNames, cmd, moduleLocation, workspace)) {
                return null;
            }
        }
        return changedFileNames;
    }

    /**
     * Returns the file name used to archive the build.
     *
     * @param build {@link AbstractBuild}.
     * @return the file name used to archive the build.
     */
    static File getArchiveFile(AbstractBuild build) {
        return new File(build.getRootDir(), "workspace.zip");
    }

    /**
     * Invokes the command with the specified command line option and wait for its completion.
     *
     * @param dir if launching locally this is a local path, otherwise a remote path.
     * @param out Receives output from the executed program.
     */
    protected final boolean run(Launcher launcher, ArgumentListBuilder cmd, TaskListener listener, FilePath dir,
                                OutputStream out) throws IOException, InterruptedException {
        Map<String, String> env = createEnvVarMap(true);

        int r = launcher.launch().cmds(cmd).envs(env).stdout(out).pwd(dir).join();
        if (r != 0) {
            listener.fatalError(getDescriptor().getDisplayName() + " failed. exit code=" + r);
        }

        return r == 0;
    }

    protected final boolean run(Launcher launcher, ArgumentListBuilder cmd, TaskListener listener, FilePath dir)
        throws IOException, InterruptedException {
        return run(launcher, cmd, listener, dir, listener.getLogger());
    }

    /**
     * @param overrideOnly true to indicate that the returned map shall only contain
     * properties that need to be overridden. This is for use with {@link Launcher}.
     * false to indicate that the map should contain complete map.
     * This is to invoke {@link Proc} directly.
     */
    protected final Map<String, String> createEnvVarMap(boolean overrideOnly) {
        Map<String, String> env = new HashMap<String, String>();
        if (!overrideOnly) {
            env.putAll(EnvVars.masterEnvVars);
        }
        buildEnvVars(null/*TODO*/, env);
        return env;
    }

    /**
     * Provides additional variables and their values.
     * <p/>
     *
     * @param project {@link AbstractProject}
     * @return additional variables and their values.
     */
    private Map<String, String> getBuildVariables(AbstractProject<?, ?> project) {
        Map<String, String> params = new HashMap<String, String>();
        if (project != null && MapUtils.isNotEmpty(project.getProperties())) {
            for (JobProperty prop : project.getProperties().values()) {
                if (prop instanceof ParametersDefinitionProperty) {
                    ParametersDefinitionProperty pp = (ParametersDefinitionProperty) prop;
                    for (ParameterDefinition parameterDefinition : pp.getParameterDefinitions()) {
                        ParameterValue parameterValue = parameterDefinition.getDefaultParameterValue();
                        String value = parameterValue.createVariableResolver(null).resolve(parameterValue.getName());
                        if (value != null) {
                            params.put(parameterDefinition.getName(), value);
                        }
                    }
                }
            }
        }
        return params;
    }

    /**
     * Archives all the CVS-controlled files in {@code dir}.
     *
     * @param relPath The path name in ZIP to store this directory with.
     */
    private void archive(File dir, String relPath, ZipOutputStream zos, boolean isRoot) throws IOException {
        Set<String> knownFiles = new HashSet<String>();
        // see http://www.monkey.org/openbsd/archive/misc/9607/msg00056.html for what Entries.Log is for
        parseCVSEntries(new File(dir, "CVS/Entries"), knownFiles);
        parseCVSEntries(new File(dir, "CVS/Entries.Log"), knownFiles);
        parseCVSEntries(new File(dir, "CVS/Entries.Extra"), knownFiles);
        boolean hasCVSdirs = !knownFiles.isEmpty();
        knownFiles.add("CVS");

        File[] files = dir.listFiles();
        if (files == null) {
            if (isRoot) {
                throw new IOException(
                    "No such directory exists. Did you specify the correct branch? Perhaps you specified a tag: "
                        + dir);
            } else {
                throw new IOException(
                    "No such directory exists. Looks like someone is modifying the workspace concurrently: " + dir);
            }
        }
        for (File f : files) {
            String name = relPath + '/' + f.getName();
            if (f.isDirectory()) {
                if (hasCVSdirs && !knownFiles.contains(f.getName())) {
                    // not controlled in CVS. Skip.
                    // but also make sure that we archive CVS/*, which doesn't have CVS/CVS
                    continue;
                }
                archive(f, name, zos, false);
            } else {
                if (!dir.getName().equals("CVS"))
                // we only need to archive CVS control files, not the actual workspace files
                {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(name));
                FileInputStream fis = new FileInputStream(f);
                Util.copyStream(fis, zos);
                fis.close();
                zos.closeEntry();
            }
        }
    }

    /**
     * Parses the CVS/Entries file and adds file/directory names to the list.
     */
    private void parseCVSEntries(File entries, Set<String> knownFiles) throws IOException {
        if (!entries.exists()) {
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(entries)));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split("/+");
                if (tokens == null || tokens.length < 2) {
                    continue;   // invalid format
                }
                knownFiles.add(tokens[1]);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    // archive the workspace to support later tagging
    @SuppressWarnings("unchecked")
    private void archiveWorkspace(final AbstractBuild build, FilePath ws) throws IOException, InterruptedException {

        File archiveFile = getArchiveFile(build);
        final OutputStream os = new RemoteOutputStream(new FileOutputStream(archiveFile));

        ws.act(new FileCallable<Void>() {
            public Void invoke(File ws, VirtualChannel channel) throws IOException {
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
                if (flatten) {
                    archive(ws, getModuleLocations()[0].getModule(), zos, true);
                } else {
                    for (ModuleLocation moduleLocation : getModuleLocations()) {
                        File mf = new File(ws, moduleLocation.getLocalDir());

                        if (!mf.exists()) {
                            // directory doesn't exist. This happens if a directory that was checked out
                            // didn't include any file.
                            continue;
                        }
                        archive(mf, ModuleLocationImpl.DEFAULT_LOCAL_DIR.equals(moduleLocation.getLocalDir())
                                ? ModuleLocationImpl.TAGGING_SUBDIR : moduleLocation.getLocalDir(), zos, true);
                    }
                }
                zos.close();
                return null;
            }
        });
    }

    /**
     * Performs cleaning workspace before checkout.
     *
     * @param moduleLocation ModuleLocation.
     * @param launcher Launcher.
     * @param dir workspace directory.
     * @param listener Listener.
     * @param buildDate date of the build than will be used for checkout operation with -D flag
     * @return true if checkout successful and false otherwise,
     *
     * @throws IOException if i/o errors.
     * @throws InterruptedException if interrupted.
     */
    boolean cleanCheckout(ModuleLocation moduleLocation, Launcher launcher, FilePath dir, TaskListener listener,
                             Date buildDate) throws InterruptedException, IOException {
        FilePath path = flatten ? dir.getParent() : dir;
        if (StringUtils.isNotEmpty(moduleLocation.getLocalDir())) {
            path = path.child(moduleLocation.getLocalDir());
        }
        try {
            path.deleteContents();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot perform cleaning.");
            return false;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Operation was interrupted");
            return false;
        }
        return checkout(moduleLocation, launcher, dir, listener, buildDate);
    }

    private boolean checkout(ModuleLocation moduleLocation, Launcher launcher, FilePath dir, TaskListener listener,
                             Date dt) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(getDescriptor().getCvsExeOrDefault(), noQuiet ? null : (debug ? "-t" : "-Q"),
            compression(moduleLocation.getCvsroot()));
        if (preventLineEndingConversion) {
            cmd.add("--lf");
        }
        cmd.add("-d", moduleLocation.getCvsroot(), "co", "-P");
        if (moduleLocation.getBranch() != null) {
            cmd.add("-r", moduleLocation.getBranch());
        }
        if (flatten) {
            cmd.add("-d", dir.getName());
        } else {
            cmd.add("-N");
            if (StringUtils.isNotEmpty(moduleLocation.getLocalDir())) {
                cmd.add("-d", moduleLocation.getLocalDir());
            }
        }
        configureDate(moduleLocation, cmd, dt);

        cmd.add(moduleLocation.getNormalizedModules());

        if (!run(launcher, cmd, listener, flatten ? dir.getParent() : dir)) {
            return false;
        }

        // clean up the sticky tag
        if (flatten) {
            dir.act(new StickyDateCleanUpTask());
        } else {
            for (String module : moduleLocation.getNormalizedModules()) {
                FilePath locationPath = (StringUtils.isNotEmpty(moduleLocation.getLocalDir()) ? dir.child(
                    moduleLocation.getLocalDir()) : dir);
                locationPath.child(module).act(new StickyDateCleanUpTask());
            }
        }
        return true;
    }

    private boolean performModuleUpdate(Launcher launcher, TaskListener listener, List<String> changedFileNames,
                                        ArgumentListBuilder cmd, ModuleLocation moduleLocation, FilePath workspace)
        throws IOException, InterruptedException {
        FilePath moduleLocationPath;
        if (StringUtils.isNotEmpty(moduleLocation.getLocalDir())) {
            moduleLocationPath = new FilePath(workspace, moduleLocation.getLocalDir());
        } else {
            moduleLocationPath = workspace;
        }
        @SuppressWarnings("unchecked") // StringTokenizer oddly has the wrong type
        final Set<String> moduleNames = populateModuleNames(moduleLocation, moduleLocationPath);

        for (String moduleName : moduleNames) {
            // capture the output during update
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FilePath modulePath = new FilePath(moduleLocationPath, moduleName);

            ArgumentListBuilder actualCmd = cmd;
            String baseName = moduleName;

            if (!modulePath.isDirectory()) {
                // updating just one file, like "foo/bar.txt".
                // run update command from "foo" directory with "bar.txt" as the command line argument
                actualCmd = cmd.clone();
                actualCmd.add(modulePath.getName());
                modulePath = modulePath.getParent();
                int slash = baseName.lastIndexOf('/');
                if (slash > 0) {
                    baseName = baseName.substring(0, slash);
                }
            }

            if (!run(launcher, actualCmd, listener,
                modulePath,
                new ForkOutputStream(baos, listener.getLogger()))) {
                return true;
            }

            // asynchronously start cleaning up the sticky tag while we work on parsing the result
            Future<Void> task = modulePath.actAsync(new StickyDateCleanUpTask());

            // we'll run one "cvs log" command with workspace as the base,
            // so use path names that are relative to moduleName.
            parseUpdateOutput(baseName + '/', baos, changedFileNames);

            join(task);
        }
        return false;
    }

    private Set<String> populateModuleNames(ModuleLocation moduleLocation, FilePath moduleLocationPath)
        throws IOException, InterruptedException {
        @SuppressWarnings("unchecked") // StringTokenizer oddly has the wrong type
        final Set<String> moduleNames = new TreeSet(Arrays.asList(moduleLocation.getNormalizedModules()));

        // Add in any existing CVS dirs, in case project checked out its own.
        moduleNames.addAll(moduleLocationPath.act(new FileCallable<Set<String>>() {
            public Set<String> invoke(File ws, VirtualChannel channel) throws IOException {
                File[] subdirs = ws.listFiles();
                if (subdirs != null) {
                    SUBDIR:
                    for (File s : subdirs) {
                        if (new File(s, "CVS").isDirectory()) {
                            String top = s.getName();
                            for (String mod : moduleNames) {
                                if (mod.startsWith(top + "/")) {
                                    // #190: user asked to check out foo/bar foo/baz quux
                                    // Our top-level dirs are "foo" and "quux".
                                    // Do not add "foo" to checkout or we will check out foo/*!
                                    continue SUBDIR;
                                }
                            }
                            moduleNames.add(top);
                        }
                    }
                }
                return moduleNames;
            }
        }));
        return moduleNames;
    }

    private void join(Future<Void> task) throws InterruptedException, IOException {
        try {
            task.get();
        } catch (ExecutionException e) {
            throw new IOException2(e);
        }
    }

    /**
     * Parses the output from CVS update and list up files that might have been changed.
     *
     * @param result list of file names whose changelog should be checked. This may include files
     * that are no longer present. The path names are relative to the workspace,
     * hence "String", not {@link File}.
     */
    private void parseUpdateOutput(String baseName, ByteArrayOutputStream output, List<String> result)
        throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(output.toByteArray())));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher matcher = UPDATE_LINE.matcher(line);
            if (matcher.matches()) {
                result.add(baseName + matcher.group(1));
                continue;
            }

            matcher = REMOVAL_LINE.matcher(line);
            if (matcher.matches()) {
                result.add(baseName + matcher.group(2));
                continue;
            }
        }
    }

    /**
     * Returns true if the contents of the file is equal to the given string.
     *
     * @return false in all the other cases.
     */
    private boolean checkContents(File file, String contents) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            try {
                String s = r.readLine();
                if (s == null) {
                    return false;
                }
                return massageForCheckContents(s).equals(massageForCheckContents(contents));
            } finally {
                r.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Normalize the string for comparison in {@link #checkContents(File, String)}.
     */
    private String massageForCheckContents(String s) {
        s = s.trim();
        // this is somewhat ugly because we only want to do this for CVS/Root but still ended up doing this
        // for all checks. OTOH, there shouldn'be really any false positive.
        Matcher m = PSERVER_CVSROOT_WITH_PASSWORD.matcher(s);
        if (m.matches()) {
            s = m.group(1) + m.group(2);  // cut off password
        }
        return s;
    }

    /**
     * Computes the changelog into an XML file.
     * <p/>
     * <p/>
     * When we update the workspace, we'll compute the changelog by using its output to
     * make it faster. In general case, we'll fall back to the slower approach where
     * we check all files in the workspace.
     *
     * @param changedFiles Files whose changelog should be checked for updates.
     * This is provided if the previous operation is update, otherwise null,
     * which means we have to fall back to the default slow computation.
     */
    private boolean calcChangeLog(AbstractBuild build, FilePath ws, final List<String> changedFiles, File changelogFile,
                                  final BuildListener listener) throws InterruptedException {
        if (build.getPreviousBuild() == null || (changedFiles != null && changedFiles.isEmpty())) {
            // nothing to compare against, or no changes
            // (note that changedFiles==null means fallback, so we have to run cvs log.
            listener.getLogger().println("$ no changes detected");
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        }
        if (skipChangeLog) {
            listener.getLogger().println("Skipping changelog computation");
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        }

        listener.getLogger().println("$ computing changelog");

        final String cvspassFile = getDescriptor().getCvspassFile();
        final String cvsExe = getDescriptor().getCvsExeOrDefault();

        OutputStream o = null;
        try {
            // range of time for detecting changes
            final Date startTime = build.getPreviousBuild().getTimestamp().getTime();
            final Date endTime = build.getTimestamp().getTime();
            final OutputStream out = o = new RemoteOutputStream(new FileOutputStream(changelogFile));

            for (ModuleLocation moduleLocation : getModuleLocations()) {
                @SuppressWarnings("unchecked")
                ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(moduleLocation,
                    build.getBuildVariables());
                ChangeLogResult result = getChangelog(parametrizedLocation, ws, changedFiles, listener, cvspassFile,
                    cvsExe, startTime, endTime, out);
                if (result != null && result.hadError) {
                    // non-fatal error must have occurred, such as cvs changelog parsing error.s
                    listener.getLogger().print(result.errorOutput);
                }
            }


            return true;
        } catch (BuildExceptionWithLog e) {
            // capture output from the task for diagnosis
            listener.getLogger().print(e.errorOutput);
            // then report an error
            BuildException x = (BuildException) e.getCause();
            PrintWriter w = listener.error(x.getMessage());
            w.println("Working directory is " + ws);
            x.printStackTrace(w);
            return false;
        } catch (RuntimeException e) {
            // an user reported a NPE inside the changeLog task.
            // we don't want a bug in Ant to prevent a build.
            e.printStackTrace(listener.error(e.getMessage()));
            return true;    // so record the message but continue
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to detect changlog"));
            return true;
        } finally {
            IOUtils.closeQuietly(o);
        }
    }

    private ChangeLogResult getChangelog(final ModuleLocation moduleLocation, FilePath ws,
                                         final List<String> changedFiles, final BuildListener listener,
                                         final String cvspassFile, final String cvsExe,
                                         final Date startTime, final Date endTime,
                                         final OutputStream out) throws IOException, InterruptedException {
        FilePath processingPath;
        if (StringUtils.isNotEmpty(moduleLocation.getLocalDir())) {
            processingPath = new FilePath(ws, moduleLocation.getLocalDir());
        } else {
            processingPath = ws;
        }
        return processingPath.act(new FileCallable<ChangeLogResult>() {
            public ChangeLogResult invoke(File processingPath, VirtualChannel channel) throws IOException {
                final StringWriter errorOutput = new StringWriter();
                final boolean[] hadError = new boolean[1];

                ChangeLogTask task = new ChangeLogTask() {
                    @Override
                    public void log(String msg, int msgLevel) {
                        if (msgLevel == org.apache.tools.ant.Project.MSG_ERR) {
                            hadError[0] = true;
                        }
                        // send error to listener. This seems like the route in which the changelog task
                        // sends output.
                        // Also in ChangeLogTask.getExecuteStreamHandler, we send stderr from CVS
                        // at WARN level.
                        if (msgLevel <= org.apache.tools.ant.Project.MSG_WARN) {
                            errorOutput.write(msg);
                            errorOutput.write('\n');
                            return;
                        }
                        if (debug) {
                            listener.getLogger().println(msg);
                        }
                    }
                };
                task.setProject(new org.apache.tools.ant.Project());
                task.setCvsExe(cvsExe);
                task.setDir(processingPath);
                if (cvspassFile.length() != 0) {
                    task.setPassfile(new File(cvspassFile));
                }
                if (canUseUpdate && moduleLocation.getCvsroot().startsWith("/")) {
                    // cvs log of built source trees unreliable in local access method:
                    // https://savannah.nongnu.org/bugs/index.php?15223
                    task.setCvsRoot(":fork:" + moduleLocation.getCvsroot());
                } else if (canUseUpdate && moduleLocation.getCvsroot().startsWith(":local:")) {
                    task.setCvsRoot(":fork:" + moduleLocation.getCvsroot().substring(7));
                } else {
                    task.setCvsRoot(moduleLocation.getCvsroot());
                }
                task.setCvsRsh(cvsRsh);
                task.setFailOnError(true);
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(out);
                task.setDeststream(bufferedOutput);
                // It's to enforce ChangeLogParser find a "branch". If tag was specified, branch does not matter (see documentation for 'cvs log -r:tag').
                if (!moduleLocation.isTag()) {
                    task.setBranch(moduleLocation.getBranch());
                }
                // It's to enforce ChangeLogTask use "baranch" in CVS command (cvs log -r...).
                // task.setTag(isTag() ? ":" + branch : branch);
                task.setTag(moduleLocation.getBranch());
                task.setStart(startTime);
                task.setEnd(endTime);
                if (changedFiles != null) {
                    // we can optimize the processing if we know what files have changed.
                    // but also try not to make the command line too long so as no to hit
                    // the system call limit to the command line length (see issue #389)
                    // the choice of the number is arbitrary, but normally we don't really
                    // expect continuous builds to have too many changes, so this should be OK.
                    if (changedFiles.size() < 100 || !Hudson.isWindows()) {
                        // if the directory doesn't exist, cvs changelog will die, so filter them out.
                        // this means we'll lose the log of those changes
                        for (String filePath : changedFiles) {
                            if (new File(processingPath, filePath).getParentFile().exists()) {
                                task.addFile(filePath);
                            }
                        }
                    }
                } else {
                    // fallback
                    if (!flatten) {
                        task.setPackage(moduleLocation.getNormalizedModules());
                    }
                }

                try {
                    task.execute();
                } catch (BuildException e) {
                    throw new BuildExceptionWithLog(e, errorOutput.toString());
                } finally {
                    bufferedOutput.close();
                }

                return new ChangeLogResult(hadError[0], errorOutput.toString());
            }
        });
    }

    private String compression(String cvsroot) {
        if (getDescriptor().isNoCompression()) {
            return null;
        }

        // CVS 1.11.22 manual:
        // If the access method is omitted, then if the repository starts with
        // `/', then `:local:' is assumed.  If it does not start with `/' then
        // either `:ext:' or `:server:' is assumed.
        boolean local = cvsroot.startsWith("/") || cvsroot.startsWith(":local:") || cvsroot.startsWith(":fork:");
        // For local access, compression is senseless. For remote, use z3:
        // http://root.cern.ch/root/CVS.html#checkout
        return local ? "-z0" : "-z3";
    }

    private List<ModuleLocationImpl> removeInvalidEntries(List<ModuleLocationImpl> locations) {
        return Lists.newArrayList(
            com.google.common.collect.Iterables.filter(locations, new Predicate<ModuleLocationImpl>() {
                public boolean apply(ModuleLocationImpl location) {
                    return StringUtils.isNotEmpty(location.getCvsroot());
                }
            }));

    }

    private void configureDate(ModuleLocation moduleLocation, ArgumentListBuilder cmd, Date date) { // #192
        if (moduleLocation.isTag()) {
            return; // don't use the -D option.
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC")); // #209
        cmd.add("-D", df.format(date));
    }

    private Pattern[] getExcludedRegionsPatterns() {
        String[] excludedRegions = getExcludedRegionsNormalized();
        if (excludedRegions != null) {
            Pattern[] patterns = new Pattern[excludedRegions.length];

            int i = 0;
            for (String excludedRegion : excludedRegions) {
                patterns[i++] = Pattern.compile(excludedRegion);
            }

            return patterns;
        }

        return null;
    }

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<CVSSCM> implements ModelObject {
        static final Pattern CVSROOT_PSERVER_PATTERN =
            Pattern.compile(":(ext|extssh|pserver):[^@:]+(:[^@:]*)?@[^:]+:(\\d+:)?.+");

        /**
         * Path to <tt>.cvspass</tt>. Null to default.
         */
        private String cvsPassFile;

        /**
         * Path to cvs executable. Null to just use "cvs".
         */
        private String cvsExe;

        /**
         * Disable CVS compression support.
         */
        private boolean noCompression;

        // compatibility only
        private transient Map<String, RepositoryBrowser> browsers;

        // compatibility only
        class RepositoryBrowser {
            String diffURL;
            String browseURL;
        }

        public DescriptorImpl() {
            this(true);
        }


        /**
         * For the tests only
         */
        DescriptorImpl(boolean isLoad) {
            super(CVSRepositoryBrowser.class);
            if (isLoad) {
                load();
            }
        }

        public String getDisplayName() {
            return "CVS";
        }
        
        @Override
        public XmlFile getConfigFile() {
            File hudsonRoot = Hudson.getInstance().getRootDir();
            File globalConfigFile = new File(hudsonRoot, CVS_SCM_GLOBAL_CONFIG_FILE);
            
            // For backward Compatibility
            File oldGlobalConfigFile = new File(hudsonRoot, "hudson.scm.CVSSCM.xml");
            if (oldGlobalConfigFile.exists()){
                oldGlobalConfigFile.renameTo(globalConfigFile);
            }
            
            return new XmlFile(globalConfigFile);
        }


        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            CVSSCM scm = req.bindJSON(CVSSCM.class, formData);
            scm.repositoryBrowser = RepositoryBrowsers.createInstance(CVSRepositoryBrowser.class, req, formData,
                "browser");
            return scm;
        }

        public String getCvspassFile() {
            String value = cvsPassFile;
            if (value == null) {
                value = "";
            }
            return value;
        }

        public String getCvsExe() {
            return cvsExe;
        }

        public void setCvsExe(String value) {
            this.cvsExe = value;
            save();
        }

        public String getCvsExeOrDefault() {
            if (Util.fixEmpty(cvsExe) == null) {
                return "cvs";
            } else {
                return cvsExe;
            }
        }

        public void setCvspassFile(String value) {
            cvsPassFile = value;
            save();
        }

        public boolean isNoCompression() {
            return noCompression;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) {
            cvsPassFile = fixEmptyAndTrim(o.getString("cvspassFile"));
            cvsExe = fixEmptyAndTrim(o.getString("cvsExe"));
            noCompression = req.getParameter("cvs_noCompression") != null;
            save();

            return true;
        }

        //
        // web methods
        //
        public FormValidation doCheckCvspassFile(@QueryParameter String value) {
            // this method can be used to check if a file exists anywhere in the file system,
            // so it should be protected.
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            value = fixEmpty(value);
            if (value == null) // not entered
            {
                return FormValidation.ok();
            }

            File cvsPassFile = new File(value);

            if (cvsPassFile.exists()) {
                if (cvsPassFile.isDirectory()) {
                    return FormValidation.error(cvsPassFile + " is a directory");
                } else {
                    return FormValidation.ok();
                }
            }

            return FormValidation.error("No such file exists");
        }

        /**
         * Checks if cvs executable exists.
         */
        public FormValidation doCheckCvsExe(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }

        /**
         * Displays "cvs --version" for trouble shooting.
         */
        public void doVersion(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, InterruptedException {
            ByteBuffer baos = new ByteBuffer();
            try {
                Hudson.getInstance().createLauncher(TaskListener.NULL).launch()
                    .cmds(getCvsExeOrDefault(), "--version").stdout(baos).join();
                rsp.setContentType("text/plain");
                baos.writeTo(rsp.getOutputStream());
            } catch (IOException e) {
                req.setAttribute("error", e);
                rsp.forward(this, "versionCheckError", req);
            }
        }

        /**
         * Checks the correctness of the branch name.
         */
        public FormValidation doCheckBranch(@QueryParameter String value) {
            String v = fixNull(value);

            if (v.equals("HEAD")) {
                return FormValidation.error(Messages.CVSSCM_HeadIsNotBranch());
            }

            return FormValidation.ok();
        }

        /**
         * Checks the entry to the CVSROOT field.
         * <p/>
         * Also checks if .cvspass file contains the entry for this.
         */
        public FormValidation doCheckCvsroot(@QueryParameter String value) throws IOException {
            String v = StringUtils.trim(StringUtils.defaultIfEmpty(value, null));
            if (v == null) {
                return FormValidation.error(Messages.CVSSCM_MissingCvsroot());
            }

            Matcher matcher = CVSROOT_PSERVER_PATTERN.matcher(v);

            if (!isCvsrootValid(v, matcher)) {
                return FormValidation.error(Messages.CVSSCM_InvalidCvsroot());
            }

            if (!isPasswordSet(v, matcher)) {
                return FormValidation.error(Messages.CVSSCM_PasswordNotSet());
            }
            return FormValidation.ok();
        }

        /**
         * Validates the excludeRegions Regex
         */
        public FormValidation doCheckExcludeRegions(@QueryParameter String value) {
            String v = fixNull(value).trim();

            for (String region : v.split("[\\r\\n]+")) {
                try {
                    Pattern.compile(region);
                } catch (PatternSyntaxException e) {
                    return FormValidation.error("Invalid regular expression. " + e.getMessage());
                }
            }
            return FormValidation.ok();
        }

        /**
         * Runs cvs login command.
         * <p/>
         * TODO: this apparently doesn't work. Probably related to the fact that
         * cvs does some tty magic to disable echo back or whatever.
         */
        public void doPostPassword(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

            String cvsroot = req.getParameter("cvsroot");
            String password = req.getParameter("password");

            if (cvsroot == null || password == null) {
                rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            rsp.setContentType("text/plain");
            Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL).launch()
                .cmds(getCvsExeOrDefault(), "-d", cvsroot, "login")
                .stdin(new ByteArrayInputStream((password + "\n").getBytes()))
                .stdout(rsp.getOutputStream()).start();
            proc.join();
        }

        protected void convert(Map<String, Object> oldPropertyBag) {
            cvsPassFile = (String) oldPropertyBag.get("cvspass");
        }

        protected Map<String, RepositoryBrowser> getBrowsers() {
            return browsers;
        }

        private boolean isPasswordSet(String url, Matcher matcher) throws IOException {
            // check .cvspass file to see if it has entry.
            // CVS handles authentication only if it's pserver.
            if (url.startsWith(":pserver") && matcher.group(2) == null) {// if password is not specified in CVSROOT
                String cvspass = getCvspassFile();
                File passfile;
                if (cvspass.equals("")) {
                    passfile = new File(new File(System.getProperty("user.home")), ".cvspass");
                } else {
                    passfile = new File(cvspass);
                }

                if (passfile.exists() && !scanCvsPassFile(passfile, url)) {
                    // It's possible that we failed to locate the correct .cvspass file location,
                    // so don't report an error if we couldn't locate this file.
                    //
                    // if this is explicitly specified, then our system config page should have
                    // reported an error.
                    return false;
                }
            }
            return true;
        }

        boolean isCvsrootValid(String url, Matcher matcher) {
            // CVSROOT format isn't really that well defined. So it's hard to check this rigorously.
            return !((url.startsWith(":pserver") || url.startsWith(":ext")) && !matcher.matches());
        }

        /**
         * Checks if the given pserver CVSROOT value exists in the pass file.
         */
        private boolean scanCvsPassFile(File passfile, String cvsroot) throws IOException {
            cvsroot += ' ';
            String cvsroot2 = "/1 " + cvsroot; // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5006835
            BufferedReader in = new BufferedReader(new FileReader(passfile));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    // "/1 " version always have the port number in it, so examine a much with
                    // default port 2401 left out
                    int portIndex = line.indexOf(":2401/");
                    String line2 = "";
                    if (portIndex >= 0) {
                        line2 = line.substring(0, portIndex + 1) + line.substring(portIndex + 5); // leave '/'
                    }

                    if (line.startsWith(cvsroot) || line.startsWith(cvsroot2) || line2.startsWith(cvsroot2)) {
                        return true;
                    }
                }
                return false;
            } finally {
                in.close();
            }
        }
    }

    /**
     * @see org.eclipse.hudson.scm.cvs.TagAction
     * @deprecated this class was left for backward compatibility.
     */
    @ExportedBean
    public final class TagAction extends org.eclipse.hudson.scm.cvs.TagAction {
        public TagAction(AbstractBuild build) {
            super(build, CVSSCM.this);
        }
    }

    /**
     * Used to communicate the result of the detection in {@link CVSSCM#calcChangeLog(AbstractBuild, FilePath, List, File, BuildListener)}
     */
    static class ChangeLogResult implements Serializable {
        boolean hadError;
        String errorOutput;

        public ChangeLogResult(boolean hadError, String errorOutput) {
            this.hadError = hadError;
            if (hadError) {
                this.errorOutput = errorOutput;
            }
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Used to propagate {@link BuildException} and error log at the same time.
     */
    static class BuildExceptionWithLog extends RuntimeException {
        final String errorOutput;

        public BuildExceptionWithLog(BuildException cause, String errorOutput) {
            super(cause);
            this.errorOutput = errorOutput;
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Recursively visits directories and get rid of the sticky date in <tt>CVS/Entries</tt> folder.
     */
    private static final class StickyDateCleanUpTask implements FileCallable<Void> {
        private static final Pattern STICKY_DATE = Pattern.compile(
            "D\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d\\.\\d\\d\\.\\d\\d\\.\\d\\d");

        public Void invoke(File f, VirtualChannel channel) throws IOException {
            process(f);
            return null;
        }

        private void process(File f) throws IOException {
            File entries = new File(f, "CVS/Entries");
            if (!entries.exists()) {
                return; // not a CVS-controlled directory. No point in recursing
            }

            boolean modified = false;
            String contents;
            try {
                contents = FileUtils.readFileToString(entries);
            } catch (IOException e) {
                // reports like http://www.nabble.com/Exception-while-checking-out-from-CVS-td24256117.html
                // indicates that CVS/Entries may contain something more than we know of. leave them as is
                LOGGER.log(INFO, "Failed to parse " + entries, e);
                return;
            }
            StringBuilder newContents = new StringBuilder(contents.length());
            String[] lines = contents.split("\n");

            for (String line : lines) {
                int idx = line.lastIndexOf('/');
                if (idx == -1) {
                    continue;       // something is seriously wrong with this line. just skip.
                }

                String date = line.substring(idx + 1);
                if (STICKY_DATE.matcher(date.trim()).matches()) {
                    // the format is like "D2008.01.21.23.30.44"
                    line = line.substring(0, idx + 1);
                    modified = true;
                }

                newContents.append(line).append('\n');
            }

            if (modified) {
                // write it back
                AtomicFileWriter w = new AtomicFileWriter(entries, null);
                try {
                    w.write(newContents.toString());
                    w.commit();
                } finally {
                    w.abort();
                }
            }

            // recursively process children
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    process(child);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CVSSCM that = (CVSSCM) o;

        return new EqualsBuilder()
            .append(canUseUpdate, that.canUseUpdate)
            .append(flatten, that.flatten)
            .append(preventLineEndingConversion, that.preventLineEndingConversion)
            .append(cvsRsh, that.cvsRsh)
            .append(excludedRegions, that.excludedRegions)
            .append(moduleLocations, that.moduleLocations)
            .append(repositoryBrowser, that.repositoryBrowser)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(canUseUpdate)
            .append(flatten)
            .append(preventLineEndingConversion)
            .append(cvsRsh)
            .append(excludedRegions)
            .append(moduleLocations)
            .append(repositoryBrowser)
            .toHashCode();
    }
}
