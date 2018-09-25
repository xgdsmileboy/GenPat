/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.java.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public abstract class AbstractFaultLocalization {
    protected final int DEFAULT_THRETHOD = 0;
    protected Subject _subject = null;
    protected List<String> _failedTests = null;
    protected List<String> _passedTests = null;
    protected List<Location> _faultyLocations = null;


    public AbstractFaultLocalization(Subject subject) {
        _subject = subject;
        _failedTests = new ArrayList<>();
        _passedTests = new ArrayList<>();
    }

    public int getTotalTestCases(){
        return _passedTests.size() + _failedTests.size();
    }

    public List<String> getPassedTestCases(){
        return _passedTests;
    }

    public List<String> getFailedTestCases(){
        return _failedTests;
    }

    protected abstract void locateFault(double threshold);

    public abstract List<Location> getLocations(int topK);
}
