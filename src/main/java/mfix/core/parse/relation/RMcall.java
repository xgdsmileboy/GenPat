/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation;

import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.parse.Pattern;
import mfix.core.parse.node.Node;
import mfix.core.stats.element.ElementCounter;
import mfix.core.stats.element.ElementException;
import mfix.core.stats.element.ElementQueryType;
import mfix.core.stats.element.MethodElement;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class RMcall extends ObjRelation {

    public enum MCallType{
        NORM_MCALL("Normal Call"),
        SUPER_MCALL("Super Call"),
        SUPER_INIT_CALL("Super Init"),
        INIT_CALL("Init Call"),
        NEW_ARRAY("New Array"),
        CAST("Cast Expression");

        private String _value;
        private MCallType(String value) {
            _value = value;
        }

        @Override
        public String toString() {
            return _value;
        }
    }

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

    private List<RArg> _args;

    public RMcall(Node node, MCallType type) {
        super(node, RelationKind.MCALL);
        _type = type;
        _args = new LinkedList<>();
    }

    public void setReciever(ObjRelation reciever) {
        _receiver = reciever;
        if(_receiver != null) {
            _receiver.usedBy(this);
        }
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
    public void addArg(RArg arg) {
        _args.add(arg);
    }


    private StringBuffer buildArgString() {
        StringBuffer buffer = new StringBuffer("(");
        boolean first = true;
        Collections.sort(_args, new Comparator<RArg>() {
            @Override
            public int compare(RArg o1, RArg o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        for(RArg r : _args) {
            if(first) {
                buffer.append(r.getExprString());
            } else {
                buffer.append("," + r.getExprString());
            }
        }
        buffer.append(")");
        return buffer;
    }

    @Override
    public String getExprString() {
        StringBuffer buffer = new StringBuffer();
        if(_receiver != null) {
            buffer.append(_receiver.getExprString() + ".");
        }
        switch(_type) {
            case NORM_MCALL:
                buffer.append(_methodName);
                buffer.append(buildArgString());
                break;
            case SUPER_MCALL:
                buffer.append("super.");
                buffer.append(_methodName);
                buffer.append(buildArgString());
                break;
            case SUPER_INIT_CALL:
                buffer.append("super");
                buffer.append(buildArgString());
                break;
            case INIT_CALL:
                buffer.append("new ");
                buffer.append(_methodName);
                buffer.append(buildArgString());
                break;
            case NEW_ARRAY:
                buffer.append("new ");
                buffer.append(_methodName);
                for(RArg r : _args) {
                    buffer.append("[");
                    buffer.append(r.getExprString());
                    buffer.append("]");
                }
                break;
            case CAST:
                buffer.append("(");
                buffer.append(_methodName);
                buffer.append(")");
                buffer.append(buildArgString());
            default:
        }
        return buffer.toString();
    }

    @Override
    protected Set<Relation> expandDownward0(Set<Relation> set) {
        if(_receiver != null) {
            set.add(_receiver);
        }
        set.addAll(_args);
        return set;
    }

    @Override
    public void doAbstraction0(ElementCounter counter) {
        if(_receiver != null) {
            _receiver.doAbstraction(counter);
        }
        switch (_type) {
            case SUPER_INIT_CALL:
            case INIT_CALL:
            case NEW_ARRAY:
            case CAST:
                break;
            case NORM_MCALL:
            case SUPER_MCALL:
                ElementQueryType qtype = new ElementQueryType(false,
                        false, ElementQueryType.CountType.COUNT_FILES);
                MethodElement methodElement = new MethodElement(_methodName, null);
                methodElement.setArgsNumber(_args.size());
                try {
                    _isAbstract = counter.count(methodElement, qtype) < Pattern.API_FREQUENCY;
                } catch (ElementException e) {
                    _isAbstract = true;
                }

                break;
        }
        for(RArg r : _args) {
            r.doAbstraction(counter);
        }
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

    @Override
    public boolean foldMatching(Relation r, Set<Pair<Relation, Relation>> dependencies,
                                Map<String, String> varMapping) {
        // TODO : to finish
        return false;
    }

    @Override
    public String toString() {
        boolean used = false;
        for(Relation r : _usedBy) {
            if(r instanceof RKid) {
              continue;
            }
            used = true;
            break;
        }
        if(used) {
            return "";
        }
        return getExprString();
    }
}
