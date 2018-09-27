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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SuperMethodInv extends Expr implements Serializable {

	private static final long serialVersionUID = -227589196009347171L;
	private Label _label = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperMethodInvocation:
     *	[ ClassName . ] super .
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public SuperMethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SMINVOCATION;
	}
	
	public void setLabel(Label label){
		_label = label;
	}
	
	public void setName(SName name){
		_name = name;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_label != null){
			stringBuffer.append(_label.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer label = null;
		StringBuffer name = null;
		StringBuffer arguments = null;
		if(_binding != null && _binding instanceof SuperMethodInv) {
			SuperMethodInv superMethodInv = (SuperMethodInv) _binding;
			for(Modification modification : superMethodInv.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == superMethodInv._label) {
						label = update.getTarString(exprMap, allUsableVars);
						if(label == null) return null;
					} else if(node == superMethodInv._name) {
						name = update.getTarString(exprMap, allUsableVars);
						if(name == null) return null;
					} else {
						arguments = update.getTarString(exprMap, allUsableVars);
						if(arguments == null) return null;
					}
				} else {
					LevelLogger.error("@SuperMethodInv Should not be this kind of modificaiton : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(label == null) {
			if(_label != null){
				tmp = _label.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(label + ".");
		}
		stringBuffer.append("super.");
		if(name == null) {
			tmp = _name.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		stringBuffer.append("(");
		if(arguments == null) {
			tmp = _arguments.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
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
		if(_label != null){
			tmp = _label.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_name.getName());
		stringBuffer.append("(");
		tmp = _arguments.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if(_label != null){
				stringBuffer.append(_label.printMatchSketch());
				stringBuffer.append(".");
			}
			stringBuffer.append("super.");
			stringBuffer.append(_name.printMatchSketch());
			stringBuffer.append("(");
			stringBuffer.append(_arguments.printMatchSketch());
			stringBuffer.append(")");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if(_label != null){
			set.addAll(_label.getAllVars());
		}
		set.addAll(_arguments.getAllVars());
		return set;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_label != null){
			_tokens.addAll(_label.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");	
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SuperMethodInv) {
			SuperMethodInv superMethodInv = (SuperMethodInv) other;
			match = (_label == null) ? (superMethodInv._label == null) : _label.compare(superMethodInv._label);
			match = match && _name.compare(superMethodInv._name) && _arguments.compare(superMethodInv._arguments);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if(_label != null) {
			children.add(_label);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_arguments.getCalledMethods());
			avoidDuplicate(_keywords, _label);
			Set<Node> set = _keywords.get(_name.getName());
			if(set == null) {
				set = new HashSet<>();
			}
			set.add(_name);
			_keywords.put(_name.getName(), set);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_label != null) {
				modifications.addAll(_label.extractModifications());
			}
			modifications.addAll(_arguments.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof SuperMethodInv) {
			_matchNodeType = true;
			SuperMethodInv superMethodInv = (SuperMethodInv) other;
			if((_label == null && superMethodInv._label != null) || !(_name.getName().equals(superMethodInv._name.getName()))) {
				_matchNodeType = false;
				return;
			}
			if(_label != null && superMethodInv._label != null) {
				_label.deepMatch(superMethodInv._label);
				if(!_label.isNodeTypeMatch()) {
					Update update = new Update(this, _label, superMethodInv._label);
					_modifications.add(update);
				}
			} else if(_label != null) {
				Update update = new Update(this, _label, superMethodInv._label);
				_modifications.add(update);
			}
			_name.deepMatch(superMethodInv._name);
			if(!_name.isNodeTypeMatch()){
				Update update = new Update(this, _name, superMethodInv._name);
				_modifications.add(update);
			}
			_arguments.deepMatch(superMethodInv._arguments);
			if(!_arguments.isNodeTypeMatch()){
				Update update = new Update(this, _arguments, superMethodInv._arguments);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof SuperMethodInv) {
			match = true;
			SuperMethodInv superMethodInv = (SuperMethodInv) sketch;
			if(!superMethodInv.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(superMethodInv._label != null && superMethodInv._label.isKeyPoint()) {
					if(_label != null) {
						match = _label.matchSketch(superMethodInv._label);
					} else {
						return false;
					}
				}
				if(match && superMethodInv._name.isKeyPoint()) {
					match = _name.getName().equals(superMethodInv._name.getName());
				}
				if(match && _arguments.isKeyPoint()) {
					match = _arguments.matchSketch(superMethodInv._arguments);
				}
 			}
			if(match) {
				superMethodInv._binding = this;
				_binding = superMethodInv;
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
		if(sketch instanceof SuperMethodInv) {
			match = true;
			SuperMethodInv superMethodInv = (SuperMethodInv) sketch;
			if(superMethodInv._label != null && superMethodInv._label.isKeyPoint()) {
				if(_label != null) {
					_label.bindingSketch(superMethodInv._label);
				}
			}
			if(superMethodInv._name.isKeyPoint()) {
				match = _name.getName().equals(superMethodInv._name.getName());
				if(match) _name.bindingSketch(superMethodInv._name);
			}
			if(match && _arguments.isKeyPoint()) {
				_arguments.bindingSketch(superMethodInv._arguments);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_label != null) {
			_label.resetAllNodeTypeMatch();
		}
		_name.resetAllNodeTypeMatch();
		_arguments.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_label != null) {
			_label.setAllNodeTypeMatch();
		}
		_name.setAllNodeTypeMatch();
		_arguments.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SUPER);
		_fVector.inc(FVector.E_MINV);
		if(_label != null){
			_fVector.combineFeature(_label.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}
		
}
