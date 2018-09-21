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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class MethodInv extends Expr {

	private Expr _expression = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 *  MethodInvocation:
     *  [ Expression . ]
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public MethodInv(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.MINVOCATION;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setName(SName name){
		_name = name;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}
	
	public Expr getExpression(){
		return _expression;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression != null) {
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if (_arguments != null) {
			stringBuffer.append(_arguments.toSrcString());
		}
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer arguments = null;
		if(_binding != null && _binding instanceof MethodInv) {
			MethodInv methodInv = (MethodInv) _binding;
			for(Modification modification : methodInv.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == methodInv._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						arguments = update.getTarString(exprMap, allUsableVars);
						if(arguments == null) return null;
					}
				} else {
					LevelLogger.error("@MethodInv Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(expression == null) {
			if (_expression != null) {
				tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(expression + ".");
		}
		stringBuffer.append(_name.getName());
		stringBuffer.append("(");
		if(arguments == null) {
			if (_arguments != null) {
				tmp = _arguments.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(arguments);
		}
		stringBuffer.append(")");
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
		if (_expression != null) {
			tmp = _expression.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(".");
		}
		stringBuffer.append(_name.getName());
		stringBuffer.append("(");
		if (_arguments != null) {
			tmp = _arguments.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if (_expression != null) {
				stringBuffer.append(_expression.printMatchSketch());
				stringBuffer.append(".");
			}
			stringBuffer.append(_name.printMatchSketch());
			stringBuffer.append("(");
			if (_arguments != null) {
				stringBuffer.append(_arguments.printMatchSketch());
			}
			stringBuffer.append(")");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_expression != null) {
			set.addAll(_expression.getAllVars());
		}
		if (_arguments != null) {
			set.addAll(_arguments.getAllVars());
		}
		return set;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_expression != null) {
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		if (_arguments != null) {
			_tokens.addAll(_arguments.tokens());
		}
		_tokens.add(")");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof MethodInv) {
			MethodInv methodInv = (MethodInv) other;
			match = _name.compare(methodInv._name);
			if (match) {
				match = _expression == null ? (methodInv._expression == null)
						: _expression.compare(methodInv._expression) && _arguments.compare(methodInv._arguments);
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_arguments.getKeywords());
			avoidDuplicate(_keywords, _expression);
			String name = _name.getName();
			if(!NodeUtils.IGNORE_METHOD_INVOKE.contains(name)) {
				Set<Node> set = _keywords.get(name);
				if(set == null) set = new HashSet<>();
				set.add(_name);
				_keywords.put(name, set);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if(_expression != null) {
			children.add(_expression);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_expression != null) {
				modifications.addAll(_expression.extractModifications());
			}
			modifications.addAll(_name.extractModifications());
			modifications.addAll(_arguments.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof MethodInv) {
			_matchNodeType = true;
			MethodInv methodInv = (MethodInv) other;
			if(_expression == null && methodInv._expression != null) {
				_matchNodeType = false;
				return;
			}
			if(!_name.getName().equals(methodInv._name.getName())) {
				_matchNodeType = false;
				return;
			}
			_name.deepMatch(methodInv._name);
			if(_expression != null && methodInv._expression != null) {
				_expression.deepMatch(methodInv._expression);
				if(!_expression.isNodeTypeMatch()) {
					Update update = new Update(this, _expression, methodInv._expression);
					_modifications.add(update);
				}
			} else if(_expression != null) {
				Update update = new Update(this, _expression, methodInv._expression);
				_modifications.add(update);
			}
			_arguments.deepMatch(methodInv._arguments);
			if(!_arguments.isNodeTypeMatch()) {
				Update update = new Update(this, _arguments, methodInv._arguments);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof MethodInv) {
			match = true;
			MethodInv methodInv = (MethodInv) sketch;
			if(!methodInv.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(methodInv._name.isKeyPoint()) {
					if(_name.getName().equals(methodInv._name.getName())){
						match = true;
						_name.matchSketch(methodInv._name);
					} else {
						match = false;
					}
				}
				
				if(match && methodInv._arguments.isKeyPoint()) {
					match = _arguments.matchSketch(methodInv._arguments);
				}
			}
			if(match) {
				methodInv._binding = this;
				_binding = methodInv;
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
		if (sketch instanceof MethodInv) {
			match = true;
			MethodInv methodInv = (MethodInv) sketch;
			if(methodInv._name.isKeyPoint()) {
				if(_name.getName().equals(methodInv._name.getName())){
					_name.bindingSketch(methodInv._name);
				} else {
					match = false;
				}
			}
			if(match) {
				if (methodInv._name.isKeyPoint()) {
					_name.bindingSketch(methodInv._name);
				}
				if(methodInv._expression != null && _expression != null && methodInv._expression.isKeyPoint()) {
					_expression.bindingSketch(methodInv._expression);
				}
				if (methodInv._arguments.isKeyPoint()) {
					_arguments.bindingSketch(methodInv._arguments);
				}
			}
		}
		return match;
	}

	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_expression != null) {
			_expression.resetAllNodeTypeMatch();
		}
		_name.resetAllNodeTypeMatch();
		_arguments.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_expression != null) {
			_expression.setAllNodeTypeMatch();
		}
		_name.setAllNodeTypeMatch();
		_arguments.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_MCALL);
		if(_expression != null){
			_fVector.combineFeature(_expression.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}
	
}
