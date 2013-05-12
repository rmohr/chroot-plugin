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
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;

/**
 *
 * @author rmohr
 */

@Extension
public final class PBuilderWorker extends ChrootWorker  {

    @Override
    public String getName() {
        return "pbuilder";
    }

    @Override
    public String getTool() {
        return "/usr/sbin/pbuilder";
    }

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

        // build setup command
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("sudo").add(getTool()).add("--create")
                .add("--basetgz").add(tarBall.getRemote())
                .add("--buildplace").add(tarBall.getParent().getRemote());
        
        // don't forget to install additional packages
        if (property != null){
            List<String> packages = property.getPackagesList();
            if (!packages.isEmpty()){
                cmd.add("--extrapackages").add(StringUtils.join(packages, " "));
            }
        }
        
        // run setup
        if (!tarBall.exists()) {
            //make pbuilder less verbose by ignoring stdout
            int ret = node.createLauncher(log).launch().cmds(cmd).stderr(log.getLogger()).join();
            if (ret != 0) {
                log.fatalError("Could not setup chroot environment");
            }
            // run additional setup command
            if (property != null && !property.getSetupCommand().isEmpty()) {
                FilePath script = rootDir.createTextTempFile("chroot", ".sh", property.getSetupCommand());
                cmd = new ArgumentListBuilder();
                cmd.add("sudo").add(getTool()).add("--execute")
                        .add("--basetgz").add(tarBall.getRemote())
                        .add("--buildplace").add(tarBall.getParent().getRemote())
                        .add("--save-after-exec").add("--").add(script);
                ret = node.createLauncher(log).launch().cmds(cmd).stdout(log).stderr(log.getLogger()).join();
                script.delete();
                if (ret != 0) {
                    log.fatalError("Post-setup command failed.");
                }
            }
        }
        return tarBall;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands) throws IOException, InterruptedException {
        String userName = super.getUserName(launcher);
        int id = super.getUID(launcher, userName);
        commands = "cd " + build.getWorkspace().getRemote() + "\n" + commands;
        FilePath script = build.getWorkspace().createTextTempFile("chroot", ".sh", commands);
        ArgumentListBuilder b = new ArgumentListBuilder().add("sudo").add(getTool()).add("--execute")
                .add("--bindmounts").add(build.getWorkspace().getRemote())
                .add("--basetgz").add(tarBall.getRemote())
                .add("--buildplace").add(build.getWorkspace())
                .add("--").add(script);
        int exitCode = launcher.launch().cmds(b).stdout(listener).stderr(listener.getLogger()).join();
        script.delete();
        return exitCode == 0;
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages) throws IOException, InterruptedException {
        ArgumentListBuilder b= new ArgumentListBuilder().add("sudo").add(getTool())
                .add("--update")
                .add("--basetgz").add(tarBall.getRemote())
                .add("--buildplace").add(build.getWorkspace())
                .add("--extrapackages")
                .add(StringUtils.join(packages, " "));
        return launcher.launch().cmds(b).stdout(listener).stderr(listener.getLogger()).join() == 0;
    }
}
