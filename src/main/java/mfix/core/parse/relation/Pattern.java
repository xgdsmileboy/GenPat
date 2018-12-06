/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class Pattern implements Serializable {

    private transient int _relationId = 0;
    private transient boolean _isOldRelation = true;
    private Map<String, RDef> _oldName2Define = new HashMap<>();
    private Map<String, RDef> _newName2Define = new HashMap<>();

    /**
     * a pattern consists of a set of relations
     */
    private List<Relation> _oldRelations;
    private List<Relation> _newRelations;

    public Pattern() {
        _oldRelations = new LinkedList<>();
        _newRelations = new LinkedList<>();
    }

    public void setOldRelationFlag(boolean isOldRelation) {
        _isOldRelation = isOldRelation;
    }

    public void addRelation(Relation relation) {
        if (_isOldRelation) {
            addOldRelation(relation);
            if (relation.getRelationKind() == Relation.RelationKind.DEFINE
                    || relation.getRelationKind() == Relation.RelationKind.VIRTUALDEFINE) {
                RDef def = (RDef) relation;
                if(def.getName() != null) {
                    _oldName2Define.put(def.getName(), def);
                }
            }
        } else {
            addNewRelation(relation);
            if (relation.getRelationKind() == Relation.RelationKind.DEFINE
                    || relation.getRelationKind() == Relation.RelationKind.VIRTUALDEFINE) {
                RDef def = (RDef) relation;
                if(def.getName() != null) {
                    _newName2Define.put(def.getName(), def);
                }
            }
        }
    }

    public RDef getVarDefine(String name) {
        if(_isOldRelation) {
            return _oldName2Define.get(name);
        } else {
            return _newName2Define.get(name);
        }
    }

    private void addOldRelation(Relation relation) {
        _oldRelations.add(relation);
    }

    public List<Relation> getOldRelations() {
        return _oldRelations;
    }

    private void addNewRelation(Relation relation) {
        _newRelations.add(relation);
    }

    public List<Relation> getNewRelations() {
        return _newRelations;
    }

    public int genRelationId() {
        return _relationId++;
    }

}
