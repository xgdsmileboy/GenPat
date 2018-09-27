/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PostfixExpression;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PostOperator extends Operator implements Serializable {

	private static final long serialVersionUID = -487330256404513705L;
	private PostfixExpression.Operator _operator;
	
	public PostOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.POSTOPERATOR;
	}
	
	public void setOperator(PostfixExpression.Operator operator) {
		this._operator = operator;
	}
	
	public PostfixExpression.Operator getOperator() {
		return _operator;
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof PostOperator) {
			return _operator.toString().equals(((PostOperator) other)._operator.toString());
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
		if(other instanceof PostOperator) {
			_matchNodeType = true;
			PostOperator postOperator = (PostOperator) other;
			if(!_operator.toString().equals(postOperator._operator.toString())) {
				_matchNodeType = false;
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof PostOperator) {
			((PostOperator) sketch)._binding = this;
			_binding = sketch;
			return true;
		}
		return false;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operator.toString());
	}

}
