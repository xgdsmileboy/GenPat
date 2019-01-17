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
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class IfStmt extends Stmt {

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

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		IfStmt ifStmt = null;
		if(getBindingNode() != null) {
			ifStmt = (IfStmt) getBindingNode();
			_condition.postAccurateMatch(ifStmt.getCondition());
			match = (ifStmt == node);
		} else if(canBinding(node)) {
			ifStmt = (IfStmt) node;
			if(_condition.postAccurateMatch(ifStmt.getCondition())) {
				setBindingNode(node);
				match = true;
			}else {
				ifStmt = null;
			}
		}
		if(ifStmt == null) {
			continueTopDownMatchNull();
		} else {
			_then.postAccurateMatch(ifStmt.getThen());
			if(_else != null) {
				_else.postAccurateMatch(ifStmt.getElse());
			}
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if (super.genModidications()) {
			IfStmt ifStmt = (IfStmt) getBindingNode();
			if(_condition.getBindingNode() != ifStmt.getCondition()) {
				Update update = new Update(this, _condition, ifStmt.getCondition());
				_modifications.add(update);
			} else {
				_condition.genModidications();
			}
			if(_then.getBindingNode() != ifStmt.getThen()) {
				Update update = new Update(this, _then, ifStmt.getThen());
				_modifications.add(update);
			} else {
				_then.genModidications();
			}
			if(_else == null) {
				if (ifStmt.getElse() != null) {
					Update update = new Update(this, _else, ifStmt.getElse());
					_modifications.add(update);
				}
			} else if (_else.getBindingNode() != ifStmt.getElse()){
				Update update = new Update(this, _else, ifStmt.getElse());
				_modifications.add(update);
			} else {
				_else.genModidications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) node;
			boolean match = _condition.ifMatch(ifStmt.getCondition(), matchedNode, matchedStrings);
			match = match && _then.ifMatch(ifStmt.getThen(), matchedNode, matchedStrings);
			if(_else != null && ifStmt.getElse() != null) {
				match = match && _else.ifMatch(ifStmt.getElse(), matchedNode, matchedStrings);
			}
			return match && super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}
}
