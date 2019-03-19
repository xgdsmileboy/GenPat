/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.pattern.cluster.NameMapping;
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
public class LambdaExpr extends Expr {

	private static final long serialVersionUID = -7877574560065780171L;

	private String _codeStr;
	//TODO : add support for lambda expression

	/**
	 * LambdaExpression:
     *	Identifier -> Body
     *	( [ Identifier { , Identifier } ] ) -> Body
     *	( [ FormalParameter { , FormalParameter } ] ) -> Body
	 */
	public LambdaExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_codeStr = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_codeStr);
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_codeStr);
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof LambdaExpr) {
			match = _codeStr.equals(((LambdaExpr) other)._codeStr);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_completeFVector = new FVector();
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		if (getBindingNode() == node) return true;
		if (getBindingNode() == null && canBinding(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}

	@Override
	public boolean genModifications() {
		return true;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
		return toSrcString();
	}
}

