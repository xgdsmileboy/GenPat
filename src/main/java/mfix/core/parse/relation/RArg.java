/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;

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

    public RArg(ObjRelation function) {
        super(RelationKind.ARGUMENT);
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
        set.add(_arg);
        return set;
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
        if(!_function.match(arg.getFunctionRelation(), dependencies)) {
            return false;
        }
        dependencies.add(new Pair<>(_function, arg.getFunctionRelation()));
        if(_arg.match(arg.getArgument(), dependencies)) {
           dependencies.add(new Pair<>(_arg, arg.getArgument()));
           return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[RArg]";
    }
}
