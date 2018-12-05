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
public class RUnion extends ObjRelation {

    private String _varName;
    private List<RAssign> _assigns = new LinkedList<RAssign>();

    public RUnion() {
        super(RelationKind.UNION);
    }

    public void setVarName(String name) {
        _varName = name;
    }

    public void addAssignRelation(RAssign assign) {
        _assigns.add(assign);
    }
}
