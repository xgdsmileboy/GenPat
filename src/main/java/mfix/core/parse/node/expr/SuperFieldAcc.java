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
public class SuperFieldAcc extends Expr implements Serializable {

	private static final long serialVersionUID = 1921879022776437618L;
	private Label _name = null;
	private SName _identifier = null;
	
	/**
	 * SuperFieldAccess:
     *	[ ClassName . ] super . Identifier
	 */
	public SuperFieldAcc(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SFIELDACC;
	}
	
	public void setName(Label name){
		_name = name;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_name != null){
			stringBuffer.append(_name.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer name = null;
		StringBuffer identifier = null;
		if(_binding != null && _binding instanceof SuperFieldAcc) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) _binding;
			for(Modification modification : superFieldAcc.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == superFieldAcc._name){
						name = update.getTarString(exprMap, allUsableVars);
						if(name == null) return null;
					} else {
						identifier = update.getTarString(exprMap, allUsableVars);
						if(identifier == null) return null;
					}
				} else {
					LevelLogger.error("@SuperFieldAcc Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(name == null) {
			if(_name != null){
				tmp = _name.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(name + ".");
		}
		stringBuffer.append("super.");
		if(identifier == null) {
			tmp = _identifier.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(identifier);
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
		if(_name != null){
			tmp = _name.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		tmp = _identifier.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if(_name != null){
				stringBuffer.append(_name.printMatchSketch());
				stringBuffer.append(".");
			}
			stringBuffer.append("super.");
			stringBuffer.append(_identifier.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_name != null){
			_tokens.addAll(_name.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SuperFieldAcc) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) other;
			match = (_name == null) ? (superFieldAcc._name == null) : _name.compare(superFieldAcc._name);
			if(match) {
				match = match && _identifier.compare(superFieldAcc._identifier);
			}
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		if(_name != null) {
			children.add(_name);
		}
		children.add(_identifier);
		return children;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) { 
			_keywords = new HashMap<>(7);
			if(_name != null) {
				_keywords.putAll(_name.getCalledMethods());
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_name != null) {
				modifications.addAll(_name.extractModifications());
			}
			modifications.addAll(_identifier.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof SuperFieldAcc) {
			_matchNodeType = true;
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) other;
			if(_name == null && superFieldAcc._name != null) {
				_matchNodeType = false;
				return;
			}
			if(_name != null && superFieldAcc._name != null) {
				_name.deepMatch(superFieldAcc._name);
				if(!_name.isNodeTypeMatch()) {
					Update update = new Update(this, _name, superFieldAcc._name);
					_modifications.add(update);
				}
			} else if(_name != null) {
				Update update = new Update(this, _name, superFieldAcc._name);
				_modifications.add(update);
			}
			_identifier.deepMatch(superFieldAcc._identifier);
			if(!_identifier.isNodeTypeMatch()) {
				Update update = new Update(this, _identifier, superFieldAcc._identifier);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof SuperFieldAcc) {
			match = true;
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) sketch;
			if(!superFieldAcc.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(superFieldAcc._name != null && superFieldAcc._name.isKeyPoint()) {
					if(_name != null) {
						match = _name.matchSketch(superFieldAcc._name);
					} else {
						match = false;
					}
				}
				if(match && superFieldAcc._identifier.isKeyPoint()) {
					match = _identifier.matchSketch(superFieldAcc._identifier);
				}
			}
			if(match) {
				superFieldAcc._binding = this;
				_binding = superFieldAcc;
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
		if (sketch instanceof SuperFieldAcc) {
			match = true;
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) sketch;
			if (superFieldAcc._name != null && superFieldAcc._name.isKeyPoint()) {
				if (_name != null) {
					_name.bindingSketch(superFieldAcc._name);
				}
			}
			if (superFieldAcc._identifier.isKeyPoint()) {
				_identifier.bindingSketch(superFieldAcc._identifier);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_name != null) {
			_name.resetAllNodeTypeMatch();
		}
		_identifier.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_name != null) {
			_name.setAllNodeTypeMatch();
		}
		_identifier.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SUPER);
		_fVector.inc(FVector.E_FACC);
		if(_name != null){
			_fVector.combineFeature(_name.getFeatureVector());
		}
		_fVector.combineFeature(_identifier.getFeatureVector());	
	}
}
