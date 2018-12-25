/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.core.parse.node.Node;

/**
 * This kind of relation denotes the relation returns an
 * expression that can be used as an argument of other expression
 * i.e., the relation represents an object
 *
 * @author: Jiajun
 * @date: 2018/12/5
 */
public abstract class ObjRelation extends Relation {

    protected String _objType;

    protected ObjRelation(Node node, RelationKind kind) {
       super(node, kind);
    }

    public void setObjType(String string) {
        _objType = string;
    }

    public String getObjType() {
        return _objType;
    }
}
