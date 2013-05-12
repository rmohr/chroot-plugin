/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.tools;

import hudson.tools.ToolProperty;
import java.util.ArrayList;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 *
 * @author roman
 */
public class ChrootToolsetTest extends HudsonTestCase {
    
    public ChrootToolsetTest() {
    }

    public void testConfigChanged(){
        ToolProperty<ChrootToolset> toolProperty = new ChrootToolsetProperty("", "wget", "test-tool");
        List<ToolProperty<ChrootToolset> > properties = new ArrayList<ToolProperty<ChrootToolset>>();
        properties.add(toolProperty);
        ChrootToolset tool = new ChrootToolset("test-tool", "./", "mock", properties);
        ChrootToolset.DescriptorImpl descriptor = new ChrootToolset.DescriptorImpl();
        long timestamp = tool.getLastModified();
        descriptor.setInstallations(tool);
        assertEquals(timestamp, tool.getLastModified());
        tool = new ChrootToolset("test-tool", "./", "mock", properties);        
        descriptor.setInstallations(tool);
        assertEquals(timestamp, tool.getLastModified());
        tool = new ChrootToolset("test-tool", "./a", "mock", properties);
        descriptor.setInstallations(tool);        
        assertFalse(tool.getLastModified() == timestamp);
        timestamp = tool.getLastModified();
        toolProperty = new ChrootToolsetProperty("", "wget", "test-tool1");
        properties = new ArrayList<ToolProperty<ChrootToolset>>();
        properties.add(toolProperty);
        tool = new ChrootToolset("test-tool", "./a", "mock", properties);
        descriptor.setInstallations(tool);
        assertFalse(tool.getLastModified() == timestamp);
    }
}