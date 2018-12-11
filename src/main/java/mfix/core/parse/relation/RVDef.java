/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public class RVDef extends RDef {

    /**
     * value of a constant
     */
    private Object _value;

    public RVDef() {
        super(RelationKind.VIRTUALDEFINE);
    }

    public void setValue(Object value) {
        _value = value;
    }

    public Object getValue() {
        return _value;
    }

    @Override
    public boolean match(Relation relation) {
        if (!super.match(relation)) {
            return false;
        }
        RVDef def = (RVDef) relation;
        return _value.equals(def.getValue());
    }
}
