/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: Jiajun
 * @date: 2018/12/15
 */
public class PairTest {

    @Test
    public void test_store() {
        Pair<Integer, Integer> pair1 = new Pair<>(1, 0);
        Assert.assertTrue(pair1.getFirst() == 1);
        Assert.assertTrue(pair1.getSecond() == 0);

        pair1.setSecond(2);
        pair1.setFirst(2);
        Assert.assertTrue(pair1.getFirst() == 2);
        Assert.assertTrue(pair1.getSecond() == 2);
    }

    @Test
    public void test_equal() {
        Pair<Integer, Integer> pair1 = new Pair<>(1, 0);
        Pair<Integer, Integer> pair2 = new Pair<>(1, 0);

        Assert.assertTrue(pair1.equals(pair2));

        pair1.setFirst(2);
        Assert.assertFalse(pair1.equals(pair2));

        pair1.setFirst(1);
        Assert.assertTrue(pair1.equals(pair2));
    }

    @Test
    public void test_equal_null_content() {
        Pair<Object, Object> pair1 = new Pair<>();
        Pair<Object, Object> pair2 = new Pair<>();
        Assert.assertTrue(pair1.getFirst() == null);
        Assert.assertTrue(pair1.getSecond() == null);
        Assert.assertTrue(pair1.equals(pair2));
    }

    @Test
    public void test_equal_null() {
        Pair<Object, Object> pair = new Pair<>();
        Assert.assertFalse(pair.equals(null));
    }

    @Test
    public void test_equal_obj() {
        Pair<Object, Object> pair1 = new Pair<>();
        Pair<Object, Object> pair2 = new Pair<>();

        Object obj = new Object();
        pair1.setFirst(obj);
        pair2.setFirst(obj);

        Assert.assertTrue(pair1.equals(pair2));

        pair1.setSecond(new Object());
        pair2.setSecond(new Object());
        Assert.assertFalse(pair1.equals(pair2));

        pair2.setSecond(0);
        Assert.assertFalse(pair1.equals(pair2));

        pair2.setSecond(null);
        Assert.assertFalse(pair1.equals(pair2));

    }

}
