/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot;

import hudson.Main;
import hudson.Plugin;
import org.jenkinsci.plugins.chroot.tools.ChrootToolFinder;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;

/**
 *
 * @author roman
 */
public class Chroot extends Plugin {
        @Override
    public void postInitialize() throws Exception
    {
        if (ChrootToolset.isEmpty() && !Main.isUnitTest)
            ChrootToolFinder.configure();
    }
}
