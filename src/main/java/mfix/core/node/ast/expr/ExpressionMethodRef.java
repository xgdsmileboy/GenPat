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
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ExpressionMethodRef extends Expr {

	private static final long serialVersionUID = -7935543365316676426L;
	private String _str;
	/**
	 * ExpressionMethodReference:
     *	Expression :: 
     *	    [ < Type { , Type } > ]
     *	    Identifier
	 */
	public ExpressionMethodRef(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_str = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_str);
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_str);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ExpressionMethodRef) {
			match = _str.equals(((ExpressionMethodRef) other)._str);
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

	@Override
	public boolean postAccurateMatch(Node node) {
		if(getBindingNode() == node) return true;
		if(getBindingNode() == null && canBinding(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}

	@Override
	public boolean genModidications() {
		return true;
	}

	@Override
	public StringBuffer transfer(Set<String> vars) {
		StringBuffer stringBuffer = super.transfer(vars);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars) {
		return toSrcString();
	}
}
