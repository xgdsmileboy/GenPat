/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.struct;

/**
 * @author: Jiajun
 * @date: 2018/12/6
 */
public class RSMethod extends Structure {

    public final static int POS_CHILD_PARAM = 0x01;
    public final static int POS_CHILD_BODY = 0x10;

    public RSMethod() {
        super(RSKind.RS_METHOD);
    }
}