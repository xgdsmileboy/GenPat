/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class MType extends Node {

	private static final long serialVersionUID = 1247523997810234312L;
	private String _typeStr;
	private transient Type _type;

	public MType(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.TYPE;
		_typeStr = oriNode.toString();
	}

	public void setType(Type type) {
		if (type == null) {
			type = AST.newAST(AST.JLS8).newWildcardType();
		}
		this._type = type;
		_typeStr = type.toString();
	}

	public Type type() {
		return _type;
	}

	public boolean isArrayType() {
		return _type.isArrayType();
	}

	public String typeStr() {
		return _typeStr;
	}

	public Type getElementType() {
		if (isArrayType()) {
			return ((ArrayType) _type).getElementType();
		} else {
			return _type;
		}
	}

	@Override
	public boolean compare(Node other) {
		if (other instanceof MType) {
			return _typeStr.equals(((MType) other)._typeStr);
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_typeStr);
		return stringBuffer;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_TYPE);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_typeStr);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MType)) {
			return false;
		}
		MType mType = (MType) obj;
		return _typeStr.equals(mType._typeStr);
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		if (getBindingNode() == node) return true;
		if (getBindingNode() == null && canBinding(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}

	@Override
	public boolean genModidications() {
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof MType) {
			return checkDependency(node, matchedNode, matchedStrings)
					&& matchSameNodeType(node, matchedNode, matchedStrings);
		} else {
			return false;
		}
	}

	@Override
	public StringBuffer transfer(Set<String> vars) {
		StringBuffer stringBuffer = super.transfer(vars);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars) {
		return toSrcString();
	}
}
