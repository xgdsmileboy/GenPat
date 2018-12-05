/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.op;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public abstract class BinaryOp extends Operation {

    public final static int POSITION_LHS = 0x01;
    public final static int POSITION_RHS = 0x10;

    protected BinaryOp(Op operator) {
        super(operator);
    }
}
