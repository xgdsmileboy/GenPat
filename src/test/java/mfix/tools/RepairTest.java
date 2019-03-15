/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.util.Utils;
import mfix.core.locator.D4JManualLocator;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-15
 */
public class RepairTest extends TestCase {

    private String base = Utils.join(Constant.SEP, testbase, "pattern4d4j");
    private String d4jHome = Utils.join(Constant.SEP, Constant.HOME, "projects");

    @Test
    public void test_chart_14() {
        test("chart", 14);
    }


    private void test(String proj, int id) {
        String buggyFile = Utils.join(Constant.SEP, base, proj + "_" + id, "buggy.java");
        String fixedFile = Utils.join(Constant.SEP, base, proj + "_" + id, "fixed.java");
        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(buggyFile, fixedFile);
        D4jSubject subject = new D4jSubject(d4jHome, proj, id);
        D4JManualLocator locator = new D4JManualLocator(subject);


    }

}
