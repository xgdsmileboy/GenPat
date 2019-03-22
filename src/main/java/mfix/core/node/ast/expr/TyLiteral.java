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
public class TyLiteral extends Expr {

	private static final long serialVersionUID = 5518643646465944075L;
	private MType _type = null;
	
	/**
	 * TypeLiteral:
     *	( Type | void ) . class
	 */
	public TyLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.TLITERAL;
		_fIndex = VIndex.EXP_TYPE_LIT;
	}

	public void setValue(MType type) {
		_type = type;
	}

	public MType getDeclType() {
		return _type;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_type.toSrcString());
		stringBuffer.append(".class");
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		StringBuffer type = _type.formalForm(nameMapping, isConsidered() || parentConsidered, keywords);
		if (type == null) {
			return leafFormalForm(nameMapping, parentConsidered, keywords);
		}
		type.append(".class");
        keywords.add(type.toString());
		return type;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_type.tokens());
		_tokens.add(".");
		_tokens.add("class");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof TyLiteral) {
			TyLiteral literal = (TyLiteral) other;
			match = _type.compare(literal._type);
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
		_selfFVector.inc(FVector.E_TYPE);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_TYPE);
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		TyLiteral tyLiteral = null;
		boolean match = false;
		if (getBindingNode() != null) {
			tyLiteral = (TyLiteral) getBindingNode();
			match = (tyLiteral == node);
		} else if (canBinding(node)) {
			tyLiteral = (TyLiteral) node;
			setBindingNode(node);
			match = true;
		}
		if (tyLiteral == null) {
			continueTopDownMatchNull();
		} else {
			_type.postAccurateMatch(tyLiteral._type);
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			TyLiteral tyLiteral = (TyLiteral) getBindingNode();
			if (!_type.compare(tyLiteral.getDeclType())) {
				Update update = new Update(this, _type, tyLiteral.getDeclType());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof TyLiteral) {
			return super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = toSrcString();
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		return toSrcString();
	}
}
