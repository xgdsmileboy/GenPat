/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
public class SwitchStmt extends Stmt {

	private Expr _expression = null;
	private List<Stmt> _statements = null;
	
	/**
	 * SwitchStatement:
     *           switch ( Expression )
     *                   { { SwitchCase | Statement } }
 	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwitchStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}
	
	public SwitchStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.SWSTMT;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setStatements(List<Stmt> statements){
		_statements = statements;
	}
	
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("swtich (");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append("){" + Constant.NEW_LINE);
		for (Stmt stmt : _statements) {
			stringBuffer.append(stmt.toSrcString());
			stringBuffer.append(Constant.NEW_LINE);
		}
		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		if(_binding != null && _binding instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) _binding;
			StringBuffer expression = null;
			List<Modification> modifications = new LinkedList<>();
			for(Modification modification : switchStmt.getNodeModification()) {
				if(modification instanceof Update){
					Update update = (Update) modification;
					if(update.getSrcNode() == switchStmt._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else {
						modifications.add(update);
					}
				} else {
					modifications.add(modification);
				}
			}
			
			Map<Node, List<StringBuffer>> insertionPositionMap = new HashMap<>();
			Map<Node, StringBuffer> map = new HashMap<>(_statements.size());
			if (!Matcher.applyStmtList(modifications, _statements, this, exprMap, insertionPositionMap, map,
					allUsableVars)) {
				return null;
			}
			StringBuffer stringBuffer = new StringBuffer("swtich (");
			StringBuffer tmp = null;
			if(expression == null) {
				tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(expression);
			}
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Node node : _statements) {
				List<StringBuffer> list = insertionPositionMap.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						stringBuffer.append(list.get(i));
						stringBuffer.append(Constant.NEW_LINE);
					}
				}
				if (map.containsKey(node)) {
					StringBuffer update = map.get(node);
					if (update != null) {
						stringBuffer.append(update);
						stringBuffer.append(Constant.NEW_LINE);
					}
				} else {
					tmp = node.applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
					stringBuffer.append(Constant.NEW_LINE);
				}
			}
			List<StringBuffer> list = insertionPositionMap.get(this);
			if(list != null) {
				for(int i = 0; i < list.size(); i ++) {
					stringBuffer.append(list.get(i));
				}
			}
			stringBuffer.append("}");
			return stringBuffer;
			
		} else {
			StringBuffer stringBuffer = new StringBuffer("swtich (");
			StringBuffer tmp = _expression.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				tmp = stmt.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(Constant.NEW_LINE);
			}

			stringBuffer.append("}");
			return stringBuffer;
		}
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("swtich (");
		StringBuffer tmp = _expression.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append("){" + Constant.NEW_LINE);
		for (Stmt stmt : _statements) {
			tmp = stmt.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(Constant.NEW_LINE);
		}

		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("swtich (");
			stringBuffer.append(_expression.printMatchSketch());
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				stringBuffer.append(stmt.printMatchSketch());
				stringBuffer.append(Constant.NEW_LINE);
			}
			stringBuffer.append("}");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("switch");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.add("{");
		for(Stmt stmt : _statements) {
			_tokens.addAll(stmt.tokens());
		}
		_tokens.add("}");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_statements.size() + 1);
		children.add(_expression);
		children.addAll(_statements);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return _statements;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) other;
			match = _expression.compare(switchStmt._expression)
					&& (_statements.size() == switchStmt._statements.size());
			for (int i = 0; match && i < _statements.size(); i++) {
				match = match && _statements.get(i).compare(switchStmt._statements.get(i));
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_expression.getKeywords());
			for(Stmt stmt : _statements) {
				avoidDuplicate(_keywords, stmt);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_expression.extractModifications());
			for(Stmt stmt : _statements) {
				modifications.addAll(stmt.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof SwitchStmt) {
			_matchNodeType = true;
			SwitchStmt switchStmt = (SwitchStmt) other;
			_expression.deepMatch(switchStmt._expression);
			if(!_expression.isNodeTypeMatch()) {
				Update update = new Update(this, _expression, switchStmt._expression);
				_modifications.add(update);
			}
			_modifications.addAll(Matcher.matchNodeList(this, _statements, switchStmt._statements));
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof SwitchStmt) {
			match = true;
			SwitchStmt switchStmt = (SwitchStmt) sketch;
			if(!switchStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(switchStmt._expression.isKeyPoint()) {
					match = _expression.matchSketch(switchStmt._expression);
				}
				if(match) {
					Set<Integer> alreadyMatch = new HashSet<>();
					for(Stmt stmt : switchStmt._statements) {
						if(stmt.isKeyPoint()) {
							boolean singleMatch = false;
							for(int i = 0; i < _statements.size(); i++) {
								if(alreadyMatch.contains(i)){
									continue;
								}
								if(_statements.get(i).matchSketch(stmt)) {
									singleMatch = true;
									alreadyMatch.add(i);
									break;
								}
							}
							if(!singleMatch) {
								match = false;
								break;
							}
						}
					}
				}
			}
			if(match) {
				switchStmt._binding = this;
				_binding = switchStmt;
			}
		}
		if(!match) {
			for(Stmt stmt : _statements) {
				match = stmt.matchSketch(sketch);
				if(match) break;
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
		if (sketch instanceof SwitchStmt) {
			match = true;
			SwitchStmt switchStmt = (SwitchStmt) sketch;
			if (switchStmt._expression.isKeyPoint()) {
				_expression.bindingSketch(switchStmt._expression);
			}
			Set<Integer> alreadyMatch = new HashSet<>();
			for (Stmt stmt : switchStmt._statements) {
				if (stmt.isKeyPoint()) {
					for (int i = 0; i < _statements.size(); i++) {
						if (alreadyMatch.contains(i)) {
							continue;
						}
						if (_statements.get(i).bindingSketch(stmt)) {
							alreadyMatch.add(i);
							break;
						} else {
							stmt.resetBinding();
						}
					}
				}
			}
		}
		return match;
	}

	@Override
	public Node bindingNode(Node patternNode) {
		boolean match = false;
		if(patternNode instanceof SwitchStmt) {
			match = true;
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					match = false;
					break;
				}
			}
		}
		if(match) {
			return this;
		} else {
			Node node = null;
			for(Stmt stmt : _statements) {
				node = stmt.bindingNode(patternNode);
				if(node != null) {
					return node;
				}
			}
			return null;
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_expression.resetAllNodeTypeMatch();
		for(Stmt stmt : _statements) {
			stmt.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_expression.setAllNodeTypeMatch();
		for(Stmt stmt : _statements) {
			stmt.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_expression.getFeatureVector());
		for(Stmt stmt : _statements) {
			_fVector.combineFeature(stmt.getFeatureVector());
		}
	}

}
