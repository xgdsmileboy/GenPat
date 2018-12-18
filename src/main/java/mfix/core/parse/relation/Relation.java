/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;

import java.util.HashSet;
import java.util.Set;

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
     * Especially, this list record the define use relation
     */
    protected Set<ObjRelation> _dependon;

    /**
     * record this relation is used by
     * which relations
     */
    private Set<Relation> _usedBy;

    /**
     * Label whether a relation is matched before
     * and after repair, initially all relations are
     * not matched.
     */
    private boolean _matched = false;

    /**
     * Label whether a relation is considered in
     * the applying procedure, initially all relations
     * are considered
     */
    private boolean _concerned = true;

    protected Relation(RelationKind kind) {
        _relationKind = kind;
        _dependon = new HashSet<>();
        _usedBy = new HashSet<>();
    }

    public RelationKind getRelationKind() {
        return _relationKind;
    }

    public void setMatched(boolean matched) {
        _matched = matched;
        _concerned = !matched;
    }

    public boolean isMatched() {
        return _matched;
    }

    public void setConcerned(boolean concerned) {
        _concerned = concerned;
    }

    public boolean isConcerned() {
        return _concerned;
    }

    public void addDependency(ObjRelation relation) {
        if(relation != null) {
            _dependon.add(relation);
        }
    }

    public Set<ObjRelation> getDependencies() {
        return _dependon;
    }

    public void usedBy(Relation relation) {
        if(relation != null) {
            _usedBy.add(relation);
        }
    }

    public Set<Relation> getUsedBy() {
        return _usedBy;
    }

    public void addArg(RArg arg) {}

    public Set<Relation> expandDownward(Set<Relation> set) {
        set.addAll(_dependon);
        return expandDownward0(set);
    }

    public String getExprString(){
        return "";
    }

    protected abstract Set<Relation> expandDownward0(Set<Relation> set);

    /**
     * The matched relation cannot be {@code null}
     * and the relation type should be the same
     * @param relation : relation to be matched
     * @return {@code true} if {@code relation} match current
     * relation, {@code false} otherwise.
     */
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
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
