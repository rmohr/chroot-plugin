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
    private File tarball; //deprecated
    private List<Repository> repositories;
    private String setupArguments;

    public String getSetupArguments() {
        return setupArguments;
    }

    @DataBoundConstructor
    public ChrootToolsetProperty(String setupCommand, String packages, String tarball, String setupArguments, List<Repository> repos) {
        super();
        this.tarball = null;
        this.packages = ChrootUtil.splitPackages(packages);
        this.setupArguments = setupArguments;
        this.setupCommand = setupCommand;
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
              .append(this.setupArguments, other.setupArguments)
              .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.packages)
                .append(this.setupCommand)
                .append(this.packages.toArray(new String[this.packages.size()]))
                .append(this.repositories.toArray(new Repository[this.packages.size()]))
                .append(this.setupArguments)
                .toHashCode();
    }
    
    public String getSetupCommand() {
        return setupCommand;
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
