/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ReturnStmt extends Stmt {

	private static final long serialVersionUID = 1986156793761228319L;
	private Expr _expression = null;
	
	/**
	 * ReturnStatement:
     *	return [ Expression ] ;
	 */
	public ReturnStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public ReturnStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.RETURN;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("return ");
		if(_expression != null){
			stringBuffer.append(_expression.toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("return");
		if(_expression != null){
			_tokens.addAll(_expression.tokens());
		}
		_tokens.add(";");
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) other;
			if(_expression == null) {
				match = (returnStmt._expression == null);
			} else {
				match = _expression.compare(returnStmt._expression);
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_RET);
		if(_expression != null) {
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		ReturnStmt returnStmt = null;
		if(getBindingNode() != null) {
			returnStmt = (ReturnStmt) getBindingNode();
			match = (returnStmt == node);
		} else if(canBinding(node)){
			returnStmt = (ReturnStmt) node;
			match = true;
		}
		if(returnStmt == null) {
			continueTopDownMatchNull();
		} else if(_expression != null) {
			_expression.postAccurateMatch(returnStmt.getExpression());
		}
		return match;
	}
}
