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
public class RMcall extends ObjRelation {

    private MCallType _type;
    private ObjRelation _reciever;
    private String _methodName;

    public RMcall(MCallType type) {
        super(RelationKind.MCALL);
        _type = type;
    }

    public void setReciever(ObjRelation reciever) {
        _reciever = reciever;
    }

    public void setMethodName(String name) {
        _methodName = name;
    }

    public enum MCallType{
        NORM_MCALL,
        SUPER_MCALL,
        SUPER_INIT_CALL,
        INIT_CALL,
        NEW_ARRAY,
        CAST,
    }
}
