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
public class ExpressionStmt extends Stmt {

	private static final long serialVersionUID = 3654727206887515381L;
	private Expr _expression = null;
	
	/**
	 * ExpressionStatement:
     *	StatementExpression ;
	 */
	public ExpressionStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public ExpressionStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.EXPRSTMT;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof ExpressionStmt) {
			ExpressionStmt expressionStmt = (ExpressionStmt) other;
			return _expression.compare(expressionStmt._expression);
		}
		return false;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_expression.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		ExpressionStmt expressionStmt = null;
		if(getBindingNode() != null) {
			expressionStmt = (ExpressionStmt) getBindingNode();
			_expression.postAccurateMatch(expressionStmt.getExpression());
			match = (expressionStmt == node);
		} else if(canBinding(node)) {
			expressionStmt = (ExpressionStmt) node;
			if(_expression.postAccurateMatch(expressionStmt.getExpression())) {
				setBindingNode(node);
				match = true;
			} else {
				expressionStmt = null;
			}
		} else if(_expression.getBindingNode() != null ) {
			Node parent = _expression.getBindingNode().getParent();
			if(canBinding(parent)) {
				setBindingNode(parent);
			}
		}
		if(expressionStmt == null) {
			continueTopDownMatchNull();
		}

		return match;
	}

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			ExpressionStmt expressionStmt = (ExpressionStmt) getBindingNode();
			if(_expression.getBindingNode() != expressionStmt.getExpression()) {
				Update update = new Update(this, _expression, expressionStmt.getExpression());
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
		if (node instanceof ExpressionStmt) {
			ExpressionStmt expressionStmt = (ExpressionStmt) node;
			if (_expression.ifMatch(expressionStmt.getExpression(), matchedNode, matchedStrings)) {
				matchedNode.put(this, node);
				return true;
			}
		}
		return false;
	}
}
