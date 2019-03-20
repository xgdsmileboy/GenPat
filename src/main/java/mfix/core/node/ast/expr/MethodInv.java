/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import mfix.core.stats.element.ElementCounter;
import mfix.core.stats.element.ElementException;
import mfix.core.stats.element.ElementQueryType;
import mfix.core.stats.element.MethodElement;
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
		boolean consider = isConsidered() || parentConsidered;
		StringBuffer exp = null;
		if (_expression != null) {
			exp = _expression.formalForm(nameMapping, consider, keywords);
		}
		StringBuffer name = _name.formalForm(nameMapping, consider, keywords);
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
		if (other instanceof MethodInv) {
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
			if (methodInv.getName().getName().equals(getName().getName())) {
				setBindingNode(methodInv);
				match = true;
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
	public void doAbstraction(ElementCounter counter) {
		if (isConsidered()) {
			ElementQueryType qtype = new ElementQueryType(false,
					false, ElementQueryType.CountType.COUNT_FILES);
			MethodElement methodElement = new MethodElement(_name.getName(), null);
			methodElement.setArgsNumber(_arguments.getExpr().size());
			try {
				_abstract = counter.count(methodElement, qtype) < Constant.API_FREQUENCY;
			} catch (ElementException e) {
				_abstract = true;
			}
		}
		super.doAbstraction(counter);
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
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof Expr) {
			if(isAbstract()) {
				return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
						&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
			} else if (node instanceof MethodInv){
				MethodInv methodInv = (MethodInv) node;
				List<Expr> exprs = _arguments.getExpr();
				List<Expr> others = methodInv.getArguments().getExpr();
				if (_name.compare(methodInv.getName()) && exprs.size() == others.size()) {
					matchedNode.put(_name, methodInv.getName());
					matchedNode.put(this, node);
					matchedStrings.put(toString(), node.toString());
					if(_expression != null && methodInv.getExpression() != null) {
						matchedNode.put(_expression, methodInv.getExpression());
						matchedStrings.put(_expression.toString(), methodInv.getExpression().toString());
					}
					for(int i = 0; i < exprs.size(); i++) {
						matchedNode.put(exprs.get(i), others.get(i));
						matchedStrings.put(exprs.get(i).toString(), others.get(i).toString());
					}
					return true;
				}
				return false;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if (_expression != null) {
				tmp = _expression.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			stringBuffer.append(_name.getName());
			stringBuffer.append("(");
			if (_arguments != null) {
				tmp = _arguments.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(")");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
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
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					} else if (changedNode == methodInv._name) {
						name = update.apply(vars, exprMap);
						if (name == null) return null;
					} else {
						arguments = update.apply(vars, exprMap);
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
				tmp = _expression.adaptModifications(vars, exprMap);
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
				tmp = _arguments.adaptModifications(vars, exprMap);
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
