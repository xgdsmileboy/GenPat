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
public class SwCase extends Stmt {

	private static final long serialVersionUID = 3371970934436172117L;
	private Expr _expression = null;
	
	/**
	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwCase(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public SwCase(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SWCASE;
		_fIndex = VIndex.STMT_CASE;
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
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer exp = _expression == null ? null : _expression.formalForm(nameMapping, isConsidered(), keywords);
		if (exp != null || isConsidered()) {
			StringBuffer buffer = new StringBuffer();
			if (_expression != null) {
				buffer.append("case ").append(exp == null ? nameMapping.getExprID(_expression) : exp).append(":\n");
			} else {
				buffer.append("default ").append(":\n");
			}
			return buffer;
		}
		return null;
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
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_CASE);
		if(_expression != null) {
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		SwCase swCase = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			swCase = (SwCase) getBindingNode();
			if (_expression  != null && swCase.getExpression() != null) {
				_expression.postAccurateMatch(swCase.getExpression());
			}
			match = (swCase == node);
		} else if(canBinding(node)) {
			swCase = (SwCase) node;
			if(_expression == swCase.getExpression()
					|| (_expression != null &&  _expression.postAccurateMatch(swCase.getExpression()))) {
					setBindingNode(node);
					match = true;
				} else {
					swCase = null;
				}
		}

		if(swCase == null) {
			continueTopDownMatchNull();
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			SwCase swCase = (SwCase) getBindingNode();
			if(_expression == null) {
				if(swCase.getExpression() != null) {
					Update update = new Update(this, _expression, swCase.getExpression());
					_modifications.add(update);
				}
			} else if(swCase.getExpression() == null
					|| _expression.getBindingNode() != swCase.getExpression()) {
				Update update = new Update(this, _expression, swCase.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof SwCase) {
			SwCase swCase = (SwCase) node;
			if(_expression == null && swCase.getExpression() != null) {
				return _expression.ifMatch(swCase.getExpression(), matchedNode, matchedStrings)
						&& super.ifMatch(node, matchedNode, matchedStrings);
			} else {
				return super.ifMatch(node, matchedNode, matchedStrings);
			}
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			if (_expression == null) {
				stringBuffer.append("default :\n");
			} else {
				stringBuffer.append("case ");
				StringBuffer tmp = _expression.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(" :\n");
			}
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			SwCase swCase = (SwCase) pnode;
			for(Modification modification : swCase.getModifications()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == swCase._expression) {
						expression = update.apply(vars, exprMap);
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
				StringBuffer tmp = _expression.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(" :\n");
			}
		} else {
			if (expression.toString().isEmpty()) {
//				if (_expression != null) return null;
				stringBuffer.append("default :\n");
			} else {
				stringBuffer.append("case ");
				stringBuffer.append(expression);
				stringBuffer.append(" :\n");
			}
		}
		return stringBuffer;
	}
}
