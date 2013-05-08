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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author roman
 */
public class ChrootToolsetProperty extends ToolProperty<ChrootToolset> implements Serializable {

    private List<String> packages;
    private String setupCommand;
    private File tarball;

    @DataBoundConstructor
    public ChrootToolsetProperty(String setupCommand, String packages, String tarball) {
        super();
        this.packages = ChrootUtil.split(packages);
        this.setupCommand = setupCommand;
        this.tarball = new File(tarball);
    }

    @Override
    public Class<ChrootToolset> type() {
        return ChrootToolset.class;
    }

    public String getPackages() {
        return StringUtils.join(packages, " ");
    }
    
    public List<String> getPackagesList(){
        return packages;
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
