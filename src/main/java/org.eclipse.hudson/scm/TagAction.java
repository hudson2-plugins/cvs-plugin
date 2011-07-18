package org.eclipse.hudson.scm;

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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.scm.AbstractScmTagAction;
import hudson.scm.SCM;
import hudson.security.Permission;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.tools.ant.taskdefs.Expand;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import static hudson.Util.fixNull;
import static org.eclipse.hudson.scm.cvs.Messages.*;

/**
 * Action for a build that performs the tagging.
 */
@ExportedBean
public class TagAction extends AbstractScmTagAction implements Describable<TagAction> {

    private volatile CVSSCM scmInstance;

    /**
     * If non-null, that means the build is already tagged.
     * If multiple tags are created, those are whitespace-separated.
     */
    private volatile String tagName;

    public TagAction(AbstractBuild build, CVSSCM scmInstance) {
        super(build);
        this.scmInstance = scmInstance;
    }

    public String getIconFileName() {
        if(tagName == null && !build.getParent().getACL().hasPermission(SCM.TAG)) {
            return null;
        }
        return "save.gif";
    }

    public String getDisplayName() {
        if(tagName == null) {
            return CVSSCM_TagThisBuild();
        }
        if(tagName.indexOf(' ') >= 0) {
            return CVSSCM_DisplayName2();
        } else {
            return CVSSCM_DisplayName1();
        }
    }

    @Exported
    public String[] getTagNames() {
        if(tagName == null) {
            return new String[0];
        }
        return tagName.split(" ");
    }

    /**
     * Checks if the value is a valid CVS tag name.
     */
    public synchronized FormValidation doCheckTag(@QueryParameter String value) {
        String tag = fixNull(value).trim();
        if(tag.length() == 0) // nothing entered yet
        {
            return FormValidation.ok();
        }
        return FormValidation.error(isInvalidTag(tag));
    }

    @Override
    public Permission getPermission() {
        return SCM.TAG;
    }

    @Override
    public String getTooltip() {
        if(tagName != null) {
            return "Tag: " + tagName;
        } else {
            return null;
        }
    }

    @Override
    public boolean isTagged() {
        return tagName != null;
    }

    /**
     * Invoked to actually tag the workspace.
     */
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp)
        throws IOException, ServletException {
        build.checkPermission(getPermission());

        Map<AbstractBuild, String> tagSet = new HashMap<AbstractBuild, String>();

        String name = fixNull(req.getParameter("name")).trim();
        String reason = isInvalidTag(name);
        if(reason != null) {
            sendError(reason, req, rsp);
            return;
        }

        tagSet.put(build, name);

        if(req.getParameter("upstream") != null) {
            // tag all upstream builds
            Enumeration e = req.getParameterNames();
            Map<AbstractProject, Integer> upstreams
                = build.getTransitiveUpstreamBuilds(); // TODO: define them at AbstractBuild level

            while(e.hasMoreElements()) {
                String upName = (String) e.nextElement();
                if(!upName.startsWith("upstream.")) {
                    continue;
                }

                String tag = fixNull(req.getParameter(upName)).trim();
                reason = isInvalidTag(tag);
                if(reason != null) {
                    sendError(CVSSCM_NoValidTagNameGivenFor(upName, reason), req, rsp);
                    return;
                }

                upName = upName.substring(9);   // trim off 'upstream.'
                AbstractProject p = Hudson.getInstance().getItemByFullName(upName, AbstractProject.class);
                if(p == null) {
                    sendError(CVSSCM_NoSuchJobExists(upName), req, rsp);
                    return;
                }

                Integer buildNum = upstreams.get(p);
                if(buildNum == null) {
                    sendError(CVSSCM_NoUpstreamBuildFound(upName), req, rsp);
                    return;
                }

                Run build = p.getBuildByNumber(buildNum);
                tagSet.put((AbstractBuild) build, tag);
            }
        }

        new TagWorkerThread(this, tagSet).start();

        doIndex(req, rsp);
    }

    /**
     * Checks if the given value is a valid CVS tag.
     * <p/>
     * If it's invalid, this method gives you the reason as string.
     */
    private String isInvalidTag(String name) {
        // source code from CVS rcs.c
        //void
        //RCS_check_tag (tag)
        //    const char *tag;
        //{
        //    char *invalid = "$,.:;@";		/* invalid RCS tag characters */
        //    const char *cp;
        //
        //    /*
        //     * The first character must be an alphabetic letter. The remaining
        //     * characters cannot be non-visible graphic characters, and must not be
        //     * in the set of "invalid" RCS identifier characters.
        //     */
        //    if (isalpha ((unsigned char) *tag))
        //    {
        //    for (cp = tag; *cp; cp++)
        //    {
        //        if (!isgraph ((unsigned char) *cp))
        //        error (1, 0, "tag `%s' has non-visible graphic characters",
        //               tag);
        //        if (strchr (invalid, *cp))
        //        error (1, 0, "tag `%s' must not contain the characters `%s'",
        //               tag, invalid);
        //    }
        //    }
        //    else
        //    error (1, 0, "tag `%s' must start with a letter", tag);
        //}
        if(name == null || name.length() == 0) {
            return CVSSCM_TagIsEmpty();
        }

        char ch = name.charAt(0);
        if(!(('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z'))) {
            return CVSSCM_TagNeedsToStartWithAlphabet();
        }

        for(char invalid : "$,.:;@".toCharArray()) {
            if(name.indexOf(invalid) >= 0) {
                return CVSSCM_TagContainsIllegalChar(invalid);
            }
        }

        return null;
    }

    /**
     * Performs tagging.
     */
    public void perform(String tagName, TaskListener listener) {
        File destdir = null;
        try {
            destdir = Util.createTempDir();

            // unzip the archive
            listener.getLogger().println(CVSSCM_ExpandingWorkspaceArchive(destdir));
            Expand e = new Expand();
            e.setProject(new org.apache.tools.ant.Project());
            e.setDest(destdir);
            e.setSrc(CVSSCM.getArchiveFile(build));
            e.setTaskType("unzip");
            e.execute();

            // run cvs tag command
            listener.getLogger().println(CVSSCM_TaggingWorkspace());
            for (ModuleLocation moduleLocation : scmInstance.getModuleLocations()) {
                @SuppressWarnings("unchecked")
                ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(moduleLocation,
                    build.getBuildVariables());
                for (String module : parametrizedLocation.getNormalizedModules()) {
                    if (!createTag(tagName, listener, destdir, parametrizedLocation.getLocalDir(), module,
                        scmInstance.isFlatten())) {
                        return;
                    }
                }
            }

            // completed successfully
            onTagCompleted(tagName);
            build.save();
        } catch(Throwable e) {
            e.printStackTrace(listener.fatalError(e.getMessage()));
        } finally {
            try {
                if(destdir != null) {
                    listener.getLogger().println("cleaning up " + destdir);
                    Util.deleteRecursive(destdir);
                }
            } catch(IOException e) {
                e.printStackTrace(listener.fatalError(e.getMessage()));
            }
        }
    }

    private boolean createTag(String tagName, TaskListener listener, File destdir, String moduleLocalDir,
                              String module, boolean isFlatten) throws IOException, InterruptedException {
        FilePath path = (isFlatten ? new FilePath(destdir).child(module)
            : new FilePath(destdir).child(moduleLocalDir).child(module));
        boolean isDir = path.isDirectory();

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(scmInstance.getDescriptor().getCvsExeOrDefault(), "tag");
        if(isDir) {
            cmd.add("-R");
        }
        cmd.add(tagName);
        if(!isDir) {
            cmd.add(path.getName());
            path = path.getParent();
        }

        if(!scmInstance.run(new Launcher.LocalLauncher(listener), cmd, listener, path)) {
            listener.getLogger().println(CVSSCM_TaggingFailed());
            return false;
        }
        return true;
    }

    /**
     * Atomically set the tag name and then be done with {@link TagWorkerThread}.
     */
    private synchronized void onTagCompleted(String tagName) {
        if(this.tagName != null) {
            this.tagName += ' ' + tagName;
        } else {
            this.tagName = tagName;
        }
        this.workerThread = null;
    }

    public Descriptor<TagAction> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(TagAction.class);
    }

    public static final class TagWorkerThread extends TaskThread {
        private final Map<AbstractBuild, String> tagSet;

        public TagWorkerThread(TagAction owner, Map<AbstractBuild, String> tagSet) {
            super(owner, TaskThread.ListenerAndText.forMemory());
            this.tagSet = tagSet;
        }

        @Override
        public synchronized void start() {
            for(Map.Entry<AbstractBuild, String> e : tagSet.entrySet()) {
                TagAction ta = e.getKey().getAction(TagAction.class);
                if(ta != null) {
                    associateWith(ta);
                }
            }

            super.start();
        }

        protected void perform(TaskListener listener) {
            for(Map.Entry<AbstractBuild, String> e : tagSet.entrySet()) {
                TagAction ta = e.getKey().getAction(TagAction.class);
                if(ta == null) {
                    listener.error(e.getKey() + " doesn't have CVS tag associated with it. Skipping");
                    continue;
                }
                listener.getLogger().println(CVSSCM_TagginXasY(e.getKey(), e.getValue()));
                try {
                    e.getKey().keepLog();
                } catch(IOException x) {
                    x.printStackTrace(listener.error(CVSSCM_FailedToMarkForKeep(e.getKey())));
                }
                ta.perform(e.getValue(), listener);
                listener.getLogger().println();
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TagAction> {
        public String getDisplayName() {
            return "";
        }
    }
}
