/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.junit.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-26
 */
public class TermFrequencyTest extends TestCase {

    @Test
    public void test() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);
        for (Pattern pattern : patterns) {
            System.out.println(pattern.formalForm());
        }
    }

    String base = Utils.join(Constant.SEP, Constant.RES_DIR, "d4j-info", "buggy_fix");


    @Test
    public void test_chart_1() {
        test("chart", 1);
    }

    @Test
    public void test_math_1() {
        test("math", 1);
    }

    @Test
    public void test_chart_4() {
        test("chart", 4);
    }

    @Test
    public void test_chart_6() {
        test("chart", 6);
    }


    private List<Pair<String, String>> buildFilePairs(String d4jproj, int id) {
        List<Pair<String, String>> list = new LinkedList<>();
        String bhome = "buggy/" + d4jproj + "/" + d4jproj + "_" + id + "_buggy";
        String fhome = "fixed/" + d4jproj + "/" + d4jproj + "_" + id + "_fixed";
        List<String> files = JavaFile.ergodic(Utils.join(Constant.SEP, base, bhome), new LinkedList<>());
        File tmpFile;
        for (String f : files) {
            tmpFile = new File(f.replace(bhome, fhome));
            if (tmpFile.exists()) {
                list.add(new Pair<>(f, tmpFile.getAbsolutePath()));
            }
        }
        return list;
    }

    public void test(String d4jproj, int id) {
        List<Pair<String, String>> pairs = buildFilePairs(d4jproj, id);

        for (Pair<String, String> pair : pairs) {
            String srcFile = pair.getFirst();
            String tarFile = pair.getSecond();

            PatternExtractor extractor = new PatternExtractor();
            Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);
            for (Pattern p : patterns) {
//                for (Modification m : p.getAllModifications()) {
//                    System.out.println(m);
//                }
                for (Node node : p.getConsideredNodes()) {
                    System.out.println(node);
                }
                System.out.println(p.formalForm());
                for (String s : p.getKeywords()) {
                    System.out.println(s);
                }
            }

        }
    }


}
