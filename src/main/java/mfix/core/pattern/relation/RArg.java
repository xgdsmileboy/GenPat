/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.node.modify.Modification;
import mfix.core.stats.element.ElementCounter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RArg extends Relation {

    /**
     * Denotes a function or operation object (relation)
     */
    private ObjRelation _function;
    /**
     * The index of the argument
     */
    private int _index;
    /**
     * The argument object (relation)
     */
    private ObjRelation _arg;

    public RArg(Node node, ObjRelation function) {
        super(node, RelationKind.ARGUMENT);
        _function = function;
        this.usedBy(function);
    }

    public void setIndex(int index) {
        _index = index;
    }

    public void setArgument(ObjRelation argument) {
        _arg = argument;
        _arg.usedBy(this);
    }

    public int getIndex() {
        return _index;
    }

    public ObjRelation getFunctionRelation() {
        return _function;
    }

    public ObjRelation getArgument() {
        return _arg;
    }

    @Override
    public String getExprString() {
        return _arg.getExprString();
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        set.add(_function);
        set.add(_arg);
        return set;
    }

    @Override
    protected void setControlDependency(RStruct rstruct, Set<Relation> controls) {
        _function.setControlDependency(rstruct, controls);
        _arg.setControlDependency(rstruct, controls);
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        _function.doAbstraction(counter);
        _arg.doAbstraction(counter);
        _isAbstract = !_arg.isConcerned() || _arg.isAbstract();
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }
        RArg arg = (RArg) relation;
        if (_index != arg.getIndex()) {
            return false;
        }
        if (!_function.match(arg.getFunctionRelation(), dependencies)) {
            return false;
        }
        dependencies.add(new Pair<>(_function, arg.getFunctionRelation()));
        if (_arg.match(arg.getArgument(), dependencies)) {
            dependencies.add(new Pair<>(_arg, arg.getArgument()));
            return true;
        }
        return false;
    }

    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        if(super.greedyMatch(r, matchedRelationMap, varMapping)) {
            RArg arg = (RArg) r;
            matchedRelationMap.put(this, r);
            if(_function.greedyMatch(arg.getFunctionRelation(), matchedRelationMap, varMapping)
                    && _arg.greedyMatch(arg.getArgument(), matchedRelationMap, varMapping)
                    && matchDependencies(arg.getDependencies(), matchedRelationMap, varMapping)) {
                if(getParent() != null) {
                    getParent().greedyMatch(arg.getParent(), matchedRelationMap, varMapping);
                }
                return true;
            } else {
                matchedRelationMap.remove(this);
            }
        }
        return false;
    }

    @Override
    public boolean foldMatching(Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public boolean canGroup(Relation r) {
        return r == _function || r == _arg;
    }

    @Override
    public boolean assemble(List<Modification> modifications, boolean isAdded) {
        return true;
    }

    @Override
    public StringBuffer buildTargetSource() {
        return new StringBuffer();
    }

    @Override
    public String toString() {
        return String.format("[RArg (%d)| %s]", _index, _function.getExprString());
    }
}
