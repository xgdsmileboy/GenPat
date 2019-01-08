/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.pattern.relation.struct.Structure;
import mfix.core.stats.element.ElementCounter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RStruct extends Relation {


    private static int ID = 0;
    /**
     * structure type
     */
    private Structure _structure;
    private int _id = 0;
    private Set<Relation> _control;

    public RStruct(Node node, Structure structure) {
        super(node, RelationKind.STRUCTURE);
        _structure = structure;
        _control = new HashSet<>();
        _id = genId();
    }

    public static int genId() {
        return ID ++;
    }

    public Structure getStructure() {
        return _structure;
    }

    public void addControls(Set<Relation> control) {
        _control.addAll(control);
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        set.addAll(_control);
        return set;
    }

    @Override
    protected void setControlDependency(RStruct rstruct, Set<Relation> controls) {
        _controlDependon = rstruct;
        controls.add(this);
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        // structures do not abstract
        _isAbstract = false;
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if(!super.match(relation, dependencies)) {
            return false;
        }
        RStruct struct = (RStruct) relation;
        return _structure.match(struct.getStructure());
    }

    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        if(super.greedyMatch(r, matchedRelationMap, varMapping)) {
            RStruct rStruct = (RStruct) r;
            if(_structure.rskind() == rStruct.getStructure().rskind()) {
                if(getParent() != null) {
                    if(getParent().greedyMatch(rStruct.getParent(), matchedRelationMap, varMapping)) {
                        matchedRelationMap.put(this, r);
                    }
                }
                return true;
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
    public String toString() {
        return String.format("[%s-%d]", _structure.toString(), _id);
    }
}