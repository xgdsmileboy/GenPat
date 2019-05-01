/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class MethodInv extends Expr {

	private static final long serialVersionUID = 3902854514191993113L;
	private Expr _expression = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 *  MethodInvocation:
     *  [ Expression . ]
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public MethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.MINVOCATION;
		_fIndex = VIndex.EXP_METHOD_INV;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public void setName(SName name) {
		_name = name;
	}

	public void setArguments(ExprList arguments) {
		_arguments = arguments;
	}

	public Expr getExpression() {
		return _expression;
	}

	public SName getName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	public void setExpanded() {
		super.setExpanded();
		if (Constant.EXPAND_PATTERN) {
			_name.setExpanded();
		}
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression != null) {
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if (_arguments != null) {
			stringBuffer.append(_arguments.toSrcString());
		}
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer exp = null;
		if (_expression != null) {
			exp = _expression.formalForm(nameMapping, consider, keywords);
		}
		StringBuffer name = null;
		if (!_name.isAbstract() && (isChanged() || isExpanded())) {
			name = _name.toSrcString();
			keywords.add(name.toString());
		} else if (isConsidered()) {
			name = new StringBuffer(nameMapping.getMethodID(this));
		}

		StringBuffer arg = _arguments.formalForm(nameMapping, consider, keywords);

		if (exp == null && name == null) {
			if (arg == null) {
				return super.toFormalForm0(nameMapping, parentConsidered, keywords);
			} else if (nameMapping.isFoldedMethod(arg.toString())){
				return new StringBuffer(arg);
			}
		}

		StringBuffer buffer = new StringBuffer();
		if (_expression != null) {
			buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp).append('.');
		}
		buffer.append(name == null ? nameMapping.getMethodID(_name) : name)
				.append('(').append(arg == null ? "" : arg).append(')');
		return buffer;
	}

	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_expression != null) {
			set.addAll(_expression.getAllVars());
		}
		if (_arguments != null) {
			set.addAll(_arguments.getAllVars());
		}
		return set;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_expression != null) {
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		if (_arguments != null) {
			_tokens.addAll(_arguments.tokens());
		}
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof MethodInv) {
			MethodInv methodInv = (MethodInv) other;
			match = _name.compare(methodInv._name);
			if (match) {
				match = (_expression == null ? (methodInv._expression == null)
						: _expression.compare(methodInv._expression)) && _arguments.compare(methodInv._arguments);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if (_expression != null) {
			children.add(_expression);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}

	@Override
	public String getAPIStr() {
		return _name.getName();
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.E_MINV);

		_completeFVector = new FVector();
		_completeFVector.combineFeature(_selfFVector);
		if (_expression != null) {
			_completeFVector.combineFeature(_expression.getFeatureVector());
		}
		_completeFVector.combineFeature(_arguments.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		MethodInv methodInv = null;
		boolean match = false;
		if (compare(node)) {
			methodInv = (MethodInv) node;
			setBindingNode(methodInv);
			match = true;
		} else if (getBindingNode() != null) {
			methodInv = (MethodInv) getBindingNode();
			match = (methodInv == node);
		} else if (canBinding(node)) {
			methodInv = (MethodInv) node;
			if (methodInv.getName().getName().equals(getName().getName())
					|| (_expression != null && _expression.compare(methodInv._expression))) {
				setBindingNode(methodInv);
				match = true;
			} else {
				methodInv = null;
			}
		}
		if (methodInv == null) {
			continueTopDownMatchNull();
		} else {
			if (_expression != null) {
				_expression.postAccurateMatch(methodInv.getExpression());
			}
			_name.postAccurateMatch(methodInv.getName());
			_arguments.postAccurateMatch(methodInv.getArguments());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			MethodInv methodInv = (MethodInv) getBindingNode();
			if (_expression == null) {
				if (methodInv.getExpression() != null) {
					Update update = new Update(this, _expression, methodInv.getExpression());
					_modifications.add(update);
				}
			} else if (methodInv.getExpression() == null
					|| _expression.getBindingNode() != methodInv.getExpression()) {
				Update update = new Update(this, _expression, methodInv.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			if (_name.getBindingNode() != methodInv.getName() || !_name.getName().equals(methodInv.getName().getName())) {
				Update update = new Update(this, _name, methodInv.getName());
				_modifications.add(update);
			}
			if (_arguments.getBindingNode() != methodInv.getArguments()) {
				Update update = new Update(this, _arguments, methodInv.getArguments());
				_modifications.add(update);
			} else {
				_arguments.genModifications();
			}
		}
		return true;
	}

	@Override
	public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node.getNodeType() == TYPE.MINVOCATION) {
			MethodInv methodInv = (MethodInv) node;
			if (_expression != null && methodInv._expression != null) {
				if (NodeUtils.matchSameNodeType(_expression, methodInv._expression, matchedNode, matchedStrings)) {
					_expression.greedyMatchBinding(methodInv.getExpression(), matchedNode, matchedStrings);
				}
			}
		}
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if(super.ifMatch(node, matchedNode, matchedStrings, level)) {
			if (node.getNodeType() == TYPE.MINVOCATION) {
				MethodInv methodInv = (MethodInv) node;
				Utils.checkCompatiblePut(NodeUtils.decorateMethodName(_name),
						NodeUtils.decorateMethodName(methodInv.getName()), matchedStrings);
				if (_expression != null && methodInv._expression != null
						&& NodeUtils.matchSameNodeType(_expression, methodInv._expression, matchedNode, matchedStrings)) {
					ExprList list = methodInv.getArguments();
					if (_arguments.getExpr().size() == list.getExpr().size()) {
						for (int i = 0; i < _arguments.getExpr().size(); i++) {
							NodeUtils.matchSameNodeType(_arguments.getExpr().get(i), list.getExpr().get(i),
									matchedNode, matchedStrings);
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if (_expression != null) {
				tmp = _expression.transfer(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			String name = exprMap.get(NodeUtils.decorateMethodName(_name));
			name = NodeUtils.dedecorateMethodName(name);
			if (name == null) {
				stringBuffer.append(_name.getName());
			} else {
				stringBuffer.append(name);
			}
			metric.inc();
			stringBuffer.append("(");
			if (_arguments != null) {
				tmp = _arguments.transfer(vars, exprMap,retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(")");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		StringBuffer expression = null;
		StringBuffer name = null;
		StringBuffer arguments = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			MethodInv methodInv = (MethodInv) node;
			for (Modification modification : methodInv.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == methodInv._expression) {
						expression = update.apply(vars, exprMap, retType, exceptions, metric);
						if (expression == null) return null;
					} else if (changedNode == methodInv._name) {
						name = update.getTarNode().toSrcString();
						metric.add(1, Adaptee.CHANGE.UPDATE);
						if (name == null) return null;
					} else {
						arguments = update.apply(vars, exprMap, retType, exceptions, metric);
						if (arguments == null) return null;
					}
				} else {
					LevelLogger.error("@MethodInv Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if(expression == null) {
			if (_expression != null) {
				tmp = _expression.adaptModifications(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else if (!expression.toString().isEmpty()){
			stringBuffer.append(expression + ".");
		}
		if (name == null) {
			stringBuffer.append(_name.getName());
		} else {
			stringBuffer.append(name);
		}
		stringBuffer.append("(");
		if(arguments == null) {
			if (_arguments != null) {
				tmp = _arguments.adaptModifications(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(arguments);
		}
		stringBuffer.append(")");
		return stringBuffer;
	}
}
