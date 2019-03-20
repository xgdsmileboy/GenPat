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
import mfix.core.node.ast.VarScope;
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
public class Svd extends Expr {

	private static final long serialVersionUID = 3849439897999091912L;
	private MType _decType = null;
	private SName _name = null;
	private Expr _initializer = null;
	
	/**
	 * { ExtendedModifier } Type {Annotation} [ ... ] Identifier { Dimension } [ = Expression ]
	 * "..." should not be appear since it is only used in method declarations
	 */
	public Svd(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SINGLEVARDECL;
		_fIndex = VIndex.EXP_SVD;
	}

	public void setDecType(MType decType) {
		_decType = decType;
	}

	public void setName(SName name) {
		_name = name;
	}

	public MType getDeclType() {
		return _decType;
	}

	public Expr getInitializer() {
		return _initializer;
	}

	public SName getName() {
		return _name;
	}

	public void setInitializer(Expr initializer) {
		_initializer = initializer;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_decType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_name.toSrcString());
		if (_initializer != null) {
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		boolean consider = isConsidered() || parentConsidered;
		StringBuffer type = _decType.formalForm(nameMapping, consider, keywords);
		StringBuffer name = _name.formalForm(nameMapping, consider, keywords);
		StringBuffer init = null;
		if (_initializer != null) {
			init = _initializer.formalForm(nameMapping, consider, keywords);
		}
		if (type == null && name == null && init == null) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(type == null ? nameMapping.getTypeID(_decType) : type).append(' ')
				.append(name == null ? nameMapping.getExprID(_name) : name);
		if (_initializer != null) {
			buffer.append(init == null ? nameMapping.getExprID(_initializer) : init);
		}
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_decType.tokens());
		_tokens.addAll(_name.tokens());
		if (_initializer != null) {
			_tokens.addFirst("=");
			_tokens.addAll(_initializer.tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof Svd) {
			Svd svd = (Svd) other;
			match = _decType.compare(svd._decType);
			match = match && _name.compare(svd._name);
			if (_initializer == null) {
				match = match && (svd._initializer == null);
			} else {
				match = match && _initializer.compare(svd._initializer);
			}
		}

		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_decType);
		children.add(_name);
		if (_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.E_VARDEF);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.E_VARDEF);
		_completeFVector.combineFeature(_decType.getFeatureVector());
		_completeFVector.combineFeature(_name.getFeatureVector());
		if (_initializer != null) {
			_selfFVector.inc(FVector.ARITH_ASSIGN);
			_completeFVector.inc(FVector.ARITH_ASSIGN);
			_completeFVector.combineFeature(_initializer.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		Svd svd = null;
		boolean match = false;
		if (compare(node)) {
			svd = (Svd) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			svd = (Svd) getBindingNode();
			match = (svd == node);
		} else if (canBinding(node)) {
			svd = (Svd) node;
			setBindingNode(node);
			match = true;
		}
		if (svd == null) {
			continueTopDownMatchNull();
		} else {
			_decType.postAccurateMatch(svd.getDeclType());
			_name.postAccurateMatch(svd.getName());
			if (_initializer != null) {
				_initializer.postAccurateMatch(svd.getInitializer());
			}
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			Svd svd = (Svd) getBindingNode();
			if (!_decType.compare(svd.getDeclType())) {
				Update update = new Update(this, _decType, svd.getDeclType());
				_modifications.add(update);
			}
			if (!_name.compare(svd.getName())) {
				Update update = new Update(this, _name, svd.getName());
				_modifications.add(update);
			}
			if (_initializer == null) {
				if (svd.getInitializer() != null) {
					Update update = new Update(this, _initializer, svd.getInitializer());
					_modifications.add(update);
				}
			} else if (svd.getModifications() == null ||
					_initializer.getBindingNode() != svd.getInitializer()) {
				Update update = new Update(this, _initializer, svd.getInitializer());
				_modifications.add(update);
			} else {
				_initializer.genModifications();
			}
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof Svd) {
			Svd svd = (Svd) node;
			if(NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
					&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings)) {
				return NodeUtils.matchSameNodeType(_name, svd.getName(), matchedNode, matchedStrings);
			}
			return false;
		} else if (node instanceof Vdf) {
			Vdf vdf = (Vdf) node;
			if(NodeUtils.checkDependency(this, node, matchedNode, matchedStrings)
					&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings)) {
				return NodeUtils.matchSameNodeType(_name, vdf.getNameNode(), matchedNode, matchedStrings);
			}
		}
		return false;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
		StringBuffer declType = null;
		StringBuffer name = null;
		StringBuffer initializer = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			Svd svd = (Svd) node;
			for (Modification modification : svd.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == svd._decType) {
						declType = update.apply(vars, exprMap);
						if (declType == null) return null;
					} else if (changedNode == svd._name) {
						name = update.apply(vars, exprMap);
						if (name == null) return null;
					} else {
						initializer = update.apply(vars, exprMap);
						if (initializer == null) return null;
					}
				} else {
					LevelLogger.error("@Svd Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if (declType == null) {
			tmp = _decType.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(declType);
		}
		stringBuffer.append(" ");
		if(name == null) {
			tmp = _name.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		if(initializer == null) {
			if(_initializer != null){
				stringBuffer.append("=");
				tmp = _initializer.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else if (!initializer.toString().isEmpty()){
			stringBuffer.append("=");
			stringBuffer.append(initializer);
		}
		return stringBuffer;
	}
}
