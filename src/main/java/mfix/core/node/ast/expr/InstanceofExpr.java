/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class InstanceofExpr extends Expr {

	private static final long serialVersionUID = 8963981016056992635L;
	private Expr _expression = null;
	private String _operator = "instanceof";
	private MType _instanceType = null;
	
	/**
	 * InstanceofExpression:
     *		Expression instanceof Type
	 */
	public InstanceofExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INSTANCEOF;
	}

	public void setExpression(Expr expression) {
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

	@Override
	public boolean postAccurateMatch(Node node) {
		InstanceofExpr instanceofExpr = null;
		boolean match = false;
		if (getBindingNode() != null) {
			instanceofExpr = (InstanceofExpr) getBindingNode();
			match = (instanceofExpr == node);
		} else if (canBinding(node)) {
			instanceofExpr = (InstanceofExpr) node;
			setBindingNode(node);
			match = true;
		}
		if (instanceofExpr == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(instanceofExpr.getExpression());
			_instanceType.postAccurateMatch(instanceofExpr.getInstanceofType());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if (super.genModidications()) {
			InstanceofExpr instanceofExpr = (InstanceofExpr) getBindingNode();
			if (_expression.getBindingNode() != instanceofExpr.getExpression()) {
				Update update = new Update(this, _expression, instanceofExpr.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModidications();
			}
			if (_instanceType.getBindingNode() != instanceofExpr.getInstanceofType()
					|| !_instanceType.typeStr().equals(instanceofExpr.getInstanceofType().typeStr())) {
				Update update = new Update(this, _instanceType, instanceofExpr.getInstanceofType());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer() {
		StringBuffer stringBuffer = super.transfer();
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			tmp = _expression.transfer();
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" instanceof ");
			tmp = _instanceType.transfer();
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications() {
		StringBuffer expression = null;
		StringBuffer instanceType = null;
		Node node = checkModification();
		if (node != null) {
			InstanceofExpr instanceofExpr = (InstanceofExpr) node;
			for (Modification modification : instanceofExpr.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == instanceofExpr._expression) {
						expression = update.apply();
						if (expression == null) return null;
					} else {
						instanceType = update.apply();
						if (instanceofExpr == null) return null;
					}
				} else {
					LevelLogger.error("@InstanceofExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if(expression == null) {
			tmp = _expression.adaptModifications();
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(" instanceof ");
		if(instanceType == null) {
			tmp = _instanceType.adaptModifications();
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(instanceType);
		}
		return stringBuffer;
	}
}
