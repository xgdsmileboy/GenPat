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
import org.eclipse.jdt.core.dom.ASTNode;
import mfix.core.parse.node.expr.Expr;

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
public class DoStmt extends Stmt {

	private Stmt _stmt = null;
	private Expr _expression = null;
	
	/**
	 * DoStatement:
     *	do Statement while ( Expression ) ;
	 */
	public DoStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
		_nodeType = TYPE.DO;
	}

	public DoStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}
	
	public void setBody(Stmt stmt){
		_stmt = stmt;
	}
	
	protected Stmt getBody() {
		return _stmt;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	protected Expr getExpression() {
		return _expression;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("do ");
		stringBuffer.append(_stmt.toSrcString());
		stringBuffer.append(" while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stmt = null;
		StringBuffer expression = null;
		if(_binding != null) {
			if(_binding instanceof DoStmt) {
				DoStmt doStmt = (DoStmt) _binding;
				for(Modification modification : doStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == doStmt._expression){
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else {
							stmt = update.getTarString(exprMap, allUsableVars);
							if(stmt == null) return null;
						}
					} else {
						LevelLogger.error("@DoStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof mfix.core.parse.node.stmt.WhileStmt) {
				mfix.core.parse.node.stmt.WhileStmt whileStmt = (mfix.core.parse.node.stmt.WhileStmt) _binding;
				for(Modification modification : whileStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == whileStmt.getExpression()) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else {
							stmt = update.getTarString(exprMap, allUsableVars);
							if(stmt == null) return null;
						}
					} else {
						LevelLogger.error("@DoStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof mfix.core.parse.node.stmt.ForStmt) {
				mfix.core.parse.node.stmt.ForStmt forStmt = (mfix.core.parse.node.stmt.ForStmt) _binding;
				for(Modification modification : forStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == forStmt.getCondition()) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else if(update.getSrcNode() == forStmt.getBody()) {
							stmt = update.getTarString(exprMap, allUsableVars);
							if(stmt == null) return null;
						} else {
							LevelLogger.error("@DoStmt Should not be match DoStmt and ForStmt.");
						}
					} else {
						LevelLogger.error("@DoStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof mfix.core.parse.node.stmt.EnhancedForStmt) {
				mfix.core.parse.node.stmt.EnhancedForStmt enhancedForStmt = (mfix.core.parse.node.stmt.EnhancedForStmt) _binding;
				for(Modification modification : enhancedForStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == enhancedForStmt.getBody()) {
							stmt = update.getTarString(exprMap, allUsableVars);
							if(stmt == null) return null;
						} else {
							LevelLogger.error("@DoStmt Should not be match DoStmt and EnhancedForStmt.");
						}
					} else {
						LevelLogger.error("@DoStmt Should not be this kind of modification : " + modification);
					}
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("do ");
		if(stmt == null) {
			tmp = _stmt.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(stmt);
		}
		stringBuffer.append(" while(");
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("do ");
		tmp = _stmt.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" while(");
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(");");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("do ");
			stringBuffer.append(_stmt.printMatchSketch());
			stringBuffer.append(" while(");
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(");");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("do");
		_tokens.addAll(_stmt.tokens());
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_stmt);
		children.add(_expression);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		if(_stmt != null) {
			children.add(_stmt);
		}
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof DoStmt) {
			DoStmt doStmt = (DoStmt) other;
			match = _expression.compare(doStmt._expression) && _stmt.compare(doStmt._stmt);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_stmt.getCalledMethods());
			avoidDuplicate(_keywords, _expression);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_stmt.extractModifications());
			modifications.addAll(_expression.extractModifications());
		}
		return modifications;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof DoStmt) {
			_matchNodeType = true;
			DoStmt doStmt = (DoStmt) other;
			_expression.deepMatch(doStmt._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, doStmt._expression);
				_modifications.add(update);
			}
			_stmt.deepMatch(doStmt._stmt);
			if(!_stmt.isNodeTypeMatch()) {
				Update update = new Update(this, _stmt, doStmt._stmt);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof DoStmt) {
			match = true;
			DoStmt doStmt = (DoStmt) sketch;
			if(!doStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(doStmt._stmt.isKeyPoint()) {
					match = _stmt.matchSketch(doStmt._stmt);
				}
				if(match && doStmt._expression.isKeyPoint()) {
					match = _expression.matchSketch(doStmt._expression);
				}
			}
			if(match) {
				doStmt._binding = this;
				_binding = doStmt;
			}
		} else if(sketch instanceof mfix.core.parse.node.stmt.WhileStmt) {
			match = true;
			mfix.core.parse.node.stmt.WhileStmt whileStmt = (mfix.core.parse.node.stmt.WhileStmt) sketch;
			if(!whileStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(whileStmt.getExpression().isKeyPoint()) {
					match = _expression.matchSketch(whileStmt.getExpression());
				}
				if(match && whileStmt.getBody().isKeyPoint()) {
					match = _stmt.matchSketch(whileStmt.getBody());
				}
			}
			if(match) {
				whileStmt.setBinding(this);
				_binding = whileStmt;
			}
		} else if(sketch instanceof mfix.core.parse.node.stmt.ForStmt) {
			match = true;
			mfix.core.parse.node.stmt.ForStmt forStmt = (mfix.core.parse.node.stmt.ForStmt) sketch;
			if(!forStmt.isNodeTypeMatch()){
				match = false;
			} else {
				if(forStmt.getInitializer().isKeyPoint()) {
					match = false;
				}
				if(match && forStmt.getUpdaters().isKeyPoint()) {
					match = false;
				}
				if(match && forStmt.getCondition() != null && forStmt.getCondition().isKeyPoint()) {
					match = _expression.matchSketch(forStmt.getCondition());
				}
				if(match && forStmt.getBody().isKeyPoint()) {
					match = _stmt.matchSketch(forStmt.getBody());
				}
 			}
			if(match) {
				forStmt.setBinding(this);
				_binding = forStmt;
			}
		} else if(sketch instanceof mfix.core.parse.node.stmt.EnhancedForStmt){
			match = true;
			mfix.core.parse.node.stmt.EnhancedForStmt enhancedForStmt = (mfix.core.parse.node.stmt.EnhancedForStmt) sketch;
			if(!enhancedForStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(enhancedForStmt.getParameter().isKeyPoint()) {
					match = false;
				}
				if(match && enhancedForStmt.getExpression().isKeyPoint()) {
					match = false;
				}
				if(match && enhancedForStmt.getBody().isKeyPoint()) {
					match = _stmt.matchSketch(enhancedForStmt.getBody());
				}
			}
			if(match) {
				enhancedForStmt.setBinding(this);
				_binding = enhancedForStmt;
			}
		}
		if(!match) {
			match = _stmt.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof DoStmt) {
			match = true;
			DoStmt doStmt = (DoStmt) sketch;
			if (doStmt._stmt.isKeyPoint()) {
				_stmt.bindingSketch(doStmt._stmt);
			}
			if (doStmt._expression.isKeyPoint()) {
				_expression.bindingSketch(doStmt._expression);
			}
		}
		return match;
	}

	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof DoStmt) {
			Map<String, Set<Node>> keywords = patternNode.getCalledMethods();
			Map<String, Set<Node>> thisKeys = getCalledMethods();
			boolean containAllKeys = true;
			for(Entry<String, Set<Node>> entry : keywords.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					containAllKeys = false;
					break;
				}
			}
			if(containAllKeys) {
				return this;
			} else {
				return _stmt.bindingNode(patternNode);
			}
		} else {
			return _stmt.bindingNode(patternNode);
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_stmt.resetAllNodeTypeMatch();
		_expression.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_stmt.setAllNodeTypeMatch();
		_expression.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_DO);
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_stmt.getFeatureVector());
	}

}
