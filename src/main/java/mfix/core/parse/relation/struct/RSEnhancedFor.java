/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.struct;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RSEnhancedFor extends Structure {

    public final static int POS_CHILD_PRAM = 0x001;
    public final static int POS_CHILD_EXPR = 0x010;
    public final static int POS_CHILD_BODY = 0x100;

    public RSEnhancedFor() {
        super(RSKind.RS_ENHANCEDFOR);
    }
}
