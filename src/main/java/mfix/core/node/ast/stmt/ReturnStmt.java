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

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			ReturnStmt returnStmt = (ReturnStmt) getBindingNode();
			if(_expression == null) {
				if(returnStmt.getExpression() != null) {
					Update update = new Update(this, _expression, returnStmt.getExpression());
					_modifications.add(update);
				}
			} else if(_expression.getBindingNode() != returnStmt.getExpression()) {
				Update update = new Update(this, _expression, returnStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModidications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) node;
			if(_expression != null && returnStmt.getExpression() != null) {
				return _expression.ifMatch(returnStmt.getExpression(), matchedNode, matchedStrings)
						&& super.ifMatch(node, matchedNode, matchedStrings);
			} else {
				return super.ifMatch(node, matchedNode, matchedStrings);
			}
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("return ");
			if(_expression != null){
				StringBuffer tmp = _expression.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(";");
			return stringBuffer;
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		Node pnode = checkModification();
		if (pnode != null) {
			ReturnStmt returnStmt = (ReturnStmt) pnode;
			for(Modification modification : returnStmt.getModifications()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == returnStmt._expression) {
						expression = update.apply(vars, exprMap);
						if(expression == null) return null;
					} else {
						LevelLogger.error("@ReturnStmt ERROR");
					}
				} else {
					LevelLogger.error("@ReturnStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("return ");
		if(expression == null) {
			if(_expression != null){
				StringBuffer tmp = _expression.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			if (_expression == null) {
				return null;
			}
			stringBuffer.append(expression);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
}
