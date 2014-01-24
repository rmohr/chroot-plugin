/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.util;

import com.gargoylesoftware.htmlunit.html.Util;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmohr
 */
public class ChrootUtilTest {
    
    public ChrootUtilTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of split method, of class ChrootUtil.
     */
    @Test
    public void testSplit() {
        String packages = " a b, c ; d,e;f    g,h ";
        String[] lst = {"a","b", "c", "d", "e", "f", "g", "h"};
        assertEquals(Arrays.asList(lst), ChrootUtil.splitPackages(packages));
    }
}