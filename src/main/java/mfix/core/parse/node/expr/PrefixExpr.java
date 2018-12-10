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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PrefixExpr extends Expr implements Serializable {

	private static final long serialVersionUID = 6945905157362942363L;
	private Expr _expression = null;
	private PrefixOperator _operator = null;
	
	/**
	 * PrefixExpression:
     *	PrefixOperator Expression
	 */
	public PrefixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PREEXPR;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setOperator(PrefixOperator operator){
		_operator = operator;
	}

	public Expr getExpression() {
		return _expression;
	}

	public PrefixOperator getOperator() {
		return _operator;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_expression.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer operator = null;
		StringBuffer expression = null;
		if(_binding != null && _binding instanceof PrefixExpr) {
			PrefixExpr prefixExpr = (PrefixExpr) _binding;
			for(Modification modification : prefixExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == prefixExpr._operator) {
						operator = update.getTarString(exprMap, allUsableVars);
						if(operator == null) return null;
					} else {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					}
				} else {
					LevelLogger.error("@PrefixExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(operator == null) {
			tmp = _operator.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(operator);
		}
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
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
		StringBuffer tmp = _operator.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_operator.printMatchSketch());
			stringBuffer.append(_expression.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_expression.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof PrefixExpr) {
			PrefixExpr prefixExpr = (PrefixExpr) other;
			return _operator.compare(prefixExpr._operator) && _expression.compare(prefixExpr._expression);
		}
		return false;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = _expression.getCalledMethods();
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_operator);
		return children;
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
		if(other instanceof PrefixExpr) {
			_matchNodeType = true;
			PrefixExpr prefixExpr = (PrefixExpr) other;
			_operator.deepMatch(prefixExpr._operator); 
			if(!_operator.isNodeTypeMatch()) {
				Update update = new Update(this, _operator, prefixExpr._operator);
				_modifications.add(update);
			}
			_expression.deepMatch(prefixExpr._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, prefixExpr._expression);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof PrefixExpr) {
			match = true;
			PrefixExpr prefixExpr = (PrefixExpr) sketch;
			if(!prefixExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(prefixExpr._operator.isKeyPoint()) {
					match = _operator.matchSketch(prefixExpr._operator);
				}
				if(match && prefixExpr._expression.isKeyPoint()) {
					match = _expression.matchSketch(prefixExpr._expression);
				}
			}
			if(match) {
				prefixExpr._binding = this;
				_binding = prefixExpr;
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
		if (sketch instanceof PrefixExpr) {
			match = true;
			PrefixExpr prefixExpr = (PrefixExpr) sketch;
			if (prefixExpr._operator.isKeyPoint()) {
				_operator.bindingSketch(prefixExpr._operator);
			}
			if (prefixExpr._expression.isKeyPoint()) {
				_expression.bindingSketch(prefixExpr._expression);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
		_operator.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
		_operator.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_PREFIX);
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_expression.getFeatureVector());
	}

}
