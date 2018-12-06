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
public class CopCond extends AbsOperation {

    public final static int POSITION_CONDITION = 0x00100;
    public final static int POSITION_THEN = 0x01000;
    public final static int POSITION_ELSE = 0x10000;

    public CopCond() {
        super(Op.C_COND);
    }
}
