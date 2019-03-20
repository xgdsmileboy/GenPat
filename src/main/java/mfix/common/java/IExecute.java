/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

/**
 * @author: Jiajun
 * @date: 2019-03-12
 */
public interface IExecute {

    boolean compile();
    boolean test();
    boolean test(String testcase);
    boolean test(String clazz, String method);

}
