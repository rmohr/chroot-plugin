/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.extensions;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;

/**
 *
 * @author roman
 */
@Extension
public class MockWorker extends ChrootWorker {

    @Override
    public FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath rootDir = node.getRootPath();

        // get path to tarball
        FilePath tarBall;
        ChrootToolsetProperty property = tool.getProperties().get(ChrootToolsetProperty.class);
        
        // take the property into account if it exists
        if (property != null) {
            
            if (property.getTarball().isAbsolute()) {
                tarBall = new FilePath(property.getTarball());
            } else {
                tarBall = rootDir.child(property.getTarball().getPath());
            }
            
        } else {
            tarBall = rootDir.child(tool.getName() + ".tgz");
        }
       
        // copy /etc/mock/default.cfg to this location
        FilePath default_cfg = new FilePath(new File("/etc/mock/default.cfg"));
        FilePath this_cfg = new FilePath(rootDir, tool.getName() + ".cfg");
        String cfg_content = String.format(
                "config_opts['basedir'] = '%s'\n"
                + "config_opts['cache_topdir'] = '%s'\n"
                + "config_opts['plugin_conf']['bind_mount_enable'] = True\n"
                + "config_opts['plugin_conf']['bind_mount_opts']['dirs'].append(('%s', '%s' ))\n"
                + "%s", rootDir.getRemote(), 
                rootDir.getRemote(), 
                node.getRootPath().absolutize().getRemote(), 
                node.getRootPath().absolutize().getRemote(),
                default_cfg.readToString());
        this_cfg.write(cfg_content, null);
        return tarBall;
    }


    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public String getTool() {
        return "/usr/bin/mock";
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
