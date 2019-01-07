/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.core.parse.node.Node;
import mfix.core.stats.element.ElementCounter;

import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RKid extends Relation {

    /**
     * Record the structure: a relation can be the
     * kid of a structure, which captures the control
     * flow of the program
     */
    private RStruct _structure;
    /**
     * The index of child node in the {@code _structure}
     * NOTE: this index does not denotes the order of
     * the child against the siblings, but the coarse-grained
     * location information, i.e., while-condition and while-body.
     */
    private int _index;
    /**
     * The child node
     */
    private Relation _child;

    public RKid(Node node, RStruct structure) {
        super(node, RelationKind.CHILD);
        _structure = structure;
        this.usedBy(structure);
    }

    public void setIndex(int index) {
        _index = index;
    }

    public void setChild(Relation child) {
        _child = child;
        _child.usedBy(this);
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
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        set.add(_structure);
        _structure.expandDownward(set);
        set.add(_child);
        if(!(_child instanceof ObjRelation)) {
            _child.expandDownward(set);
        }
        return set;
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        _child.doAbstraction(counter);
        _isAbstract = !_child.isConcerned() || _child.isAbstract();
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }

        RKid kid = (RKid) relation;
        if (_index != kid.getIndex()) {
            return false;
        }

        if(_child.match(kid.getChildRelation(), dependencies)) {
            dependencies.add(new Pair<>(_child, kid.getChildRelation()));
            return true;
        }
        return false;
    }

    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> dependencies, Map<String, String> varMapping) {
        if(super.greedyMatch(r, dependencies, varMapping)) {
            RKid kid = (RKid) r;
            if(_index == kid._index && _structure.greedyMatch(kid._structure, dependencies, varMapping)
                    && _child.greedyMatch(kid._child, dependencies, varMapping)) {
                dependencies.put(this, r);
                if(getParent() != null) {
                    getParent().greedyMatch(kid.getParent(), dependencies, varMapping);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean foldMatching(Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public String toString() {
        return String.format("[KID (%d)| p@%s | c@%s]", _index, _structure.toString(), _child);
    }
}
