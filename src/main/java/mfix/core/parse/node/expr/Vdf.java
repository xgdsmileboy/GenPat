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
import mfix.core.parse.node.stmt.Stmt;
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
public class Vdf extends Node {

	private SName _identifier = null;
	private int _dimensions = 0; 
	private Expr _expression = null;
	
	/**
	 * VariableDeclarationFragment:
     *	Identifier { Dimension } [ = Expression ]
	 */
	public Vdf(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.VARDECLFRAG;
	}
	
	public Vdf(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}
	
	public void setName(SName identifier){
		_identifier = identifier;
	}
	
	public String getName() {
		return _identifier.getName();
	}
	
	public void setDimensions(int dimensions){
		_dimensions = dimensions;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_identifier.toSrcString());
		for(int i = 0; i < _dimensions; i++){
			stringBuffer.append("[]");
		}
		if(_expression != null){
			stringBuffer.append("=");
			stringBuffer.append(_expression.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer identifier = null;
		StringBuffer expression = null;
		if(_binding != null && _binding instanceof Vdf) {
			Vdf vdf = (Vdf) _binding;
			for(Modification modification : vdf.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == vdf._identifier) {
						identifier = update.getTarString(exprMap, allUsableVars);
						if(identifier == null) return null;
					} else {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					}
				} else {
					LevelLogger.error("@Vdf Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(identifier == null) {
			tmp = _identifier.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(identifier);
		}
		for(int i = 0; i < _dimensions; i++){
			stringBuffer.append("[]");
		}
		if(expression == null) {
			if(_expression != null){
				stringBuffer.append("=");
				tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append("=");
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
		StringBuffer tmp = null;
		tmp = _identifier.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		for(int i = 0; i < _dimensions; i++){
			stringBuffer.append("[]");
		}
		if(_expression != null){
			stringBuffer.append("=");
			tmp = _expression.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_identifier.printMatchSketch());
			for(int i = 0; i < _dimensions; i++){
				stringBuffer.append("[]");
			}
			if(_expression != null){
				stringBuffer.append("=");
				stringBuffer.append(_expression.printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_identifier.tokens());
		for(int i = 0; i < _dimensions; i++){
			_tokens.add("[]");
		}
		if(_expression != null){
			_tokens.add("=");
			_tokens.addAll(_expression.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof Vdf) {
			Vdf vdf = (Vdf) other;
			match = (_dimensions == vdf._dimensions) && _identifier.compare(vdf._identifier);
			if(_expression == null) {
				match = match && (vdf._expression == null);
			} else {
				match = match && _expression.compare(vdf._expression);
			}
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_identifier);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			if(_expression != null) {
				_keywords.putAll(_expression.getCalledMethods());
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
		if(other instanceof Vdf) {
			_matchNodeType = true;
			Vdf vdf = (Vdf) other;
			if(_expression == null && vdf._expression != null) {
				_matchNodeType = false;
				return;
			}
			if(_dimensions != vdf._dimensions){
				_matchNodeType = false;
			} else {
				_identifier.deepMatch(vdf._identifier);
				if(!_identifier.isNodeTypeMatch()) {
					Update update = new Update(this, _identifier, vdf._identifier);
					_modifications.add(update);
				}
			}
			
			if(_expression != null && vdf._expression != null) {
				_expression.deepMatch(vdf._expression);
				if(!_expression.isNodeTypeMatch()) {
					Update update = new Update(this, _expression, vdf._expression);
					_modifications.add(update);
				}
			} else if(_expression != null) {
				Update update = new Update(this, _expression, vdf._expression);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof Vdf) {
			match = true;
			Vdf vdf = (Vdf) sketch;
			if(!vdf.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(vdf._identifier.isKeyPoint()) {
					match = _identifier.matchSketch(vdf._identifier);
				}
				if(match && vdf._expression != null && vdf._expression.isKeyPoint()) {
					if(_expression == null) {
						return false;
					} else {
						match = _expression.matchSketch(vdf._expression);
					}
				}
			}
			if(match) {
				vdf._binding = this;
				_binding = vdf;
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
		if (sketch instanceof Vdf) {
			match = true;
			Vdf vdf = (Vdf) sketch;
			if (vdf._identifier.isKeyPoint()) {
				_identifier.bindingSketch(vdf._identifier);
			}
			if (vdf._expression != null && vdf._expression.isKeyPoint()) {
				if (_expression != null) {
					_expression.bindingSketch(vdf._expression);
				}
			}
		}
		return match;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_identifier.resetAllNodeTypeMatch();
		if(_expression != null) {
			_expression.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_identifier.setAllNodeTypeMatch();
		if(_expression != null) {
			_expression.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_identifier.getFeatureVector());
		if(_expression != null){
		    _fVector.inc(FVector.ARITH_ASSIGN);
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

}
