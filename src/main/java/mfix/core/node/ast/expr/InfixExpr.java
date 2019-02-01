/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
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
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_lhs.tokens());
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_rhs.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof InfixExpr) {
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
		_fVector = new FVector();
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_lhs.getFeatureVector());
		_fVector.combineFeature(_rhs.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		InfixExpr infixExpr = null;
		if (getBindingNode() != null) {
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
	public boolean genModidications() {
		if (super.genModidications()) {
			InfixExpr infixExpr = (InfixExpr) getBindingNode();
			if (_lhs.getBindingNode() != infixExpr.getLhs()) {
				Update update = new Update(this, _lhs, infixExpr.getLhs());
				_modifications.add(update);
			} else {
				_lhs.genModidications();
			}
			if (_rhs.getBindingNode() != infixExpr.getRhs()) {
				Update update = new Update(this, _rhs, infixExpr.getRhs());
				_modifications.add(update);
			} else {
				_rhs.genModidications();
			}
			if (!_operator.compare(infixExpr.getOperator())) {
				Update update = new Update(this, _operator, infixExpr.getOperator());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			tmp = _lhs.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			tmp = _operator.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			tmp = _rhs.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer lhs = null;
		StringBuffer operator = null;
		StringBuffer rhs = null;
		Node node = checkModification();
		if (node != null) {
			InfixExpr infixExpr = (InfixExpr) node;
			for (Modification modification : infixExpr.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == infixExpr._lhs) {
						lhs = update.apply(vars, exprMap);
						if (lhs == null) return null;
					} else if (changedNode == infixExpr._operator) {
						operator = update.apply(vars, exprMap);
						if (operator == null) return null;
					} else {
						rhs = update.apply(vars, exprMap);
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
			tmp = _lhs.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(lhs);
		}
		if(operator == null) {
			tmp = _operator.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(operator);
		}
		if(rhs == null) {
			tmp = _rhs.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(rhs);
		}
		return stringBuffer;
	}
}
