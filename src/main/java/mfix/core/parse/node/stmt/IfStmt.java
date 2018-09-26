/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class IfStmt extends Stmt {

	private Expr _condition = null;
	private Stmt _then = null;
	private Stmt _else = null;
	
	/**
	 * IfStatement:
     *	if ( Expression ) Statement [ else Statement]
	 */
	public IfStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
		_nodeType = TYPE.IF;
	}
	
	public IfStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}
	
	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setThen(Stmt then){
		_then = then;
	}
	
	public void setElse(Stmt els){
		_else = els;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Stmt getThen(){
		return _then;
	}
	
	public Stmt getElse(){
		return _else;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("if(");
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_then.toSrcString());
		if(_else != null) {
			stringBuffer.append("else ");
			stringBuffer.append(_else.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer condition = null;
		StringBuffer then = null;
		StringBuffer els = null;
		if(_binding != null && _binding instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) _binding;
			for(Modification modification : ifStmt.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == ifStmt._condition) {
						condition = update.getTarString(exprMap, allUsableVars);
						if(condition == null) return null;
					} else if(node == ifStmt._then) {
						then = update.getTarString(exprMap, allUsableVars);
						if(then == null) return null;
					} else {
						els = update.getTarString(exprMap, allUsableVars);
						if(els == null) return null;
					}
				} else {
					LevelLogger.error("@IfStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("if(");
		StringBuffer tmp = null;
		if(condition == null) {
			tmp = _condition.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(condition);
		}
		stringBuffer.append(")");
		if(then == null) {
			tmp = _then.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(then);
		}
		if(els == null) {
			if(_else != null) {
				stringBuffer.append("else ");
				tmp = _else.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(els);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("if(");
		StringBuffer tmp = null;
		tmp = _condition.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		tmp = _then.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		if(_else != null) {
			stringBuffer.append("else ");
			tmp = _else.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("if(");
			stringBuffer.append(_condition.printMatchSketch());
			stringBuffer.append(")");
			stringBuffer.append(_then.printMatchSketch());
			if(_else != null) {
				stringBuffer.append("else ");
				stringBuffer.append(_else.printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("if");
		_tokens.add("(");
		_tokens.addAll(_condition.tokens());
		_tokens.add(")");
		_tokens.addAll(_then.tokens());
		if(_else != null) {
			_tokens.add("else");
			_tokens.addAll(_else.tokens());
		}
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_then);
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(2);
		if(_then != null) {
			children.add(_then);
		}
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) other;
			match = _condition.compare(ifStmt._condition) && _then.compare(ifStmt._then);
			if(_else == null) {
				match = match && (ifStmt._else == null);
			} else {
				match = match && _else.compare(ifStmt._else);
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_condition.getCalledMethods());
			avoidDuplicate(_keywords, _then);
			avoidDuplicate(_keywords, _else);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_condition.extractModifications());
			modifications.addAll(_then.extractModifications());
			if(_else != null) {
				modifications.addAll(_else.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof IfStmt) {
			_matchNodeType = true;
			IfStmt ifStmt = (IfStmt) other;
			if(_else == null && ifStmt._else != null) {
				_matchNodeType = false;
				return;
			}
			_condition.deepMatch(ifStmt._condition);
			if(!_condition.isNodeTypeMatch()) {
				Update update = new Update(this, _condition, ifStmt._condition);
				_modifications.add(update);
			}
			_then.deepMatch(ifStmt._then);
			if(!_then.isNodeTypeMatch()) {
				Update update = new Update(this, _then, ifStmt._then);
				_modifications.add(update);
			}
			if(_else != null && ifStmt._else != null) {
				_else.deepMatch(ifStmt._else);
				if(!_else.isNodeTypeMatch()) {
					Update update = new Update(this, _else, ifStmt._else);
					_modifications.add(update);
				}
			} else if(_else != null){
				Update update = new Update(this, _else, ifStmt._else);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof IfStmt) {
			match = true;
			IfStmt ifStmt = (IfStmt) sketch;
			if(!ifStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(ifStmt._condition.isKeyPoint()) {
					match = _condition.matchSketch(ifStmt._condition);
				}
				if(ifStmt._then.isKeyPoint()) {
					match = _then.matchSketch(ifStmt._then);
				}
				if(match && ifStmt._else != null && ifStmt._else.isKeyPoint()) {
					if(_else == null) {
						match = false;
					} else {
						match = _else.matchSketch(ifStmt._else);
					}
				}
			}
			if(match) {
				ifStmt._binding = this;
				_binding = ifStmt;
			}
		}
		if(!match) {
			match = _then.matchSketch(sketch);
		}
		if(!match && _else != null) {
			match = _else.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof IfStmt) {
			match = true;
			IfStmt ifStmt = (IfStmt) sketch;
			if (ifStmt._condition.isKeyPoint()) {
				_condition.bindingSketch(ifStmt._condition);
			}
			if (ifStmt._then.isKeyPoint()) {
				_then.bindingSketch(ifStmt._then);
			}
			if (ifStmt._else != null && ifStmt._else.isKeyPoint()) {
				if (_else != null) {
					_else.bindingSketch(ifStmt._else);
				}
			}
		}
		return match;
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		boolean match = false;
		if(patternNode instanceof IfStmt) {
			match = true;
			Map<String, Set<Node>> map = patternNode.getCalledMethods();
			Map<String, Set<Node>> thisKeys = getCalledMethods();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					match = false;
					break;
				}
			}
		}
		if(match) {
			return this;
		} else {
			Node node = _then.bindingNode(patternNode);
			if(node == null && _else != null) {
				return _else.bindingNode(patternNode);
			} else {
				return node;
			}
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_condition.resetAllNodeTypeMatch();
		_then.resetAllNodeTypeMatch();
		if(_else != null) {
			_else.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_condition.setAllNodeTypeMatch();
		_then.setAllNodeTypeMatch();
		if(_else != null) {
			_else.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_IF);
		_fVector.combineFeature(_then.getFeatureVector());
		if(_else != null) {
			_fVector.inc(FVector.KEY_ELSE);
			_fVector.combineFeature(_else.getFeatureVector());
		}
	}
	
}
