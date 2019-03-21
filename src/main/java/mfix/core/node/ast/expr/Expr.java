/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.pattern.cluster.NameMapping;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Expr extends Node {

    private static final long serialVersionUID = 1325289211050496258L;
    protected String _exprTypeStr = "?";
    protected transient Type _exprType = null;

    protected boolean _abstractNode = true;
    protected boolean _abstractType = true;

    protected Expr(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node, null);
    }

    public void setType(Type exprType) {
        _exprType = exprType;
        if (exprType == null) {
            _exprTypeStr = "?";
        } else {
            _exprTypeStr = exprType.toString();
        }
    }

    public Type getType() {
        return _exprType;
    }

    public String getTypeString() {
        return _exprTypeStr;
    }

    @Override
    public Stmt getParentStmt() {
        return getParent().getParentStmt();
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean genModifications() {
        if (getBindingNode() == null) {
            LevelLogger.warn("Should not be null since we cannot delete an expression : " + getFileName());
            return false;
        }
        return true;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if ((!_modifications.isEmpty() && node.getNodeType() == getNodeType())
                || (_modifications.isEmpty() && node instanceof Expr)) {
            if ((!"boolean".equals(getTypeString()) || "boolean".equals(((Expr) node).getTypeString()))
                    && !(node instanceof Operator)) {
                boolean match = isAbstract() || ifMatch0(node, matchedNode, matchedStrings);
                return match && NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
                        && NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
            }
        }
        return false;
    }

    // this method should be abstract and reimplemented in all the sub expression classes
    // currently, I did not consider the structure of the expression but only the keywords
    public boolean ifMatch0(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        Set<String> keys = flattenTreeNode(new LinkedList<>()).stream()
                .filter(n -> NodeUtils.isSimpleExpr(n) && !n.isAbstract())
                .map(n -> n.toSrcString().toString())
                .collect(Collectors.toSet());
        String content = node.toSrcString().toString();
        for (String key : keys) {
            if (!content.contains(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (isConsidered()) {
            return new StringBuffer(nameMapping.getExprID(this));
        } else {
            return null;
        }
    }

    protected StringBuffer leafFormalForm(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (!isAbstract() && (isChanged() || isExpanded())) {
            StringBuffer buffer = toSrcString();
            keywords.add(buffer.toString());
            return buffer;
        } else if (isConsidered()) {
            return new StringBuffer(nameMapping.getExprID(this));
        } else {
            return null;
        }
    }

    @Override
    public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
        if (node == null || isConsidered() != node.isConsidered()) {
            return false;
        }
        if (isConsidered()) {
            int size = getModifications().size() + node.getModifications().size();
            if ((size == 0 && node instanceof Expr) || getNodeType() == node.getNodeType()) {
                return NodeUtils.patternMatch(this, node, matchedNode, false);
            }
            return false;
        }
        return true;
    }
}