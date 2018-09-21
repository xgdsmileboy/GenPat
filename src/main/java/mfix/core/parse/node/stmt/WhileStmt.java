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
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class WhileStmt extends Stmt {

	private Expr _expression = null;
	private Stmt _body = null;
	
	/**
	 * WhileStatement:
     *	while ( Expression ) Statement
	 */
	public WhileStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}

	public WhileStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.WHILE;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	protected Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt body){
		_body = body;
	}
	
	protected Stmt getBody() {
		return _body;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_body.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer body = null;
		if(_binding != null) {
			if(_binding instanceof WhileStmt) {
				WhileStmt whileStmt = (WhileStmt) _binding;
				for(Modification modification : whileStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == whileStmt._expression) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else {
							body = update.getTarString(exprMap, allUsableVars);
							if(body == null) return null;
						}
					} else {
						LevelLogger.error("WhileStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof DoStmt) {
				DoStmt doStmt = (DoStmt) _binding;
				for(Modification modification : doStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == doStmt.getExpression()) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else {
							body = update.getTarString(exprMap, allUsableVars);
							if(body == null) return null;
						}
					} else {
						LevelLogger.error("WhileStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof ForStmt) {
				ForStmt forStmt = (ForStmt) _binding;
				for(Modification modification : forStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						Node node = update.getSrcNode();
						if(node == forStmt.getCondition()) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else if(node == forStmt.getBody()) {
							body = update.getTarString(exprMap, allUsableVars);
							if(body == null) return null;
						} else {
							LevelLogger.error("@WhileStmt ERROR");
						}
					} else {
						LevelLogger.error("WhileStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof EnhancedForStmt) {
				EnhancedForStmt enhancedForStmt = (EnhancedForStmt) _binding;
				for(Modification modification : enhancedForStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == enhancedForStmt.getBody()) {
							body = update.getTarString(exprMap, allUsableVars);
							if(body == null) return null;
						} else {
							LevelLogger.error("@WhileStmt ERROR");
						}
					} else {
						LevelLogger.error("WhileStmt Should not be this kind of modification : " + modification);
					}
				}
			}
		}
		
		StringBuffer stringBuffer = new StringBuffer("while(");
		StringBuffer tmp = null;
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(")");
		if(body == null) {
			tmp = _body.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(body);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("while(");
		StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		tmp = _body.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("while(");
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(")");
			stringBuffer.append(_body.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_body.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_body);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_body);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) other;
			match = _expression.compare(whileStmt._expression) && _body.compare(whileStmt._body);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_expression.getKeywords());
			avoidDuplicate(_keywords, _body);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_expression.extractModifications());
			modifications.addAll(_body.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof WhileStmt) {
			_matchNodeType = true;
			WhileStmt whileStmt = (WhileStmt) other;
			_expression.deepMatch(whileStmt._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, whileStmt._expression);
				_modifications.add(update);
			}
			_body.deepMatch(whileStmt._body);
			if(!_body.isNodeTypeMatch()) {
				Update update = new Update(this, _body, whileStmt._body);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof WhileStmt) {
			match = true;
			WhileStmt whileStmt = (WhileStmt) sketch;
			if(!whileStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(whileStmt._expression.isKeyPoint()) {
					match = _expression.matchSketch(whileStmt);
				}
				if(match && whileStmt._body.isKeyPoint()) {
					match = _body.matchSketch(whileStmt._body);
				}
			}
			if(match) {
				whileStmt._binding = this;
				_binding = whileStmt;
			}
		} else if(sketch instanceof DoStmt) {
			match = true;
			DoStmt doStmt = (DoStmt) sketch;
			if(!doStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(doStmt.getExpression().isKeyPoint()) {
					match = _expression.matchSketch(doStmt.getExpression());
				}
				if(match && doStmt.getBody().isKeyPoint()) {
					match = _body.matchSketch(doStmt.getBody());
				}
			}
			if(match) {
				doStmt.setBinding(this);
				_binding = doStmt;
			}
		} else if(sketch instanceof ForStmt) {
			match = true;
			ForStmt forStmt = (ForStmt) sketch;
			if(!forStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(forStmt.getInitializer() != null && forStmt.getInitializer().isKeyPoint()) {
					match = false;
				} else if(forStmt.getUpdaters() != null && forStmt.getUpdaters().isKeyPoint()) {
					match = false;
				} else if(forStmt.getCondition() != null && forStmt.getCondition().isKeyPoint()) {
					match = _expression.matchSketch(forStmt.getCondition());
				}
				if(match && forStmt.getBody().isKeyPoint()) {
					match = _body.matchSketch(forStmt.getBody());
				}
			}
			if(match) {
				forStmt.setBinding(this);
				_binding = forStmt;
			}
		} else if(sketch instanceof EnhancedForStmt) {
			match = true;
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) sketch;
			if(!enhancedForStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(enhancedForStmt.getParameter().isKeyPoint()) {
					match = false;
				} else if(enhancedForStmt.getExpression().isKeyPoint()) {
					match = false;
				} else if(enhancedForStmt.getBody().isKeyPoint()) {
					match = _body.matchSketch(enhancedForStmt.getBody());
				}
			}
			if(match) {
				enhancedForStmt.setBinding(this);
				_binding = enhancedForStmt;
			}
		}
		if(!match) {
			_body.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof WhileStmt) {
			match = true;
			WhileStmt whileStmt = (WhileStmt) sketch;
			if (whileStmt._expression.isKeyPoint()) {
				_expression.bindingSketch(whileStmt);
			}
			if (whileStmt._body.isKeyPoint()) {
				_body.bindingSketch(whileStmt._body);
			}
		}
		return match;
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		boolean match = false;
		if(patternNode instanceof WhileStmt) {
			match = true;
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())){
					match = false;
					break;
				}
			}
		}
		if(match) {
			return this;
		} else {
			return _body.bindingNode(patternNode);
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
		_body.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
		_body.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_STRUCT_WHILE);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_body.getFeatureVector());
	}
}

