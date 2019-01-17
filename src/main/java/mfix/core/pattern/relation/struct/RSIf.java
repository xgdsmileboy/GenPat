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
public class RSIf extends Structure {

    public final static int POS_CHILD_COND = 0x001;
    public final static int POS_CHILD_THEN = 0x010;
    public final static int POS_CHILD_ELSE = 0x100;

    public RSIf() {
        super(RSKind.RS_IF);
    }
}
