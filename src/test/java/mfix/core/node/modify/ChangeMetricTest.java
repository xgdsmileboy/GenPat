/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.TestCase;
import org.junit.Test;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-04-30
 */
public class ChangeMetricTest extends TestCase {

    @Test
    public void test_sort() {
        List<ChangeMetric> metrics = new LinkedList<>();
        metrics.add(new ChangeMetric(2, 0, 5, 0));
        metrics.add(new ChangeMetric(4, 0, 4, 0));
        metrics.add(new ChangeMetric(2, 2, 1, 0));
        metrics.add(new ChangeMetric(2, 1, 2, 0));
        metrics.add(new ChangeMetric(3, 1, 1, 0));
        metrics.add(new ChangeMetric(5, 4, 3, 0));
        metrics.add(new ChangeMetric(2, 4, 1, 0));
        metrics.add(new ChangeMetric(4, 4, 2, 0));
        metrics.add(new ChangeMetric(8, 6, 1, 0));
        metrics.add(new ChangeMetric(2, 0, 1, 0));

        metrics = metrics.stream().sorted(Comparator.comparingInt(ChangeMetric::getChangeNumber)
                .thenComparingInt(ChangeMetric::negUpd).thenComparingInt(ChangeMetric::negIns))
                .collect(Collectors.toList());
        for (ChangeMetric metric : metrics) {
            System.out.println(metric);
        }
    }

}
