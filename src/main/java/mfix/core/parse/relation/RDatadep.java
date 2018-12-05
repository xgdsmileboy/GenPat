/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RDatadep extends Relation {

    private List<ObjRelation> _valueDecideRelation = new LinkedList<>();
    private Relation _useRelation;

    public RDatadep() {
        super(RelationKind.DATADEPEND);
    }

    public void addDecideRelation(ObjRelation relation) {
        _valueDecideRelation.add(relation);
    }

    public void setUseRelation(Relation relation) {
        _useRelation = relation;
    }

}
