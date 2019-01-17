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
public class RSFor extends Structure {

    public final static int POS_CHILD_INIT = 0x0001;
    public final static int POS_CHILD_COND = 0x0010;
    public final static int POS_CHILD_UPD = 0x0100;
    public final static int POS_CHILD_BODY = 0x1000;

    public RSFor() {
        super(RSKind.RS_FOR);
    }
}
