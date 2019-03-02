/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs.id;

/**
 * @author: Jiajun
 * @date: 2019-02-20
 */
public class AbsMethod extends AbsNode {

    private static final long serialVersionUID = 2782218564013313007L;
    private final static String IDENTIFIER="METH";

    public AbsMethod(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
