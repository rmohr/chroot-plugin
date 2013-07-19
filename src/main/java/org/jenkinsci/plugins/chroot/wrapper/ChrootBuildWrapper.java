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
