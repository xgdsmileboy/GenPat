/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.ast.expr.MType;
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
public class ConstructorInv  extends Stmt {

	private static final long serialVersionUID = -680765569439500998L;
	private MType _thisType = null;
	private ExprList _arguments = null;
	
	/**
	 * ConstructorInvocation:
     *	[ < Type { , Type } > ]
     *	       this ( [ Expression { , Expression } ] ) ;
	 */
	public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.CONSTRUCTORINV;
	}

	public String getClassStr() {
		return _thisType == null ? "DUMMY" : _thisType.toSrcString().toString();
	}

	public void setThisType(MType thisType){
		_thisType = thisType;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
//		if(_thisType != null) {
//			stringBuffer.append(_thisType.toSrcString());
//			stringBuffer.append(".");
//		}
		stringBuffer.append("this(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ConstructorInv) {
			ConstructorInv constructorInv = (ConstructorInv) other;
			match = _arguments.compare(constructorInv._arguments);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_THIS);
		_fVector.combineFeature(_arguments.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		ConstructorInv constructorInv = null;
		if(getBindingNode() != null) {
			constructorInv = (ConstructorInv) getBindingNode();
			match = (constructorInv == node);
		} else if (canBinding(node)) {
			constructorInv = (ConstructorInv) node;
			setBindingNode(constructorInv);
			match = true;
		}
		if(constructorInv == null) {
			continueTopDownMatchNull();
		} else {
			_arguments.postAccurateMatch(constructorInv.getArguments());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			ConstructorInv constructorInv = (ConstructorInv) getBindingNode();
			if(_arguments.getBindingNode() != constructorInv.getArguments()) {
				Update update = new Update(this, _arguments, constructorInv.getArguments());
				_modifications.add(update);
			} else {
				_arguments.genModidications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof ConstructorInv) {
			return checkDependency(node, matchedNode, matchedStrings)
					&& matchSameNodeType(node, matchedNode, matchedStrings);
		}
		return false;
	}
}
