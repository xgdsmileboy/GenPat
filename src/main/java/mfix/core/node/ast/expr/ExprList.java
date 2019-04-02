/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ExprList extends Node {

    private static final long serialVersionUID = -1155629329446419826L;
    private List<Expr> _exprs;

    public ExprList(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        _nodeType = TYPE.EXPRLST;
        _fIndex = VIndex.EXP_LIST;
    }

    public void setExprs(List<Expr> exprs) {
        this._exprs = exprs;
    }

    public List<Expr> getExpr() {
        return _exprs;
    }

    @Override
    public boolean compare(Node other) {
        if (other instanceof ExprList) {
            ExprList exprList = (ExprList) other;
            if (_exprs.size() == exprList._exprs.size()) {
                for (int i = 0; i < _exprs.size(); i++) {
                    if (!_exprs.get(i).compare(exprList._exprs.get(i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (_exprs.size() > 0) {
            stringBuffer.append(_exprs.get(0).toSrcString());
            for (int i = 1; i < _exprs.size(); i++) {
                stringBuffer.append(",");
                stringBuffer.append(_exprs.get(i).toSrcString());
            }
        }
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//        boolean consider = isConsidered() || parentConsidered;
        boolean consider = isConsidered();
        if (_exprs.size() > 0) {
            List<StringBuffer> strings = new ArrayList<>(_exprs.size());
            for (Expr expr : _exprs) {
                if (expr.formalForm(nameMapping, consider, keywords) != null) {
                    strings.add(expr.formalForm(nameMapping, consider, keywords));
                } else if (isConsidered()) {
                    strings.add(new StringBuffer(nameMapping.getExprID(expr)));
                }
            }
            StringBuffer buffer = null;
            if (!strings.isEmpty()) {
                buffer = new StringBuffer(strings.get(0));
                for (int i = 1; i < strings.size(); i++) {
                    buffer.append(',');
                    buffer.append(strings.get(i));
                }
            } else if (isConsidered()){
                return new StringBuffer();
            }
            return buffer;
        } else if (isConsidered()) {
            return new StringBuffer();
        } else {
            return null;
        }
    }

    @Override
    public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
        if (node == null || isConsidered() != node.isConsidered()){
            return false;
        }
        if (isConsidered()) {
            if (getModifications().isEmpty() || node.getNodeType() == TYPE.EXPRLST) {
                return NodeUtils.patternMatch(this, node, matchedNode, false);
            }
            return false;
        }
        return true;
    }

    @Override
    public Stmt getParentStmt() {
        return getParent().getParentStmt();
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_exprs.size());
        children.addAll(_exprs);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _selfFVector.inc(FVector.E_EXPLIST);

        _completeFVector = new FVector();
        _completeFVector.inc(FVector.E_EXPLIST);
        for (Expr expr : _exprs) {
            _completeFVector.combineFeature(expr.getFeatureVector());
        }
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        if (_exprs.size() > 0) {
            _tokens.addAll(_exprs.get(0).tokens());
            for (int i = 1; i < _exprs.size(); i++) {
                _tokens.add(",");
                _tokens.addAll(_exprs.get(i).tokens());
            }
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        ExprList exprList = null;
        boolean match = false;
        if (canBinding(node)) {
            exprList = (ExprList) node;
            setBindingNode(node);
            match = true;
        } else if (getBindingNode() != null) {
            exprList = (ExprList) getBindingNode();
            match = (exprList == node);
        }

        if (exprList == null) {
            continueTopDownMatchNull();
        } else {
            List<Expr> exprs = exprList.getExpr();
            int start = 0;
            for (int i = 0; i < _exprs.size(); i++) {
                for (int j = start; j < exprs.size(); j ++) {
                    if (_exprs.get(i).postAccurateMatch(exprs.get(j))) {
                        start = j + 1;
                        break;
                    }
                }
            }
//            NodeUtils.greedyMatchListNode(_exprs, exprList.getExpr());
        }
        return match;
    }

    @Override
    public boolean genModifications() {
        if (getBindingNode() != null) {
            ExprList exprList = (ExprList) getBindingNode();
            _modifications = NodeUtils.genModificationList(this, _exprs, exprList.getExpr());
        }
        return true;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if(node instanceof ExprList) {
            return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
                    && NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
        }
        return false;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            if (!_exprs.isEmpty()) {
                tmp = _exprs.get(0).transfer(vars, exprMap, retType, exceptions);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                for (int i = 1; i < _exprs.size(); i++) {
                    stringBuffer.append(",");
                    tmp = _exprs.get(i).transfer(vars, exprMap, retType, exceptions);
                    if (tmp == null) return null;
                    stringBuffer.append(tmp);
                }
            }
        }
        return stringBuffer;
    }


    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                 Set<String> exceptions) {
        Node pnode = NodeUtils.checkModification(this);
        if (pnode != null) {

            Map<Node, List<StringBuffer>> insertionBefore = new HashMap<>();
            Map<Node, List<StringBuffer>> insertionAfter = new HashMap<>();
            Map<Integer, List<StringBuffer>> insertionAt = new HashMap<>();
            Map<Node, StringBuffer> map = new HashMap<>(_exprs.size());
            if (!Matcher.applyNodeListModifications(pnode.getModifications(), _exprs, insertionBefore,
                    insertionAfter, insertionAt, map, vars, exprMap, retType, exceptions)) {
                return null;
            }

            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer tmp;

            boolean first = true;
            int curIndex = 0;
            for(int index = 0; index < _exprs.size(); index ++) {
                Node node = _exprs.get(index);
                List<StringBuffer> list;
                while(insertionAt.containsKey(curIndex)) {
                    list = insertionAt.get(curIndex);
                    for (int i = 0; i < list.size(); i++) {
                        if (!first) {
                            stringBuffer.append(',');
                        }
                        first = false;
                        stringBuffer.append(list.get(i));
                    }
                    curIndex += list.size();
                    insertionAt.remove(index);
                }

                if (map.containsKey(node)) {
                    StringBuffer update = map.get(node);
                    if (update != null) {
                        if(!first) {
                            stringBuffer.append(",");
                        }
                        first = false;
                        stringBuffer.append(update);
                        curIndex ++;
                    }
                } else {
                    if(!first) {
                        stringBuffer.append(",");
                    }
                    first = false;
                    tmp = node.adaptModifications(vars, exprMap, retType, exceptions);
                    if(tmp == null) return null;
                    stringBuffer.append(tmp);
                    curIndex ++;
                }
            }
            if (!insertionAt.isEmpty()) {
                List<Map.Entry<Integer, List<StringBuffer>>> list = new ArrayList<>(insertionAt.entrySet());
                list.stream().sorted(Comparator.comparingInt(Map.Entry::getKey));
                for (Map.Entry<Integer, List<StringBuffer>> entry : list) {
                    for (StringBuffer s : entry.getValue()) {
                        stringBuffer.append(',').append(s);
                    }
                }
            }
            return stringBuffer;
        } else {
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer tmp;
            if (!_exprs.isEmpty()) {
                tmp = _exprs.get(0).adaptModifications(vars, exprMap, retType, exceptions);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                for (int i = 1; i < _exprs.size(); i++) {
                    stringBuffer.append(",");
                    tmp = _exprs.get(i).adaptModifications(vars, exprMap, retType, exceptions);
                    if (tmp == null) return null;
                    stringBuffer.append(tmp);
                }
            }
            return stringBuffer;
        }
    }

//    @Override
//    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
//                                           Set<String> exceptions) {
//        StringBuffer stringBuffer = new StringBuffer();
//        StringBuffer tmp;
//        Node node = NodeUtils.checkModification(this);
//        if (node != null) {
//            return ((Update) node.getModifications().get(0)).apply(vars, exprMap, retType, exceptions);
//        }
//
//        if(!_exprs.isEmpty()) {
//            tmp = _exprs.get(0).adaptModifications(vars, exprMap, retType, exceptions);
//            if(tmp == null) return null;
//            stringBuffer.append(tmp);
//            for(int i = 1; i < _exprs.size(); i++) {
//                stringBuffer.append(",");
//                tmp = _exprs.get(i).adaptModifications(vars, exprMap, retType, exceptions);
//                if(tmp == null) return null;
//                stringBuffer.append(tmp);
//            }
//        }
//        return stringBuffer;
//    }
}
