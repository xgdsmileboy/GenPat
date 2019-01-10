/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class EmptyStmt extends Stmt {

	private static final long serialVersionUID = -7049209332809553395L;

	/**
	 * EmptyStatement:
     *	;
	 */
	public EmptyStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public EmptyStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(";");
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof EmptyStmt) {
			return true;
		}
		return false;
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
		if(getBindingNode() != null) {
			return getBindingNode() == node;
		} else if(canBinding(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}
}
