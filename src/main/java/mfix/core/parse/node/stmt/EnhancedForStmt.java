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
import mfix.core.parse.node.expr.Svd;
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
public class EnhancedForStmt extends Stmt {

	private Svd _varDecl = null;
	private Expr _expression = null;
	private Stmt _statement = null;
	
	
	/**
	 * EnhancedForStatement:
     *	for ( FormalParameter : Expression )
     *	                   Statement
	 */
	public EnhancedForStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}
	
	public EnhancedForStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.EFOR;
	}
	
	public void setParameter(Svd varDecl){
		_varDecl = varDecl;
	}
	
	protected Svd getParameter() {
		return _varDecl;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	protected Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt statement){
		_statement = statement;
	}
	
	protected Stmt getBody() {
		return _statement;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("for(");
		stringBuffer.append(_varDecl.toSrcString());
		stringBuffer.append(" : ");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_statement.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer varDecl = null;
		StringBuffer expression = null;
		StringBuffer statement = null;
		if(_binding != null) {
			if(_binding instanceof EnhancedForStmt) {
				EnhancedForStmt enhancedForStmt = (EnhancedForStmt) _binding;
				for(Modification modification : enhancedForStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						Node node = update.getSrcNode();
						if(node == enhancedForStmt._varDecl) {
							varDecl = update.getTarString(exprMap, allUsableVars);
							if(varDecl == null) return null;
						} else if(node == enhancedForStmt._expression) {
							expression = update.getTarString(exprMap, allUsableVars);
							if(expression == null) return null;
						} else {
							statement = update.getTarString(exprMap, allUsableVars);
							if(statement == null) return null;
						}
					} else {
						LevelLogger.error("@EnhancedForStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof mfix.core.parse.node.stmt.ForStmt) {
				mfix.core.parse.node.stmt.ForStmt forStmt = (mfix.core.parse.node.stmt.ForStmt) _binding;
				for(Modification modification : forStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == forStmt.getBody()) {
							statement = update.getTarString(exprMap, allUsableVars);
							if(statement == null) return null;
						} else {
							LevelLogger.error("@EnhancedForStmt should not match EnhancedForStmt and ForStmt.");
						}
					} else {
						LevelLogger.error("@EnhancedForStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof DoStmt) {
				DoStmt doStmt = (DoStmt) _binding;
				for(Modification modification : doStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == doStmt.getBody()) {
							statement = update.getTarString(exprMap, allUsableVars);
							if(statement == null) return null;
						} else {
							LevelLogger.error("@EnhancedForStmt should not match EnhancedForStmt and DoStmt.");
						}
					} else {
						LevelLogger.error("@EnhancedForStmt Should not be this kind of modification : " + modification);
					}
				}
			} else if(_binding instanceof mfix.core.parse.node.stmt.WhileStmt) {
				mfix.core.parse.node.stmt.WhileStmt whileStmt = (mfix.core.parse.node.stmt.WhileStmt) _binding;
				for(Modification modification : whileStmt.getNodeModification()) {
					if(modification instanceof Update) {
						Update update = (Update) modification;
						if(update.getSrcNode() == whileStmt.getBody()) {
							statement = update.getTarString(exprMap, allUsableVars);
							if(statement == null) return null;
						} else {
							LevelLogger.error("@EnhancedForStmt should not match EnhancedForStmt and WhileStmt.");
						}
					} else {
						LevelLogger.error("@EnhancedForStmt Should not be this kind of modification : " + modification);
					}
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("for(");
		if(varDecl == null) {
			tmp = _varDecl.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(varDecl);
		}
		stringBuffer.append(" : ");
		if(expression == null) {
			tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(")");
		if(statement == null) {
			tmp = _statement.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(statement);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("for(");
		tmp = _varDecl.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" : ");
		tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		tmp = _statement.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("for(");
			stringBuffer.append(_varDecl.printMatchSketch());
			stringBuffer.append(" : ");
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append(")");
			stringBuffer.append(_statement.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("for");
		_tokens.add("(");
		_tokens.addAll(_varDecl.tokens());
		_tokens.add(":");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_statement.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_varDecl);
		children.add(_expression);
		children.add(_statement);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_statement);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof EnhancedForStmt) {
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) other;
			match = _varDecl.compare(enhancedForStmt._varDecl) && _expression.compare(enhancedForStmt._expression) && _statement.compare(enhancedForStmt._statement);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_statement.getKeywords());
			avoidDuplicate(_keywords, _varDecl);
			avoidDuplicate(_keywords, _expression);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_varDecl.extractModifications());
			modifications.addAll(_expression.extractModifications());
			modifications.addAll(_statement.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof EnhancedForStmt) {
			_matchNodeType = true;
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) other;
			_varDecl.deepMatch(enhancedForStmt._varDecl);
			if(!_varDecl.isNodeTypeMatch()) {
				Update update = new Update(this, _varDecl, enhancedForStmt._varDecl);
				_modifications.add(update);
			}
			_expression.deepMatch(enhancedForStmt._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, enhancedForStmt._expression);
				_modifications.add(update);
			}
			_statement.deepMatch(enhancedForStmt._statement);
			if(!_statement.isNodeTypeMatch()) {
				Update update = new Update(this, _statement, enhancedForStmt._statement);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof EnhancedForStmt) {
			match = true;
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) sketch;
			if(!enhancedForStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(enhancedForStmt._varDecl.isKeyPoint()) {
					match = _varDecl.matchSketch(enhancedForStmt._varDecl);
				}
				if(match && enhancedForStmt._expression.isKeyPoint()) {
					match = _expression.matchSketch(enhancedForStmt._expression);
				}
				if(match && enhancedForStmt._statement.isKeyPoint()) {
					match = _statement.matchSketch(enhancedForStmt._statement);
				}
			}
			if(match) {
				enhancedForStmt._binding = this;
				_binding = enhancedForStmt;
			}
		} else if(sketch instanceof mfix.core.parse.node.stmt.ForStmt) {
			match = true;
			mfix.core.parse.node.stmt.ForStmt forStmt = (mfix.core.parse.node.stmt.ForStmt) sketch;
			if(!forStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(forStmt.getInitializer().isKeyPoint()) {
					match = false;
				} else if(forStmt.getUpdaters().isKeyPoint()) {
					match = false;
				} else if(forStmt.getCondition() != null && forStmt.getCondition().isKeyPoint()) {
					match = false;
				} else if(forStmt.getBody().isKeyPoint()) {
					match = _statement.matchSketch(forStmt.getBody());
				}
			}
			if(match) {
				forStmt.setBinding(this);
				_binding = forStmt;
			}
		} else if(sketch instanceof DoStmt) {
			match = true;
			DoStmt doStmt = (DoStmt) sketch;
			if(!doStmt.isNodeTypeMatch()) {
				match = false;
			} else {
				if(doStmt.getExpression().isKeyPoint()) {
					match = false;
				} else if(doStmt.getBody().isKeyPoint()){
					match = _statement.matchSketch(doStmt.getBody());
				}
			}
			if(match) {
				doStmt.setBinding(this);
				_binding = doStmt;
			}
		} else if(sketch instanceof mfix.core.parse.node.stmt.WhileStmt) {
			match = true;
			mfix.core.parse.node.stmt.WhileStmt whileStmt = (mfix.core.parse.node.stmt.WhileStmt) sketch;
			if(whileStmt.getExpression().isKeyPoint()) {
				match = false;
			} else if(whileStmt.getBody().isKeyPoint()) {
				match = _statement.matchSketch(whileStmt.getBody());
			}
			if(match) {
				whileStmt.setBinding(this);
				_binding = whileStmt;
			}
		}
		if(!match) {
			match = _statement.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}

	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof EnhancedForStmt) {
			match = true;
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) sketch;
			if (enhancedForStmt._varDecl.isKeyPoint()) {
				_varDecl.bindingSketch(enhancedForStmt._varDecl);
			}
			if (enhancedForStmt._expression.isKeyPoint()) {
				_expression.bindingSketch(enhancedForStmt._expression);
			}
			if (enhancedForStmt._statement.isKeyPoint()) {
				_statement.bindingSketch(enhancedForStmt._statement);
			}
		}
		return match;
	}

	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof EnhancedForStmt) {
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			boolean containsAllKeys = true;
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					containsAllKeys = false;
					break;
				}
			}
			if(containsAllKeys) {
				return this;
			} else {
				return _statement.bindingNode(patternNode);
			}
		} else {
			return _statement.bindingNode(patternNode);
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_varDecl.resetAllNodeTypeMatch();
		_expression.resetAllNodeTypeMatch();
		_statement.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_varDecl.setAllNodeTypeMatch();
		_expression.setAllNodeTypeMatch();
		_statement.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_STRUCT_ENFOR);
		_fVector.combineFeature(_varDecl.getFeatureVector());
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_statement.getFeatureVector());
	}

}
