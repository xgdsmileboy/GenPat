/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.AST;
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

	public MType(String fileName, int startLine, int endLine, Type oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.TYPE;
		_fIndex = VIndex.EXP_TYPE;
		_type = oriNode;
		_typeStr = oriNode.toString();
	}

	public void setType(Type type) {
		if (type == null) {
			type = AST.newAST(AST.JLS8).newWildcardType();
		}
		if (_type == null) {
			this._type = type;
			_typeStr = type.toString();
		}
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
	public String getTypeStr() {
		return NodeUtils.distillBasicType(this);
	}

	public int getDimension() {
		if (isArrayType()) {
			return ((ArrayType) _type).getDimensions();
		} else {
			return 0;
		}
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof MType) {
			return _typeStr.equals(((MType) other)._typeStr);
		}
		return false;
	}

	@Override
	public void doAbstraction(CodeAbstraction abstracter) {
		if (isConsidered()) {
			_abstract = abstracter.shouldAbstract(NodeUtils.distillBasicType(this),
					CodeAbstraction.Category.TYPE_TOKEN);
		}
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_typeStr);
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (!isAbstract() && (parentConsidered || isConsidered())) {
			StringBuffer buffer = toSrcString();
			keywords.add(NodeUtils.distillBasicType(this));
			return buffer;
		} else if (isConsidered()) {
			return new StringBuffer(nameMapping.getTypeID(this));
		} else {
			return null;
		}
	}

	@Override
	public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
		if (node == null || isConsidered() != node.isConsidered()){
			return false;
		}
		if (isConsidered()) {
			return NodeUtils.patternMatch(this, node, matchedNode);
		}
		return true;
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
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.E_TYPE);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_TYPE);
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
		if (getBindingNode() == null && compare(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}

	@Override
	public boolean genModifications() {
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if(node instanceof MType) {
			MType type = (MType) node;
			if (!isChanged() || compare(node)) {
				String typeStr1 = type.getTypeStr();
				String typeStr2 = getTypeStr();
				typeStr1 = "?".equals(typeStr1) ? typeStr2 : typeStr1;
				typeStr2 = "?".equals(typeStr2) ? typeStr1 : typeStr2;
				boolean matchType = isAbstract() ? true : Utils.safeStringEqual(typeStr1, typeStr2);
				return NodeUtils.match(this, node,true, matchType, level)
						&& NodeUtils.checkDependency(this, node, matchedNode, matchedStrings, level)
						&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
			}
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			metric.inc();
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		return toSrcString();
	}
}
