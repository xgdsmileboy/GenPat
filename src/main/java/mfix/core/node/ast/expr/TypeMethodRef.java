/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.match.metric.FVector;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class TypeMethodRef extends Expr {

	private static final long serialVersionUID = 7592430157234480970L;

	/**
	 * TypeMethodReference:
     *	Type :: 
     *	    [ < Type { , Type } > ]
     *	    Identifier
	 */
	public TypeMethodRef(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_oriNode.toString());
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_oriNode.toString());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof TypeMethodRef) {
			match = _oriNode.toString().equals(((TypeMethodRef) other)._oriNode.toString());
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
