/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
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
public class NillLiteral extends Expr {

	private String _value = "null";
	
	/**
	 * Null literal node.
	 */
	public NillLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.NULL;
		_fIndex = VIndex.EXP_NULL_LIT;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_value);
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		return leafFormalForm(nameMapping, parentConsidered, keywords);
		if (isConsidered()) {
			return new StringBuffer(nameMapping.getExprID(this));
		} else {
			return null;
		}
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("null");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof NillLiteral) {
			match = true;
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
		_selfFVector.inc(FVector.E_NULL);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_NULL);
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		if(getBindingNode() == node) return true;
		if (getBindingNode() == null && canBinding(node)) {
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
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
		return toSrcString();
	}
}
