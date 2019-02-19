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
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.cluster.NameMapping;
import mfix.core.node.cluster.VIndex;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
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
public class Vdf extends Node {

	private static final long serialVersionUID = -1445761649599489420L;
	private SName _identifier = null;
	private int _dimensions = 0; 
	private Expr _expression = null;
	
	/**
	 * VariableDeclarationFragment:
     *	Identifier { Dimension } [ = Expression ]
	 */
	public Vdf(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public Vdf(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.VARDECLFRAG;
		_fIndex = VIndex.EXP_VAR_FRAG;
	}

	public void setName(SName identifier) {
		_identifier = identifier;
	}

	public String getName() {
		return _identifier.getName();
	}

	public void setDimensions(int dimensions) {
		_dimensions = dimensions;
	}

	public int getDimension() {
		return _dimensions;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_identifier.toSrcString());
		for (int i = 0; i < _dimensions; i++) {
			stringBuffer.append("[]");
		}
		if (_expression != null) {
			stringBuffer.append("=");
			stringBuffer.append(_expression.toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered) {
		boolean consider = isConsidered() || parentConsidered;
		StringBuffer identifier = _identifier.formalForm(nameMapping, consider);
		StringBuffer exp = _expression == null ? null : _expression.formalForm(nameMapping, consider);
		if (identifier == null && exp == null) {
			if (isConsidered()) {
				return new StringBuffer(nameMapping.getExprID(this));
			} else {
				return null;
			}
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(identifier == null ? nameMapping.getExprID(_identifier) : identifier);
		for (int i = 0; i < _dimensions; i++) {
			buffer.append("[]");
		}
		if (_expression != null) {
			buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp);
		}
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_identifier.tokens());
		for (int i = 0; i < _dimensions; i++) {
			_tokens.add("[]");
		}
		if (_expression != null) {
			_tokens.add("=");
			_tokens.addAll(_expression.tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof Vdf) {
			Vdf vdf = (Vdf) other;
			match = (_dimensions == vdf._dimensions) && _identifier.compare(vdf._identifier);
			if (_expression == null) {
				match = match && (vdf._expression == null);
			} else {
				match = match && _expression.compare(vdf._expression);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_identifier);
		if (_expression != null) {
			children.add(_expression);
		}
		return children;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_identifier.getFeatureVector());
		if (_expression != null) {
			_fVector.inc(FVector.ARITH_ASSIGN);
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		Vdf vdf = null;
		boolean match = false;
		if (compare(node)) {
			vdf = (Vdf) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			vdf = (Vdf) getBindingNode();
			match = (vdf == node);
		} else if (canBinding(node)) {
			vdf = (Vdf) node;
			setBindingNode(node);
			match = true;
		}
		if (vdf == null) {
			continueTopDownMatchNull();
		} else {
			_identifier.postAccurateMatch(vdf._identifier);
			if (_expression != null) {
				_expression.postAccurateMatch(vdf.getExpression());
			}
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (getBindingNode() != null) {
			Vdf vdf = (Vdf) getBindingNode();
			if (_identifier.compare(vdf._identifier)) {
				if(_expression == null) {
					if(vdf.getExpression() != null) {
						Update update = new Update(this, _expression, vdf.getExpression());
						_modifications.add(update);
					}
				} else if(_expression.getBindingNode() != vdf.getExpression()) {
					Update update = new Update(this, _expression, vdf.getExpression());
					_modifications.add(update);
				} else {
					_expression.genModifications();
				}
			}
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof Vdf) {
			Vdf vdf = (Vdf) node;
			if(NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
					&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings)) {
				matchedNode.put(_identifier, vdf._identifier);
				matchedStrings.put(_identifier.getName(), vdf.getName());
				return true;
			}
		}
		return false;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			Vdf vdf = (Vdf) node;
			for (Modification modification : vdf.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == vdf._expression) {
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					}
				} else {
					LevelLogger.error("@Vdf Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		tmp = _identifier.adaptModifications(vars, exprMap);
		if (tmp == null) return null;
		stringBuffer.append(tmp);
		for (int i = 0; i < _dimensions; i++){
			stringBuffer.append("[]");
		}
		if(expression == null) {
			if(_expression != null){
				stringBuffer.append("=");
				tmp = _expression.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append("=");
			stringBuffer.append(expression);
		}
		return stringBuffer;
	}
}
