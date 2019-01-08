/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AnonymousClassDecl extends Node {

	private String _codeStr;

	private static final long serialVersionUID = -5993526474963543721L;

	public AnonymousClassDecl(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ANONYMOUSCDECL;
		_codeStr = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_codeStr);
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_codeStr);
	}
	
	@Override
	public mfix.core.parse.node.stmt.Stmt getParentStmt() {
		return getParent().getParentStmt();
	}
	
	@Override
	public List<mfix.core.parse.node.stmt.Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof AnonymousClassDecl) {
			return _codeStr.equals(((AnonymousClassDecl) other)._codeStr);
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
		_fVector.inc(FVector.E_ANONY);
	}
}
