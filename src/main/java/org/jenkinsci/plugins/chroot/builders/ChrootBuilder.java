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

    @DataBoundConstructor
    public ChrootBuilder(String chrootName, boolean ignoreExit,
            String additionalPackages, String packagesFile, boolean clear,
            String command) {
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
        FilePath tarBall = new FilePath(Computer.currentComputer().getChannel(), installation.getHome());
        FilePath workerTarBall = build.getWorkspace().child(tarBall.getName());
        
        // force environment recreation when clear is selected
        if (this.clear && workerTarBall.exists()){
            workerTarBall.delete();
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
        
        return ignoreExit || installation.getChrootWorker().perform(build, launcher, listener, workerTarBall, this.command);
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
