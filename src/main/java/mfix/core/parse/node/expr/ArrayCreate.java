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
import mfix.core.parse.Matcher;
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
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ArrayCreate extends Expr {

	private MType _type = null;
	private List<Expr> _dimension = null;
	private ArrayInitial _initializer = null;

	/**
	 * ArrayCreation: new PrimitiveType [ Expression ] { [ Expression ] } { [ ]
	 * } new TypeName [ < Type { , Type } > ] [ Expression ] { [ Expression ] }
	 * { [ ] } new PrimitiveType [ ] { [ ] } ArrayInitializer new TypeName [ <
	 * Type { , Type } > ] [ ] { [ ] } ArrayInitializer
	 */
	public ArrayCreate(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.ARRCREAT;
	}

	public void setArrayType(MType type) {
		_type = type;
	}

	public void setDimension(List<Expr> dimension) {
		_dimension = dimension;
	}

	public void setInitializer(ArrayInitial initializer) {
		_initializer = initializer;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_dimension.size() + 1);
		children.addAll(_dimension);
		if(_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("new ");
		stringBuffer.append(_type.toSrcString());
		for(Expr expr : _dimension) {
			stringBuffer.append("[");
			stringBuffer.append(expr.toSrcString());
			stringBuffer.append("]");
		}
		if(_initializer != null) {
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer initializer = null;
		if(_binding != null && _binding instanceof ArrayCreate) {
			ArrayCreate arrayCreate = (ArrayCreate) _binding;
			for(Modification modification : arrayCreate.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == arrayCreate._initializer) {
						initializer = update.getTarString(exprMap, allUsableVars);
						if(initializer == null) return null;
					} else {
						LevelLogger.error("@ArrayCreate ERROR!");
					}
				} else {
					LevelLogger.error("@ArrayCreate Should not be this kind of modification : " + modification.toString());
				}
			}
		}
		stringBuffer.append("new ");
		StringBuffer tmp = null;
		stringBuffer.append(_type.applyChange(exprMap, allUsableVars));
		for(Expr expr : _dimension) {
			stringBuffer.append("[");
			tmp = expr.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("]");
		}
		if(initializer == null) {
			if(_initializer != null) {
				stringBuffer.append("=");
				tmp = _initializer.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append("=");
			stringBuffer.append(initializer);
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
		stringBuffer.append("new ");
		stringBuffer.append(_type.replace(exprMap, allUsableVars));
		for(Expr expr : _dimension) {
			stringBuffer.append("[");
			tmp = expr.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("]");
		}
		if(_initializer != null) {
			stringBuffer.append("=");
			tmp = _initializer.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
		stringBuffer.append("new ");
		stringBuffer.append(_type.printMatchSketch());
		for(Expr expr : _dimension) {
			stringBuffer.append("[");
			stringBuffer.append(expr.printMatchSketch());
			stringBuffer.append("]");
		}
		if(_initializer != null) {
			stringBuffer.append("=");
			stringBuffer.append(_initializer.printMatchSketch());
		}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		for(Expr expr : _dimension) {
			set.addAll(expr.getAllVars());
		}
		if(_initializer != null) {
			set.addAll(_initializer.getAllVars());
		}
		return set;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("new");
		_tokens.addAll(_type.tokens());
		for(Expr expr : _dimension) {
			_tokens.add("[");
			_tokens.addAll(expr.tokens());
			_tokens.add("]");
		}
		if(_initializer != null) {
			_tokens.add("=");
			_tokens.addAll(_initializer.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ArrayCreate) {
			ArrayCreate arrayCreate = (ArrayCreate) other;
			match = _type.compare(arrayCreate._type);
			if(match) {
				match = match && (_dimension.size() == arrayCreate._dimension.size());
				for(int i = 0; match && i < _dimension.size(); i++) {
					match = match && _dimension.get(i).compare(arrayCreate._dimension.get(i));
				}
				if(_initializer == null) {
					match = match && (arrayCreate._initializer == null);
				} else {
					match = match && _initializer.compare(arrayCreate._initializer);
				}
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			if(_initializer != null) {
				_keywords.putAll(_initializer.getCalledMethods());
			}
			for(Expr expr : _dimension) {
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
			for(Expr expr : _dimension) {
				modifications.addAll(expr.extractModifications());
			}
			if(_initializer != null) {
				modifications.addAll(_initializer.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ArrayCreate) {
			_matchNodeType = true;
			ArrayCreate	arrayCreate = (ArrayCreate) other;
			if(_initializer == null && arrayCreate._initializer != null) {
				_matchNodeType = false;
				return;
			}
			if(!Matcher.matchNodeList(this, _dimension, arrayCreate._dimension).isEmpty()) {
				_matchNodeType = false;
				return;
			}
			
			if(_initializer != null && arrayCreate._initializer != null) {
				_initializer.deepMatch(arrayCreate._initializer);
				if(!_initializer.isNodeTypeMatch()) {
					Update update = new Update(this, _initializer, arrayCreate._initializer);
					_modifications.add(update);
				}
			} else if(_initializer != null){
				Update update = new Update(this, _initializer, arrayCreate._initializer);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ArrayCreate) {
			match = true;
			ArrayCreate	 arrayCreate = (ArrayCreate) sketch;
			// find changed node
			if(!arrayCreate.isNodeTypeMatch()){
				if(!NodeUtils.matchNode(sketch, this)){
					return false;
				}
				bindingSketch(sketch);
			} else {
				// match sub-nodes which are match point
				if(arrayCreate._type.isKeyPoint()) {
					match = _type.matchSketch(arrayCreate._type);
				}
				if(match) {
					if(arrayCreate._initializer != null && arrayCreate._initializer.isKeyPoint()) {
						match = (_initializer == null) ? false : _initializer.matchSketch(arrayCreate._initializer);
					}
				}
			}
			if(match) {
				arrayCreate._binding = this;
				_binding = arrayCreate;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof ArrayCreate) {
			ArrayCreate arrayCreate = (ArrayCreate) sketch;
			
			if (arrayCreate._type.isKeyPoint()) {
				_type.bindingSketch(arrayCreate._type);
			}
			if (arrayCreate._initializer != null && arrayCreate._initializer.isKeyPoint()) {
				if (_initializer != null) {
					_initializer.bindingSketch(arrayCreate._initializer);
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_type.resetAllNodeTypeMatch();
		for(Expr expr : _dimension) {
			expr.resetAllNodeTypeMatch();
		}
		if(_initializer != null) {
			_initializer.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_type.setAllNodeTypeMatch();
		for(Expr expr : _dimension) {
			expr.setAllNodeTypeMatch();
		}
		if(_initializer != null) {
			_initializer.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_NEW);
		_fVector.inc(FVector.E_ACREAR);
		if(_dimension != null){
			for(Expr expr : _dimension){
				_fVector.combineFeature(expr.getFeatureVector());
			}
		}
		if(_initializer != null){
			_fVector.combineFeature(_initializer.getFeatureVector());
		}
	}

}
