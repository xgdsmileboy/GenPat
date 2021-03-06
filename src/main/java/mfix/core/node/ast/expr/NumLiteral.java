/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NumLiteral extends Expr {

	private static final long serialVersionUID = -8592908830390293970L;
	private String _token = null;
	
	/**
	 * Null literal node.
	 */
	public NumLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.NUMBER;
		_fIndex = VIndex.EXP_NUM_LIT;
	}

	public void setValue(String token) {
		_token = token;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_token);
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		return leafFormalForm(nameMapping, parentConsidered, keywords);
		if (isChanged()) {
			keywords.add(toString());
			return toSrcString();
		} else if (isConsidered()) {
			StringBuffer buffer = new StringBuffer();
			if (!_abstractType) {
				keywords.add(_exprTypeStr);
				buffer.append(_exprTypeStr).append("::");
			}
			buffer.append(nameMapping.getExprID(this));
			return buffer;
		} else {
			return null;
		}
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_token);
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof NumLiteral) {
			NumLiteral numLiteral = (NumLiteral) other;
			match = _token.equals(numLiteral._token);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.E_NUMBER);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_NUMBER);
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		if (getBindingNode() == node) return true;
		if (getBindingNode() == null && canBinding(node)) {
			if (node.toSrcString().toString().equals(toSrcString().toString())) {
				setBindingNode(node);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			if (!compare(getBindingNode())) {
				Update update = new Update(this, this, getBindingNode());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		metric.inc();
		return toSrcString();
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			return ((Update) node.getModifications().get(0)).apply(vars, exprMap, retType, exceptions, metric);
		}
		return toSrcString();
	}
}
