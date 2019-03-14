/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
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
        boolean consider = isConsidered() || parentConsidered;
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
        _fVector = new FVector();
        for (Expr expr : _exprs) {
            _fVector.combineFeature(expr.getFeatureVector());
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
            NodeUtils.greedyMatchListNode(_exprs, exprList.getExpr());
        }
        return match;
    }

    @Override
    public boolean genModifications() {
        if (getBindingNode() != null) {
            ExprList exprList = (ExprList) getBindingNode();
            _modifications = NodeUtils.genModificationList(this, _exprs, exprList.getExpr());
            if (!_modifications.isEmpty()) {
                _modifications.clear();
                _modifications.add(new Update(this, this, exprList));
            }
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
    public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            if (!_exprs.isEmpty()) {
                tmp = _exprs.get(0).transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                for (int i = 1; i < _exprs.size(); i++) {
                    stringBuffer.append(",");
                    tmp = _exprs.get(i).transfer(vars, exprMap);
                    if (tmp == null) return null;
                    stringBuffer.append(tmp);
                }
            }
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp;
        Node node = NodeUtils.checkModification(this);
        if (node != null) {
            return ((Update) node.getModifications().get(0)).apply(vars, exprMap);
        }

        if(!_exprs.isEmpty()) {
            tmp = _exprs.get(0).adaptModifications(vars, exprMap);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
            for(int i = 1; i < _exprs.size(); i++) {
                stringBuffer.append(",");
                tmp = _exprs.get(i).adaptModifications(vars, exprMap);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
            }
        }
        return stringBuffer;
    }
}
