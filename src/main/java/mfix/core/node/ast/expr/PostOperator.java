/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PostfixExpression;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PostOperator extends Operator {

	private static final long serialVersionUID = -487330256404513705L;
	private String _operatorStr;
	private transient PostfixExpression.Operator _operator;

	public PostOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.POSTOPERATOR;
		_fIndex = VIndex.EXP_POSTFIX_OP;
	}

	public void setOperator(PostfixExpression.Operator operator) {
		this._operator = operator;
		this._operatorStr = operator.toString();
	}

	public PostfixExpression.Operator getOperator() {
		return _operator;
	}

	public String getOperatorStr() {
		return _operatorStr;
	}

	@Override
	public boolean compare(Node other) {
		if (other instanceof PostOperator) {
			return _operatorStr.equals(((PostOperator) other)._operatorStr);
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
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operatorStr);
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof PostOperator) {
			matchedNode.put(this, node);
			return true;
		}
		return false;
	}
}
