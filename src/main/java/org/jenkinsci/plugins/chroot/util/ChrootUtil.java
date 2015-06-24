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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import hudson.FilePath;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author rmohr
 */
public class ChrootUtil {

    public static final String MD5_SUFFIX = ".md5";
    private static final Splitter stringSplitter = Splitter.on(CharMatcher.anyOf(",; \t\n\r")).trimResults().omitEmptyStrings();

    public static List<String> splitPackages(String packages) {
        if (packages != null) {
            return Lists.newArrayList(stringSplitter.split(packages));
        } else {
            return Lists.newArrayList();
        }
    }

    public static List<String> splitFiles(String files) {
        if (files != null) {
            return Lists.newArrayList(stringSplitter.split(files));
        } else {
            return Lists.newArrayList();
        }
    }

    public static FilePath saveDigest(FilePath file) throws IOException, InterruptedException {
        FilePath md5 = getDigestFile(file);
        md5.write(file.digest(), null);
        return md5;
    }

    public static String loadDigest(FilePath file) throws IOException, InterruptedException {
        FilePath md5 = getDigestFile(file);
        if (!md5.exists()) {
            return null;
        }
        return md5.readToString().trim();
    }

    public static FilePath getDigestFile(FilePath file) {
        return new FilePath(file.getParent(), file.getName() + MD5_SUFFIX);
    }

    public static boolean isFileIntact(FilePath file) throws IOException, InterruptedException {
        if (!file.exists()) {
            return false;
        }
        String digest = loadDigest(file);
        if (digest == null) {
            return false;
        }
        if (!file.digest().equals(digest)) {
            return false;
        }
        return true;
    }
}
