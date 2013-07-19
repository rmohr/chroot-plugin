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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author roman
 */
public class ChrootBuilder extends Builder implements Serializable{

    private String chrootName;
    private boolean ignoreExit;
    private List<String> additionalPackages;
    private String packagesFile;
    private List<String> packagesFromFile;
    private boolean clear;
    private String command;
    private boolean loginAsRoot;

    @DataBoundConstructor
    public ChrootBuilder(String chrootName, boolean ignoreExit,
            String additionalPackages, String packagesFile, boolean clear,
            String command, boolean loginAsRoot) {
        this.loginAsRoot = loginAsRoot;
        this.chrootName = chrootName;
        this.ignoreExit = ignoreExit;
        this.additionalPackages = ChrootUtil.split(additionalPackages);
        this.packagesFile = packagesFile;
        this.clear = clear;
        this.command = command;
        try {
            this.packagesFromFile = ChrootUtil.split(FileUtils.readFileToString(
                    new File(packagesFile)));
        } catch (IOException ex) {
        }
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        ChrootToolset installation = ChrootToolset.getInstallationByName(this.chrootName);
        installation = installation.forNode(Computer.currentComputer().getNode(), listener);
        installation = installation.forEnvironment(env);
        if (installation.getHome() == null) {
            listener.fatalError("Installation of chroot environment failed");
            return false;
        }
        FilePath tarBall = new FilePath(Computer.currentComputer().getChannel(), installation.getHome());
        FilePath workerTarBall = build.getWorkspace().child(this.chrootName).child(tarBall.getName());
        workerTarBall.getParent().mkdirs();
        
        // force environment recreation when clear is selected
        if (this.clear){
            boolean ret = installation.getChrootWorker().cleanUp(build, launcher, listener, workerTarBall);
            if (ret == false) {
            listener.fatalError("Chroot environment cleanup failed");
            return ret || ignoreExit;
            }
        }
        
        if (! workerTarBall.exists() || tarBall.lastModified() > workerTarBall.lastModified()){
            tarBall.copyTo(workerTarBall);
        }
        
        //install extra packages
        List<String> packages = new LinkedList<String>(this.additionalPackages);
        FilePath packageFile = new FilePath(build.getWorkspace(), getPackagesFile());
        if (packageFile.exists() && !packageFile.isDirectory()) {
            String packageFilePackages = packageFile.readToString();
            packages.addAll(ChrootUtil.split(packageFilePackages));
        }
        
        if (!packages.isEmpty()) {
            boolean ret = installation.getChrootWorker().installPackages(build, launcher, listener, workerTarBall, packages);
            if (ret == false) {
                listener.fatalError("Installing additional packages in chroot environment failed.");   
                return ret || ignoreExit;
            }
        }        
        return ignoreExit || installation.getChrootWorker().perform(build, launcher, listener, workerTarBall, this.command, isLoginAsRoot());
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public ListBoxModel doFillChrootNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (ChrootToolset set : ChrootToolset.list()) {
                items.add(set.getName(), set.getName());
            }
            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Chroot Builder";
        }
        
        public FormValidation doCheckPackagesFile(@QueryParameter String value)
                throws IOException, ServletException, InterruptedException {
                if (value.length() != 0) {
                FilePath x = new FilePath(new File(value));
                if (!x.exists()) {
                    return FormValidation.warning("File does not yet exist.");
                } else if (x.isDirectory()) {
                    return FormValidation.error("This is a directory. Enter a file.");
                }
            }
            return FormValidation.ok();
        }        
    }
}
