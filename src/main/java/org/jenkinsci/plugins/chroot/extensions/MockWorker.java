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
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.tools.ChrootToolsetProperty;
import org.jenkinsci.plugins.chroot.tools.Repository;

/**
 *
 * @author roman
 */
@Extension
public final class MockWorker extends ChrootWorker {

    @Override
    public FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath rootDir = node.getRootPath();
        ChrootToolset toolset = ChrootToolset.getInstallationByName(tool.getName());
        // get path to tarball
        FilePath default_cfg;
        ChrootToolsetProperty property = tool.getProperties().get(ChrootToolsetProperty.class);
        // take the property into account if it exists
        if (property != null) {

            if (property.getTarball().isAbsolute()) {
                default_cfg = new FilePath(property.getTarball());
            } else {
                default_cfg = rootDir.child(property.getTarball().getPath());
            }

        } else {
            default_cfg = rootDir.child(tool.getName());
        }
        FilePath chrootDir = node.getRootPath().createTempDir(tool.getName(), "");
        FilePath cacheDir = chrootDir.child("cache");
        FilePath baseDir = chrootDir.child("base");
        FilePath cfgDir = chrootDir.child("cfg");
        FilePath resultDir = chrootDir.child("result");
        FilePath system_default_cfg = node.createPath("/etc/mock/default.cfg");

        if (!default_cfg.exists() || default_cfg.lastModified() < system_default_cfg.lastModified() || default_cfg.lastModified() < toolset.getLastModified()) {
            FilePath system_logging_cfg = node.createPath("/etc/mock/logging.ini");
            default_cfg = new FilePath(cfgDir, "default.cfg");

            if (property != null) {
                addRepositories(cfgDir, node.createLauncher(log), log, property.getRepos());
            }

            FilePath logging_cfg = new FilePath(cfgDir, "logging.ini");
            FilePath site_default_cfg = new FilePath(cfgDir, "site-defaults.cfg");

            system_default_cfg.copyTo(default_cfg);
            system_logging_cfg.copyTo(logging_cfg);

            String cfg_content = String.format(
                    "config_opts['basedir'] = '%s'\n"
                    + "config_opts['cache_topdir'] = '%s'\n",
                    baseDir.getRemote(),
                    cacheDir.getRemote());

            site_default_cfg.write(cfg_content, null);
            ArgumentListBuilder cmd = new ArgumentListBuilder();
            cmd.add(getTool())
                    .add("-r").add(default_cfg.getBaseName())
                    .add("--configdir").add(cfgDir.getRemote())
                    .add("--resultdir").add(resultDir.getRemote())
                    .add("--init");
            Launcher launcher = node.createLauncher(log);
            int ret = launcher.launch().cmds(cmd).stdout(log).stderr(log.getLogger()).join();
            if (ret != 0) {
                log.fatalError("Could not setup chroot environment");
                return null;
            }
            cmd.add(getTool())
                    .add("-r").add(default_cfg.getBaseName())
                    .add("--configdir").add(cfgDir.getRemote())
                    .add("--resultdir").add(resultDir.getRemote())
                    .add("--scrub").add("chroot");
            ret = launcher.launch().cmds(cmd).stdout(log).stderr(log.getLogger()).join();
            if (ret != 0) {
                log.fatalError("Could not setup chroot environment");
                return null;
            }
            cfg_content = String.format(
                    "config_opts['cache_topdir'] = '%s'\n",
                    cacheDir.getRemote());
            site_default_cfg.delete();
            site_default_cfg = new FilePath(cfgDir, "site-defaults.cfg.tmpl");
            site_default_cfg.write(cfg_content, null);

        } else {
        }
        return cfgDir;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands, boolean runAsRoot) throws IOException, InterruptedException {
        String userName = super.getUserName(launcher);
        int id = super.getUID(launcher, userName);
        commands = "cd " + build.getWorkspace().getRemote() + "\n" + commands;
        FilePath script = build.getWorkspace().createTextTempFile("chroot", ".sh", commands);
        String userHome = build.getWorkspace().getRemote();
        FilePath cfgDir = tarBall;
        FilePath site_default_cfg_tmpl = cfgDir.child("site-defaults.cfg.tmpl");
        FilePath site_default_cfg = cfgDir.child("site-defaults.cfg");
        FilePath resultDir = build.getWorkspace();
        String cfg_content = String.format(
                "config_opts['basedir'] = '%s'\n",
                "config_opts['plugin_conf']['bind_mount_enable'] = True\n"
                + "config_opts['plugin_conf']['bind_mount_opts']['dirs'].append(('%s', '%s' ))\n"
                + "%s",
                userHome,
                userHome,
                userHome,
                site_default_cfg_tmpl.readToString());
        site_default_cfg.write(cfg_content, null);
        ArgumentListBuilder b = new ArgumentListBuilder().add(getTool())
                .add("-r").add(site_default_cfg.getBaseName())
                .add("--configdir").add(cfgDir.getRemote())
                .add("--resultdir").add(resultDir.getRemote())
                .add("--shell")
                .add(script);

        int exitCode = launcher.launch().cmds(b).stdout(listener).stderr(listener.getLogger()).join();
        script.delete();
        return exitCode == 0;
    }

    @Override
    public boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages, boolean forceInstall) throws IOException, InterruptedException {
        FilePath cfgDir = tarBall;
        FilePath site_default_cfg_tmpl = cfgDir.child("site-defaults.cfg.tmpl");
        FilePath site_default_cfg = cfgDir.child("site-defaults.cfg");
        String userHome = build.getWorkspace().getRemote();

        String cfg_content = String.format(
                "config_opts['basedir'] = '%s'\n"
                + "%s",
                userHome,
                site_default_cfg_tmpl.readToString());
        site_default_cfg.write(cfg_content, null);

        ArgumentListBuilder b = new ArgumentListBuilder().add(getTool())
                .add("--configdir").add(cfgDir.getRemote())
                .add("--install")
                .add(StringUtils.join(packages, " "));
        return launcher.launch().cmds(b).stderr(listener.getLogger()).join() == 0;
    }

    public List<String> getDefaultPackages() {
        return new ImmutableList.Builder<String>().build();
    }

    @Override
    public boolean addRepositories(FilePath tarBall, Launcher launcher, TaskListener log, List<Repository> Repositories) throws IOException, InterruptedException {
        FilePath cfgDir = tarBall;
        FilePath default_cfg = cfgDir.child("default.cfg");
        String content = default_cfg.readToString();
        for (Repository r : Repositories) {
            content += String.format(
                    "[%s]\n"
                    + "name=%s\n"
                    + "mirrorlist=%s\n"
                    + "failovermethod=priority\n", r.getName(), r.getName(), r.getUri());
        }
        default_cfg.write(content, null);
        return true;
    }

    @Override
    public boolean cleanUp(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException {
        FilePath cfgDir = tarBall;
        FilePath site_default_cfg_tmpl = cfgDir.child("site-defaults.cfg.tmpl");
        FilePath site_default_cfg = cfgDir.child("site-defaults.cfg");
        String userHome = build.getWorkspace().getRemote();

        String cfg_content = String.format(
                "config_opts['basedir'] = '%s'\n"
                + "%s",
                userHome,
                site_default_cfg_tmpl.readToString());
        site_default_cfg.write(cfg_content, null);

        ArgumentListBuilder b = new ArgumentListBuilder().add(getTool())
                .add("--configdir").add(cfgDir.getRemote())
                .add("--scrub").add("chroot");
        return launcher.launch().cmds(b).stderr(listener.getLogger()).join() == 0;
    }

    @Override
    public boolean updateRepositories(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException {
        FilePath cfgDir = tarBall;
        FilePath site_default_cfg_tmpl = cfgDir.child("site-defaults.cfg.tmpl");
        FilePath site_default_cfg = cfgDir.child("site-defaults.cfg");
        String userHome = build.getWorkspace().getRemote();

        String cfg_content = String.format(
                "config_opts['basedir'] = '%s'\n"
                + "%s",
                userHome,
                site_default_cfg_tmpl.readToString());
        site_default_cfg.write(cfg_content, null);

        ArgumentListBuilder b = new ArgumentListBuilder().add(getTool())
                .add("--configdir").add(cfgDir.getRemote())
                .add("--update");
        return launcher.launch().cmds(b).stderr(listener.getLogger()).join() == 0;
    }
}
