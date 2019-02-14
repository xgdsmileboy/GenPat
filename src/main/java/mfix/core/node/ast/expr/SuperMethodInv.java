/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.cluster.VIndex;
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
public class SuperMethodInv extends Expr {

	private static final long serialVersionUID = -227589196009347171L;
	private Label _label = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperMethodInvocation:
     *	[ ClassName . ] super .
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public SuperMethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SMINVOCATION;
		_fIndex = VIndex.EXP_SUPER_METHOD_INV;
	}

	public void setLabel(Label label) {
		_label = label;
	}

	public void setName(SName name) {
		_name = name;
	}

	public void setArguments(ExprList arguments) {
		_arguments = arguments;
	}

	public SName getMethodName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_label != null) {
			stringBuffer.append(_label.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_label != null) {
			set.addAll(_label.getAllVars());
		}
		set.addAll(_arguments.getAllVars());
		return set;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_label != null) {
			_tokens.addAll(_label.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof SuperMethodInv) {
			SuperMethodInv superMethodInv = (SuperMethodInv) other;
			match = (_label == null) ? (superMethodInv._label == null) : _label.compare(superMethodInv._label);
			match = match && _name.compare(superMethodInv._name) && _arguments.compare(superMethodInv._arguments);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if (_label != null) {
			children.add(_label);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SUPER);
		_fVector.inc(FVector.E_MINV);
		if (_label != null) {
			_fVector.combineFeature(_label.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		SuperMethodInv methodInv = null;
		boolean match = false;
		if (compare(node)) {
			methodInv = (SuperMethodInv) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			methodInv = (SuperMethodInv) getBindingNode();
			match = (methodInv == node);
		} else if (canBinding(node)) {
			methodInv = (SuperMethodInv) node;
			setBindingNode(node);
			match = true;
		}
		if (methodInv == null) {
			continueTopDownMatchNull();
		} else {
			if (_label != null) {
				_label.postAccurateMatch(methodInv._label);
			}
			_name.postAccurateMatch(methodInv.getMethodName());
			_arguments.postAccurateMatch(methodInv.getArguments());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			SuperMethodInv methodInv = (SuperMethodInv) getBindingNode();
			if (_label == null) {
				if (methodInv._label != null) {
					Update update = new Update(this, _label, methodInv._label);
					_modifications.add(update);
				}
			} else if (methodInv._label == null || !_label.compare(methodInv._label)) {
				Update update = new Update(this, _label, methodInv._label);
				_modifications.add(update);
			}
			if (!_name.compare(methodInv._name)) {
				Update update = new Update(this, _name, methodInv._name);
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
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if(_label != null){
				tmp = _label.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			stringBuffer.append("super.");
			stringBuffer.append(_name.getName());
			stringBuffer.append("(");
			tmp = _arguments.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer label = null;
		StringBuffer name = null;
		StringBuffer arguments = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			SuperMethodInv superMethodInv = (SuperMethodInv) node;
			for (Modification modification : superMethodInv.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == superMethodInv._label) {
						label = update.apply(vars, exprMap);
						if (label == null) return null;
					} else if (changedNode == superMethodInv._name) {
						name = update.apply(vars, exprMap);
						if (name == null) return null;
					} else {
						arguments = update.apply(vars, exprMap);
						if (arguments == null) return null;
					}
				} else {
					LevelLogger.error("@SuperMethodInv Should not be this kind of modificaiton : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if(label == null) {
			if(_label != null){
				tmp = _label.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(label + ".");
		}
		stringBuffer.append("super.");
		if(name == null) {
			tmp = _name.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		stringBuffer.append("(");
		if(arguments == null) {
			tmp = _arguments.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(arguments);
		}
		stringBuffer.append(")");
		return stringBuffer;
	}
}
