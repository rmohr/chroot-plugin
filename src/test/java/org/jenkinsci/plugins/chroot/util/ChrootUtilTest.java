/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.util;

import hudson.FilePath;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 * @author rmohr
 */
public class ChrootUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public ChrootUtilTest() {
    }

    @Test
    public void testSplit() {
        String packages = " a b, c ; d,e;f    g,h\ti\nj\r\nk ";
        String[] lst = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"};
        assertEquals(Arrays.asList(lst), ChrootUtil.splitPackages(packages));
    }

    @Test
    public void testDigestFile() throws IOException, InterruptedException {
        FilePath randomFile = new FilePath(folder.newFile());
        randomFile.write("testcontent", null);
        ChrootUtil.saveDigest(randomFile);
        assertThat(ChrootUtil.getDigestFile(randomFile).exists()).isTrue();
        assertThat(ChrootUtil.loadDigest(randomFile)).isEqualTo(randomFile.digest());
        assertThat(ChrootUtil.isFileIntact(randomFile)).isTrue();
        randomFile.write("newcontent", null);
        assertThat(ChrootUtil.loadDigest(randomFile)).isNotEqualTo(randomFile.digest());
        assertThat(ChrootUtil.isFileIntact(randomFile)).isFalse();
        randomFile.delete();
        randomFile.delete();
    }
}
