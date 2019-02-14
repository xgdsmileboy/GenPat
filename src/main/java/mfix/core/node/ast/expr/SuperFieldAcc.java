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
import mfix.core.node.cluster.VIndex;
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
		if (other instanceof SuperFieldAcc) {
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

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SUPER);
		_fVector.inc(FVector.E_FACC);
		if (_name != null) {
			_fVector.combineFeature(_name.getFeatureVector());
		}
		_fVector.combineFeature(_identifier.getFeatureVector());
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
	public boolean genModidications() {
		if (super.genModidications()) {
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
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if(_name != null){
				tmp = _name.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			stringBuffer.append("super.");
			tmp = _identifier.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer name = null;
		StringBuffer identifier = null;
		Node node = checkModification();
		if (node != null) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) node;
			for (Modification modification : superFieldAcc.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == superFieldAcc._name) {
						name = update.apply(vars, exprMap);
						if (name == null) return null;
					} else {
						identifier = update.apply(vars, exprMap);
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
				tmp = _name.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(name + ".");
		}
		stringBuffer.append("super.");
		if(identifier == null) {
			tmp = _identifier.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(identifier);
		}
		return stringBuffer;
	}
}
