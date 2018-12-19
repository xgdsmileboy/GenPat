/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.relation.struct.Structure;

import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RStruct extends Relation {

    /**
     * structure type
     */
    private Structure _structure;

    public RStruct(Structure structure) {
        super(RelationKind.STRUCTURE);
        _structure = structure;
    }

    public Structure getStructure() {
        return _structure;
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        return set;
    }

    @Override
    public void doAbstraction0(double frequency) {
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
    public String toString() {
        return _structure.toString();
    }
}