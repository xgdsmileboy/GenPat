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
public abstract class Relation {

    private RelationKind _relationKind;
    /**
     * a relation may rely on some other relation
     */
    protected Relation _parent;

    protected Relation(RelationKind kind) {
        _relationKind = kind;
    }

    public RelationKind getRelationKind() {
        return _relationKind;
    }

    public boolean match(Relation relation) {
        if(relation == null || relation.getRelationKind() != _relationKind) {
            return false;
        }
        return true;
    }

    public enum RelationKind{
        ARGUMENT,
        OPERATION,
        DEFINE,
        ASSIGN,
        MCALL,
        RETURN,
        STRUCTURE,
        CHILD,

        VIRTUALDEFINE,
        UNION,
        DATADEPEND
    }

}
