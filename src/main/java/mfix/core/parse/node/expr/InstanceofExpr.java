/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
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
public class InstanceofExpr extends Expr {

	private Expr _expression = null;
	private String _operator = "instanceof";
	private MType _instanceType = null;
	
	/**
	 * InstanceofExpression:
     *	Expression instanceof Type
	 */
	public InstanceofExpr(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.INSTANCEOF;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	public void setInstanceType(MType instanceType) {
		_instanceType = instanceType;
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
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer instanceType = null;
		if(_binding != null && _binding instanceof InstanceofExpr) {
			InstanceofExpr instanceofExpr = (InstanceofExpr) _binding;
			for(Modification modification : instanceofExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == instanceofExpr._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						instanceType = update.getTarString(exprMap, allUsableVars);
						if(instanceofExpr == null) return null;
					}
				} else {
					LevelLogger.error("@InstanceofExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(" instanceof ");
		if(instanceType == null) {
			tmp = _instanceType.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(instanceType);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(toSrcString().toString());
		if(result != null) {
			return new StringBuffer(result);
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" instanceof ");
		tmp = _instanceType.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(" instanceof ");
			stringBuffer.append(_instanceType.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
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
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = _expression.getCalledMethods();
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_expression.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof InstanceofExpr) {
			_matchNodeType = true;
			InstanceofExpr instanceofExpr = (InstanceofExpr) other;
			_expression.deepMatch(instanceofExpr._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, instanceofExpr._expression);
				_modifications.add(update);
			}
			_instanceType.deepMatch(instanceofExpr._instanceType);
			if(!_instanceType.isNodeTypeMatch()) {
				Update update = new Update(this, _instanceType, instanceofExpr._instanceType);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof InstanceofExpr) {
			match = true;
			InstanceofExpr instanceofExpr = (InstanceofExpr) sketch;
			if(!instanceofExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(instanceofExpr._expression.isKeyPoint()) {
					match = _expression.matchSketch(instanceofExpr._expression);
				}
				if(match && instanceofExpr._instanceType.isKeyPoint()) {
					match = _instanceType.matchSketch(instanceofExpr._instanceType);
				}
			}
			if (match) {
				instanceofExpr._binding = this;
				_binding = instanceofExpr;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof InstanceofExpr) {
			match = true;
			InstanceofExpr instanceofExpr = (InstanceofExpr) sketch;
			if (instanceofExpr._expression.isKeyPoint()) {
				_expression.bindingSketch(instanceofExpr._expression);
			}
			if (instanceofExpr._instanceType.isKeyPoint()) {
				_instanceType.bindingSketch(instanceofExpr._instanceType);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_instanceType.resetAllNodeTypeMatch();
		_expression.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_instanceType.setAllNodeTypeMatch();
		_expression.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(_operator);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_instanceType.getFeatureVector());
	}

}
