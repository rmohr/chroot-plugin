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
package org.jenkinsci.plugins.chroot.extensions;

import com.google.common.base.Strings;
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
import hudson.util.QuotedStringTokenizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;
import org.jenkinsci.plugins.chroot.tools.Repository;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;

/**
 *
 * @author rmohr
 */
@Extension
public final class PBuilderWorker extends ChrootWorker {

    private static final Logger logger = Logger.getLogger("jenkins.plugins.chroot.extensions.PBuilderWorker");

    @Override
    public String getName() {
        return "pbuilder";
    }

    @Override
    public String getTool() {
        return "/usr/sbin/pbuilder";
    }

    /* Build a suitable command line for starting the tool */
    private ArgumentListBuilder makeCommand(FilePath tarBall, String action, ArgumentListBuilder extraArgs) {
        ArgumentListBuilder cmd = new ArgumentListBuilder()
                .add("sudo").add(getTool())
                .add(action);
        if (tarBall != null) {
            cmd.add("--basetgz").add(tarBall.getRemote());
        }
        if (extraArgs != null) {
            cmd.add(extraArgs);
        }
        return cmd;
    }

    /* Launch pbuilder with specified action, logging only stderr output on build console log */
    private boolean doLaunch_LogStderr(Launcher launcher, TaskListener log, FilePath tarBall, String action, ArgumentListBuilder extraArgs) throws IOException, InterruptedException {
        return launcher.launch().cmds(makeCommand(tarBall, action, extraArgs)).stderr(log.getLogger()).join() == 0;
    }

    /* Launch pbuilder with specified action, logging both stdout and stderr output on build console log */
    private boolean doLaunch_LogStdout(Launcher launcher, TaskListener log, FilePath tarBall, String action, ArgumentListBuilder extraArgs) throws IOException, InterruptedException {
        // .stdout with a TaskListener collects both stdout and stderr
        return launcher.launch().cmds(makeCommand(tarBall, action, extraArgs)).stdout(log).join() == 0;
    }

    private boolean doSetUp(FilePath tarBall, List<String> packages, ChrootToolsetProperty property, Launcher launcher, TaskListener log) throws IOException, InterruptedException {
        // setup "extra" arguments
        ArgumentListBuilder args = new ArgumentListBuilder();

        // don't forget to install additional packages
        if (!getDefaultPackages().isEmpty()) {
            args.add("--extrapackages").add(StringUtils.join(packages, " "));
        }

        if (property != null && !Strings.isNullOrEmpty(property.getSetupArguments())) {
            args.add(QuotedStringTokenizer.tokenize(property.getSetupArguments()));
        }
        //make pbuilder less verbose by ignoring stdout
        return doLaunch_LogStderr(launcher, log, tarBall, "--create", args);
    }

    @Override
    public FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath rootDir = node.getRootPath();
        // get path to tarball
        FilePath tarBall;
        ChrootToolset toolset = ChrootToolset.getInstallationByName(tool.getName());
        ChrootToolsetProperty property = tool.getProperties().get(ChrootToolsetProperty.class);
        tarBall = rootDir.child(getName()).child(tool.getName() + ".tgz");

        // run setup
        if (!tarBall.exists() || !ChrootUtil.isFileIntact(tarBall) || tarBall.lastModified() <= toolset.getLastModified()) {

            ChrootUtil.getDigestFile(tarBall).delete();
            tarBall.delete();

            tarBall.getParent().mkdirs();

            if (!doSetUp(tarBall, getDefaultPackages(), property, node.createLauncher(log), log)) {
                if (!doSetUp(tarBall, getFallbackPackages(), property, node.createLauncher(log), log)) {
                    log.fatalError("Could not setup chroot environment");
                    return null;
                }
            }
            FilePath script;

            // add repositories
            if (property != null) {
                addRepositories(tarBall, node.createLauncher(log), log, property.getRepos());
            }

            // add additional packages
            if (property != null && !property.getPackagesList().isEmpty()) {
                ArgumentListBuilder extraPkg = new ArgumentListBuilder()
                        .add("--extrapackages")
                        .add(StringUtils.join(property.getPackagesList(), " "));
                if (!doLaunch_LogStderr(node.createLauncher(log), log, tarBall, "--update", extraPkg)) {
                    log.fatalError("Could not install additional packages.");
                    return null;
                }
            }

            // run additional setup command
            if (property != null && !property.getSetupCommand().isEmpty()) {
                String shebang = "#!/usr/bin/env bash\n";
                String command = shebang + "set -e\nset -x verbose\n" + property.getSetupCommand();
                script = rootDir.createTextTempFile("chroot", ".sh", command);
                ArgumentListBuilder args = new ArgumentListBuilder()
                        .add("--save-after-exec")
                        .add("--").add(script);
                boolean ok = doLaunch_LogStdout(node.createLauncher(log), log, tarBall, "--execute", args);
                script.delete();
                if (!ok) {
                    log.fatalError("Post-setup command failed.");
                    return null;
                }
            }
        }
        ChrootUtil.saveDigest(tarBall);
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
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            commands = String.format("if [ -z ${%s} ]; then export %s=\"%s\"; fi;\n", entry.getKey(), entry.getKey(), entry.getValue()) + commands;
        }
        FilePath script = build.getWorkspace().createTextTempFile("chroot", ".sh", commands);
        String create_group = String.format("groupadd -g %d %s | :\n", gid, groupName);
        String create_user = String.format("useradd %s -u %d -g %d -m | : \n", userName, id, gid);
        String run_script;
        String sudoUser = userName;
        if (runAsRoot) {
            sudoUser = "root";
        }

        run_script = String.format("chmod u+x %s\n ret=1; sudo -i -u %s bash -- %s; if [ $? -eq 0 ]; then ret=0; fi;cd %s; chown %s:%s ./ -R; exit $ret\n", script.getRemote(), sudoUser, script.getRemote(), build.getWorkspace().getRemote(), userName, groupName);

        String shebang = "#!/usr/bin/env bash\n";
        String setup_command = shebang + create_group + create_user + run_script;
        FilePath setup_script = build.getWorkspace().createTextTempFile("chroot", ".sh", setup_command);
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("--bindmounts").add(userHome)
                .add("--").add(setup_script);
        boolean exitOk = doLaunch_LogStdout(launcher, listener, tarBall, "--execute", args);
        script.delete();
        setup_script.delete();
        return exitOk;
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages, boolean forceInstall) throws IOException, InterruptedException {
        ArgumentListBuilder extraPkg = new ArgumentListBuilder()
                .add("--extrapackages")
                .add(StringUtils.join(packages, " "));
        if (forceInstall) {
            extraPkg.add("--allow-untrusted");
        }
        return doLaunch_LogStderr(launcher, listener, tarBall, "--update", extraPkg);
    }

    public List<String> getDefaultPackages() {
        return new ImmutableList.Builder<String>()
                .add("software-properties-common")
                .add("python3-software-properties") // fix for ubunt 12.04 to select the fallback packages
                .add("sudo")
                .add("wget").build();
    }

    // really ugly quickfix for https://github.com/rmohr/chroot-plugin/pull/2
    public List<String> getFallbackPackages() {
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

            ArgumentListBuilder args = new ArgumentListBuilder()
                    .add("--save-after-exec")
                    .add("--").add(script);
            boolean ok = doLaunch_LogStderr(launcher, log, tarBall, "--execute", args);
            script.delete();
            if (!ok) {
                log.fatalError("Could not add custom repositories.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean cleanUp(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException {
        return doLaunch_LogStderr(launcher, listener, tarBall, "--clean", null);
    }

    @Override
    public boolean updateRepositories(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException {
        return doLaunch_LogStderr(launcher, listener, tarBall, "--update", null);
    }

    @Override
    public boolean healthCheck(Launcher launcher) {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ArgumentListBuilder cmd = makeCommand(null, "--help", null);
        try {
            /* This function will need to parse stdout, so we don't use doLaunch_X here */
            launcher.launch().cmds(cmd).stderr(stderr).stdout(stdout).join();
            if (stdout.toString().contains("--basetgz")) {
                return true;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        logger.log(Level.SEVERE, stderr.toString());
        return false;
    }
}
