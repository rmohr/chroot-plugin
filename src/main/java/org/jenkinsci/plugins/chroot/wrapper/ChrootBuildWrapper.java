/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.wrapper;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import java.io.IOException;

/**
 *
 * @author rmohr
 */
public class ChrootBuildWrapper extends BuildWrapper {

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        
        return super.setUp(build, launcher, listener);
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return super.decorateLauncher(build, launcher, listener);
    }
    
    
    
    
    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public ListBoxModel doFillChrootEnvironmentItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("pbuilder-cpp-base", "1");
            items.add("pbuilder-cpp-base", "2");
            return items;
        }   
        
        @Override
        public boolean isApplicable(AbstractProject<?, ?> job) {
            return FreeStyleProject.class.isAssignableFrom(job.getClass());
        }

        @Override
        public String getDisplayName() {
            return "Chroot Environment";
        }
        
    }
    
}
