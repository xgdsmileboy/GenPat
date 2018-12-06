/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.core.parse.relation.op.AbsOperation;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class ROpt extends ObjRelation {

    private AbsOperation _operation;

    public ROpt(AbsOperation operation) {
        super(RelationKind.OPERATION);
        _operation = operation;
    }

    public void setOperation(AbsOperation operation) {
        _operation = operation;
    }
}
