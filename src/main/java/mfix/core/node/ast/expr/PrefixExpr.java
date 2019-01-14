/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PrefixExpr extends Expr {

	private static final long serialVersionUID = 6945905157362942363L;
	private Expr _expression = null;
	private PrefixOperator _operator = null;
	
	/**
	 * PrefixExpression:
     *	PrefixOperator Expression
	 */
	public PrefixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PREEXPR;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setOperator(PrefixOperator operator){
		_operator = operator;
	}

	public Expr getExpression() {
		return _expression;
	}

	public PrefixOperator getOperator() {
		return _operator;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_expression.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_expression.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof PrefixExpr) {
			PrefixExpr prefixExpr = (PrefixExpr) other;
			return _operator.compare(prefixExpr._operator) && _expression.compare(prefixExpr._expression);
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_operator);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_PREFIX);
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_expression.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		PrefixExpr prefixExpr = null;
		boolean match = false;
		if(getBindingNode() != null) {
			prefixExpr = (PrefixExpr) getBindingNode();
			match = (prefixExpr == node);
		} else if (canBinding(node)) {
			prefixExpr = (PrefixExpr) node;
			setBindingNode(prefixExpr);
			match = true;
		}
		if(prefixExpr == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(prefixExpr.getExpression());
			_operator.postAccurateMatch(prefixExpr.getOperator());
		}
		return match;
	}

	@Override
	public void genModidications() {
		//todo
	}
}
