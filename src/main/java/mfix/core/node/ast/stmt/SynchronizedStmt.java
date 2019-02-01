/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
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

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		SynchronizedStmt synchronizedStmt = null;
		if(getBindingNode() != null) {
			synchronizedStmt = (SynchronizedStmt) getBindingNode();
			match = (synchronizedStmt == node);
		} else if(canBinding(node)) {
			synchronizedStmt = (SynchronizedStmt) node;
			match = true;
		}

		if(synchronizedStmt == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(synchronizedStmt.getExpression());
			_blk.postAccurateMatch(synchronizedStmt.getBody());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			SynchronizedStmt synchronizedStmt = (SynchronizedStmt) getBindingNode();
			if(_expression.getBindingNode() != synchronizedStmt.getExpression()) {
				Update update = new Update(this, _expression, synchronizedStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModidications();
			}
			_blk.genModidications();
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof SynchronizedStmt) {
			SynchronizedStmt stmt = (SynchronizedStmt) node;
			return _expression.ifMatch(stmt.getExpression(), matchedNode, matchedStrings)
					&& _blk.ifMatch(stmt.getBody(), matchedNode, matchedStrings)
					&& super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("synchronized(");
			StringBuffer tmp = _expression.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
			tmp = _blk.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		Node pnode = checkModification();
		if (pnode != null) {
			SynchronizedStmt synchronizedStmt = (SynchronizedStmt) pnode;
			for (Modification modification : synchronizedStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == synchronizedStmt._expression) {
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					}
				} else {
					LevelLogger.error("SynchronizedStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("synchronized(");
		StringBuffer tmp;
		if (expression == null) {
			tmp = _expression.adaptModifications(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(")");
		tmp = _blk.adaptModifications(vars, exprMap);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
}
