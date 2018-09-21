/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.LinkedList;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class InfixOperator extends Operator {

	private InfixExpression.Operator _operator;
	
	public InfixOperator(int startLine, int endLine, ASTNode oriNode) {
		super(startLine, endLine, oriNode);
		_nodeType = TYPE.INFIXOPERATOR;
	}
	
	public void setOperator(InfixExpression.Operator operator) {
		_operator = operator;
	}
	
	public InfixExpression.Operator getOperator() {
		return _operator;
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof InfixOperator) {
			InfixOperator infixOperator = (InfixOperator) other;
			return _operator.toString().equals(infixOperator.getOperator().toString());
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof InfixOperator) {
			_matchNodeType = _operator.toString().equals(((InfixOperator) other)._operator.toString());
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof InfixOperator) {
			match = true;
			InfixOperator infixOperator = (InfixOperator) sketch;
			infixOperator._binding = this;
			_binding = infixOperator;
		}
		return match;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operator.toString());
	}

}
