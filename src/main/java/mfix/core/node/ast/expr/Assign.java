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
public class Assign extends Expr {

	private static final long serialVersionUID = 508933142391046341L;
	private Expr _lhs = null;
	private AssignOperator _operator = null;
	private Expr _rhs = null;
	
	/**
	 * Assignment:
     *	Expression AssignmentOperator Expression
	 */
	public Assign(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ASSIGN;
	}

	public void setLeftHandSide(Expr lhs){
		_lhs = lhs;
	}
	
	public void setOperator(AssignOperator operator){
		_operator = operator;
	}
	
	public void setRightHandSide(Expr rhs){
		_rhs = rhs;
	}

	public AssignOperator getOperator() {
		return _operator;
	}

	public Expr getLhs(){
		return _lhs;
	}
	
	public Expr getRhs(){
		return _rhs;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_lhs);
		children.add(_operator);
		children.add(_rhs);
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_lhs.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_rhs.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_lhs.tokens());
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_rhs.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof Assign) {
			Assign assign = (Assign) other;
			match = _operator.compare(assign._operator) && _lhs.compare(assign._lhs) && _rhs.compare(assign._rhs);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_ASSIGN);
		_fVector.combineFeature(_lhs.getFeatureVector());
		_fVector.combineFeature(_rhs.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		Assign assign = null;
		if(getBindingNode() != null) {
			assign = (Assign) getBindingNode();
			match = (assign == node);
		} else if(canBinding(node)) {
			assign = (Assign) node;
			setBindingNode(node);
			match = true;
		}

		if(assign == null) {
			continueTopDownMatchNull();
		} else {
			_lhs.postAccurateMatch(assign.getLhs());
			_rhs.postAccurateMatch(assign.getRhs());
		}

		return match;
	}

	@Override
	public void genModidications() {
		//todo
	}
}
