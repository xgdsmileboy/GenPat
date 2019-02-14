/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.cluster;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class VectorTest {

    @Test
    public void test() {
        Vector vector = new Vector();
        // test initialize
        for (int i = 0; i < 64; i++) {
            Assert.assertTrue(vector.get(i) == 0);
        }

        // test equals
        Vector other = new Vector();
        Assert.assertTrue(vector.equals(other));

        // test set and get
        vector.set(1);
        Assert.assertTrue(vector.get(1) == 1);

        vector.set(9);
        Assert.assertTrue(vector.get(1) == 1);
        Assert.assertTrue(vector.get(9) == 1);

        vector.clear(9);
        Assert.assertTrue(vector.get(1) == 1);
        Assert.assertTrue(vector.get(9) == 0);

        vector.reset();
        other.reset();
        Assert.assertTrue(vector.equals(other));

        other.set(1);
        vector.and(other);
        Assert.assertTrue(!vector.equals(other));

        vector.or(other);
        Assert.assertTrue(vector.equals(other));

        vector.clear(1);
        vector.xor(other);
        Assert.assertTrue(vector.get(1) == 1);

    }

}
