/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
		_fVector = new FVector();
		_fVector.inc(FVector.E_TYPE);
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
	public boolean genModidications() {
		if (super.genModidications()) {
			TyLiteral tyLiteral = (TyLiteral) getBindingNode();
			if (!_type.compare(tyLiteral.getDeclType())) {
				Update update = new Update(this, _type, tyLiteral.getDeclType());
				_modifications.add(update);
			}
		}
		return true;
	}
}
