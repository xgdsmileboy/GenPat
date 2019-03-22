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
public class SName extends Label {

	private static final long serialVersionUID = 6548845608841663421L;
	private String _name = null;
	
	/**
	 * SimpleName:
     *	Identifier
	 */
	public SName(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SNAME;
		_fIndex = VIndex.EXP_SNAME;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getName() {
		return _name;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_name);
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		return leafFormalForm(nameMapping, parentConsidered, keywords);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_name);
	}

	@Override
	public boolean compare(Node other) {
		if (other instanceof SName) {
			SName sName = (SName) other;
			return _name.equals(sName._name);
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.E_VAR);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_VAR);
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
	public boolean genModifications() {
		if (super.genModifications()) {
			SName sName = (SName) getBindingNode();
			if (!_name.equals(sName.getName())) {
				Update update = new Update(this, this, sName);
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
			if (!Character.isUpperCase(stringBuffer.charAt(0))) {
				if (!vars.canUse(stringBuffer.toString(), _exprTypeStr, _startLine)) {
					return null;
				}
			}
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			return ((Update) node.getModifications().get(0)).apply(vars, exprMap, retType, exceptions);
		}
		return toSrcString();
	}
}
