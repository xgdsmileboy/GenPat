/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ParenthesiszedExpr extends Expr {

	private Expr _expression = null;
	
	/**
	 * ParenthesizedExpression:
     *	( Expression )
	 */
	public ParenthesiszedExpr(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.PARENTHESISZED;
	}

	public void setExpr(Expr expression){
		_expression = expression;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		StringBuffer tmp = _expression.applyChange(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
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
		stringBuffer.append("(");
		StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("(");
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(")");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
			match = _expression.compare(parenthesiszedExpr._expression);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = _expression.getKeywords();
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
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
		if(other instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
			_expression.deepMatch(parenthesiszedExpr._expression);
			_matchNodeType = _expression.isNodeTypeMatch();
//			_matchNodeType = true;
//			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
//			_expression.deepMatch(parenthesiszedExpr._expression);
//			if(!_expression.isNodeTypeMatch()) {
//				Update update = new Update(this, _expression, parenthesiszedExpr._expression);
//				_modifications.add(update);
//			}
		} else {
			_expression.deepMatch(other);
			_matchNodeType = _expression.isNodeTypeMatch();
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ParenthesiszedExpr) {
			match = true;
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) sketch;
			if(!parenthesiszedExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				match = _expression.matchSketch(parenthesiszedExpr._expression);
			}
			if(match) {
				parenthesiszedExpr._binding = this;
				_binding = parenthesiszedExpr;
			}
		} 
		if(!match) {
			match = _expression.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		if (sketch instanceof ParenthesiszedExpr) {
			match = true;
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) sketch;
			_expression.bindingSketch(parenthesiszedExpr._expression);
			parenthesiszedExpr._binding = this;
			_binding = parenthesiszedExpr;
		} else {
			match = _expression.bindingSketch(sketch);
		}
		if(!match) {
			_binding = sketch;
			sketch.setBinding(this);
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_expression.getFeatureVector());
	}

}
