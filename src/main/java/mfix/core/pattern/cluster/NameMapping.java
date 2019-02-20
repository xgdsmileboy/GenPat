/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

import mfix.core.node.abs.id.AbsExpr;
import mfix.core.node.abs.id.AbsMethod;
import mfix.core.node.abs.id.AbsType;
import mfix.core.node.ast.Node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author: Jiajun
 * @date: 2019-02-17
 */
public class NameMapping implements Serializable {

    private static final long serialVersionUID = 7322982291608702980L;
    private int _EXPR_ID;
    private int _METHOD_ID;
    private int _TYPE_ID;
    private Map<String, AbsExpr> _expr2Id;
    private Map<String, AbsMethod> _methodName2Id;
    private Map<String, AbsType> _type2Id;

    private final static boolean debug = false;
    private final static Pattern REGEX = Pattern.compile(debug ? "(EXPR|METH|TYPE)_\\d+" : "EXPR|METH|TYPE");

    public NameMapping() {
        _EXPR_ID = 0;
        _METHOD_ID = 0;
        _TYPE_ID = 0;
        _expr2Id = new HashMap<>();
        _methodName2Id = new HashMap<>();
        _type2Id = new HashMap<>();
    }

    public String getExprID(Node node) {
        return getExprID(node.toSrcString().toString());
    }

    private String getExprID(String str) {
        AbsExpr expr = _expr2Id.get(str);
        if (expr == null) {
            expr = new AbsExpr(_EXPR_ID);
            _EXPR_ID ++;
            _expr2Id.put(str, expr);
        }
        return expr.toString();
    }

    public String getMethodID(Node node) {
        return getMethodID(node.toSrcString().toString());
    }

    private String getMethodID(String name) {
        AbsMethod meth = _methodName2Id.get(name);
        if (meth == null) {
            meth = new AbsMethod(_METHOD_ID);
            _METHOD_ID ++;
            _methodName2Id.put(name, meth);
        }
        return meth.toString();
    }

    public String getTypeID(Node node) {
        return getTypeID(node.toSrcString().toString());
    }

    private String getTypeID(String type) {
        AbsType absType = _type2Id.get(type);
        if (absType == null) {
            absType = new AbsType(_TYPE_ID);
            _TYPE_ID ++;
            _type2Id.put(type, absType);
        }
        return absType.toString();
    }

    public boolean isPlaceHolder(String string) {
        return REGEX.matcher(string).matches();
    }

    public Map<String, AbsExpr> getExprIds() {
        return _expr2Id;
    }

    public Map<String, AbsMethod> getMethodIds() {
        return _methodName2Id;
    }

    public Map<String, AbsType> getType2Id() {
        return _type2Id;
    }
}
