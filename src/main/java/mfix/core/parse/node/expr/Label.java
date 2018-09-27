/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Label extends Expr implements Serializable {

	private static final long serialVersionUID = -6660200671704024539L;

	/**
	 * Name:
     *	SimpleName
     *	QualifiedName
	 */
	public Label(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}
}
