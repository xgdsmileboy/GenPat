/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
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
		_fIndex = VIndex.EXP_FIELD_ACC;
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
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		boolean consider = isConsidered() || parentConsidered;
		StringBuffer exp = _expression.formalForm(nameMapping, consider, keywords);
		StringBuffer identifier = _identifier.formalForm(nameMapping, consider, keywords);
		if (exp == null && identifier == null) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp)
				.append('.')
				.append(identifier == null ? nameMapping.getExprID(_identifier) : identifier);
		return buffer;
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
		if (getBindingNode() != null) {
			fieldAcc = (FieldAcc) getBindingNode();
			match = (fieldAcc == node);
		} else if (canBinding(node)) {
			fieldAcc = (FieldAcc) node;
			setBindingNode(node);
			match = true;
		}

		if (fieldAcc == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(fieldAcc.getExpression());
			_identifier.postAccurateMatch(fieldAcc.getIdentifier());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			FieldAcc fieldAcc = (FieldAcc) getBindingNode();
			if (_expression.getBindingNode() != fieldAcc.getExpression()) {
				Update update = new Update(this, _expression, fieldAcc.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			if (_identifier.getBindingNode() != fieldAcc.getIdentifier()
					|| !_identifier.compare(fieldAcc.getIdentifier())) {
				Update update = new Update(this, _identifier, fieldAcc.getIdentifier());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			tmp = _expression.transfer(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(".");
			tmp = _identifier.transfer(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		StringBuffer identifier = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			FieldAcc fieldAcc = (FieldAcc) node;
			for (Modification modification : fieldAcc.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == fieldAcc._expression) {
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					} else {
						identifier = update.apply(vars, exprMap);
						if (identifier == null) return null;
					}
				} else {
					LevelLogger.error("@FieldAcc Should not be this kind of modification : " + modification);
				}
			}
		}

		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if(expression == null) {
			tmp = _expression.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(".");
		if(identifier == null) {
			tmp = _identifier.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(identifier);
		}
		return stringBuffer;
	}
}
