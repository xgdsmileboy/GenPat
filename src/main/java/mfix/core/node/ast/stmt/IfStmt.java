/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
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
public class IfStmt extends Stmt {

	private static final long serialVersionUID = -7247565482784755352L;
	private Expr _condition = null;
	private Stmt _then = null;
	private Stmt _else = null;
	
	/**
	 * IfStatement:
     *	if ( Expression ) Statement [ else Statement]
	 */
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.IF;
		_fIndex = VIndex.STMT_IF;
	}
	
	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setThen(Stmt then){
		_then = then;
	}
	
	public void setElse(Stmt els){
		_else = els;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Stmt getThen(){
		return _then;
	}
	
	public Stmt getElse(){
		return _else;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("if(");
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_then.toSrcString());
		if(_else != null) {
			stringBuffer.append("else ");
			stringBuffer.append(_else.toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer cond = _condition.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer then = _then.formalForm(nameMapping, false, keywords);
		StringBuffer els = _else == null ? null : _else.formalForm(nameMapping, false, keywords);
		if (isConsidered() || cond != null || then != null || els != null) {
			StringBuffer buffer = new StringBuffer("if(");
			buffer.append(cond == null ? nameMapping.getExprID(_condition) : cond).append(')');
			buffer.append(then == null ? "{}" : then);
			if (_else != null) {
				buffer.append("\nelse").append(els == null ? "{}" : els);
			}
			return buffer;
		}
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("if");
		_tokens.add("(");
		_tokens.addAll(_condition.tokens());
		_tokens.add(")");
		_tokens.addAll(_then.tokens());
		if(_else != null) {
			_tokens.add("else");
			_tokens.addAll(_else.tokens());
		}
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_then);
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(2);
		if(_then != null) {
			children.add(_then);
		}
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) other;
			match = _condition.compare(ifStmt._condition) && _then.compare(ifStmt._then);
			if(_else == null) {
				match = match && (ifStmt._else == null);
			} else {
				match = match && _else.compare(ifStmt._else);
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_IF);
		_fVector.combineFeature(_then.getFeatureVector());
		if(_else != null) {
			_fVector.inc(FVector.KEY_ELSE);
			_fVector.combineFeature(_else.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		IfStmt ifStmt = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			ifStmt = (IfStmt) getBindingNode();
			_condition.postAccurateMatch(ifStmt.getCondition());
			_then.postAccurateMatch(ifStmt.getThen());
			match = (ifStmt == node);
		} else if(canBinding(node)) {
			ifStmt = (IfStmt) node;
			match = _condition.postAccurateMatch(ifStmt.getCondition());
			match = _then.postAccurateMatch(ifStmt.getThen()) || match;
			if(match) {
				setBindingNode(node);
			}else {
				ifStmt = null;
			}
		}
		if(ifStmt == null) {
			continueTopDownMatchNull();
		} else {
			_then.postAccurateMatch(ifStmt.getThen());
			if(_else != null) {
				_else.postAccurateMatch(ifStmt.getElse());
			}
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			IfStmt ifStmt = (IfStmt) getBindingNode();
			if(_condition.getBindingNode() != ifStmt.getCondition()) {
				Update update = new Update(this, _condition, ifStmt.getCondition());
				_modifications.add(update);
			} else {
				_condition.genModifications();
			}
			if(_then.getBindingNode() != ifStmt.getThen()) {
				Update update = new Update(this, _then, ifStmt.getThen());
				_modifications.add(update);
			} else {
				_then.genModifications();
			}
			if(_else == null) {
				if (ifStmt.getElse() != null) {
					Update update = new Update(this, _else, ifStmt.getElse());
					_modifications.add(update);
				}
			} else if (_else.getBindingNode() != ifStmt.getElse()){
				Update update = new Update(this, _else, ifStmt.getElse());
				_modifications.add(update);
			} else {
				_else.genModifications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) node;
			boolean match = _condition.ifMatch(ifStmt.getCondition(), matchedNode, matchedStrings);
			match = match && _then.ifMatch(ifStmt.getThen(), matchedNode, matchedStrings);
			if(_else != null && ifStmt.getElse() != null) {
				match = match && _else.ifMatch(ifStmt.getElse(), matchedNode, matchedStrings);
			}
			return match && super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("if(");
			StringBuffer tmp;
			tmp = _condition.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
			tmp = _then.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			if(_else != null) {
				stringBuffer.append("else ");
				tmp = _else.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
		StringBuffer condition = null;
		StringBuffer then = null;
		StringBuffer els = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			IfStmt ifStmt = (IfStmt) pnode;
			for(Modification modification : ifStmt.getModifications()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == ifStmt._condition) {
						condition = update.apply(vars, exprMap);
						if(condition == null) return null;
					} else if(node == ifStmt._then) {
						then = update.apply(vars, exprMap);
						if(then == null) return null;
					} else {
						els = update.apply(vars, exprMap);
						if(els == null) return null;
					}
				} else {
					LevelLogger.error("@IfStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("if(");
		StringBuffer tmp;
		if(condition == null) {
			tmp = _condition.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(condition);
		}
		stringBuffer.append(")");
		if(then == null) {
			tmp = _then.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(then);
		}
		if(els == null) {
			if(_else != null) {
				stringBuffer.append("else ");
				tmp = _else.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			if (!els.toString().isEmpty()) {
				stringBuffer.append("else ");
				stringBuffer.append(els);
			}
		}
		return stringBuffer;
	}
}
