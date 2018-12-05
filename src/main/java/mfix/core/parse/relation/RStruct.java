/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.core.parse.relation.struct.Structure;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RStruct extends Relation {

    private Structure _structure;

    public RStruct() {
        super(RelationKind.STRUCTURE);
    }

    public void setStructure(Structure structure) {
        _structure = structure;
    }
}
