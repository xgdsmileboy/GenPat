/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.config.Constant;
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
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class ReturnStmt extends Stmt {

	private Expr _expression = null;
	
	/**
	 * ReturnStatement:
     *	return [ Expression ] ;
	 */
	public ReturnStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
		_nodeType = TYPE.RETURN;
	}

	public ReturnStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("return ");
		if(_expression != null){
			stringBuffer.append(_expression.toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		if(_binding != null && _binding instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) _binding;
			for(Modification modification : returnStmt.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == returnStmt._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						LevelLogger.error("@ReturnStmt ERROR");
					}
				} else {
					LevelLogger.error("@ReturnStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("return ");
		if(expression == null) {
			if(_expression != null){
				StringBuffer tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("return ");
		if(_expression != null){
			StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("return ");
			if(_expression != null){
				stringBuffer.append(_expression.printMatchSketch());
			}
			stringBuffer.append(";");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("return");
		if(_expression != null){
			_tokens.addAll(_expression.tokens());
		}
		_tokens.add(";");
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) other;
			if(_expression == null) {
				match = (returnStmt._expression == null);
			} else {
				match = _expression.compare(returnStmt._expression);
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			if(_expression != null) {
				_keywords.putAll(_expression.getKeywords());
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_expression != null) {
				modifications.addAll(_expression.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) other;
			if(_expression == null && returnStmt._expression != null) {
				_matchNodeType = false;
				return;
			}
			_matchNodeType = true;
			if(_expression != null && returnStmt._expression != null) {
				_expression.deepMatch(returnStmt._expression);
				if(!_expression.isNodeTypeMatch()) {
					Update update = new Update(this, _expression, returnStmt._expression);
					_modifications.add(update);
				}
			} else if(_expression != null) {
				Update update = new Update(this, _expression, returnStmt._expression);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ReturnStmt) {
			match = true;
			ReturnStmt returnStmt = (ReturnStmt) sketch;
			if(!returnStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(returnStmt._expression != null && returnStmt._expression.isKeyPoint()) {
					if(_expression == null) {
						match = false;
					} else {
						match = _expression.matchSketch(returnStmt._expression);
					}
				}
			}
			if(match) {
				returnStmt._binding = this;
				_binding = returnStmt;
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
		if (sketch instanceof ReturnStmt) {
			match = true;
			ReturnStmt returnStmt = (ReturnStmt) sketch;
			if (returnStmt._expression != null && returnStmt._expression.isKeyPoint()) {
				if (_expression != null) {
					_expression.bindingSketch(returnStmt._expression);
				}
			}
		}
		return match;
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof ReturnStmt) {
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					return null;
				}
			}
			return this;
		} else {
			return null;
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_expression != null) {
			_expression.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_expression != null) {
			_expression.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_STRUCT_OTHER);
		if(_expression != null) {
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}
}
