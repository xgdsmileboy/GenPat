/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.util.Utils;
import org.junit.Test;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-03-20
 */
public class SBFLocatorTest {

    private String d4jHome = Utils.join(Constant.SEP, Constant.HOME, "projects");

    @Test
    public void test() {
        D4jSubject subject = new D4jSubject(d4jHome, "lang", 33);
        SBFLocator sbfLocator = new SBFLocator(subject);
        List<Location> locations = sbfLocator.getLocations(100);
        for (Location l : locations) {
            System.out.println(l);
        }
    }

    @Test
    public void test_chart() {
        D4jSubject subject = new D4jSubject(d4jHome, "chart", 1);
        SBFLocator sbfLocator = new SBFLocator(subject);
        List<Location> locations = sbfLocator.getLocations(100);
        for (Location l : locations) {
            System.out.println(l);
        }
    }

}
