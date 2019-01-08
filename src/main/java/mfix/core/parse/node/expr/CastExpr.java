/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class CastExpr extends Expr implements Serializable {

	private static final long serialVersionUID = -8485318151476589525L;
	private MType _castType = null;
	private Expr _expression = null;

	/**
	 * CastExpression: ( Type ) Expression
	 */
	public CastExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.CAST;
	}

	public void setCastType(MType type) {
		_castType = type;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public MType getCastType() {
		return _castType;
	}

	public Expr getExpresion() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_castType.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_expression.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_castType.tokens());
		_tokens.add(")");
		_tokens.addAll(_expression.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_castType);
		children.add(_expression);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) other;
			match = _castType.compare(castExpr._castType);
			match = match && _expression.compare(castExpr._expression);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_CAST);
		_fVector.combineFeature(_expression.getFeatureVector());
	}
}
