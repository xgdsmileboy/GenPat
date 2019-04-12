/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.ast.MatchLevel;
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

    protected boolean _abstractName = true;
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
    public void doAbstraction(CodeAbstraction abstracter) {
        if (isChanged() || isExpanded()) {
            _abstractType = _abstractType && abstracter.shouldAbstract(NodeUtils.distillBasicType(_exprTypeStr),
                    CodeAbstraction.Category.TYPE_TOKEN);
        }
        super.doAbstraction(abstracter);
        _abstract = _abstract && _abstractName && _abstractType;
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
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
        if ((!_modifications.isEmpty() && node.getNodeType() == getNodeType())
                || (_modifications.isEmpty() && node instanceof Expr)) {
            String typeStr = node.getTypeStr();
            boolean matchType = _abstractType ? true : Utils.safeStringEqual(getTypeStr(), typeStr);
            boolean matchName = _abstractName ? true : Utils.safeBufferEqual(toSrcString(), node.toSrcString());
            if (NodeUtils.match(matchName, matchType, level) && guarantee(node)) {
                return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings, level)
                        && NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
            }
        }
        return false;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (isConsidered()) {
            StringBuffer buff = new StringBuffer();
            if (!_abstractType) {
                String type = NodeUtils.distillBasicType(_exprTypeStr);
                if (!"?".equals(type)) {
                    keywords.add(type);
                    buff.append(type).append("::");
                }
            }
            buff.append(nameMapping.getExprID(this));
            return buff;
        } else {
            return null;
        }
    }

    protected StringBuffer leafFormalForm(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (!isAbstract() && (isChanged() || isExpanded())) {
            StringBuffer buffer = new StringBuffer();
            if (!_abstractType) {
                String type = NodeUtils.distillBasicType(_exprTypeStr);
                if (!"?".equals(type)) {
                    keywords.add(type);
                    buffer.append(type).append("::");
                }
            }
            if (!_abstractName) {
                keywords.add(toSrcString().toString());
                buffer.append(toSrcString());
            }else {
                // should not happen
                buffer.append(nameMapping.getExprID(this));
            }
            return buffer;
        } else if (isConsidered()) {
            return new StringBuffer(nameMapping.getExprID(this));
        } else {
            return null;
        }
    }

    private boolean guarantee(Node node) {
        return NodeUtils.isMethodName(this) == NodeUtils.isMethodName(node)
                && node.getNodeType() != TYPE.VARDECLEXPR && node.getNodeType() != TYPE.SINGLEVARDECL;
    }

    @Override
    public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
        if (node == null || isConsidered() != node.isConsidered() || !(node instanceof Expr)) {
            return false;
        }
        if (isConsidered()) {
            String typeStr = node.getTypeStr();
            boolean matchType = _abstractType ? true : Utils.safeStringEqual(getTypeStr(), typeStr);
            boolean matchName = _abstractName ? true : Utils.safeBufferEqual(toSrcString(), node.toSrcString());
            if (NodeUtils.match(matchName, matchType, MatchLevel.ALL) && guarantee(node) ) {
                int size = getModifications().size() + node.getModifications().size();
                if ((size == 0 && node instanceof Expr) || getNodeType() == node.getNodeType()) {
                    return NodeUtils.patternMatch(this, node, matchedNode);
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public String getTypeStr() {
        return NodeUtils.distillBasicType(_exprTypeStr);
    }
}