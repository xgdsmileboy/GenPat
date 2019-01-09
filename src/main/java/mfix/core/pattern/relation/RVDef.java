/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.node.modify.Modification;
import mfix.core.stats.element.ElementCounter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RVDef extends RDef {

    /**
     * value of a constant
     */
    private Object _value;

    public RVDef(Node node) {
        super(node, RelationKind.VIRTUALDEFINE);
    }

    public void setValue(Object value) {
        _value = value;
    }

    public Object getValue() {
        return _value;
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        // TODO : we can simply abstract all virtual variables
        _isAbstract = true;
    }

    @Override
    public String getExprString() {
        if(_name != null) {
            return _name;
        }
        if(_value != null) {
            return _value.toString();
        }
        return "null";
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }
        RVDef def = (RVDef) relation;
        if(_value == null) {
            return def.getValue() == null;
        }
        return _value.equals(def.getValue());
    }

    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        return super.greedyMatch(r, matchedRelationMap, varMapping);
    }

    @Override
    public boolean foldMatching(Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public boolean canGroup(Relation r) {
        return super.canGroup(r);
    }

    @Override
    public boolean assemble(List<Modification> modifications, boolean isAdded) {
        // TODO
        return super.assemble(modifications, isAdded);
    }

    @Override
    public StringBuffer buildTargetSource() {
        // TODO
        return super.buildTargetSource();
    }

    @Override
    public String toString() {
        if(_name != null) {
            return _name;
        } else {
            return _value == null ? "null" : _value.toString();
        }
    }
}
