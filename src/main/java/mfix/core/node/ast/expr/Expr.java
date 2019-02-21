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
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Expr extends Node {

    private static final long serialVersionUID = 1325289211050496258L;
    protected String _exprTypeStr = "?";
    protected transient Type _exprType = null;

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
            LevelLogger.error("Should not be null since we cannot delete an expression");
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
                return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
                        && NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
            }
        }
        return false;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (isConsidered()) {
            return new StringBuffer(nameMapping.getExprID(this));
        } else {
            return null;
        }
    }

    protected StringBuffer leafFormalForm(boolean parentConsidered, Set<String> keywords) {
        if (!isAbstract() && (parentConsidered || isConsidered())) {
            StringBuffer buffer = toSrcString();
            keywords.add(buffer.toString());
            return buffer;
        } else {
            return null;
        }
    }

    @Override
    public boolean patternMatch(Node node) {
        if (node == null || isConsidered() != node.isConsidered()) {
            return false;
        }
        if (isConsidered()) {
            if (node instanceof Expr) {
                return NodeUtils.patternMatch(this, node, false);
            }
            return false;
        }
        return true;
    }
}