/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.Extension;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author roman
 */
public final class ChrootToolsetProperty extends ToolProperty<ChrootToolset> implements Serializable {

    private List<String> packages;
    private String setupCommand;
    private File tarball;
    private List<Repository> repositories;

    @DataBoundConstructor
    public ChrootToolsetProperty(String setupCommand, String packages, String tarball, List<Repository> repos) {
        super();
        this.packages = ChrootUtil.split(packages);
        this.setupCommand = setupCommand;
        this.tarball = new File(tarball);
        if (repos == null){
            repos = new ArrayList<Repository>();
        }
        this.repositories = repos;
    }

    @Override
    public Class<ChrootToolset> type() {
        return ChrootToolset.class;
    }

    public String getPackages() {
        return StringUtils.join(packages, " ");
    }
    
    public List<Repository> getRepos(){
        return this.repositories;
    }
    
    public List<String> getPackagesList(){
        return packages;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ChrootToolsetProperty == false)  
      {  
        return false;  
      }  
      if (this == obj)  
      {  
         return true;  
      } 
      final ChrootToolsetProperty other = (ChrootToolsetProperty)obj;
      
      return new EqualsBuilder()
              .append(this.packages.toArray(new String[this.packages.size()]),
              other.packages.toArray(new String[other.packages.size()])
              )
              .append(this.repositories.toArray(new Repository[this.packages.size()]),
              other.repositories.toArray(new Repository[other.packages.size()])
              )
              .append(this.setupCommand, other.setupCommand)
              .append(this.tarball, other.tarball)
              .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.packages)
                .append(this.setupCommand)
                .append(this.tarball)
                .append(this.packages.toArray(new String[this.packages.size()]))
                .append(this.repositories.toArray(new Repository[this.packages.size()]))
                .toHashCode();
    }
    
    public String getSetupCommand() {
        return setupCommand;
    }

    public File getTarball() {
        return tarball;
    } 

    @Extension
    public static final class ChrootToolsetPropertyDescriptor extends ToolPropertyDescriptor {

        public ChrootToolsetPropertyDescriptor() {
            super(ChrootToolsetProperty.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Advanced configuration";
        }   

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            boolean x = super.configure(req, json);
            save();
            return x;
        }
        
        
    }
}
