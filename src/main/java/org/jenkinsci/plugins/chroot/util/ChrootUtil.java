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
package org.jenkinsci.plugins.chroot.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author rmohr
 */
public class ChrootUtil {
    
    
    public static List<String> splitPackages(String packages){
        if (packages != null && !packages.isEmpty()){
            return Arrays.asList(packages.trim().split("\\s*,\\s*|\\s*;\\s*|\\s+"));
        } else{
            return new LinkedList<String>();
        }
    }
    
    public static List<String> splitFiles(String files){
        if (files != null && !files.isEmpty()){
            return Arrays.asList(files.trim().split("\\s*,\\s*|\\s*;\\s*"));
        } else{
            return new LinkedList<String>();
        }
    }
    
}
