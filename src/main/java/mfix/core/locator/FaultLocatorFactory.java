/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.java.D4jSubject;
import mfix.common.java.FakeSubject;
import mfix.common.java.Subject;
import mfix.common.util.LevelLogger;

/**
 * @author: Jiajun
 * @date: 2019-03-12
 */
public class FaultLocatorFactory {

    public static AbstractFaultLocator dispatch(Subject subject) {
        AbstractFaultLocator locator = null;
        switch(subject.getType()) {
            case FakeSubject.NAME:
                locator = new FakeLocator((FakeSubject) subject);
                break;
            case D4jSubject.NAME:
                locator = new D4JManualLocator((D4jSubject) subject);
                break;
            default:
                LevelLogger.error("Cannot parse subject type : " + subject.toString());
        }
        return locator;
    }
}
