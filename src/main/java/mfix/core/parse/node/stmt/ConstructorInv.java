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
import mfix.core.parse.node.expr.ExprList;
import mfix.core.parse.node.expr.MType;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ConstructorInv  extends Stmt implements Serializable {

	private static final long serialVersionUID = -680765569439500998L;
	private MType _thisType = null;
	private ExprList _arguments = null;
	
	/**
	 * ConstructorInvocation:
     *	[ < Type { , Type } > ]
     *	       this ( [ Expression { , Expression } ] ) ;
	 */
	public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.CONSTRUCTORINV;
	}
	
	public void setThisType(MType thisType){
		_thisType = thisType;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
//		if(_thisType != null) {
//			stringBuffer.append(_thisType.toSrcString());
//			stringBuffer.append(".");
//		}
		stringBuffer.append("this(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer arguments = null;
		if(_binding != null && _binding instanceof ConstructorInv) {
			ConstructorInv constructorInv = (ConstructorInv) _binding;
			for(Modification modification : constructorInv.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == constructorInv._arguments) {
						arguments = update.getTarString(exprMap, allUsableVars);
						if(arguments == null) return null;
					} else {
						LevelLogger.error("@ConstructorInv ERROR");
					}
				} else {
					LevelLogger.error("@ConstructorInv Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("this(");
		if(arguments == null) {
			StringBuffer tmp = _arguments.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(arguments);
		}
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("this(");
		StringBuffer tmp = _arguments.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("this(");
			stringBuffer.append(_arguments.printMatchSketch());
			stringBuffer.append(");");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_arguments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ConstructorInv) {
			ConstructorInv constructorInv = (ConstructorInv) other;
			match = _arguments.compare(constructorInv._arguments);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_arguments.getCalledMethods());
			String thisStr = "this";
			Set<Node> set = _keywords.get(thisStr);
			if(set == null) {
				set = new HashSet<>();
			}
			set.add(this);
			_keywords.put(thisStr, set);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_arguments.extractModifications());
		}
		return modifications;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ConstructorInv) {
			_matchNodeType = true;
			ConstructorInv constructorInv = (ConstructorInv) other;
			_arguments.deepMatch(constructorInv._arguments);
			if(!_arguments.isNodeTypeMatch()) {
				Update update = new Update(this, _arguments, constructorInv._arguments);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ConstructorInv) {
			match = true;
			ConstructorInv constructorInv = (ConstructorInv) sketch;
			if(!constructorInv.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(constructorInv._thisType != null && constructorInv._thisType.isKeyPoint()) {
					if(_thisType == null) {
						match = false;
					} else {
						match = _thisType.matchSketch(constructorInv._thisType);
					}
				}
				if(match && constructorInv._arguments.isKeyPoint()) {
					match = _arguments.matchSketch(constructorInv._arguments);
				}
			}
			if(match) {
				constructorInv._binding = this;
				_binding = constructorInv;
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
		if (sketch instanceof ConstructorInv) {
			match = true;
			ConstructorInv constructorInv = (ConstructorInv) sketch;
			if (constructorInv._thisType != null && constructorInv._thisType.isKeyPoint()) {
				if (_thisType != null) {
					_thisType.bindingSketch(constructorInv._thisType);
				}
			}
			if (constructorInv._arguments.isKeyPoint()) {
				_arguments.bindingSketch(constructorInv._arguments);
			}
		}
		return match;
	}

	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof ConstructorInv) {
			Map<String, Set<Node>> keywords = patternNode.getCalledMethods();
			Map<String, Set<Node>> thisKeys = getCalledMethods();
			Node binding = this;
			for(Entry<String, Set<Node>> entry : keywords.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					binding = null;
					break;
				}
			}
			return binding;
		}
		return null;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_arguments.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_arguments.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_THIS);
		_fVector.combineFeature(_arguments.getFeatureVector());
	}

}
