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
public class PostfixExpr extends Expr implements Serializable {

	private static final long serialVersionUID = 3427634272372187596L;
	private Expr _expression = null;
	private PostOperator _operator = null;
	
	/**
	 * PostfixExpression:
     *	Expression PostfixOperator
	 */
	public PostfixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.POSTEXPR;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setOperator(PostOperator operator){
		_operator = operator;
	}

	public Expr getExpression() {
		return _expression;
	}

	public PostOperator getOperator() {
		return _operator;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.addAll(_operator.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof PostfixExpr) {
			PostfixExpr postfixExpr = (PostfixExpr) other;
			match = _operator.compare(postfixExpr._operator) && _expression.compare(postfixExpr._expression);
		}
		return match;
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
		_fVector.inc(FVector.E_POSTFIX);
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_expression.getFeatureVector());
	}

}
