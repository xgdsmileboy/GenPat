/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.common.util.Utils;
import mfix.core.pattern.Pattern;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author: Jiajun
 * @date: 2019-04-30
 */
public class ChangeCounter implements Callable<ChangeMetric> {

    private double _counter = 0;
    private String _patternFile;

    public ChangeCounter(String file) {
        _patternFile = file;
    }

    @Override
    public ChangeMetric call() {
        Pattern p;
        try {
            p = (Pattern) Utils.deserialize(_patternFile);
        } catch (Exception e) {
            return null;
        }
        int upd = 0, ins = 0, del = 0;
        Set<Modification> modifications = p.getAllModifications();
        int size = modifications.size();
        if (size == 0) return null;
        for (Modification m : modifications) {
            if (m instanceof Update || m instanceof Wrap) {
                upd += m.size();
            } else if (m instanceof Insertion) {
                ins += m.size();
            } else {
                del += m.size();
            }
        }
        return new ChangeMetric(_patternFile, size, upd, ins, del);
    }
}
