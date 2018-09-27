/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import mfix.core.parse.node.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Expr extends Node implements Serializable {

	private static final long serialVersionUID = 1325289211050496258L;
	protected String _exprTypeStr = null;
	protected transient Type _exprType = null;

	protected Expr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node, null);
		_exprTypeStr = node.toString();
	}
	
	public void setType(Type exprType){
		_exprType = exprType;
		_exprTypeStr = exprType.toString();
	}
	
	public Type getType(){
		return _exprType;
	}
	
	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	
}
