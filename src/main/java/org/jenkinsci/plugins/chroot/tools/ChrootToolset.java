/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import org.jenkinsci.plugins.chroot.extensions.ChrootWorker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author roman
 */
public class ChrootToolset extends ToolInstallation implements EnvironmentSpecific<ChrootToolset>,
        NodeSpecific<ChrootToolset> {
    private String toolName;
    
    @DataBoundConstructor
    public ChrootToolset(String name, String home, String toolName, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
    
    public ChrootWorker getChrootWorker(){
        return ChrootWorker.getByName(this.getToolName());
    }

    public ChrootToolset forEnvironment(EnvVars environment) {
        return new ChrootToolset(getName(), environment.expand(getHome()), getToolName(), getProperties().toList());
    }

    public ChrootToolset forNode(Node node, TaskListener listener) throws IOException, InterruptedException {
        return new ChrootToolset(getName(), translateFor(node, listener), getToolName(), getProperties().toList());
    }
    
    public static ChrootToolset getInstallationByName(String name) {
        if (name == null) return null;
        for (ChrootToolset installation : list()) {
            if (name.equals(installation.getName())) {
                return installation;
            }
        }
        return null;
    }
    
    public static boolean isEmpty()
    {
        return list().length == 0;
    }

    public static ChrootToolset[] list()
    {
        return ToolInstallation.all().get(DescriptorImpl.class).getInstallations();
    }

    @Extension
    public static final class DescriptorImpl extends ToolDescriptor<ChrootToolset> {

        @CopyOnWrite
        private volatile ChrootToolset[] installations = new ChrootToolset[0];
        
        public ListBoxModel doFillToolNameItems() {
            ListBoxModel items = new ListBoxModel();
            for(ChrootWorker w : ChrootWorker.all()){
            items.add(w.getName());
            }
            return items;
        }
        
        public DescriptorImpl() {
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "Chroot Environment";
        }

        @Override
        public void setInstallations(ChrootToolset... installations) {
            this.installations = installations;
            save();
        }

        @Override
        public ChrootToolset[] getInstallations() {
            return this.installations;
        }
        
        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new ChrootCreator(null));
        }  
        
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please enter a unique descriptive name.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckHome(@QueryParameter String value)
                throws IOException, ServletException, InterruptedException {
            if (value.length() != 0) {
                FilePath x = new FilePath(new File(value));
                if (!x.exists()) {
                    return FormValidation.error("Directory does not exist.");
                } else if (!x.isDirectory()){
                    return FormValidation.error("This is a file. Enter a directory.");
                }
            }
            return FormValidation.ok();
        }
    }
}
