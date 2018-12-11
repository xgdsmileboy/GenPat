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

    /**
     * Designed to boost relation matching process.
     *
     * This field denotes the type of the relation,
     * which is used for relation matching.
     * NOTE: this kind of relation is coarse-grained
     * since different concrete expressions may
     * have the same relation kind, the matching relation
     * should rely on more concrete matching process.
     */
    private RelationKind _relationKind;
    /**
     * a relation may rely on some other relation
     */
    protected Relation _parent;

    /**
     * Label a relation changed after the repair,
     * which is the core of the pattern if true.
     */
    private boolean _isMinimized = false;

    protected Relation(RelationKind kind) {
        _relationKind = kind;
    }

    public RelationKind getRelationKind() {
        return _relationKind;
    }

    public void setIsminimized() {
        _isMinimized = true;
    }

    public boolean isMinimized() {
        return _isMinimized;
    }

    /**
     * The matched relation cannot be {@code null}
     * and the relation type should be the same
     * @param relation : relation to be matched
     * @return {@code true} if {@code relation} match current
     * relation, {@code false} otherwise.
     */
    public boolean match(Relation relation) {
        if(relation == null || relation.getRelationKind() != _relationKind) {
            return false;
        }
        return true;
    }

    /**
     * types of relations.
     */
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
