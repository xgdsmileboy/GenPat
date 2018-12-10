/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RKid extends Relation {

    private RStruct _structure;
    private int _index;
    private Relation _child;

    public RKid(RStruct structure) {
        super(RelationKind.CHILD);
        _structure = structure;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public void setChild(Relation child) {
        _child = child;
    }

    public RStruct getStructure() {
        return _structure;
    }

    public int getIndex() {
        return _index;
    }

    public Relation getChildRelation() {
        return _child;
    }

    @Override
    public boolean match(Relation relation) {
        if(!super.match(relation)) {
            return false;
        }

        RKid kid = (RKid) relation;
        if(_index != kid.getIndex()) {
            return false;
        }

        return _child.match(kid.getChildRelation());
    }
}
