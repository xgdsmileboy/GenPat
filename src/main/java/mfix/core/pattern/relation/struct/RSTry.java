/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation.struct;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RSTry extends Structure {

    public final static int POS_CHILD_RES = 0x0001;
    public final static int POS_CHILD_BODY = 0x0010;
    public final static int POS_CHILD_CATCH = 0x0100;
    public final static int POS_CHILD_FINALLY = 0x1000;

    public RSTry() {
        super(RSKind.RS_TRY);
    }
}
