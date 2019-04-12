/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs.id;

/**
 * @author: Jiajun
 * @date: 2019-03-26
 */
public class AbsOp extends AbsNode {

    private static final long serialVersionUID = 4124458532960457523L;

    private final static String IDENTIFIER="<OP>";

    public AbsOp(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
