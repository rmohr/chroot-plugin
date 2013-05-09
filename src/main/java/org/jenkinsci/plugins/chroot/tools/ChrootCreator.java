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
        return ChrootWorker.getByName(toolset.getToolName()).setUp(tool, node, log);
    }
    
    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<ChrootCreator>{

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
        if ( ! super.appliesTo(node)){
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
