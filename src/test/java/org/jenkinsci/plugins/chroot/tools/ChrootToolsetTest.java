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
        ToolProperty<ChrootToolset> toolProperty = new ChrootToolsetProperty("", "wget", "test-tool","", null);
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
        toolProperty = new ChrootToolsetProperty("", "wget", "test-tool1","", null);
        properties = new ArrayList<ToolProperty<ChrootToolset>>();
        properties.add(toolProperty);
        tool = new ChrootToolset("test-tool", "./a", "mock", properties);
        descriptor.setInstallations(tool);
        assertFalse(tool.getLastModified() == timestamp);
    }
}