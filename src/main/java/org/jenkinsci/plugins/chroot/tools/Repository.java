/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author rmohr
 */
public final class Repository extends AbstractDescribableImpl<Repository> {

    String uri;
    String keyfile;
    String name;

    @DataBoundConstructor
    public Repository(String uri, String keyfile, String name) {
        this.uri = uri.trim();
        this.keyfile = keyfile.trim();
        this.name = name.trim();
        
    }

    public String getUri() {
        return uri;
    }

    public String getKeyfile() {
        return keyfile;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.keyfile).append(this.uri).append(this.name).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Repository == false)  
      {  
        return false;  
      }  
      if (this == obj)  
      {  
         return true;  
      } 
      final Repository other = (Repository)obj;
      
      return new EqualsBuilder()
              .append(this.name, other.name)
              .append(this.uri, other.uri)
              .append(this.keyfile, other.keyfile)
              .isEquals();
    }
    
    public String setUpCommand() {
        String commands = "";
        if (keyfile != null && keyfile.length() > 0) {
            commands = "wget -O - " + keyfile + " | apt-key add -\n";
        }
        if (!uri.startsWith("ppa")) {
            commands += "echo \"" + uri + "\" > /etc/apt/sources.list.d/" + name + ".list\n";
        } else {
            commands += "add-apt-repository "  + uri  + "\n";
        }
        return commands;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Repository> {

        public String getDisplayName() {
            return "Repository";
        }
    }
}
