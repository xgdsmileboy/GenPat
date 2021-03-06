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
import mfix.core.node.modify.Adaptee;
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
public class QName extends Label {

	private static final long serialVersionUID = -7347252879776740292L;
	private Label _name = null;
	private SName _sname = null;
	
	/**
	 * QualifiedName:
     *	Name . SimpleName
	 */
	public QName(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.QNAME;
		_fIndex = VIndex.EXP_QNAME;
	}

	public void setName(Label namee, SName sname) {
		_name = namee;
		_sname = sname;
	}

	public SName getSName() {
		return _sname;
	}

	public String getIdentifier() {
		return _sname.getName();
	}

	public String getLabel() {
		return _name.toSrcString().toString();
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append(".");
		stringBuffer.append(_sname.toSrcString());
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer name = _name.formalForm(nameMapping, consider, keywords);
		StringBuffer sname = _sname.formalForm(nameMapping, consider, keywords);
		if (name == null && sname == null) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(name == null ? nameMapping.getExprID(_name) : name)
				.append('.')
				.append(sname == null ? nameMapping.getExprID(_sname) : sname);
		return buffer;
	}

	public Set<SName> getAllVars() {
		return _name.getAllVars();
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_name.tokens());
		_tokens.add(".");
		_tokens.addAll(_sname.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof QName) {
			QName qName = (QName) other;
			match = _name.compare(qName._name) && _sname.compare(qName._sname);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_name);
		children.add(_sname);
		return children;
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();

		_completeFVector = new FVector();
		String sname = _sname.getName();
		if (_name instanceof SName && NodeUtils.possibleClassName(((SName) _name).getName())
				&& sname.toUpperCase().equals(sname)) {
			_selfFVector.inc(FVector.OTHER);
			_completeFVector.inc(FVector.OTHER);
		} else {
			_completeFVector.combineFeature(_name.getFeatureVector());
			_completeFVector.combineFeature(_sname.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		QName qName = null;
		boolean match = false;
		if (getBindingNode() != null) {
			qName = (QName) getBindingNode();
			match = (qName == node);
		} else if (canBinding(node)) {
			qName = (QName) node;
			setBindingNode(node);
			match = true;
		}
		if (qName == null) {
			continueTopDownMatchNull();
		} else {
			_name.postAccurateMatch(qName._name);
			_sname.postAccurateMatch(qName.getSName());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			QName qName = (QName) getBindingNode();
			if (_name.getBindingNode() != qName._name) {
				Update update = new Update(this, _name, qName._name);
				_modifications.add(update);
			} else {
				_name.genModifications();
			}
			if (!_sname.compare(qName.getSName())) {
				Update update = new Update(this, _sname, qName.getSName());
				_modifications.add(update);
			}
		}
		return true;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp = toSrcString();
			if (!Character.isUpperCase(tmp.charAt(0))) {
				tmp = _name.transfer(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) {
					return null;
				}
				stringBuffer.append(tmp);
				stringBuffer.append(".");
				// Field access need not check, 2020-6-8 @jiajun
				stringBuffer.append(_sname.getName());
// 				tmp = _sname.transfer(vars, exprMap, retType, exceptions, metric);
// 				if(tmp == null) return null;
// 				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(tmp);
			}
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		StringBuffer name = null;
		StringBuffer sname = null;
		Node node = NodeUtils.checkModification(this);
		if (node != null) {
			QName qName = (QName) node;
			for (Modification modification : qName.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == qName._name) {
						name = update.apply(vars, exprMap, retType, exceptions, metric);
						if (name == null) return null;
					} else {
						sname = update.apply(vars, exprMap, retType, exceptions, metric);
						if (sname == null) return null;
					}
				} else {
					LevelLogger.error("@QName Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		if (name == null) {
			tmp = _name.adaptModifications(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		stringBuffer.append(".");
		if(sname == null) {
			tmp = _sname.adaptModifications(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(sname);
		}
		return stringBuffer;
	}
}
