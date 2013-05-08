/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.builders;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

/**
 *
 * @author roman
 */
public class PackageBuilder extends Builder {
    
    @Extension
    public static  class DescriptorImpl extends BuildStepDescriptor<Builder>{

        public ListBoxModel doFillChrootEnvironmentItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("pbuilder-cpp-base", "1");
            items.add("pbuilder-cpp-base", "2");
            return items;
        }         
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Package Builder"; //To change body of generated methods, choose Tools | Templates.
        }   
    }
}
