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
public class InstanceofExpr extends Expr implements Serializable {

	private static final long serialVersionUID = 8963981016056992635L;
	private Expr _expression = null;
	private String _operator = "instanceof";
	private MType _instanceType = null;
	
	/**
	 * InstanceofExpression:
     *	Expression instanceof Type
	 */
	public InstanceofExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INSTANCEOF;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	public void setInstanceType(MType instanceType) {
		_instanceType = instanceType;
	}

	public Expr getExpression() {
		return _expression;
	}

	public MType getInstanceofType() {
		return _instanceType;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(" instanceof ");
		stringBuffer.append(_instanceType.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.add("instanceof");
		_tokens.addAll(_instanceType.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof InstanceofExpr) {
			InstanceofExpr instanceofExpr = (InstanceofExpr) other;
			match = _instanceType.compare(instanceofExpr._instanceType)
					&& _expression.compare(instanceofExpr._expression);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_instanceType);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(_operator);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_instanceType.getFeatureVector());
	}

}
