/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.config.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class SwCase extends Stmt {

	private Expr _expression = null;
	
	/**
	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwCase(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}
	
	public SwCase(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.SWCASE;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression(){
		return _expression;
	}
	
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression == null) {
			stringBuffer.append("default :\n");
		} else {
			stringBuffer.append("case ");
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(" :\n");
		}

		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		if(_binding != null && _binding instanceof SwCase) {
			SwCase swCase = (SwCase) _binding;
			for(Modification modification : swCase.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == swCase._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						LevelLogger.error("SwCase ERROR");
					}
				} else {
					LevelLogger.error("@SwCase Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		if(expression == null) {
			if (_expression == null) {
				stringBuffer.append("default :\n");
			} else {
				stringBuffer.append("case ");
				StringBuffer tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(" :\n");
			}
		} else {
			stringBuffer.append("case ");
			stringBuffer.append(expression);
			stringBuffer.append(" :\n");
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression == null) {
			stringBuffer.append("default :\n");
		} else {
			stringBuffer.append("case ");
			StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" :\n");
		}

		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if (_expression == null) {
				stringBuffer.append("default :\n");
			} else {
				stringBuffer.append("case ");
				stringBuffer.append(_expression.printMatchSketch());
				stringBuffer.append(" :\n");
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_expression == null) {
			_tokens.add("default");
		} else {
			_tokens.add("case");
			_tokens.addAll(_expression.tokens());
		}
		_tokens.add(":");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SwCase) {
			match = true;
			SwCase swCase = (SwCase) other;
			if(_expression != null) {
				return _expression.compare(swCase._expression);
			} else {
				return swCase._expression == null;
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			if(_expression != null) {
				_keywords = _expression.getKeywords();
			} else {
				_keywords = new HashMap<>(0);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_expression != null) {
				modifications.addAll(_expression.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof SwCase) {
			_matchNodeType = true;
			SwCase swCase = (SwCase) other;
			if(_expression == null && swCase._expression != null) {
				_matchNodeType = false;
				return;
			}
			if(_expression != null && swCase._expression != null) {
				_expression.deepMatch(swCase._expression);
				if(!_expression.isNodeTypeMatch()) {
					Update update = new Update(this, _expression, swCase._expression);
					_modifications.add(update);
				}
			} else if(_expression != null) {
				Update update = new Update(this, _expression, swCase._expression);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof SwCase) {
			match = true;
			SwCase swCase = (SwCase) sketch;
			if(!swCase.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(swCase._expression != null && swCase._expression.isKeyPoint()) {
					if(_expression == null) {
						return false;
					} else {
						match = _expression.matchSketch(swCase._expression);
					}
				}
			}
			if(match) {
				swCase._binding = this;
				_binding = swCase;
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
		if (sketch instanceof SwCase) {
			match = true;
			SwCase swCase = (SwCase) sketch;
			if (swCase._expression != null && swCase._expression.isKeyPoint()) {
				if (_expression != null) {
					_expression.bindingSketch(swCase._expression);
				}
			}
		}
		return match;
	}

	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof SwCase) {
			SwCase swCase = (SwCase) patternNode;
			if(_expression != null && swCase._expression != null) {
				Map<String, Set<Node>> map = patternNode.getKeywords();
				Map<String, Set<Node>> thisKeys = getKeywords();
				for(Entry<String, Set<Node>> entry : map.entrySet()) {
					if(!thisKeys.containsKey(entry.getKey())) {
						return null;
					}
				}
				return this;
			} else if(_expression != null || swCase._expression != null){
				return null;
			} else {
				return this;
			}
		} else {
			return null;
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_expression != null) {
			_expression.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_expression != null) {
			_expression.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_STRUCT_SWC);
		if(_expression != null) {
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

}
