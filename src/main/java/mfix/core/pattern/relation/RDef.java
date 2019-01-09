/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.stats.element.ElementCounter;

import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RDef extends ObjRelation {

    /**
     * modifiers of the defined variables
     */
    private String _modifiers;
    /**
     * {@code java.lang.String} format of variable type
     */
    protected String _typeStr;
    /**
     * variable names
     * NOTE: this can be null if it is a constant value
     * (virtual variable definition)
     */
    protected String _name;
    /**
     * The initializer of the variable definition
     */
    private ObjRelation _initializer;

    public RDef(Node node) {
        this(node, RelationKind.DEFINE);
    }

    protected RDef(Node node, RelationKind kind) {
        super(node, kind);
    }

    public void setModifiers(String modifiers) {
        _modifiers = modifiers;
    }

    public void setTypeStr(String typeStr) {
        _typeStr = typeStr;
    }

    public void setName(String name) {
        _name = name;
    }

    public void setInitializer(ObjRelation initializer) {
        _initializer = initializer;
        _initializer.usedBy(this);
    }

    public String getModifiers() {
        return _modifiers;
    }

    public String getTypeString() {
        return _typeStr;
    }

    public String getName() {
        return _name;
    }

    public ObjRelation getInitializer() {
        return _initializer;
    }

    @Override
    public String getExprString() {
        return _name;
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        if(_initializer != null) {
            set.add(_initializer);
        }
        return set;
    }

    @Override
    protected void setControlDependency(RStruct rstruct, Set<Relation> controls) {
        if(getParent() == rstruct) {
            controls.add(this);
            if (_initializer != null) {
                _initializer.setControlDependency(rstruct, controls);
            }
        }
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        if(_initializer != null) {
            _initializer.doAbstraction(counter);
        }
        // this relation should be concretely matched
        // NOTE: here it does not denote the name of
        // variables but the relation it self (var-define)
        _isAbstract = false;
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }
        RDef def = (RDef) relation;
//        if(!Utils.safeStringEqual(_modifiers, def.getModifiers())
//                || !Utils.safeStringEqual(_typeStr, def.getTypeString())
//                || !Utils.safeStringEqual(_name, def.getName())) {
//            return false;
//        }
        if(!Utils.safeStringEqual(_name, def.getName())) {
            return false;
        }

        if(_initializer == null) {
            return def.getInitializer() == null;
        }

        if(_initializer.match(def.getInitializer(), dependencies)) {
            dependencies.add(new Pair<>(_initializer, def.getInitializer()));
            return true;
        }
        return false;
    }

    @Override
    public boolean greedyMatch(Relation r, Map<Relation, Relation> matchedRelationMap, Map<String, String> varMapping) {
        if(super.greedyMatch(r, matchedRelationMap, varMapping)
                || (r.getRelationKind() == RelationKind.VIRTUALDEFINE && ((RDef) r).getName() != null)) {
            RDef def = (RDef) r;
            if(matchDependencies(r.getDependencies(), matchedRelationMap, varMapping)) {
                String type = varMapping.get(_typeStr);
                if (type == null || type.equals(def.getTypeString())) {
                    _matchedBinding = r;
                    r._matchedBinding = this;
                    matchedRelationMap.put(this, r);
                    varMapping.put(_typeStr, def.getTypeString());
                    varMapping.put(_name, def.getName());
                    return true;
                }
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
        StringBuffer buffer = new StringBuffer();
        if(_modifiers != null) {
            buffer.append(_modifiers + " ");
        }
        buffer.append(_typeStr + " ");
        buffer.append(_name);
        if(_initializer != null) {
            buffer.append("=");
            buffer.append(_initializer.getExprString());
        }
        return buffer.toString();
    }
}
