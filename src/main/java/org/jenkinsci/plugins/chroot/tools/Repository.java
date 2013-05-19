/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author rmohr
 */
public final class Repository extends AbstractDescribableImpl<Repository> {

    String uri;
    String keyfile;

    @DataBoundConstructor
    public Repository(String uri, String keyfile) {
        this.uri = uri;
        this.keyfile = keyfile;
    }

    public String getUri() {
        return uri;
    }

    public String getKeyfile() {
        return keyfile;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Repository> {

        public String getDisplayName() {
            return "Repository";
        }
    }
}
