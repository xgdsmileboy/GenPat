/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.diff;

import mfix.common.util.Constant;
import mfix.core.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class DiffTest extends TestCase {

    @Test
    public void test_extractFileDiff() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";
        List<Diff> diffs = Diff.extractFileDiff(srcFile, tarFile, AstDiff.class);
        Assert.assertTrue(diffs.size() == 7);
        for(int i = 0; i < 6; i++) {
            Assert.assertTrue(diffs.get(0).getMiniDiff().size() == 3);
        }

    }

    @Test
    public void test_extractFileDiff2() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";
        List<Diff> diffs = Diff.extractFileDiff(srcFile, tarFile, TextDiff.class);
        Assert.assertTrue(diffs.size() == 7);
    }
}
