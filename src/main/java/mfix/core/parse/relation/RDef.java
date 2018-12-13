/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.common.util.Utils;

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
    private String _typeStr;
    /**
     * variable names
     * NOTE: this can be null if it is a constant value
     * (virtual variable definition)
     */
    private String _name;
    /**
     * The initializer of the variable definition
     */
    private ObjRelation _initializer;

    public RDef() {
        this(RelationKind.DEFINE);
    }

    protected RDef(RelationKind kind) {
        super(kind);
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
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (!super.match(relation, dependencies)) {
            return false;
        }
        RDef def = (RDef) relation;
        if(!Utils.safeStringEqual(_modifiers, def.getModifiers())
                || !Utils.safeStringEqual(_typeStr, def.getTypeString())
                || !Utils.safeStringEqual(_name, def.getName())) {
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
    public String toString() {
        StringBuffer buffer = new StringBuffer("[");
        if(_modifiers != null) {
            buffer.append(_modifiers + " ");
        }
        buffer.append(_typeStr + " ");
        buffer.append(_name);
        if(_initializer != null) {
            buffer.append("=");
            buffer.append(_initializer.toString());
        }
        buffer.append("]");
        return buffer.toString();
    }
}
