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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jenkinsci.plugins.chroot.extensions.ChrootWorker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author roman
 */
public final class ChrootToolset extends ToolInstallation implements EnvironmentSpecific<ChrootToolset>,
        NodeSpecific<ChrootToolset> {
    private String toolName;
    private long lastModified;
    
    @DataBoundConstructor
    public ChrootToolset(String name, String home, String toolName, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
        this.toolName = toolName;
        lastModified = System.currentTimeMillis();
    }

    public String getToolName() {
        return toolName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
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

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ChrootToolset == false)  
      {  
        return false;  
      }  
      if (this == obj)  
      {  
         return true;  
      } 
      final ChrootToolset other = (ChrootToolset)obj;
      ChrootToolsetProperty p_this = this.getProperties().get(ChrootToolsetProperty.class);
      ChrootToolsetProperty p_other = other.getProperties().get(ChrootToolsetProperty.class);

      return new EqualsBuilder()
              .append(this.getName(), other.getName())
              .append(this.getHome(), other.getHome())
              .append(this.getToolName(), other.getToolName())
              .append(p_this, p_other)
              .isEquals();
    }

    @Override
    public int hashCode() {
        ChrootToolsetProperty p_this = this.getProperties().get(ChrootToolsetProperty.class);
        return new HashCodeBuilder()
                .append(this.getName())
                .append(this.getHome())
                .append(this.getToolName())
                .append(p_this)
                .toHashCode();
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
         private volatile ChrootToolset[] installations;
        
        public ListBoxModel doFillToolNameItems() {
            ListBoxModel items = new ListBoxModel();
            for(ChrootWorker w : ChrootWorker.all()){
            items.add(w.getName());
            }
            return items;
        }
        
        public DescriptorImpl() {
            this.installations = new ChrootToolset[0];
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "Chroot Environment";
        }

        @Override
        public void setInstallations(ChrootToolset... installations) {
            
            List<ChrootToolset> old_installations = Arrays.asList(this.installations);
            for (ChrootToolset tool: installations){
                int index = old_installations.indexOf(tool);
                if (index != -1){
                 tool.setLastModified(old_installations.get(index).getLastModified());
                }
            }
            
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
