/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

import mfix.common.util.Constant;
import mfix.core.TestCase;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-19
 */
public class ClusterTest extends TestCase {

    @Test
    public void test_cluster() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        Cluster cluster = new Cluster();
        Set<Pattern> clustered = cluster.cluster(patterns);

        Assert.assertTrue(clustered.size() == 7);
        Assert.assertTrue(clustered.iterator().next().getFrequency() == 7);
    }

}
