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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PostfixExpr extends Expr {

	private Expr _expression = null;
	private PostOperator _operator = null;
	
	/**
	 * PostfixExpression:
     *	Expression PostfixOperator
	 */
	public PostfixExpr(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.POSTEXPR;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setOperator(PostOperator operator){
		_operator = operator;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(_operator.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer operator = null;
		if(_binding != null && _binding instanceof PostfixExpr) {
			PostfixExpr postfixExpr = (PostfixExpr) _binding;
			for(Modification modification : postfixExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification ;
					if(update.getSrcNode() == postfixExpr._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						operator = update.getTarString(exprMap, allUsableVars);
						if(operator == null) return null;
					}
				} else {
					LevelLogger.error("@PostfixExpr Should not be this kind of modification : " + modification);
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
		if(operator == null) {
			tmp = _operator.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(operator);
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
		StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		tmp = _operator.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(_operator.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.addAll(_operator.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof PostfixExpr) {
			PostfixExpr postfixExpr = (PostfixExpr) other;
			match = _operator.compare(postfixExpr._operator) && _expression.compare(postfixExpr._expression);
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
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_operator);
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
		if(other instanceof PostfixExpr) {
			_matchNodeType = true;
			PostfixExpr postfixExpr = (PostfixExpr) other;
			_expression.deepMatch(postfixExpr._expression); 
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, postfixExpr._expression);
				_modifications.add(update);
			}
			_operator.deepMatch(postfixExpr._operator);
			if(!_operator.isNodeTypeMatch()){
				Update update = new Update(this, _operator, postfixExpr._operator);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof PostfixExpr) {
			match = true;
			PostfixExpr postfixExpr = (PostfixExpr) sketch;
			if(!postfixExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(postfixExpr._operator.isKeyPoint()) {
					match = _operator.matchSketch(postfixExpr._operator);
				}
				if(match && postfixExpr._expression.isKeyPoint()) {
					match = _expression.matchSketch(postfixExpr._expression);
				}
			}
			if(match) {
				postfixExpr._binding = this;
				_binding = postfixExpr;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		if (sketch instanceof PostfixExpr) {
			match = true;
			PostfixExpr postfixExpr = (PostfixExpr) sketch;
			if (postfixExpr._operator.isKeyPoint()) {
				_operator.bindingSketch(postfixExpr._operator);
			}
			if (postfixExpr._expression.isKeyPoint()) {
				_expression.bindingSketch(postfixExpr._expression);
			}
			postfixExpr._binding = this;
			_binding = postfixExpr;
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
		_operator.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
		_operator.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_POSTFIX);
		_fVector.inc(_operator.toSrcString().toString());
		_fVector.combineFeature(_expression.getFeatureVector());
	}

}
