/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.node.Node;
import mfix.core.parse.relation.struct.Structure;
import mfix.core.stats.element.ElementCounter;

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

    public RStruct(Node node, Structure structure) {
        super(node, RelationKind.STRUCTURE);
        _structure = structure;
        _id = genId();
    }

    public static int genId() {
        return ID ++;
    }

    public Structure getStructure() {
        return _structure;
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        return set;
    }

    @Override
    public void doAbstraction0(ElementCounter counter, double frequency) {
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
    public boolean foldMatching(Relation r, Set<Pair<Relation, Relation>> dependencies,
                                Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public String toString() {
        return String.format("[%s-%d]", _structure.toString(), _id);
    }
}