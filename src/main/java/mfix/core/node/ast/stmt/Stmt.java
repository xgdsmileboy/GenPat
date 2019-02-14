/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Stmt extends Node {

	private static final long serialVersionUID = -4168850816999087148L;

	protected Stmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
	}

	@Override
	public Stmt getParentStmt() {
		return this;
	}

	@Override
	public boolean genModifications() {
		if (getBindingNode() == null) {
			return false;
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
				&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
	}
}