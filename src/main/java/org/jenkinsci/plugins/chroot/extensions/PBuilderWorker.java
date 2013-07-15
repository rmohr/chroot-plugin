/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.extensions;

import com.google.common.collect.ImmutableList;
import hudson.EnvVars;
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
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;
import org.jenkinsci.plugins.chroot.tools.Repository;

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
    
    
    private ArgumentListBuilder defaultArgumentList(FilePath tarBall, String action){
        return new ArgumentListBuilder().add("sudo").add(getTool())
                .add(action)
                .add("--basetgz").add(tarBall.getRemote());
    }

    @Override
    public FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath rootDir = node.getRootPath();
        // get path to tarball
        FilePath tarBall;
        ChrootToolset toolset = ChrootToolset.getInstallationByName(tool.getName());
        ChrootToolsetProperty property = tool.getProperties().get(ChrootToolsetProperty.class);
        tarBall = rootDir.child(getName()).child(tool.getName() + ".tgz");
        // build setup command
        ArgumentListBuilder cmd = defaultArgumentList(tarBall, "--create");
        
        // don't forget to install additional packages
        if (!getDefaultPackages().isEmpty()) {
            cmd.add("--extrapackages").add(StringUtils.join(getDefaultPackages(), " "));
        }
        
        // run setup
        if (!tarBall.exists() || tarBall.lastModified() <= toolset.getLastModified()) {
            tarBall.getParent().mkdirs();
            int ret;
            //make pbuilder less verbose by ignoring stdout
            ret = node.createLauncher(log).launch().cmds(cmd).stderr(log.getLogger()).join();
            if (ret != 0) {
                log.fatalError("Could not setup chroot environment");
                return null;
            }
            
            FilePath script;
            
            // add repositories
            if (property != null) {
                addRepositories(tarBall, node.createLauncher(log), log, property.getRepos());
            }

            // add additional packages
            if (property != null && !property.getPackagesList().isEmpty()) {
                cmd = defaultArgumentList(tarBall, "--update")
                        .add("--extrapackages")
                        .add(StringUtils.join(property.getPackagesList(), " "));
                ret = node.createLauncher(log).launch().cmds(cmd).stderr(log.getLogger()).join();
                if (ret != 0) {
                    log.fatalError("Could not install additional packages.");
                    return null;
                }
            }
            
            // run additional setup command
            if (property != null && !property.getSetupCommand().isEmpty()) {
                String shebang = "#!/usr/bin/env bash\n";
                String command = shebang + "set -e\nset -x verbose\n" + property.getSetupCommand();
                script = rootDir.createTextTempFile("chroot", ".sh", command);
                cmd = defaultArgumentList(tarBall, "--execute")
                        .add("--save-after-exec")
                        .add("--").add(script);
                ret = node.createLauncher(log).launch().cmds(cmd).stdout(log).stderr(log.getLogger()).join();
                script.delete();
                if (ret != 0) {
                    log.fatalError("Post-setup command failed.");
                    return null;
                }
            }
        }
        return tarBall;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands, boolean runAsRoot) throws IOException, InterruptedException {
        String userName = super.getUserName(launcher);
        String groupName = super.getGroupName(launcher, userName);
        String userHome = build.getWorkspace().getRemote();
        int id = super.getUID(launcher, userName);
        int gid = super.getGID(launcher, userName);
        EnvVars environment = build.getEnvironment(listener);
        commands = "cd " + build.getWorkspace().getRemote() + "\n" + commands + "\n";
        commands = "set -e\nset -x verbose\n" + commands;
        commands = String.format("export BUILD_NUMBER=%s\n", environment.get("BUILD_NUMBER")) + commands;
        commands = String.format("export BUILD_TAG=%s\n", environment.get("BUILD_TAG")) + commands;
        FilePath script = build.getWorkspace().createTextTempFile("chroot", ".sh", commands);
        String create_group = String.format("groupadd -g %d %s | :\n", gid, groupName);
        String create_user = String.format("useradd %s -u %d -g %d -m | : \n", userName, id, gid);
        String run_script;  
        if (!runAsRoot){
            run_script = String.format("chmod u+x %s\n sudo -i -u %s bash -- %s\n", script.getRemote(), userName, script.getRemote());
        } else {
            run_script = String.format("chmod u+x %s\n ret=1; bash %s; if [ $? -eq 0 ]; then ret=0; fi;cd %s; chown %s:%s ./ -R; exit $ret\n", script.getRemote(), script.getRemote(),build.getWorkspace().getRemote(), userName, groupName );
        }
        String shebang = "#!/usr/bin/env bash\n";
        String setup_command = shebang + create_group + create_user + run_script;
        FilePath setup_script = build.getWorkspace().createTextTempFile("chroot", ".sh", setup_command);
        ArgumentListBuilder b = new ArgumentListBuilder().add("sudo").add(getTool()).add("--execute")
                .add("--bindmounts").add(userHome)
                .add("--basetgz").add(tarBall.getRemote())
                .add("--").add(setup_script);
        int exitCode = launcher.launch().cmds(b).envs(environment).stdout(listener).stderr(listener.getLogger()).join();
            script.delete();
        setup_script.delete();
        return exitCode == 0;
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages) throws IOException, InterruptedException {
        ArgumentListBuilder b= new ArgumentListBuilder().add("sudo").add(getTool())
                .add("--update")
                .add("--basetgz").add(tarBall.getRemote())
                .add("--extrapackages")
                .add(StringUtils.join(packages, " "));
        return launcher.launch().cmds(b).stderr(listener.getLogger()).join() == 0;
    }
    
    
    public List<String> getDefaultPackages(){
        return new ImmutableList.Builder<String>()
                .add("python-software-properties")
                .add("sudo")
                .add("wget").build();
    }

    @Override
    public boolean addRepositories(FilePath tarBall, Launcher launcher, TaskListener log, List<Repository> repositories) throws IOException, InterruptedException {
        if (repositories.size() > 0) {
            String commands = "";
            for (Repository repo : repositories) {
                commands += repo.setUpCommand();
            }
            FilePath script = tarBall.getParent().createTextTempFile("chroot", ".sh", commands);

            ArgumentListBuilder cmd = defaultArgumentList(tarBall, "--execute")
                    .add("--save-after-exec")
                    .add("--").add(script);
            int ret = launcher.launch().cmds(cmd).stderr(log.getLogger()).join();
            script.delete();
            if (ret != 0) {
                log.fatalError("Could not add custom repositories.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean cleanUp(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException {
        ArgumentListBuilder a = defaultArgumentList(tarBall, "--clean");
        return launcher.launch().cmds(a).stdout(listener).stderr(listener.getLogger()).join() == 0;
    }
}
