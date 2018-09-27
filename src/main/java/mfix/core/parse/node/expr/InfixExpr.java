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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class InfixExpr extends Expr implements Serializable {

	private static final long serialVersionUID = -5825228781443981995L;
	private Expr _lhs = null;
	private InfixOperator _operator = null;
	private Expr _rhs = null;
	
	/**
	 * InfixExpression:
     *	Expression InfixOperator Expression { InfixOperator Expression }
	 */
	public InfixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INFIXEXPR;
	}
	
	public void setLeftHandSide(Expr lhs){
		_lhs = lhs;
	}
	
	public void setOperator(InfixOperator operator){
		_operator = operator;
	}
	
	public void setRightHandSide(Expr rhs){
		_rhs = rhs;
	}
	
	public InfixOperator getOperator(){
		return _operator;
	}
	
	public Expr getLhs(){
		return _lhs;
	}
	
	public Expr getRhs(){
		return _rhs;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_lhs.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_rhs.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer lhs = null;
		StringBuffer operator = null;
		StringBuffer rhs = null;
		if(_binding != null && _binding instanceof InfixExpr) {
			InfixExpr infixExpr = (InfixExpr) _binding;
			for(Modification modification : infixExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == infixExpr._lhs) {
						lhs = update.getTarString(exprMap, allUsableVars);
						if(lhs == null) return null;
					} else if(node == infixExpr._operator) {
						operator = update.getTarString(exprMap, allUsableVars);
						if(operator == null) return null;
					} else {
						rhs = update.getTarString(exprMap, allUsableVars);
						if(rhs == null) return null;
					}
				} else {
					LevelLogger.error("@InfixExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(lhs == null) {
			tmp = _lhs.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(lhs);
		}
		if(operator == null) {
			tmp = _operator.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(operator);
		}
		if(rhs == null) {
			tmp = _rhs.applyChange(exprMap,allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(rhs);
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
		tmp = _lhs.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		tmp = _operator.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		tmp = _rhs.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_lhs.printMatchSketch());
			stringBuffer.append(_operator.printMatchSketch());
			stringBuffer.append(_rhs.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_lhs.tokens());
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_rhs.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof InfixExpr) {
			InfixExpr infixExpr = (InfixExpr) other;
			match = _operator.compare(infixExpr._operator) && _lhs.compare(infixExpr._lhs)
					&& _rhs.compare(infixExpr._rhs);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_lhs);
		children.add(_operator);
		children.add(_rhs);
		return children;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_lhs.getCalledMethods());
			avoidDuplicate(_keywords, _rhs);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_lhs.extractModifications());
			modifications.addAll(_rhs.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof InfixExpr) {
			_matchNodeType = true;
			InfixExpr infixExpr = (InfixExpr) other;
			_lhs.deepMatch(infixExpr._lhs);
			if(!_lhs.isNodeTypeMatch()) {
				Update update = new Update(this, _lhs, infixExpr._lhs);
				_modifications.add(update);
			}
			_operator.deepMatch(infixExpr._operator);
			if(!_operator.isNodeTypeMatch()) {
				Update update = new Update(this, _operator, infixExpr._operator);
				_modifications.add(update);
			}
			_rhs.deepMatch(infixExpr._rhs);
			if(!_rhs.isNodeTypeMatch()) {
				Update update = new Update(this, _rhs, infixExpr._rhs);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof InfixExpr) {
			match = true;
			InfixExpr infixExpr = (InfixExpr) sketch;
			if(!infixExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(infixExpr._lhs.isKeyPoint()) {
					match = _lhs.matchSketch(infixExpr._lhs);
				}
				if(match && infixExpr._operator.isKeyPoint()) {
					match = _operator.matchSketch(infixExpr._operator);
				}
				if(match && infixExpr._rhs.isKeyPoint()) {
					match = _rhs.matchSketch(infixExpr._rhs);
				}
			}
			if(match) {
				infixExpr._binding = this;
				_binding = infixExpr;
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
		if (sketch instanceof InfixExpr) {
			match = true;
			InfixExpr infixExpr = (InfixExpr) sketch;
			if (infixExpr._lhs.isKeyPoint()) {
				_lhs.bindingSketch(infixExpr._lhs);
			}
			if (infixExpr._operator.isKeyPoint()) {
				_operator.bindingSketch(infixExpr._operator);
			}
			if (infixExpr._rhs.isKeyPoint()) {
				_rhs.bindingSketch(infixExpr._rhs);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_lhs.resetAllNodeTypeMatch();
		_operator.resetAllNodeTypeMatch();
		_rhs.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_lhs.setAllNodeTypeMatch();
		_operator.setAllNodeTypeMatch();
		_rhs.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_lhs.getFeatureVector());
		_fVector.combineFeature(_rhs.getFeatureVector());
	}

}
