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
public class ConditionalExpr extends Expr implements Serializable {

	private static final long serialVersionUID = -6125079576530376280L;
	private Expr _condition = null;
	private Expr _first = null;
	private Expr _snd = null;
	
	/**
	 * ConditionalExpression:
     *	Expression ? Expression : Expression
	 */
	public ConditionalExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.CONDEXPR;
	}

	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setFirst(Expr first){
		_first = first;
	}
	
	public void setSecond(Expr snd){
		_snd = snd;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Expr getfirst(){
		return _first;
	}
	
	public Expr getSecond(){
		return _snd;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append("?");
		stringBuffer.append(_first.toSrcString());
		stringBuffer.append(":");
		stringBuffer.append(_snd.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer condition = null;
		StringBuffer first = null;
		StringBuffer snd = null;
		if(_binding != null && _binding instanceof ConditionalExpr) {
			ConditionalExpr	 conditionalExpr = (ConditionalExpr) _binding;
			for(Modification modification : conditionalExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == conditionalExpr._condition) {
						condition = update.getTarString(exprMap, allUsableVars);
						if(condition == null) return null;
					} else if(node == conditionalExpr._first) {
						first = update.getTarString(exprMap, allUsableVars);
						if(first == null) return null;
					} else if(node == conditionalExpr._snd) {
						snd = update.getTarString(exprMap, allUsableVars);
						if(snd == null) return null;
					}
				} else {
					LevelLogger.error("@ConditionalExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(condition == null) {
			tmp = _condition.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(condition);
		}
		stringBuffer.append("?");
		if(first == null) {
			tmp = _first.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(first);
		}
		stringBuffer.append(":");
		if(snd == null) {
			tmp = _snd.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(snd);
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
		tmp = _condition.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append("?");
		tmp = _first.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(":");
		tmp = _snd.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_condition.printMatchSketch());
			stringBuffer.append("?");
			stringBuffer.append(_first.printMatchSketch());
			stringBuffer.append(":");
			stringBuffer.append(_snd.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_condition.tokens());
		_tokens.add("?");
		_tokens.addAll(_first.tokens());
		_tokens.add(":");
		_tokens.addAll(_snd.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof ConditionalExpr) {
			ConditionalExpr conditionalExpr = (ConditionalExpr) other;
			match = _condition.compare(conditionalExpr._condition) && _first.compare(conditionalExpr._first)
					&& _snd.compare(conditionalExpr._snd);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_condition.getCalledMethods());
			avoidDuplicate(_keywords, _first);
			avoidDuplicate(_keywords, _snd);
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_first);
		children.add(_snd);
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_condition.extractModifications());
			modifications.addAll(_first.extractModifications());
			modifications.addAll(_snd.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ConditionalExpr) {
			_matchNodeType = true;
			ConditionalExpr conditionalExpr = (ConditionalExpr) other;
			_condition.deepMatch(conditionalExpr._condition);
			if(!_condition.isNodeTypeMatch()) {
				Update update = new Update(this, _condition, conditionalExpr._condition);
				_modifications.add(update);
			}
			_first.deepMatch(conditionalExpr._first);
			if(!_first.isNodeTypeMatch()) {
				Update update = new Update(this, _first, conditionalExpr._first);
				_modifications.add(update);
			}
			_snd.deepMatch(conditionalExpr._snd);
			if(!_snd.isNodeTypeMatch()) {
				Update update = new Update(this, _snd, conditionalExpr._snd);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ConditionalExpr) {
			match = true;
			ConditionalExpr conditionalExpr = (ConditionalExpr) sketch;
			if(!conditionalExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(conditionalExpr._condition.isKeyPoint()) {
					match = _condition.matchSketch(conditionalExpr._condition);
				}
				if(conditionalExpr._first.isKeyPoint()) {
					match = match && _first.matchSketch(conditionalExpr._first);
				}
				if(conditionalExpr._snd.isKeyPoint()) {
					match = match && _snd.matchSketch(conditionalExpr._snd);
				}
			}
			if(match) {
				conditionalExpr._binding = this;
				_binding = conditionalExpr;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof ConditionalExpr) {
			ConditionalExpr conditionalExpr = (ConditionalExpr) sketch;
			if (conditionalExpr._condition.isKeyPoint()) {
				_condition.bindingSketch(conditionalExpr._condition);
			}
			if (conditionalExpr._first.isKeyPoint()) {
				_first.bindingSketch(conditionalExpr._first);
			}
			if (conditionalExpr._snd.isKeyPoint()) {
				_snd.bindingSketch(conditionalExpr._snd);
			}
			return true;
		}
		return false;
	}

	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_condition.resetAllNodeTypeMatch();
		_first.resetAllNodeTypeMatch();
		_snd.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_condition.setAllNodeTypeMatch();
		_first.setAllNodeTypeMatch();
		_snd.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_COND);
		_fVector.combineFeature(_condition.getFeatureVector());
		_fVector.combineFeature(_first.getFeatureVector());
		_fVector.combineFeature(_snd.getFeatureVector());
	}
	
}
