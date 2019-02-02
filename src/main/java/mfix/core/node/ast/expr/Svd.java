/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
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
		_fVector = new FVector();
		_fVector.combineFeature(_decType.getFeatureVector());
		_fVector.combineFeature(_name.getFeatureVector());
		if (_initializer != null) {
			_fVector.inc(FVector.ARITH_ASSIGN);
			_fVector.combineFeature(_initializer.getFeatureVector());
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
	public boolean genModidications() {
		if (super.genModidications()) {
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
			} else if (_initializer.getBindingNode() != svd.getInitializer()) {
				Update update = new Update(this, _initializer, svd.getInitializer());
				_modifications.add(update);
			} else {
				_initializer.genModidications();
			}
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof Svd) {
			return checkDependency(node, matchedNode, matchedStrings)
					&& matchSameNodeType(node, matchedNode, matchedStrings);
		} else {
			return false;
		}
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer declType = null;
		StringBuffer name = null;
		StringBuffer initializer = null;
		Node node = checkModification();
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
		} else {
			stringBuffer.append(initializer);
		}
		return stringBuffer;
	}
}
