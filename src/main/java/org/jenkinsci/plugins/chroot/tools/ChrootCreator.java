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
package org.jenkinsci.plugins.chroot.tools;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import java.io.IOException;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jenkinsci.plugins.chroot.extensions.ChrootWorker;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author roman
 */
public class ChrootCreator extends ToolInstaller {

    @DataBoundConstructor
    public ChrootCreator(String label) {
        super(label);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        ChrootToolset toolset = ChrootToolset.getInstallationByName(tool.getName());
        FilePath path = ChrootWorker.getByName(toolset.getToolName()).setUp(tool, node, log);
        if (path == null) {
            throw new IOException("Installation of tool " + tool.getName() + " on node " + node.getDisplayName() + " failed.");
        }
        return path;
    }

    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<ChrootCreator> {

        @Override
        public String getDisplayName() {
            return "Create the chroot environment on demand.";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == ChrootToolset.class;
        }
    }

    @Override
    public boolean appliesTo(Node node) {
        // check if the label applies
        if (!super.appliesTo(node)) {
            return false;
        }
        // check if we are on unix
        Launcher launcher = node.createLauncher(TaskListener.NULL);
        if (!launcher.isUnix()) {
            return false;
        }

        // check if the required tools are installed
        ChrootToolset toolset = ChrootToolset.getInstallationByName(tool.getName());
        FilePath tool = new FilePath(node.getChannel(), toolset.getChrootWorker().getTool());
        try {
            if (!tool.exists()) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            return false;
        }

        //TODO: check for correct permissions
        return true;
    }
}
