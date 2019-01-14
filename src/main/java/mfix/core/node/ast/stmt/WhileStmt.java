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

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class WhileStmt extends Stmt {

	private static final long serialVersionUID = -5763865331484587703L;
	private Expr _expression = null;
	private Stmt _body = null;
	
	/**
	 * WhileStatement:
     *	while ( Expression ) Statement
	 */
	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.WHILE;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt body){
		_body = body;
	}
	
	public Stmt getBody() {
		return _body;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_body.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_body.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_body);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_body);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) other;
			match = _expression.compare(whileStmt._expression) && _body.compare(whileStmt._body);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_WHILE);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_body.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		WhileStmt whileStmt = null;
		if(getBindingNode() != null) {
			whileStmt = (WhileStmt) getBindingNode();
			match = (whileStmt == node);
		} else if(canBinding(node)) {
			whileStmt = (WhileStmt) node;
			setBindingNode(node);
			match = true;
		}
		if(whileStmt == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(whileStmt.getExpression());
			_body.postAccurateMatch(whileStmt.getBody());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			WhileStmt whileStmt = (WhileStmt) getBindingNode();
			if (_expression.getBindingNode() != whileStmt.getExpression()) {
				Update update = new Update(this, _expression, whileStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModidications();
			}
			_body.genModidications();
			return true;
		}
		return false;
	}
}

