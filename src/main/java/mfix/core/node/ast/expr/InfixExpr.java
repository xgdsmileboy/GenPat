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
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Modification;
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
public class InfixExpr extends Expr {

	private static final long serialVersionUID = -5825228781443981995L;
	private Expr _lhs = null;
	private InfixOperator _operator = null;
	private Expr _rhs = null;
	
	/**
	 * InfixExpression:
     *		Expression InfixOperator Expression { InfixOperator Expression }
	 */
	public InfixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INFIXEXPR;
		_fIndex = VIndex.EXP_INFIX;
	}

	public void setLeftHandSide(Expr lhs) {
		_lhs = lhs;
	}

	public void setOperator(InfixOperator operator) {
		_operator = operator;
	}

	public void setRightHandSide(Expr rhs) {
		_rhs = rhs;
	}

	public InfixOperator getOperator() {
		return _operator;
	}

	public Expr getLhs() {
		return _lhs;
	}

	public Expr getRhs() {
		return _rhs;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_lhs.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_rhs.toSrcString());
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer lhs = _lhs.formalForm(nameMapping, consider, keywords);
		StringBuffer op = _operator.formalForm(nameMapping, consider, keywords);
		StringBuffer rhs = _rhs.formalForm(nameMapping, consider, keywords);
		if (op == null && lhs == null && rhs == null) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(lhs == null ? nameMapping.getExprID(_lhs) : lhs)
				.append(op == null ? _operator.toSrcString().toString() : op)
				.append(rhs == null ? nameMapping.getExprID(_rhs) : rhs);
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_lhs.tokens());
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_rhs.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof InfixExpr) {
			InfixExpr infixExpr = (InfixExpr) other;
			match = _operator.compare(infixExpr._operator) && _lhs.compare(infixExpr._lhs)
					&& _rhs.compare(infixExpr._rhs);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_lhs);
		children.add(_operator);
		children.add(_rhs);
		return children;
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(_operator.toSrcString().toString());

		_completeFVector = new FVector();
		_completeFVector.combineFeature(_selfFVector);
		_completeFVector.combineFeature(_lhs.getFeatureVector());
		_completeFVector.combineFeature(_rhs.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		InfixExpr infixExpr = null;
		if (compare(node)) {
			infixExpr = (InfixExpr) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			infixExpr = (InfixExpr) getBindingNode();
			match = (infixExpr == node);
		} else if (canBinding(node)) {
			infixExpr = (InfixExpr) node;
			setBindingNode(node);
			match = true;
		}
		if (infixExpr == null) {
			continueTopDownMatchNull();
		} else {
			_lhs.postAccurateMatch(infixExpr.getLhs());
			_operator.postAccurateMatch(infixExpr.getOperator());
			_rhs.postAccurateMatch(infixExpr.getRhs());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			InfixExpr infixExpr = (InfixExpr) getBindingNode();
			boolean lhs = false, rhs = false;
			if (_lhs.getBindingNode() != infixExpr.getLhs()) {
				Update update = new Update(this, _lhs, infixExpr.getLhs());
				_modifications.add(update);
			} else {
				lhs = true;
				_lhs.genModifications();
			}
			if (_rhs.getBindingNode() != infixExpr.getRhs()) {
				Update update = new Update(this, _rhs, infixExpr.getRhs());
				_modifications.add(update);
			} else {
				rhs = true;
				_rhs.genModifications();
			}
			if (lhs != rhs) {
				_operator.setExpanded();
			}
			if (!_operator.compare(infixExpr.getOperator())) {
				Update update = new Update(this, _operator, infixExpr.getOperator());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof InfixExpr) {
			InfixExpr infixExpr = (InfixExpr) node;
			if (NodeUtils.matchSameNodeType(getLhs(), infixExpr.getLhs(), matchedNode, matchedStrings)
					&& NodeUtils.matchSameNodeType(getRhs(), infixExpr.getRhs(), matchedNode, matchedStrings)) {
				getLhs().greedyMatchBinding(infixExpr.getLhs(), matchedNode, matchedStrings);
				getRhs().greedyMatchBinding(infixExpr.getRhs(), matchedNode, matchedStrings);
			}
		}
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			tmp = _lhs.transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			tmp = _operator.transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			tmp = _rhs.transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		StringBuffer lhs = null;
		StringBuffer operator = null;
		StringBuffer rhs = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			InfixExpr infixExpr = (InfixExpr) node;
			for (Modification modification : infixExpr.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == infixExpr._lhs) {
						lhs = update.apply(vars, exprMap, retType, exceptions, metric);
						if (lhs == null) return null;
					} else if (changedNode == infixExpr._operator) {
						if ("&&".equals(_operator.getOperatorStr())
								&& "||".equals(update.getTarNode().toString())){
							return null;
						}
						operator = update.apply(vars, exprMap, retType, exceptions, metric);
						if (operator == null) return null;
					} else {
						rhs = update.apply(vars, exprMap, retType, exceptions, metric);
						if (rhs == null) return null;
					}
				} else {
					LevelLogger.error("@InfixExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if(lhs == null) {
			tmp = _lhs.adaptModifications(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(lhs);
		}
		if(operator == null) {
			tmp = _operator.adaptModifications(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(operator);
		}
		if(rhs == null) {
			tmp = _rhs.adaptModifications(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(rhs);
		}
		return stringBuffer;
	}
}
