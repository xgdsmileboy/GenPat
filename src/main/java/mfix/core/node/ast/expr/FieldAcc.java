/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class FieldAcc extends Expr {

	private static final long serialVersionUID = -7480080890886474478L;
	private Expr _expression = null;
	private SName _identifier = null;
	
	
	/**
	 * FieldAccess:
     *           Expression . Identifier
	 */
	public FieldAcc(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.FIELDACC;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}

	public Expr getExpression() {
		return _expression;
	}

	public SName getIdentifier() {
		return _identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(".");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof FieldAcc) {
			FieldAcc fieldAcc = (FieldAcc) other;
			match = _expression.compare(fieldAcc._expression);
			match = match && _identifier.compare(fieldAcc._identifier);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>();
		children.add(_expression);
		children.add(_identifier);
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_FACC);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_identifier.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		FieldAcc fieldAcc = null;
		boolean match = false;
		if(getBindingNode() != null) {
			fieldAcc = (FieldAcc) getBindingNode();
			match = (fieldAcc == node);
		} else if(canBinding(node)) {
			fieldAcc = (FieldAcc) node;
			setBindingNode(node);
			match = true;
		}

		if(fieldAcc == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(fieldAcc.getExpression());
			_identifier.postAccurateMatch(fieldAcc.getIdentifier());
		}
		return match;
	}
}
