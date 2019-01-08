/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.match.metric.FVector;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class MethodInv extends Expr {

	private static final long serialVersionUID = 3902854514191993113L;
	private Expr _expression = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 *  MethodInvocation:
     *  [ Expression . ]
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public MethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.MINVOCATION;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setName(SName name){
		_name = name;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}
	
	public Expr getExpression(){
		return _expression;
	}

	public SName getName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression != null) {
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if (_arguments != null) {
			stringBuffer.append(_arguments.toSrcString());
		}
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_expression != null) {
			set.addAll(_expression.getAllVars());
		}
		if (_arguments != null) {
			set.addAll(_arguments.getAllVars());
		}
		return set;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_expression != null) {
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		if (_arguments != null) {
			_tokens.addAll(_arguments.tokens());
		}
		_tokens.add(")");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof MethodInv) {
			MethodInv methodInv = (MethodInv) other;
			match = _name.compare(methodInv._name);
			if (match) {
				match = _expression == null ? (methodInv._expression == null)
						: _expression.compare(methodInv._expression) && _arguments.compare(methodInv._arguments);
			}
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if(_expression != null) {
			children.add(_expression);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_MINV);
		if(_expression != null){
			_fVector.combineFeature(_expression.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}
	
}