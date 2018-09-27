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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class CastExpr extends Expr implements Serializable {

	private static final long serialVersionUID = -8485318151476589525L;
	private MType _castType = null;
	private Expr _expression = null;

	/**
	 * CastExpression: ( Type ) Expression
	 */
	public CastExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.CAST;
	}

	public void setCastType(MType type) {
		_castType = type;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_castType.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_expression.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer castType = null;
		StringBuffer expression = null;
		if(_binding != null && _binding instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) _binding;
			for(Modification modification : castExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == _castType) {
						castType = update.getTarString(exprMap, allUsableVars);
						if(castType == null) return null;
					} else {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					}
				} else {
					LevelLogger.error("@CastExpr Should not ");
				}
			}
		}
		
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("(");
		if(castType == null) {
			tmp = _castType.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(castType);
		}
		stringBuffer.append(")");
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
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
		stringBuffer.append("(");
		tmp = _castType.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("(");
			stringBuffer.append(_castType.printMatchSketch());
			stringBuffer.append(")");
			stringBuffer.append(_expression.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_castType.tokens());
		_tokens.add(")");
		_tokens.addAll(_expression.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_castType);
		children.add(_expression);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) other;
			match = _castType.compare(castExpr._castType);
			match = match && _expression.compare(castExpr._expression);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = _expression.getCalledMethods();
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_expression.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof CastExpr) {
			_matchNodeType = true;
			CastExpr castExpr = (CastExpr) other;
			_castType.deepMatch(castExpr._castType);
			if(!_castType.isNodeTypeMatch()) {
				Update update = new Update(this, _castType, castExpr._castType);
				_modifications.add(update);
			}
			_expression.deepMatch(castExpr._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, castExpr._expression);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof CastExpr) {
			match = true;
			CastExpr castExpr = (CastExpr) sketch;
			if(!castExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(castExpr._castType.isKeyPoint()){
					match = _castType.matchSketch(castExpr._castType);
				}
				if(castExpr._expression.isKeyPoint()) {
					match = match && _expression.matchSketch(castExpr._expression);
				}
			}
			if(match) {
				castExpr._binding = this;
				_binding = castExpr;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) sketch;
			if (castExpr._castType.isKeyPoint()) {
				_castType.bindingSketch(castExpr._castType);
			}
			if (castExpr._expression.isKeyPoint()) {
				_expression.bindingSketch(castExpr._expression);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_castType.resetAllNodeTypeMatch();
		_expression.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_castType.setAllNodeTypeMatch();
		_expression.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_CAST);
		_fVector.combineFeature(_expression.getFeatureVector());
	}
}
