/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.Z3Solver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class Pattern implements Serializable {

    private transient int _relationId = 0;
    /**
     * A flag denoting current added relations is from the
     * code before repair {@code true} or after repair {@code false}
     */
    private transient boolean _isOldRelation = true;
    /**
     * A flag to record whether the pattern has been minimized
     */
    public boolean _minimized = false;
    /**
     * Record the variables defined in the old relations,
     * including real variable definitions and virtual
     * variables (such fields).
     */
    private Map<String, RDef> _oldName2Define = new HashMap<>();
    /**
     * Record the variables defined in the new relations,
     * including real variable definitions and virtual
     * variables (such fields).
     */
    private Map<String, RDef> _newName2Define = new HashMap<>();

    /**
     * a pattern consists of a set of relations before
     * repair and after repair
     */
    private List<Relation> _oldRelations;
    private List<Relation> _newRelations;

    public Pattern() {
        _oldRelations = new ArrayList<>();
        _newRelations = new ArrayList<>();
    }

    /**
     * Set current added relations are from the code before repair
     * @param isOldRelation
     */
    public void setOldRelationFlag(boolean isOldRelation) {
        _isOldRelation = isOldRelation;
    }

    /**
     * Add relations to the pattern, this function will add the relation
     * into {@code _oldRelations} or {@code _newRelations} based on the
     * flag {@code )_isOldRelation}
     *
     * NOTE: if the added relation is a variable definition,
     * this function will automatically record the variable
     *
     * @param relation
     */
    public void addRelation(Relation relation) {
        if (_isOldRelation) {
            addOldRelation(relation);
            if (relation.getRelationKind() == Relation.RelationKind.DEFINE
                    || relation.getRelationKind() == Relation.RelationKind.VIRTUALDEFINE) {
                RDef def = (RDef) relation;
                // if the name of the variable definition relation is null,
                // it is a constant (a virtual variable definition) and wont
                // be record
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

    /**
     * Search the variable definition relation based
     * on the variable name
     * @param name : name of variables
     * @return
     */
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

    /**
     * Minimize current pattern by removing some unrelated
     * relations according to the given relation expansion scope.
     * @param expandLevel : denotes how many levels should be expanded
     */
    public Pattern minimize(int expandLevel) {
        return minimize(expandLevel, false);
    }

    public Pattern minimize(int expandLevel, boolean force) {
        if(_minimized && !force) return this;
        _minimized = true;
        Map<Relation, Integer> oldR2index = mapRelation2LstIndex(_oldRelations);
        Map<Relation, Integer> newR2index = mapRelation2LstIndex(_newRelations);
        int oldLen = _oldRelations.size();
        int newLen = _newRelations.size();

        int[][] matrix = new int[oldLen][newLen];
        Map<String, Set<Pair<Integer, Integer>>> loc2dependencies = new HashMap<>();
        Set<Pair<Relation, Relation>> dependencies = new HashSet<>();
        Set<Pair<Integer, Integer>> set;
        for(int i = 0; i < oldLen; i++) {
            for(int j = 0; j < newLen; j++) {
                dependencies.clear();
                if(_oldRelations.get(i).match(_newRelations.get(j), dependencies)) {
                    matrix[i][j] = 1;
                    String key = i + "_" + j;
                    set = new HashSet<>();
                    for(Pair<Relation, Relation> pair : dependencies) {
                        set.add(new Pair<>(oldR2index.get(pair.getFirst()), newR2index.get(pair.getSecond())));
                    }
                    loc2dependencies.put(key, set);
                }
            }
        }
        Z3Solver solver = new Z3Solver();
        Map<Integer, Integer> old2new = solver.build(matrix, loc2dependencies);
        for(Map.Entry<Integer, Integer> entry : old2new.entrySet()) {
            _oldRelations.get(entry.getKey()).setMatched(true);
            _newRelations.get(entry.getValue()).setMatched(true);
        }

        // TODO: after obtain the minimal changes,
        // expand the relations based on "expandLevel"
        Set<Relation> toExpend = new HashSet<>();
        for(int i = 0; i < _oldRelations.size(); i++) {
            if(!old2new.containsKey(i)) {
                toExpend.add(_oldRelations.get(i));
            }
        }

        Set<Relation> expanded;
        int currentLevel = 0;
        while((expandLevel--) > 0) {
            currentLevel ++;
            expanded = new HashSet<>();
            for(Relation r : toExpend) {
                r.expandDownward(expanded);
                expanded.addAll(r.getUsedBy());
            }

            for(Relation r : expanded) {
                Integer oldIndex = oldR2index.get(r);
                _oldRelations.get(oldIndex).setExpendedLevel(currentLevel);
                Integer newIndex = old2new.get(oldIndex);
                if(newIndex != null) {
                    _newRelations.get(newIndex).setExpendedLevel(currentLevel);
                }
            }

            toExpend = expanded;
        }



        return this;
    }

    private Map<Relation, Integer> mapRelation2LstIndex(List<Relation> relations) {
        Map<Relation, Integer> map = new HashMap<>();
        if(relations != null) {
            for (int i = 0; i < relations.size(); i++) {
                map.put(relations.get(i), i);
            }
        }
        return map;
    }

    public List<Relation> getMinimizedOldRelations(boolean concerned) {
        if(!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for(Relation r : _oldRelations) {
            if(!r.isMatched() || (concerned && r.isConcerned())) {
                relations.add(r);
            }
        }
        return relations;
    }

    public List<Relation> getMinimizedNewRelations(boolean concerned) {
        if(!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for(Relation r : _newRelations) {
            if(!r.isMatched() || (concerned && r.isConcerned())) {
                relations.add(r);
            }
        }
        return relations;
    }

}
