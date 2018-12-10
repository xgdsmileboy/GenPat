/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RAssign extends Relation {
    /**
     * the following assignment operators should be
     * normalized to an {@code ROpt} relation
     * and an {@code RAssign} relation
     *
     * =, +=, -=, *=. /=, %=, <<=, >>=
     * &=, |=, ^=
     *
     */

    private ObjRelation _lhs;
    private ObjRelation _rhs;

    public RAssign(ObjRelation lhs) {
        super(RelationKind.ASSIGN);
        _lhs = lhs;
    }

    public void setRhs(ObjRelation rhs) {
        _rhs = rhs;
    }

    public ObjRelation getLhs() {
        return _lhs;
    }

    public ObjRelation getRhs() {
        return _rhs;
    }

    @Override
    public boolean match(Relation relation) {
        if(!super.match(relation)) {
            return false;
        }
        RAssign assign = (RAssign) relation;
        return _lhs.match(assign.getLhs()) && _rhs.match(assign.getRhs()) ;
    }
}
