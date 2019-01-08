/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
     *	Expression InfixOperator Expression { InfixOperator Expression }
	 */
	public InfixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INFIXEXPR;
	}
	
	public void setLeftHandSide(Expr lhs){
		_lhs = lhs;
	}
	
	public void setOperator(InfixOperator operator){
		_operator = operator;
	}
	
	public void setRightHandSide(Expr rhs){
		_rhs = rhs;
	}
	
	public InfixOperator getOperator(){
		return _operator;
	}
	
	public Expr getLhs(){
		return _lhs;
	}
	
	public Expr getRhs(){
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

}
