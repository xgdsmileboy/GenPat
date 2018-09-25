/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.java.D4jSubject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class D4JManualLocatorTest {

    @Test
    public void test_lang_1() {
        D4jSubject subject = new D4jSubject("", "lang", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        Assert.assertTrue(locations.size() == 3);
        boolean contains = false;
        for (Location loc : locations) {
            if ("org.apache.commons.lang3.math.NumberUtils".equals(loc.getRelClazz()) && 471 == loc.getLine()) {
                contains = true;
            }
            Assert.assertNull("Inner class should be null.", loc.getInnerClazz());
            Assert.assertNotNull("Method name should not be null.", loc.getFaultyMethodName());
        }
        Assert.assertTrue("Should contain location : org.apache.commons.lang3.math.NumberUtils.createNumber:471",
                contains);
    }

    @Test
    public void test_time_6() {
        D4jSubject subject = new D4jSubject("", "time", 6);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        Assert.assertTrue(locations.size() == 4);
        boolean contains = false;
        for (Location loc : locations) {
            if ("org.joda.time.chrono.GJChronology".equals(loc.getRelClazz()) && 979 == loc.getLine()
                    && "ImpreciseCutoverField".equals(loc.getInnerClazz()) && "add".equals(loc.getFaultyMethodName())) {
                contains = true;
                break;
            }
        }
        Assert.assertTrue("Should contain location : org.joda.time.chrono.GJChronology$ImpreciseCutoverField.add:979",
                contains);
    }
}
