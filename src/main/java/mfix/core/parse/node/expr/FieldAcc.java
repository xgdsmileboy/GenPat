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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class FieldAcc extends Expr {

	private Expr _expression = null;
	private SName _identifier = null;
	
	
	/**
	 * FieldAccess:
     *           Expression . Identifier
	 */
	public FieldAcc(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.FIELDACC;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(".");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer identifier = null;
		if(_binding != null && _binding instanceof FieldAcc) {
			FieldAcc fieldAcc = (FieldAcc) _binding;
			for(Modification modification : fieldAcc.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == fieldAcc._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						identifier = update.getTarString(exprMap, allUsableVars);
						if(identifier == null) return null;
					}
				} else {
					LevelLogger.error("@FieldAcc Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(".");
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
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(".");
		tmp = _identifier.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(".");
			stringBuffer.append(_identifier.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof FieldAcc) {
			FieldAcc fieldAcc = (FieldAcc) other;
			match = _expression.compare(fieldAcc._expression);
			match = match && _identifier.compare(fieldAcc._identifier);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_expression.getKeywords());
			avoidDuplicate(_keywords, _identifier);
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>();
		children.add(_expression);
		children.add(_identifier);
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_expression.extractModifications());
			modifications.addAll(_identifier.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof FieldAcc) {
			_matchNodeType = true;
			FieldAcc fieldAcc = (FieldAcc) other;
			_expression.deepMatch(fieldAcc._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, fieldAcc._expression);
				_modifications.add(update);
			}
			_identifier.deepMatch(fieldAcc._identifier);
			if(!_identifier.isNodeTypeMatch()) {
				Update update = new Update(this, _identifier, fieldAcc._identifier);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof FieldAcc) {
			match = true;
			FieldAcc fieldAcc = (FieldAcc) sketch;
			if(!fieldAcc.isNodeTypeMatch()){
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(fieldAcc._expression.isKeyPoint()) {
					match = _expression.matchSketch(fieldAcc._expression);
				}
				if(match && fieldAcc._identifier.isKeyPoint()) {
					match = _identifier.matchSketch(fieldAcc._identifier);
				}
			}
			if(match) {
				fieldAcc._binding = this;
				_binding = fieldAcc;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof FieldAcc) {
			FieldAcc fieldAcc = (FieldAcc) sketch;
			if (fieldAcc._expression.isKeyPoint()) {
				_expression.bindingSketch(fieldAcc._expression);
			}
			if (fieldAcc._identifier.isKeyPoint()) {
				_identifier.bindingSketch(fieldAcc._identifier);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
		_identifier.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
		_identifier.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_identifier.getFeatureVector());
	}
	
}
