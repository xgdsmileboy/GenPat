/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.match.metric.FVector;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SynchronizedStmt extends Stmt {

	private static final long serialVersionUID = -5285955539744811517L;
	private Expr _expression = null;
	private Blk _blk = null;
	
	/**
	 * SynchronizedStatement:
     *	synchronized ( Expression ) Block
	 */
	public SynchronizedStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public SynchronizedStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SYNC;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setBlock(Blk blk){
		_blk = blk;
	}

	public Expr getExpression() { return _expression; }

	public Blk getBody() { return _blk; }

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("synchronized(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_blk.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("synchronized");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_blk.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_blk);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_blk);
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof SynchronizedStmt) {
			SynchronizedStmt synchronizedStmt = (SynchronizedStmt) other;
			match = _expression.compare(synchronizedStmt._expression) && _blk.compare(synchronizedStmt._blk);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SYNC);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_blk.getFeatureVector());
	}

}