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
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
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
public class ArrayInitial extends Expr implements Serializable {

	private static final long serialVersionUID = 5694794734726396689L;
	private List<Expr> _expressions = null;

	/**
	 * ArrayInitializer: { [ Expression { , Expression} [ , ]] }
	 */
	public ArrayInitial(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ARRINIT;
	}

	public void setExpressions(List<Expr> expressions) {
		_expressions = expressions;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_expressions.size());
		children.addAll(_expressions);
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("{");
		if (_expressions.size() > 0) {
			stringBuffer.append(_expressions.get(0).toSrcString());
			for (int i = 1; i < _expressions.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_expressions.get(i).toSrcString());
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("{");
		StringBuffer tmp = null;
		if (_expressions.size() > 0) {
			tmp = _expressions.get(0).applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _expressions.size(); i++) {
				stringBuffer.append(",");
				tmp = _expressions.get(i).applyChange(exprMap, allUsableVars);
				stringBuffer.append(tmp);
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("{");
		StringBuffer tmp = null;
		if (_expressions.size() > 0) {
			tmp = _expressions.get(0).replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _expressions.size(); i++) {
				stringBuffer.append(",");
				tmp = _expressions.get(i).replace(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("{");
			if (_expressions.size() > 0) {
				stringBuffer.append(_expressions.get(0).printMatchSketch());
				for (int i = 1; i < _expressions.size(); i++) {
					stringBuffer.append(",");
					stringBuffer.append(_expressions.get(i).printMatchSketch());
				}
			}
			stringBuffer.append("}");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("{");
		if (_expressions.size() > 0) {
			_tokens.addAll(_expressions.get(0).tokens());
			for (int i = 1; i < _expressions.size(); i++) {
				_tokens.add(",");
				_tokens.addAll(_expressions.get(i).tokens());
			}
		}
		_tokens.add("}");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ArrayInitial) {
			ArrayInitial arrayInitial = (ArrayInitial) other;
			match = (_expressions.size() == arrayInitial._expressions.size());
			for(int i = 0; match && i < _expressions.size(); i ++) {
				match = match && _expressions.get(i).compare(arrayInitial._expressions.get(i));
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			for(Expr expr : _expressions) {
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
			for(Expr expr : _expressions) {
				modifications.addAll(expr.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ArrayInitial) {
			_matchNodeType = true;
			ArrayInitial arrayInitial = (ArrayInitial) other;
			if(!Matcher.matchNodeList(this, _expressions, arrayInitial._expressions).isEmpty()){
				_matchNodeType = false;
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ArrayInitial) {
			match = true;
			ArrayInitial arrayInitial = (ArrayInitial) sketch;
			if(!arrayInitial.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				Set<Integer> alreadyMatch = new HashSet<>();
				for(Expr expr : arrayInitial._expressions) {
					if(expr.isKeyPoint()) {
						boolean singleMatch = false;
						for(int i = 0; i < _expressions.size(); i++) {
							if(alreadyMatch.contains(i)) {
								continue;
							}
							if(_expressions.get(i).matchSketch(expr)) {
								singleMatch = true;
								alreadyMatch.add(i);
								break;
							}
						}
						if(!singleMatch) {
							match = false;
							break;
						}
					}
				}
			}
			if(match) {
				arrayInitial._binding = this;
				_binding = arrayInitial;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof ArrayInitial) {
			ArrayInitial arrayInitial = (ArrayInitial) sketch;
			
			Set<Integer> alreadyMatch = new HashSet<>();
			for (Expr expr : arrayInitial._expressions) {
				if (expr.isKeyPoint()) {
					for (int i = 0; i < _expressions.size(); i++) {
						if (alreadyMatch.contains(i)) {
							continue;
						}
						if (_expressions.get(i).bindingSketch(expr)) {
							alreadyMatch.add(i);
							break;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		for(Expr expr : _expressions) {
			expr.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		for(Expr expr : _expressions) {
			expr.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_AINIT);
		if(_expressions != null){
			for(Expr expr : _expressions){
				_fVector.combineFeature(expr.getFeatureVector());
			}
		}
	}
	
}
