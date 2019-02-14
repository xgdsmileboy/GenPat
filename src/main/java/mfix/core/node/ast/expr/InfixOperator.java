/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.vector.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class InfixOperator extends Operator {

	private static final long serialVersionUID = -4702533056648468078L;
	private String _operatorStr;
	private transient InfixExpression.Operator _operator;

	public InfixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.INFIXOPERATOR;
		_fIndex = VIndex.EXP_INFIX_OP;
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
		if (other instanceof InfixOperator) {
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
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operatorStr);
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof InfixOperator) {
			matchedNode.put(this, node);
			return true;
		}
		return false;
	}
}
