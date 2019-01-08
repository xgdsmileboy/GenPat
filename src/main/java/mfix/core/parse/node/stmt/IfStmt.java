/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class IfStmt extends Stmt implements Serializable {

	private static final long serialVersionUID = -7247565482784755352L;
	private Expr _condition = null;
	private Stmt _then = null;
	private Stmt _else = null;
	
	/**
	 * IfStatement:
     *	if ( Expression ) Statement [ else Statement]
	 */
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.IF;
	}
	
	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setThen(Stmt then){
		_then = then;
	}
	
	public void setElse(Stmt els){
		_else = els;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Stmt getThen(){
		return _then;
	}
	
	public Stmt getElse(){
		return _else;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("if(");
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_then.toSrcString());
		if(_else != null) {
			stringBuffer.append("else ");
			stringBuffer.append(_else.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("if");
		_tokens.add("(");
		_tokens.addAll(_condition.tokens());
		_tokens.add(")");
		_tokens.addAll(_then.tokens());
		if(_else != null) {
			_tokens.add("else");
			_tokens.addAll(_else.tokens());
		}
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_then);
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(2);
		if(_then != null) {
			children.add(_then);
		}
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) other;
			match = _condition.compare(ifStmt._condition) && _then.compare(ifStmt._then);
			if(_else == null) {
				match = match && (ifStmt._else == null);
			} else {
				match = match && _else.compare(ifStmt._else);
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_IF);
		_fVector.combineFeature(_then.getFeatureVector());
		if(_else != null) {
			_fVector.inc(FVector.KEY_ELSE);
			_fVector.combineFeature(_else.getFeatureVector());
		}
	}
	
}
