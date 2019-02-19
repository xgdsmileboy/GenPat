/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

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
    private Map<String, String> _expr2Id;
    private Map<String, String> _methodName2Id;
    private Map<String, String> _type2Id;

    private final static Pattern REGEX = Pattern.compile("(EXPR|METH|TYPE)_\\d+");

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
        String id = _expr2Id.get(str);
        if (id == null) {
            id = "EXPR_" + _EXPR_ID;
            _EXPR_ID ++;
            _expr2Id.put(str, id);
        }
        return id;
    }

    public String getMethodID(Node node) {
        return getMethodID(node.toSrcString().toString());
    }

    private String getMethodID(String name) {
        String id = _methodName2Id.get(name);
        if (id == null) {
            id = "METH_" + _METHOD_ID;
            _METHOD_ID ++;
            _methodName2Id.put(name, id);
        }
        return id;
    }

    public String getTypeID(Node node) {
        return getTypeID(node.toSrcString().toString());
    }

    private String getTypeID(String type) {
        String id = _type2Id.get(type);
        if (id == null) {
            id = "TYPE_" + _TYPE_ID;
            _TYPE_ID ++;
            _type2Id.put(type, id);
        }
        return id;
    }

    public boolean isPlaceHolder(String string) {
        return REGEX.matcher(string).matches();
    }

    public Map<String, String> getExprIds() {
        return _expr2Id;
    }

    public Map<String, String> getMethodIds() {
        return _methodName2Id;
    }
}
