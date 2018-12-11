/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import java.io.Serializable;
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
        _oldRelations = new LinkedList<>();
        _newRelations = new LinkedList<>();
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
        if(_minimized) return this;
        _minimized = true;
        //TODO : to replace the following process with SAT process
        Map<Integer, Integer> old2newRelationMap = new HashMap<>();
        Map<Integer, Integer> new2oldRlationMap = new HashMap<>();
        for(int row = 0; row < _oldRelations.size(); row ++) {
            int preMatchColumn = -1;
            for(int column = 0; column < _newRelations.size(); column++) {
                if(_oldRelations.get(row).match(_newRelations.get(column))) {
                    // this row is already match some column before
                    if(preMatchColumn != -1) {
                        // remove the matched column and select a best matching column
                        new2oldRlationMap.remove(preMatchColumn);
                        preMatchColumn = processColumnConflict(row, preMatchColumn, column);
                    } else {
                        preMatchColumn = column;
                    }
                    // find the best matching column, otherwise, no match for current row
                    if(preMatchColumn >= 0) {
                        // check whether the selected column is matched by any previous row
                        Integer preMatchedRow = new2oldRlationMap.get(preMatchColumn);
                        if(preMatchedRow != null) {
                            // if matched, first remove the match relation
                            // then find a proper match for the column
                            new2oldRlationMap.remove(preMatchColumn);
                            int selectedRow = processRowConfilict(preMatchColumn, preMatchedRow, row);
                            if(selectedRow >= 0) {
                                // find the best match row, re-map the matching relation.
                                new2oldRlationMap.put(preMatchColumn, selectedRow);
                            }
                        } else {
                            // the column is not matched by previous rows
                            // match the current row with the selected column (no conflict)
                            new2oldRlationMap.put(preMatchColumn, row);
                        }
                    }
                }
            }
        }
        // label all matched relations.
        for(Map.Entry<Integer, Integer> entry : new2oldRlationMap.entrySet()) {
            _newRelations.get(entry.getKey()).setMatched(true);
            _oldRelations.get(entry.getValue()).setMatched(true);
        }
        return this;
    }

    public List<Relation> getMinimizedOldRelations() {
        if(!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for(Relation r : _oldRelations) {
            if(!r.isMatched()) {
                relations.add(r);
            }
        }
        return relations;
    }

    public List<Relation> getMinimizedNewRelations() {
        if(!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for(Relation r : _newRelations) {
            if(!r.isMatched()) {
                relations.add(r);
            }
        }
        return relations;
    }

    private int processColumnConflict(int row, int column1, int column2) {
        // TODO
        return column2;
    }

    private int processRowConfilict(int column, int row1, int row2) {
        // TODO
        return row1;
    }

    private Set<Integer> obtainOnes(int[] vector) {
        Set<Integer> set = new HashSet<>();
        for(int i = 0; i < vector.length; i++) {
            if(vector[i] == 1) {
                set.add(i);
            }
        }
        return set;
    }


}
