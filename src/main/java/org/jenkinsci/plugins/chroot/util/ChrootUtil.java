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
    
    
    public static List<String> split(String packages){
        if (!packages.isEmpty()){
            return Arrays.asList(packages.split("\\s+"));
        } else{
            return new LinkedList<String>();
        }
    }
    
}
