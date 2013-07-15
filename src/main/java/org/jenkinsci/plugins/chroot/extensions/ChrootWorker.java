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
    
    public abstract List<String> getDefaultPackages();
        
    public abstract boolean cleanUp(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall) throws IOException, InterruptedException;
    
    public abstract FilePath setUp(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException;
    
    public abstract boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, String commands, boolean runAsRoot) throws IOException, InterruptedException;

    public abstract boolean installPackages(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, FilePath tarBall, List<String> packages) throws IOException, InterruptedException; 
    
    public abstract boolean addRepositories(FilePath tarBall, Launcher launcher, TaskListener log, List<Repository> Repositories) throws IOException, InterruptedException;
    
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
    
    protected String getUserName(Launcher launcher) throws IOException, InterruptedException{
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("whoami");
        return run(launcher, builder);
    }
    
    protected int getUID(Launcher launcher, String userName) throws IOException, InterruptedException{
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-u").add(userName);
        Integer id = new Integer(run(launcher, builder));
        return id;
    }
    
    protected int getGID(Launcher launcher, String userName) throws IOException, InterruptedException{
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-g").add(userName);
        Integer id = new Integer(run(launcher, builder));
        return id;
    }
    
    protected String getGroupName(Launcher launcher, String userName) throws IOException, InterruptedException{
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add("id").add("-n").add("-g").add(userName);
        String name = run(launcher, builder);
        return name;
    }
    
    private String run(Launcher launcher, ArgumentListBuilder builder) throws IOException, InterruptedException{
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        launcher.launch().cmds(builder).stdout(stdout).join();
        return stdout.toString().trim();
    }
    
}
