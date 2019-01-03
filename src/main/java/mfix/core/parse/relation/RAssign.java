/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.node.Node;
import mfix.core.stats.element.ElementCounter;

import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RAssign extends ObjRelation {
    /**
     * the following assignment operators should be
     * normalized to an {@code ROpt} relation
     * and an {@code RAssign} relation
     *
     * =, +=, -=, *=. /=, %=, <<=, >>=
     * &=, |=, ^=
     *
     */

    /**
     * Left hand side of the assignment
     */
    private ObjRelation _lhs;
    /**
     * Rgiht hand side of the assignment
     */
    private ObjRelation _rhs;

    public RAssign(Node node, ObjRelation lhs) {
        super(node, RelationKind.ASSIGN);
        _lhs = lhs;
        _lhs.usedBy(this);
    }

    public void setLhs(ObjRelation lhs) {
        _lhs = lhs;
        _lhs.usedBy(this);
    }

    public void setRhs(ObjRelation rhs) {
        _rhs = rhs;
        _rhs.usedBy(this);
    }

    public ObjRelation getLhs() {
        return _lhs;
    }

    public ObjRelation getRhs() {
        return _rhs;
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        set.add(_lhs);
        set.add(_rhs);
        return set;
    }

    @Override
    public String getExprString() {
        return _lhs.getExprString();
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        _lhs.doAbstraction(counter);
        _rhs.doAbstraction(counter);
        _isAbstract = (!_lhs.isConcerned() || _lhs.isAbstract())
                && (!_rhs.isConcerned() || _rhs.isAbstract());
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> denpendencies) {
        if(!super.match(relation, denpendencies)) {
            return false;
        }
        RAssign assign = (RAssign) relation;
        if (_lhs.match(assign.getLhs(), denpendencies) && _rhs.match(assign.getRhs(), denpendencies)) {
            denpendencies.add(new Pair<>(_lhs, assign.getLhs()));
            denpendencies.add(new Pair<>(_rhs, assign.getRhs()));
            return true;
        }
        return false;
    }

    @Override
    public boolean foldMatching(Relation r, Set<Pair<Relation, Relation>> dependencies,
                                Map<String, String> varMapping) {
        if(!isConcerned()) return true;
        if(r instanceof  RAssign) {
            RAssign assign = (RAssign) r;
            boolean match = true;
            if(!_lhs.isAbstract()) {
                match = match && _lhs.foldMatching(assign.getLhs(), dependencies, varMapping);
            }
            if(!_rhs.isAbstract()) {
                match = match && _rhs.foldMatching(assign.getRhs(), dependencies, varMapping);
            }
            return match;
        }
        return false;
    }

    @Override
    public String toString() {
        return _lhs.getExprString() + "=" + _rhs.getExprString();
    }
}
