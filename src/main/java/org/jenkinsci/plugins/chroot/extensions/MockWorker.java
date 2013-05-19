/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.extensions;

import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.util.List;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;

/**
 *
 * @author roman
 */
@Extension
public final class MockWorker extends ChrootWorker {

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
        FilePath chrootDir = node.getRootPath().createTempDir(tool.getName(), "");
        FilePath cacheDir = chrootDir.child("cache");
        FilePath buildDir = chrootDir.child("build");
        FilePath resultDir = chrootDir.child("result");
        
        if (!tarBall.exists()) {
            // copy /etc/mock/default.cfg to this location
            FilePath system_default_cfg = node.createPath("/etc/mock/default.cfg");
            FilePath system_logging_cfg = node.createPath("/etc/mock/logging.ini");
            FilePath default_cfg = new FilePath(chrootDir, tool.getName() + ".cfg");
            FilePath logging_cfg = new FilePath(chrootDir, "logging.ini");            
            FilePath site_default_cfg = new FilePath(chrootDir,"site-defaults.cfg");
            
            system_default_cfg.copyTo(default_cfg);
            system_logging_cfg.copyTo(logging_cfg);
            
            String cfg_content = String.format(
                    "config_opts['basedir'] = '%s'\n"
                    + "config_opts['cache_topdir'] = '%s'\n", 
                    buildDir.getRemote(),
                    cacheDir.getRemote());
            
            site_default_cfg.write(cfg_content, null);
            ArgumentListBuilder cmd = new ArgumentListBuilder();
            cmd.add(getTool())
                    .add("-r").add(default_cfg.getBaseName())
                    .add("--configdir").add(chrootDir.getRemote())
                    .add("--resultdir").add(resultDir.getRemote())
                    .add("--init");
            Launcher launcher = node.createLauncher(log);
            int ret = launcher.launch().cmds(cmd).stdout(log).stderr(log.getLogger()).join();
        }
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
        String userName = super.getUserName(launcher);
        int id = super.getUID(launcher, userName);
        commands = "cd " + build.getWorkspace().getRemote() + "\n" + commands;
        FilePath script = build.getWorkspace().createTextTempFile("chroot", ".sh", commands);
  
//            String cfg_content = String.format(
//                    "config_opts['basedir'] = '%s'\n"
//                    + "config_opts['cache_topdir'] = '%s'\n"
//                    + "config_opts['plugin_conf']['bind_mount_enable'] = True\n"
//                    + "config_opts['plugin_conf']['bind_mount_opts']['dirs'].append(('%s', '%s' ))\n"
//                    + "%s", rootDir.getRemote(),
//                    rootDir.getRemote(),
//                    node.getRootPath().absolutize().getRemote(),
//                    node.getRootPath().absolutize().getRemote(),
//                    default_cfg.readToString());        
        
        
        ArgumentListBuilder b = new ArgumentListBuilder().add(getTool()).add(script);
        
        int exitCode = launcher.launch().cmds(b).stdout(listener).stderr(listener.getLogger()).join();
        script.delete();
        return exitCode == 0;
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public List<String> getDefaultPackages() {
        return new ImmutableList.Builder<String>().build();
    }

}
