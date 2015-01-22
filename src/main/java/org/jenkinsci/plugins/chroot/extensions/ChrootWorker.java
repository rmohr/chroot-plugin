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

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.chroot.tools.Repository;

/**
 *
 * @author roman
 */
public abstract class ChrootWorker implements ExtensionPoint {

    public abstract String getName();

    public abstract String getTool();

    public abstract boolean healthCheck(Launcher launcher);

    public abstract List<String> getDefaultPackages();

    public abstract boolean cleanUp(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException;

    public abstract FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException;

    public abstract boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands, boolean runAsRoot) throws IOException, InterruptedException;

    public abstract boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages, boolean forceInstall) throws IOException, InterruptedException;

    public abstract boolean addRepositories(FilePath tarBall, Launcher launcher, TaskListener log, List<Repository> Repositories) throws IOException, InterruptedException;

    public abstract boolean updateRepositories(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException;

    public static ExtensionList<ChrootWorker> all() {
        return Jenkins.getInstance().getExtensionList(ChrootWorker.class);
    }

    public static ChrootWorker getByName(String name) {
        for (ChrootWorker w : ChrootWorker.all()) {
            if (w.getName().equals(name)) {
                return w;
            }
        }
        //TODO: make sure that this line is never read
        assert false;
        return null;
    }

    protected String getUserName(Launcher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("whoami");
        return run(launcher, builder);
    }

    protected int getUID(Launcher launcher, String userName) throws IOException, InterruptedException {
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-u").add(userName);
        Integer id = new Integer(run(launcher, builder));
        return id;
    }

    protected int getGID(Launcher launcher, String userName) throws IOException, InterruptedException {
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-g").add(userName);
        Integer id = new Integer(run(launcher, builder));
        return id;
    }

    protected String getGroupName(Launcher launcher, String userName) throws IOException, InterruptedException {
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-n").add("-g").add(userName);
        String name = run(launcher, builder);
        return name;
    }

    protected String run(Launcher launcher, ArgumentListBuilder builder) throws IOException, InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        launcher.launch().cmds(builder).stdout(stdout).join();
        return stdout.toString().trim();
    }
}
