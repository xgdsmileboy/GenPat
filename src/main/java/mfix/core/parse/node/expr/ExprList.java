/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.Matcher;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ExprList extends Node {

	private List<Expr> _exprs;
	
	public ExprList(int startLine, int endLine, ASTNode oriNode) {
		super(startLine, endLine, oriNode);
	}
	
	public void setExprs(List<Expr> exprs) {
		this._exprs = exprs;
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof ExprList) {
			ExprList exprList = (ExprList) other;
			if(_exprs.size() == exprList._exprs.size()) {
				for(int i = 0; i < _exprs.size(); i++) {
					if(!_exprs.get(i).compare(exprList._exprs.get(i))) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_exprs.size() > 0) {
			stringBuffer.append(_exprs.get(0).toSrcString());
			for(int i = 1; i < _exprs.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_exprs.get(i).toSrcString());
			}
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(_exprs.size() > 0) {
			tmp = _exprs.get(0).applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for(int i = 1; i < _exprs.size(); i++) {
				stringBuffer.append(",");
				tmp = _exprs.get(i).applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
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
		if(_exprs.size() > 0) {
			tmp = _exprs.get(0).replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for(int i = 1; i < _exprs.size(); i++) {
				stringBuffer.append(",");
				tmp = _exprs.get(i).replace(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if(_exprs.size() > 0) {
				stringBuffer.append(_exprs.get(0).printMatchSketch());
				for(int i = 1; i < _exprs.size(); i++) {
					stringBuffer.append(",");
					stringBuffer.append(_exprs.get(i).printMatchSketch());
				}
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_exprs.size());
		children.addAll(_exprs);
		return children;
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		for(Expr expr : _exprs) {
			_fVector.combineFeature(expr.getFeatureVector());
		}
	}

	@Override
	public void resetAllNodeTypeMatch() {
		for(Expr expr : _exprs) {
			expr.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		for(Expr expr : _exprs) {
			expr.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			for(Expr expr : _exprs) {
				avoidDuplicate(_keywords, expr);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			for(Expr expr : _exprs) {
				modifications.addAll(expr.extractModifications());
			}
		}
		return modifications;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ExprList) {
			_matchNodeType = true;
			ExprList exprList = (ExprList) other;
			if(!Matcher.matchNodeList(this, _exprs, exprList._exprs).isEmpty()){
				_matchNodeType = false;
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ExprList) {
			match = true;
			ExprList exprList = (ExprList) sketch;
			if(!exprList.isNodeTypeMatch()) {
				match = true;
				bindingSketch(sketch);
			} else {
				Set<Integer> alreadyMatch = new HashSet<>();
				for(Expr expr : exprList._exprs) {
					if(expr.isKeyPoint()) {
						boolean singleMatch = false;
						for(int i = 0; i < _exprs.size(); i++) {
							if(alreadyMatch.contains(i)) {
								continue;
							}
							if(_exprs.get(i).matchSketch(expr)) {
								singleMatch = true;
								alreadyMatch.add(i);
								break;
							}
						}
						if(!singleMatch) {
							return false;
						}
					}
				}
				match = true;
			}
			if(match) {
				exprList._binding = this;
				_binding = exprList;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof ExprList) {
			ExprList exprList = (ExprList) sketch;
			
			Set<Integer> alreadyMatch = new HashSet<>();
			for (Expr expr : exprList._exprs) {
				if (expr.isKeyPoint()) {
					for (int i = 0; i < _exprs.size(); i++) {
						if (alreadyMatch.contains(i)) {
							continue;
						}
						if (_exprs.get(i).bindingSketch(expr)) {
							alreadyMatch.add(i);
							break;
						} else {
							expr.resetBinding();
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_exprs.size() > 0) {
			_tokens.addAll(_exprs.get(0).tokens());
			for(int i = 1; i < _exprs.size(); i++) {
				_tokens.add(",");
				_tokens.addAll(_exprs.get(i).tokens());
			}
		}
	}

}
