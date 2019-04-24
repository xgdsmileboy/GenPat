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
public class SuperFieldAcc extends Expr {

	private static final long serialVersionUID = 1921879022776437618L;
	private Label _name = null;
	private SName _identifier = null;
	
	/**
	 * SuperFieldAccess:
     *	[ ClassName . ] super . Identifier
	 */
	public SuperFieldAcc(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SFIELDACC;
		_fIndex = VIndex.EXP_SUPER_FIELD_ACC;
	}

	public void setName(Label name) {
		_name = name;
	}

	public void setIdentifier(SName identifier) {
		_identifier = identifier;
	}

	public SName getIdentifier() {
		return _identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_name != null) {
			stringBuffer.append(_name.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer name = null;
		if (_name != null) {
			name = _name.formalForm(nameMapping, consider, keywords);
		}
		StringBuffer identifier = _identifier.formalForm(nameMapping, consider, keywords);
		if (name == null && identifier == null) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		if (_name != null) {
			buffer.append(name == null ? nameMapping.getExprID(_name) : name);
			buffer.append('.');
		}
		buffer.append("super.").append(identifier == null ? nameMapping.getExprID(_identifier) : identifier);
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_name != null) {
			_tokens.addAll(_name.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof SuperFieldAcc) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) other;
			match = (_name == null) ? (superFieldAcc._name == null) : _name.compare(superFieldAcc._name);
			if (match) {
				match = match && _identifier.compare(superFieldAcc._identifier);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		if (_name != null) {
			children.add(_name);
		}
		children.add(_identifier);
		return children;
	}

	public List<Node> flattenTreeNode(List<Node> nodes) {
		nodes.add(this);
		return nodes;
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.KEY_SUPER);
		_selfFVector.inc(FVector.E_FACC);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.KEY_SUPER);
		_completeFVector.inc(FVector.E_FACC);
		if (_name != null) {
			_completeFVector.combineFeature(_name.getFeatureVector());
		}
		_completeFVector.combineFeature(_identifier.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		SuperFieldAcc superFieldAcc = null;
		boolean match = false;
		if (compare(node)) {
			superFieldAcc = (SuperFieldAcc) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			superFieldAcc = (SuperFieldAcc) getBindingNode();
			match = (superFieldAcc == node);
		} else if (canBinding(node)) {
			superFieldAcc = (SuperFieldAcc) node;
			setBindingNode(node);
			match = true;
		}
		if (superFieldAcc == null) {
			continueTopDownMatchNull();
		} else {
			if (_name != null) {
				_name.postAccurateMatch(superFieldAcc._name);
			}
			_identifier.postAccurateMatch(superFieldAcc._identifier);
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) getBindingNode();
			if (_identifier.getBindingNode() != superFieldAcc.getIdentifier()
					|| !_identifier.getName().equals(superFieldAcc.getIdentifier().getName())) {
				Update update = new Update(this, _identifier, superFieldAcc.getIdentifier());
				_modifications.add(update);
			}
			if (_name != null && superFieldAcc._name != null) {
				if (!_name.compare(superFieldAcc._name)) {
					Update update = new Update(this, _name, superFieldAcc._name);
					_modifications.add(update);
				}
			} else if(_name != superFieldAcc._name) {
				Update update = new Update(this, _name, superFieldAcc._name);
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if(_name != null){
				tmp = _name.transfer(vars, exprMap, retType, exceptions);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			stringBuffer.append("super.");
			tmp = _identifier.transfer(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		StringBuffer name = null;
		StringBuffer identifier = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) node;
			for (Modification modification : superFieldAcc.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == superFieldAcc._name) {
						name = update.apply(vars, exprMap, retType, exceptions);
						if (name == null) return null;
					} else {
						identifier = update.apply(vars, exprMap, retType, exceptions);
						if (identifier == null) return null;
					}
				} else {
					LevelLogger.error("@SuperFieldAcc Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if (name == null) {
			if (_name != null){
				tmp = _name.adaptModifications(vars, exprMap, retType, exceptions);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(name + ".");
		}
		stringBuffer.append("super.");
		if(identifier == null) {
			tmp = _identifier.adaptModifications(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(identifier);
		}
		return stringBuffer;
	}
}
