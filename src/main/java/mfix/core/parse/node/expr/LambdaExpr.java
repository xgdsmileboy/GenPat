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
public class LambdaExpr extends Expr implements Serializable {

	private static final long serialVersionUID = -7877574560065780171L;

	//TODO : add support for lambda expression

	/**
	 * LambdaExpression:
     *	Identifier -> Body
     *	( [ Identifier { , Identifier } ] ) -> Body
     *	( [ FormalParameter { , FormalParameter } ] ) -> Body
	 */
	public LambdaExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_oriNode.toString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_oriNode.toString());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof LambdaExpr) {
			match = _oriNode.toString().equals(((LambdaExpr) other)._oriNode.toString());
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
	}
}

