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

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class InfixOperator extends Operator implements Serializable {

	private static final long serialVersionUID = -4702533056648468078L;
	private String _operatorStr;
	private transient InfixExpression.Operator _operator;
	
	public InfixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.INFIXOPERATOR;
	}
	
	public void setOperator(InfixExpression.Operator operator) {
		_operator = operator;
		_operatorStr = operator.toString();
	}
	
	public InfixExpression.Operator getOperator() {
		return _operator;
	}

	public String getOperatorStr() {
		return _operatorStr;
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof InfixOperator) {
			InfixOperator infixOperator = (InfixOperator) other;
			return _operatorStr.equals(infixOperator._operatorStr);
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operatorStr);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operatorStr);
		return stringBuffer;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof InfixOperator) {
			_matchNodeType = _operatorStr.equals(((InfixOperator) other)._operatorStr);
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
		_tokens.add(_operatorStr);
	}

}
