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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class Assign extends Expr {

	private Expr _lhs = null;
	private AssignOperator _operator = null;
	private Expr _rhs = null;
	
	/**
	 * Assignment:
     *	Expression AssignmentOperator Expression
	 */
	public Assign(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.ASSIGN;
	}

	public void setLeftHandSide(Expr lhs){
		_lhs = lhs;
	}
	
	public void setOperator(AssignOperator operator){
		_operator = operator;
	}
	
	public void setRightHandSide(Expr rhs){
		_rhs = rhs;
	}
	
	public Expr getLhs(){
		return _lhs;
	}
	
	public Expr getRhs(){
		return _rhs;
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
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_lhs.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_rhs.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer operator = null;
		StringBuffer lhs = null;
		StringBuffer rhs = null;
		if(_binding != null && _binding instanceof Assign) {
			Assign assign = (Assign) _binding;
			for(Modification modification : assign.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == assign._operator) {
						operator = update.getTarString(exprMap, allUsableVars);
						if(operator == null) return null;
					} else if(update.getSrcNode() == assign._lhs) {
						lhs = update.getTarString(exprMap, allUsableVars);
						if(lhs == null) return null;
					} else {
						rhs = update.getTarString(exprMap, allUsableVars);
						if(rhs == null) return null;
					}
				} else {
					LevelLogger.error("@Assign Should not be this kind of modification : " + modification.toString());
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
		if (other instanceof Assign) {
			Assign assign = (Assign) other;
			match = _operator.compare(assign._operator) && _lhs.compare(assign._lhs) && _rhs.compare(assign._rhs);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_lhs.getKeywords());
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
		if(other instanceof Assign) {
			_matchNodeType = true;
			Assign assign = (Assign) other;
			_operator.deepMatch(assign._operator);
			if(!_operator.isNodeTypeMatch()) {
				Update update = new Update(this, _operator, assign._operator);
				_modifications.add(update);
			}
			_lhs.deepMatch(assign._lhs);
			if(!_lhs.isNodeTypeMatch()) {
				Update update = new Update(this, _lhs, assign._lhs);
				_modifications.add(update);
			}
			_rhs.deepMatch(assign._rhs);
			if(!_rhs.isNodeTypeMatch()) {
				Update update = new Update(this, _rhs, assign._rhs);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof Assign) {
			match = true;
			Assign assign = (Assign) sketch;
			if(!assign.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(assign._lhs.isKeyPoint()) {
					match = _lhs.matchSketch(assign._lhs);
				}
				if(assign._operator.isKeyPoint()) {
					match = match && _operator.matchSketch(assign._operator);
				}
				if(assign._rhs.isKeyPoint()) {
					match = match && _rhs.matchSketch(assign._rhs);
				}
			}
			if(match) {
				assign._binding = this;
				_binding = assign;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof Assign) {
			Assign assign = (Assign) sketch;
			if (assign._lhs.isKeyPoint()) {
				_lhs.bindingSketch(assign._lhs);
			}
			if (assign._operator.isKeyPoint()) {
				_operator.bindingSketch(assign._operator);
			}
			if (assign._rhs.isKeyPoint()) {
				_rhs.bindingSketch(assign._rhs);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_lhs.resetAllNodeTypeMatch();
		_rhs.resetAllNodeTypeMatch();
		_operator.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_lhs.setAllNodeTypeMatch();
		_rhs.setAllNodeTypeMatch();
		_operator.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_OP_ASSIGN);
		_fVector.combineFeature(_lhs.getFeatureVector());
		_fVector.combineFeature(_rhs.getFeatureVector());
	}

}
