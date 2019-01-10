/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ParenthesiszedExpr extends Expr {

	private static final long serialVersionUID = -8417799816148324460L;
	private Expr _expression = null;
	
	/**
	 * ParenthesizedExpression:
     *	( Expression )
	 */
	public ParenthesiszedExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PARENTHESISZED;
	}

	public void setExpr(Expr expression){
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
			match = _expression.compare(parenthesiszedExpr._expression);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_expression.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		ParenthesiszedExpr parenthesiszedExpr = null;
		boolean match = false;
		if(getBindingNode() != null) {
			parenthesiszedExpr = (ParenthesiszedExpr) getBindingNode();
			match = (parenthesiszedExpr == node);
		} else if(canBinding(node)) {
			parenthesiszedExpr = (ParenthesiszedExpr) node;
			setBindingNode(node);
			match = true;
		}
		if(parenthesiszedExpr == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(parenthesiszedExpr.getExpression());
		}
		return match;
	}
}
