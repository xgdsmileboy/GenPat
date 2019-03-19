/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.java.D4jSubject;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class D4JManualLocator extends AbstractFaultLocalization {


    public D4JManualLocator(D4jSubject subject) {
        super(subject);

    }

    @Override
    protected void locateFault(double threshold) {
    }

    @Override
    public List<Location> getLocations(int topK) {
        if (_faultyLocations == null) {
            _faultyLocations = new LinkedList<>();
            D4jSubject subject = (D4jSubject) _subject;
            String filePath = Utils.join(Constant.SEP, Constant.D4J_FAULT_LOC, subject.getName(),
                    String.valueOf(subject.getId()));
            List<String> rows = JavaFile.readFileToStringList(filePath);
            for (String string : rows) {
                String[] locations = string.split("\\|\\|");
                for (String loc : locations) {
                    String[] clazzAndLine = loc.split(":");
                    if (clazzAndLine.length != 2) {
                        LevelLogger.error("@D4JManualLocator #getLocations : data format error : " + loc);
                        continue;
                    }
                    String clazz = clazzAndLine[0];
                    Integer line = Integer.valueOf(clazzAndLine[1]);
                    int index = clazz.lastIndexOf(".");
                    String method = clazz.substring(index + 1);
                    clazz = clazz.substring(0, index);
                    index = clazz.indexOf("$");
                    String innerClazz = null;
                    if (index > 0) {
                        innerClazz = clazz.substring(index + 1);
                        clazz = clazz.substring(0, index);
                    }
                    _faultyLocations.add(new Location(clazz, innerClazz, method, line, 1.0));
                }

            }
        }
        return _faultyLocations;
    }
}
