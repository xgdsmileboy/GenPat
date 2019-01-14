/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Label extends Expr {

	private static final long serialVersionUID = -6660200671704024539L;

	/**
	 * Name:
     *	SimpleName
     *	QualifiedName
	 */
	public Label(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}

	@Override
	public void genModidications() {
		//todo
	}
}
