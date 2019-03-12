/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

/**
 * @author: Jiajun
 * @date: 2019-03-11
 */
public class MiningUtils {

    public final static String buggyFileSubDirName() {
        return "buggy-version";
    }

    public final static String fixedFileSubDirName() {
        return "fixed-version";
    }

    public final static String patternSubDirName() {
        return "pattern-" + Constant.PATTERN_VERSION + "-serial";
    }

}
