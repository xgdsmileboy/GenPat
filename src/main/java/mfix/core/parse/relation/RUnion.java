/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RUnion extends ObjRelation {

    private String _varName;
    // the relation should be assignment or variable declaration
    private List<Relation> _assigns = new LinkedList<>();

    public RUnion() {
        super(RelationKind.UNION);
    }

    public void setVarName(String name) {
        _varName = name;
    }

    public void addAssignRelation(Relation assign) {
        _assigns.add(assign);
    }

    public String getVarname() {
        return _varName;
    }

    public List<Relation> getAssignVarValueRelations() {
        return _assigns;
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }
        RUnion union = (RUnion) relation;
        if(!_varName.equals(union.getVarname())) {
            return false;
        }
        List<Relation> relations = union.getAssignVarValueRelations();
        if(_assigns.size() != relations.size()) {
            return false;
        }
        for(int i = 0; i < _assigns.size(); i++) {
            if(!_assigns.get(i).match(relations.get(i), dependencies)) {
                return false;
            }
            dependencies.add(new Pair<>(_assigns.get(i), relations.get(i)));
        }
        return true;
    }
}
