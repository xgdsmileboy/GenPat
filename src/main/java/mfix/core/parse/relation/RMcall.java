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
public class RMcall extends ObjRelation {

    /**
     * This field is to distinguish different
     * kinds of "method calls",
     *
     * In the current model, the method call contains
     * normal method invocation, super method invocation,
     * class instance creation, case expression, ... etc.
     */
    private MCallType _type;
    /**
     * The receiver object of the method call
     */
    private ObjRelation _receiver;
    /**
     * Name of the method call
     * possible values:
     *  normal method invocation : method name
     *  class instance creation : class name
     *  super method invocation : method name
     *  super constructor invocation : null
     *  array creation : array element type
     *  cast expression : case type
     */
    private String _methodName;

    public RMcall(MCallType type) {
        super(RelationKind.MCALL);
        _type = type;
    }

    public void setReciever(ObjRelation reciever) {
        _receiver = reciever;
    }

    public void setMethodName(String name) {
        _methodName = name;
    }

    public MCallType getCallType() {
        return _type;
    }

    public ObjRelation getReciever() {
        return _receiver;
    }

    public String getMethodName() {
        return _methodName;
    }

    @Override
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if(!super.match(relation, dependencies)) {
            return false;
        }
        RMcall mcall = (RMcall) relation;
        if(_type != mcall.getCallType()) {
            return false;
        }

        if(!Utils.safeStringEqual(_methodName, mcall.getMethodName())) {
            return false;
        }

        if(_receiver == null) {
            if (mcall.getReciever() != null) {
                return false;
            } else {
                return true;
            }
        }
        if(_receiver.match(mcall.getReciever(), dependencies)) {
            dependencies.add(new Pair<>(_receiver, mcall.getReciever()));
            return true;
        }
        return false;
    }

    public enum MCallType{
        NORM_MCALL,
        SUPER_MCALL,
        SUPER_INIT_CALL,
        INIT_CALL,
        NEW_ARRAY,
        CAST,
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("[");
        if(_receiver != null) {
            buffer.append(_receiver.toString() + ".");
        }
        switch(_type) {
            case NORM_MCALL:
                buffer.append(_methodName);
                buffer.append("()");
                break;
            case SUPER_MCALL:
                buffer.append("super.");
                buffer.append(_methodName);
                buffer.append("()");
                break;
            case SUPER_INIT_CALL:
                buffer.append("super");
                buffer.append("()");
                break;
            case INIT_CALL:
                buffer.append("new ");
                buffer.append(_methodName);
                buffer.append("()");
                break;
            case NEW_ARRAY:
                buffer.append("new ");
                buffer.append(_methodName);
                buffer.append("[]");
                break;
            case CAST:
                buffer.append("(");
                buffer.append(_methodName);
                buffer.append(")");
                buffer.append("()");
            default:
        }
        buffer.append("]");
        return buffer.toString();
    }
}
