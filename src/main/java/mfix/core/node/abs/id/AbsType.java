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
public class AbsType extends AbsNode {

    private static final long serialVersionUID = -8749984837172859946L;

    private final static String IDENTIFIER="TYPE";

    public AbsType(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
