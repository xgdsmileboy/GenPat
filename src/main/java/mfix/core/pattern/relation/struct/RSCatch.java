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
public class RSCatch extends Structure {

    public final static int POS_CHILD_DEF = 0x01;
    public final static int POS_CHILD_HANDLER = 0x10;

    public RSCatch() {
        super(RSKind.RS_CATCH);
    }
}
