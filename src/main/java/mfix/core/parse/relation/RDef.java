/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RDef extends ObjRelation {

    private String _modifiers;
    private String _typeStr;
    private String _name;
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

    public String getName() {
        return _name;
    }
}
