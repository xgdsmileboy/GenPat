/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.util.Utils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-21
 */
public class PatternTest extends TestCase {

    @Test
    public void test_pattern_serialization() throws IOException, ClassNotFoundException {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        Assert.assertTrue(patterns.size() == 1);

        Pattern p = patterns.iterator().next();

        Set<String> keywords = p.getKeywords();
        Set<String> tarKeys = p.getTargetKeywords();
        StringBuffer buffer = p.formalForm();

        Utils.serialize(p, "/tmp/test.pattern");
        p = (Pattern) Utils.deserialize("/tmp/test.pattern");
        FileUtils.forceDelete(new File("/tmp/test.pattern"));

        Set<String> keywords2 = p.getKeywords();
        Set<String> tarKeys2 = p.getTargetKeywords();
        StringBuffer buffer2 = p.formalForm();

        Assert.assertTrue(Utils.safeCollectionEqual(keywords, keywords2));
        Assert.assertTrue(Utils.safeCollectionEqual(tarKeys, tarKeys2));
        Assert.assertTrue(Utils.safeBufferEqual(buffer, buffer2));

    }

    @Test
    public void test_pattern_delete_expr() {
        String srcFile = testbase + Constant.SEP  + "buggy_del_initializer.java";
        String tarFile = testbase + Constant.SEP + "fixed_del_initializer.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        for (Pattern pattern : patterns) {
            System.out.println(pattern.formalForm());
            System.out.println(pattern.getPatternVector());
            for (String m : pattern.formalModifications()) {
                System.out.println(m);
            }
        }

    }

//    @Test
//    public void test() throws Exception {
//        Set<Integer> ids = new HashSet<>(Arrays.asList(16, 75, 18, 72, 54, 14, 6));
//        Set<Pattern> patterns = new HashSet<>();
//        Pattern p;
//        String file;
//        String base = System.getProperty("user.dir") + "/tmp/";
////        for (int i = 1; i < 100; i++) {
//        for (Integer i : ids) {
//            file = base + i + ".pattern";
//            p = (Pattern) Utils.deserialize(file);
//            p.setPatternName(file);
//            patterns.add(p);
//        }
//        for (Pattern pt : patterns) {
//            for (Modification m : pt.getAllModifications()) {
//                System.out.println(m);
//            }
//            System.out.println(pt.formalForm());
//        }
//        ClusterImpl cluster = new ClusterImpl();
//        Set<Group> groups = cluster.cluster(patterns);
//
//        int count = 0;
//        for (Group g : groups) {
//            System.out.println(g.getRepresentPattern().formalForm());
//            int size = g.getIsomorphicPatternPath().size() + 1;
//            count += size;
//        }
//        System.out.println(groups.size() + " | " + count);
//    }

}
