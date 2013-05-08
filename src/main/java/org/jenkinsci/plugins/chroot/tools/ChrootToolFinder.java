/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.model.Computer;
import hudson.tools.ToolProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import jenkins.model.Jenkins;

/**
 *
 * @author roman
 */
public class ChrootToolFinder {
    public static List<ChrootToolset> configure() throws IOException, InterruptedException {
        // Get the installations
        List<ChrootToolset> installations = getInstallations();
        // Set the installations
        Jenkins.getInstance().getDescriptorByType(ChrootToolset.DescriptorImpl.class)
                .setInstallations(installations.toArray(new ChrootToolset[installations.size()]));
        // Return the installations
        return installations;
    }
        
    private static LinkedList<List<String>> getCandidates() {
        LinkedList<List<String>> candidates = new LinkedList<List<String>>();
        List<String> debian = new LinkedList<String>();
        debian.add("debian-derivative");
        debian.add("pbuilder");
        debian.add("apt-get");
        debian.add("pbuilder");

        List<String> fedora = new LinkedList<String>();
        fedora.add("fedora-derivative");
        fedora.add("mock");
        fedora.add("yum");
        fedora.add("mock");

        candidates.add(debian);
        candidates.add(fedora);
        return candidates;
    }     
    
    private static List<ChrootToolset> getInstallations() throws InterruptedException {
        
        LinkedList<List<String>> candidates = getCandidates();
        
        List<ChrootToolset> installations = new LinkedList<ChrootToolset>();
        for (List<String> properties : candidates) {
            try {
                List<String> command = new ArrayList<String>();
                command.add("command");
                command.add("-v");
                command.add(properties.get(1));
                Process proc = Runtime.getRuntime().exec(command.toArray(new String[0]));
                proc.waitFor();
                BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String location;
                if ((location = read.readLine()) != null) {
                    installations.add(new ChrootToolset(properties.get(0) + "-" + properties.get(1), "", properties.get(1), Collections.<ToolProperty<?>>emptyList()));
                }
            } catch (IOException ex) {
                continue;
            }
        }

        return installations;
    }    
}
