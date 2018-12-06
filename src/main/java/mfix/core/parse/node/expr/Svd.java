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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class Svd extends Expr implements Serializable {

	private static final long serialVersionUID = 3849439897999091912L;
	private MType _decType = null;
	private SName _name = null;
	private Expr _initializer = null;
	
	/**
	 * { ExtendedModifier } Type {Annotation} [ ... ] Identifier { Dimension } [ = Expression ]
	 * "..." should not be appear since it is only used in method declarations
	 */
	public Svd(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SINGLEVARDECL;
	}
	
	public void setDecType(MType decType){
		_decType = decType;
	}
	
	public void setName(SName name){
		_name = name;
	}

	public MType getDeclType() {
		return _decType;
	}

	public Expr getInitializer() {
		return _initializer;
	}

	public SName getName(){
		return _name;
	}
	
	public void setInitializer(Expr initializer){
		_initializer = initializer;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_decType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_name.toSrcString());
		if(_initializer != null){
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer declType = null;
		StringBuffer name = null;
		StringBuffer initializer = null;
		if(_binding != null && _binding instanceof Svd) {
			Svd svd = (Svd) _binding;
			for(Modification modification : svd.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == svd._decType) {
						declType = update.getTarString(exprMap, allUsableVars);
						if(declType == null) return null;
					} else if(node == svd._name) {
						name = update.getTarString(exprMap, allUsableVars);
						if(name == null) return null;
					} else {
						initializer = update.getTarString(exprMap, allUsableVars);
						if(initializer == null) return null;
					}
				} else {
					LevelLogger.error("@Svd Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(declType == null) {
			tmp = _decType.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(declType);
		}
		stringBuffer.append(" ");
		if(name == null) {
			tmp = _name.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		if(initializer == null) {
			if(_initializer != null){
				stringBuffer.append("=");
				tmp = _initializer.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
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
		tmp = _decType.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" ");
		tmp = _name.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		if(_initializer != null){
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
			stringBuffer.append(_decType.printMatchSketch());
			stringBuffer.append(" ");
			stringBuffer.append(_name.printMatchSketch());
			if(_initializer != null){
				stringBuffer.append("=");
				stringBuffer.append(_initializer.printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_decType.tokens());
		_tokens.addAll(_name.tokens());
		if(_initializer != null) {
			_tokens.addFirst("=");
			_tokens.addAll(_initializer.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof Svd) {
			Svd svd	= (Svd) other;
			match = _decType.compare(svd._decType);
			match = match && _name.compare(svd._name);
			if(_initializer == null) {
				match = match && (svd._initializer == null); 
			} else {
				match = match && _initializer.compare(svd._initializer);
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
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_decType);
		children.add(_name);
		if(_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_initializer != null) {
				modifications.addAll(_initializer.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof Svd) {
			_matchNodeType = true;
			Svd svd = (Svd) other;
			if(_initializer == null && svd._initializer != null) {
				_matchNodeType = false;
				return;
			}
			_decType.deepMatch(svd._decType);
			if(!_decType.isNodeTypeMatch()) {
				Update update = new Update(this, _decType, svd._decType);
				_modifications.add(update);
			}
			_name.deepMatch(svd._name);
			if(!_name.isNodeTypeMatch()) {
				Update update = new Update(this, _name, svd._name);
				_modifications.add(update);
			}
			
			if(_initializer != null && svd._initializer != null) {
				_initializer.deepMatch(svd._initializer);
				if(!_initializer.isNodeTypeMatch()) {
					Update update = new Update(this, _initializer, svd._initializer);
					_modifications.add(update);
				}
			} else if(_initializer != null) {
				Update update = new Update(this, _initializer, svd._initializer);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof Svd) {
			match = true;
			Svd svd = (Svd) sketch;
			if(!svd.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(svd._decType.isKeyPoint()) {
					match = _decType.matchSketch(svd._decType);
				}
				if(match && svd._name.isKeyPoint()) {
					match = _name.matchSketch(svd._name);
				}
				if(match && svd._initializer != null && svd._initializer.isKeyPoint()) {
					match = (_initializer == null) ? false : _initializer.matchSketch(svd._initializer);
				}
			}
			if(match) {
				svd._binding = this;
				_binding = svd;
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
		if (sketch instanceof Svd) {
			match = true;
			Svd svd = (Svd) sketch;
			if (svd._decType.isKeyPoint()) {
				_decType.bindingSketch(svd._decType);
			}
			if (svd._name.isKeyPoint()) {
				_name.bindingSketch(svd._name);
			}
			if (svd._initializer != null && svd._initializer.isKeyPoint()) {
				if(_initializer != null){
					_initializer.bindingSketch(svd._initializer);
				}
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_name.resetAllNodeTypeMatch();
		_decType.resetAllNodeTypeMatch();
		if(_initializer != null) {
			_initializer.resetAllNodeTypeMatch();
		}
	}
	
	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_name.setAllNodeTypeMatch();
		_decType.setAllNodeTypeMatch();
		if(_initializer != null) {
			_initializer.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_decType.getFeatureVector());
		_fVector.combineFeature(_name.getFeatureVector());
		if(_initializer != null){
			_fVector.inc(FVector.ARITH_ASSIGN);
			_fVector.combineFeature(_initializer.getFeatureVector());
		}
	}

}
