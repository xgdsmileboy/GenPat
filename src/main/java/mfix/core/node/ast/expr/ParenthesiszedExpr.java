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
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
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
public class ParenthesiszedExpr extends Expr {

	private static final long serialVersionUID = -8417799816148324460L;
	private Expr _expression = null;
	
	/**
	 * ParenthesizedExpression:
     *	( Expression )
	 */
	public ParenthesiszedExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PARENTHESISZED;
		_fIndex = VIndex.EXP_PARENTHESISED;
	}

	public void setExpr(Expr expression) {
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer buffer = _expression.formalForm(nameMapping, consider, keywords);
		if (buffer != null) {
			if (nameMapping.isPlaceHolder(buffer.toString())) {
				return buffer;
			}
			return new StringBuffer("(").append(buffer).append(')');
		}
		return super.toFormalForm0(nameMapping, parentConsidered, keywords);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
			match = _expression.compare(parenthesiszedExpr._expression);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}

	@Override
	public void computeFeatureVector() {
		_completeFVector = new FVector();
		_completeFVector.combineFeature(_expression.getFeatureVector());

		_selfFVector = new FVector();
		_selfFVector.combineFeature(_expression.getSingleFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		ParenthesiszedExpr parenthesiszedExpr = null;
		boolean match = false;
		if (compare(node)) {
			parenthesiszedExpr = (ParenthesiszedExpr) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			parenthesiszedExpr = (ParenthesiszedExpr) getBindingNode();
			match = (parenthesiszedExpr == node);
		} else if (canBinding(node)) {
			parenthesiszedExpr = (ParenthesiszedExpr) node;
			setBindingNode(node);
			match = true;
		}
		if (parenthesiszedExpr == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(parenthesiszedExpr.getExpression());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) getBindingNode();
			if (_expression.getBindingNode() != parenthesiszedExpr.getExpression()) {
				Update update = new Update(this, _expression, parenthesiszedExpr.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
		}
		return true;
	}

	@Override
	public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr pe = (ParenthesiszedExpr) node;
			if (NodeUtils.matchSameNodeType(getExpression(), pe.getExpression(), matchedNode, matchedStrings)) {
				getExpression().greedyMatchBinding(pe.getExpression(), matchedNode, matchedStrings);
			}
		}
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			stringBuffer.append("(");
			StringBuffer tmp = _expression.transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			return ((Update) node.getModifications().get(0)).apply(vars, exprMap, retType, exceptions, metric);
		}

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		StringBuffer tmp = _expression.adaptModifications(vars, exprMap, retType, exceptions, metric);
		if (tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		return stringBuffer;
	}
}
