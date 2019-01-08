/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.pattern.relation.op.AbsOperation;
import mfix.core.stats.element.ElementCounter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class ROpt extends ObjRelation {

    /**
     * operator
     */
    private AbsOperation _operation;

    private List<RArg> _args;

    public ROpt(Node node, AbsOperation operation) {
        super(node, RelationKind.OPERATION);
        _operation = operation;
        _args = new LinkedList<>();
    }

    public void setOperation(AbsOperation operation) {
        _operation = operation;
    }

    public AbsOperation getOperation() {
        return _operation;
    }

    @Override
    public void addArg(RArg arg) {
        _args.add(arg);
    }

    @Override
    public String getExprString() {
        return _operation.getExprString(_args);
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        set.addAll(_args);
        return set;
    }

    @Override
    protected void setControlDependency(RStruct rstruct, Set<Relation> controls) {
        if(_controlDependon == null) {
            _controlDependon = rstruct;
            controls.add(this);
            for (Relation r : _args) {
                r.setControlDependency(rstruct, controls);
            }
        }
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        _isAbstract = true;
        for(RArg arg : _args) {
            arg.doAbstraction(counter);
            _isAbstract = _isAbstract && (!arg.isConcerned() || arg.isAbstract());
        }
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if(!super.match(relation, dependencies)) {
            return false;
        }
        ROpt opt = (ROpt) relation;
        return _operation.match(opt.getOperation());
    }


    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        return super.greedyMatch(r, matchedRelationMap, varMapping);
    }

    @Override
    public boolean foldMatching(Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public String toString() {
        return getExprString();
    }
}