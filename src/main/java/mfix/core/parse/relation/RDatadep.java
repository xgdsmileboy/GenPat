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
public class RDatadep extends Relation {

    /**
     * Relations that an relation may depend on
     */
    private List<ObjRelation> _valueDecideRelation = new LinkedList<>();
    /**
     * Relation that use the objects in {@code _valueDecideRelation}
     */
    private Relation _useRelation;

    public RDatadep() {
        super(RelationKind.DATADEPEND);
    }

    public void addDecideRelation(ObjRelation relation) {
        _valueDecideRelation.add(relation);
    }

    public void setUseRelation(Relation relation) {
        _useRelation = relation;
    }

    public List<ObjRelation> getDependedRelations() {
        return _valueDecideRelation;
    }

    public Relation getUseRelation() {
        return _useRelation;
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if(!super.match(relation, dependencies)) {
            return false;
        }
        RDatadep datadep = (RDatadep) relation;
        List<ObjRelation> deps = datadep.getDependedRelations();
        if(_valueDecideRelation.size() != deps.size()) {
            return false;
        }

        for(int i = 0; i < _valueDecideRelation.size(); i++) {
            if(!_valueDecideRelation.get(i).match(deps.get(i), dependencies)) {
                return false;
            }
            dependencies.add(new Pair<>(_valueDecideRelation.get(i), deps.get(i)));
        }

        if(_useRelation.match(datadep.getUseRelation(), dependencies)) {
            dependencies.add(new Pair<>(_useRelation, datadep.getUseRelation()));
            return true;
        }
        return false;
    }
}
