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
public class SuperMethodInv extends Expr {

	private static final long serialVersionUID = -227589196009347171L;
	private Label _label = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperMethodInvocation:
     *	[ ClassName . ] super .
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public SuperMethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SMINVOCATION;
	}
	
	public void setLabel(Label label){
		_label = label;
	}
	
	public void setName(SName name){
		_name = name;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}

	public SName getMethodName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_label != null){
			stringBuffer.append(_label.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if(_label != null){
			set.addAll(_label.getAllVars());
		}
		set.addAll(_arguments.getAllVars());
		return set;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_label != null){
			_tokens.addAll(_label.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");	
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SuperMethodInv) {
			SuperMethodInv superMethodInv = (SuperMethodInv) other;
			match = (_label == null) ? (superMethodInv._label == null) : _label.compare(superMethodInv._label);
			match = match && _name.compare(superMethodInv._name) && _arguments.compare(superMethodInv._arguments);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if(_label != null) {
			children.add(_label);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SUPER);
		_fVector.inc(FVector.E_MINV);
		if(_label != null){
			_fVector.combineFeature(_label.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}
		
}