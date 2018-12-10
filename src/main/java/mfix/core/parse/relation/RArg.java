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
public class RArg extends Relation {

    private ObjRelation _function;
    private int _index;
    private ObjRelation _arg;

    public RArg(ObjRelation function) {
        super(RelationKind.ARGUMENT);
        _function = function;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public void setArgument(ObjRelation argument) {
        _arg = argument;
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
    public boolean match(Relation relation) {
        if (!super.match(relation)) {
            return false;
        }
        RArg arg = (RArg) relation;
        if (_index != arg.getIndex()) {
            return false;
        }
        return _function.match(arg.getFunctionRelation());
    }
}
