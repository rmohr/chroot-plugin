/*
 *  Copyright 2013, Roman Mohr <roman@fenkhuber.at>
 *
 *  This file is part of Chroot-plugin.
 *
 *  Chroot-plugin is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Chroot-plugin is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Chroot-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.builders;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.AncestorInPath;

/**
 *
 * @author roman
 */
public class ChrootBuilder extends Builder implements Serializable {

    private String chrootName;
    private boolean ignoreExit;
    private List<String> additionalPackages;
    private String packagesFile;
    private boolean clear;
    private String command;
    private boolean loginAsRoot;
    private boolean noUpdate;
    private boolean forceInstall;

    public boolean isForceInstall() {
        return forceInstall;
    }

    public boolean isNoUpdate() {
        return noUpdate;
    }

    @DataBoundConstructor
    public ChrootBuilder(String chrootName, boolean ignoreExit,
            String additionalPackages, String packagesFile, boolean clear,
            String command, boolean loginAsRoot, boolean noUpdate, boolean forceInstall) throws IOException {
        this.loginAsRoot = loginAsRoot;
        this.chrootName = chrootName;
        this.ignoreExit = ignoreExit;
        this.additionalPackages = ChrootUtil.splitPackages(additionalPackages);
        this.packagesFile = packagesFile;
        this.clear = clear;
        this.command = command;
        this.noUpdate = noUpdate;
        this.forceInstall = forceInstall;
    }

    public boolean isLoginAsRoot() {
        return loginAsRoot;
    }

    public String getChrootName() {
        return chrootName;
    }

    public String getCommand() {
        return command;
    }

    public boolean isIgnoreExit() {
        return ignoreExit;
    }

    public String getAdditionalPackages() {
        return StringUtils.join(additionalPackages, " ");
    }

    public String getPackagesFile() {
        return packagesFile;
    }

    public boolean isClear() {
        return clear;
    }

    private static final class LocalCopyTo implements FileCallable<Void> {

        private final String target;

        public LocalCopyTo(String target) {
            this.target = target;
        }

        public Void invoke(File source, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath _source = new FilePath(source);
            FilePath _target = new FilePath(new File(target));
            _source.copyTo(_target);
            return null;
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        ChrootToolset installation = ChrootToolset.getInstallationByName(env.expand(this.chrootName));
        installation = installation.forNode(build.getBuiltOn(), listener);
        installation = installation.forEnvironment(env);
        if (installation.getHome() == null) {
            listener.fatalError("Installation of chroot environment failed");
            listener.fatalError("Please check if pbuilder is installed on the selected node, and that"
                    + " the user Jenkins uses can run pbuilder with sudo.");
            return false;
        }
        FilePath tarBall = new FilePath(build.getBuiltOn().getChannel(), installation.getHome());

        FilePath workerTarBall = build.getWorkspace().child(env.expand(this.chrootName)).child(tarBall.getName());
        workerTarBall.getParent().mkdirs();

        if (!workerTarBall.exists() || !ChrootUtil.isFileIntact(workerTarBall) || tarBall.lastModified() > workerTarBall.lastModified()) {
            tarBall.act(new LocalCopyTo(workerTarBall.getRemote()));
            ChrootUtil.getDigestFile(tarBall).act(new LocalCopyTo(ChrootUtil.getDigestFile(workerTarBall).getRemote()));
        }

        // force environment recreation when clear is selected
        if (workerTarBall.exists() && isClear()) {
            boolean ret = installation.getChrootWorker().cleanUp(build, launcher, listener, workerTarBall);
            if (ret == false) {
                listener.fatalError("Chroot environment cleanup failed");
                return ret || ignoreExit;
            }
        }

        //install extra packages
        List<String> packages = new LinkedList<String>(this.additionalPackages);
        for (String packagesFile : ChrootUtil.splitFiles(getPackagesFile())) {
            FilePath packageFile = new FilePath(build.getWorkspace(), packagesFile);
            if (packageFile.exists() && !packageFile.isDirectory()) {
                String packageFilePackages = packageFile.readToString();
                packages.addAll(ChrootUtil.splitPackages(packageFilePackages));
            } else {
                listener.error("Requirements file '" + packagesFile + "' is not an existing file.");
                return false || ignoreExit;
            }
        }

        if (!packages.isEmpty()) {
            boolean ret = installation.getChrootWorker().installPackages(build, launcher, listener, workerTarBall, packages, isForceInstall());
            if (ret == false) {
                listener.fatalError("Installing additional packages in chroot environment failed.");
                return ret || ignoreExit;
            }
        } else if (!this.isNoUpdate()) {
            boolean ret = installation.getChrootWorker().updateRepositories(build, launcher, listener, workerTarBall);
            if (ret == false) {
                listener.fatalError("Updating repository indices in chroot environment failed.");
                return ret || ignoreExit;
            }
        }
        ChrootUtil.saveDigest(workerTarBall);
        return ignoreExit || installation.getChrootWorker().perform(build, launcher, listener, workerTarBall, this.command, isLoginAsRoot());
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public AutoCompletionCandidates doAutoCompleteChrootName(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            for (ChrootToolset set : ChrootToolset.list()) {
                if(set.getName().startsWith(value))
                    c.add(set.getName());
            }
            return c;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Chroot Builder";
        }

        public FormValidation doCheckPackagesFile(@AncestorInPath AbstractProject project, @QueryParameter String value)
                throws IOException, ServletException, InterruptedException {
            List<String> validationList = new LinkedList<String>();
            Boolean warn = false;
            Boolean error = false;
            FilePath workspace = project.getSomeWorkspace();
            for (String file : ChrootUtil.splitFiles(value)) {
                if (workspace == null) {
                    // return here => exactly one warning, iff field has a value
                    return FormValidation.warning("Workspace does not yet exist.");
                }
                FilePath x = new FilePath(workspace, file);
                if (!x.exists()) {
                    warn = true;
                    validationList.add(String.format("File %s does not yet exist.", file));
                } else if (x.isDirectory()) {
                    error = true;
                    validationList.add(String.format("%s is a directory. Enter a file.", file));
                }
            }
            if (error == true) {
                return FormValidation.error(StringUtils.join(validationList.listIterator(), "\n"));
            } else if (warn == true) {
                return FormValidation.warning(StringUtils.join(validationList.listIterator(), "\n"));
            }
            return FormValidation.ok();
        }
    }
}
