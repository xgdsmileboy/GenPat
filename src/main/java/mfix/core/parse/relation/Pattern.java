/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.Z3Solver;
import mfix.core.stats.element.ElementCounter;

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
    private Map<Integer, Integer> _oldR2newRidxMap;

    private Set<String> _apis;

    public Pattern() {
        _oldRelations = new ArrayList<>();
        _newRelations = new ArrayList<>();
    }

    /**
     * Set current added relations are from the code before repair
     *
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
                if (def.getName() != null) {
                    _oldName2Define.put(def.getName(), def);
                }
            }
        } else {
            addNewRelation(relation);
            if (relation.getRelationKind() == Relation.RelationKind.DEFINE
                    || relation.getRelationKind() == Relation.RelationKind.VIRTUALDEFINE) {
                RDef def = (RDef) relation;
                if (def.getName() != null) {
                    _newName2Define.put(def.getName(), def);
                }
            }
        }
    }

    /**
     * Search the variable definition relation based
     * on the variable name
     *
     * @param name : name of variables
     * @return
     */
    public RDef getVarDefine(String name) {
        if (_isOldRelation) {
            return _oldName2Define.get(name);
        } else {
            return _newName2Define.get(name);
        }
    }

    /**
     * perform pattern abstraction process based on
     * the given frequency
     * @param frequency : threshold for abstraction
     */
    public void doAbstraction(double frequency) {
        ElementCounter counter = new ElementCounter();
        counter.open();
        for (int i = 0; i < _oldRelations.size(); i++) {
            _oldRelations.get(i).doAbstraction(counter, frequency);
        }
        counter.close();
    }

    /**
     *
     * @return
     */
    /**
     * Perform pattern matching between the repair pattern {@code this} and the
     * pattern {@code p} for a buggy code.
     * @param p : pattern for a potential buggy code
     * @param exprMapping : the mapping relation for expressions if the pattern matches
     * @return true if matches, false otherwise
     */
    public boolean foldMatching(Pattern p, Map<String, String> exprMapping) {
        // TODO : p is an concrete instance for a potential buggy code
        return false;
    }

    /**
     * Get concrete APIs used in the pattern, which
     * should be exactly matched
     * @return : a set of API (method) names
     */
    public Set<String> getRelatedAPIs() {
        if (_apis == null) {
            _apis = new HashSet<>();
            for (Relation r : _oldRelations) {
                if (r.isConcerned() && !r.isAbstract()
                        && r.getRelationKind() == Relation.RelationKind.MCALL) {
                    _apis.add(((RMcall) r).getMethodName());
                }
            }
        }
        return _apis;
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

    /**
     * Minimize current pattern by removing some unrelated
     * relations according to the given relation expansion scope.
     *
     * @param expandLevel : denotes how many levels should be expanded
     */
    public Pattern minimize(int expandLevel) {
        return minimize(expandLevel, 100, false);
    }

    public Pattern minimize(int expandLevel, int maxRNumbers) {
        return minimize(expandLevel, maxRNumbers, false);
    }

    public Pattern minimize(int expandLevel, int maxRNumbers, boolean force) {
        if (_minimized && !force) return this;
        _minimized = true;
        Map<Relation, Integer> oldR2index = mapRelation2LstIndex(_oldRelations);
        Map<Relation, Integer> newR2index = mapRelation2LstIndex(_newRelations);

        if (_oldR2newRidxMap == null) {
            int oldLen = _oldRelations.size();
            int newLen = _newRelations.size();

            int[][] matrix = new int[oldLen][newLen];
            Map<String, Set<Pair<Integer, Integer>>> loc2dependencies = new HashMap<>();
            Set<Pair<Relation, Relation>> dependencies = new HashSet<>();
            Set<Pair<Integer, Integer>> set;
            for (int i = 0; i < oldLen; i++) {
                for (int j = 0; j < newLen; j++) {
                    dependencies.clear();
                    if (_oldRelations.get(i).match(_newRelations.get(j), dependencies)) {
                        matrix[i][j] = 1;
                        String key = i + "_" + j;
                        set = new HashSet<>();
                        for (Pair<Relation, Relation> pair : dependencies) {
                            set.add(new Pair<>(oldR2index.get(pair.getFirst()), newR2index.get(pair.getSecond())));
                        }
                        loc2dependencies.put(key, set);
                    }
                }
            }
            Z3Solver solver = new Z3Solver();
            _oldR2newRidxMap = solver.build(matrix, loc2dependencies);
        }
        Map<Integer, Integer> newR2OldRidxMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : _oldR2newRidxMap.entrySet()) {
            _oldRelations.get(entry.getKey()).setMatched(true);
            _newRelations.get(entry.getValue()).setMatched(true);
            newR2OldRidxMap.put(entry.getValue(), entry.getKey());
        }

        // after obtain the minimal changes,
        // expand the relations based on "expandLevel"
        Set<Relation> toExpend = new HashSet<>();
        if (_oldRelations.size() > _oldR2newRidxMap.size()) {
            for (int i = 0; i < _oldRelations.size(); i++) {
                if (!_oldR2newRidxMap.containsKey(i)) {
                    toExpend.add(_oldRelations.get(i));
                }
            }
        } else {
            // if all relations in the old code are matched
            // this condition happens when add new code but revising existing code
            // if happen, we expand the new added code as the minimal
            Set<Relation> temp = new HashSet<>();
            for (int i = 0; i < _newRelations.size(); i++) {
                if (!newR2OldRidxMap.containsKey(i)) {
                    _newRelations.get(i).expandDownward(temp);
                    temp.addAll(_newRelations.get(i).getUsedBy());
                }
            }
            for (Relation r : temp) {
                Integer index = newR2OldRidxMap.get(newR2index.get(r));
                if (index != null) {
                    toExpend.add(_oldRelations.get(index));
                    _oldRelations.get(index).setMatched(false);
                    r.setMatched(false);
                    _oldR2newRidxMap.remove(index);
                }
            }
        }

        // remove the number of minimal changed relations
        maxRNumbers -= _oldRelations.size() + _newRelations.size() - 2 * _oldR2newRidxMap.size();

        Set<Relation> expanded;
        Set<Relation> relations2Tag;
        int currentLevel = 0;
        while ((expandLevel--) > 0) {
            currentLevel++;
            expanded = new HashSet<>();
            for (Relation r : toExpend) {
                r.expandDownward(expanded);
                expanded.addAll(r.getUsedBy());
            }

            relations2Tag = new HashSet<>();
            // using the max relation number to
            // restrain the expansion
            for (Relation r : expanded) {
                // filter already considered relations
                if (!r.isConcerned()) {
                    relations2Tag.add(r);
                    Integer index = _oldR2newRidxMap.get(oldR2index.get(r));
                    if (index != null) {
                        relations2Tag.add(_newRelations.get(index));
                    }
                }
            }
            maxRNumbers -= relations2Tag.size();
            if (maxRNumbers < 0) {
                break;
            }

            // to tag all expended relations with
            // current expand level
            for (Relation r : relations2Tag) {
                r.setExpendedLevel(currentLevel);
            }
            toExpend = relations2Tag;
        }

        return this;
    }

    private Map<Relation, Integer> mapRelation2LstIndex(List<Relation> relations) {
        Map<Relation, Integer> map = new HashMap<>();
        if (relations != null) {
            for (int i = 0; i < relations.size(); i++) {
                map.put(relations.get(i), i);
            }
        }
        return map;
    }

    public List<Relation> getMinimizedOldRelations(boolean concerned) {
        if (!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for (Relation r : _oldRelations) {
            if (!r.isMatched() || (concerned && r.isConcerned())) {
                relations.add(r);
            }
        }
        return relations;
    }

    public List<Relation> getMinimizedNewRelations(boolean concerned) {
        if (!_minimized) minimize(0);
        List<Relation> relations = new LinkedList<>();
        for (Relation r : _newRelations) {
            if (!r.isMatched() || (concerned && r.isConcerned())) {
                relations.add(r);
            }
        }
        return relations;
    }

}
