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
public class ConditionalExpr extends Expr {

	private static final long serialVersionUID = -6125079576530376280L;
	private Expr _condition = null;
	private Expr _first = null;
	private Expr _snd = null;
	
	/**
	 * ConditionalExpression:
     *	Expression ? Expression : Expression
	 */
	public ConditionalExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.CONDEXPR;
	}

	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setFirst(Expr first){
		_first = first;
	}
	
	public void setSecond(Expr snd){
		_snd = snd;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Expr getfirst(){
		return _first;
	}
	
	public Expr getSecond(){
		return _snd;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append("?");
		stringBuffer.append(_first.toSrcString());
		stringBuffer.append(":");
		stringBuffer.append(_snd.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_condition.tokens());
		_tokens.add("?");
		_tokens.addAll(_first.tokens());
		_tokens.add(":");
		_tokens.addAll(_snd.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof ConditionalExpr) {
			ConditionalExpr conditionalExpr = (ConditionalExpr) other;
			match = _condition.compare(conditionalExpr._condition) && _first.compare(conditionalExpr._first)
					&& _snd.compare(conditionalExpr._snd);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_first);
		children.add(_snd);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_COND);
		_fVector.combineFeature(_condition.getFeatureVector());
		_fVector.combineFeature(_first.getFeatureVector());
		_fVector.combineFeature(_snd.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		ConditionalExpr conditionalExpr = null;
		if(getBindingNode() != null) {
			conditionalExpr = (ConditionalExpr) getBindingNode();
			match = (conditionalExpr == node);
		} else if(canBinding(node)) {
			conditionalExpr = (ConditionalExpr) node;
			setBindingNode(node);
			match = true;
		}
		if(conditionalExpr == null) {
			continueTopDownMatchNull();
		} else {
			_condition.postAccurateMatch(conditionalExpr.getCondition());
			_first.postAccurateMatch(conditionalExpr.getfirst());
			_snd.postAccurateMatch(conditionalExpr.getSecond());
		}
		return match;
	}

	@Override
	public void genModidications() {
		//todo
	}
}
