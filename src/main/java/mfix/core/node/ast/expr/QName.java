/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.match.metric.FVector;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
	}
	
	public void setName(Label namee, SName sname){
		_name = namee;
		_sname = sname;
	}
	
	public SName getSName() {
		return _sname;
	}
	
	public String getIdentifier(){
		return _sname.getName();
	}
	
	public String getLabel(){
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
		if(other instanceof QName) {
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
		_fVector = new FVector();
		String name = _name.toString();
		String sname = _sname.toString();
		if(_name instanceof SName && Character.isUpperCase(name.charAt(0)) && sname.toUpperCase().equals(sname)){
			_fVector.inc(FVector.OTHER);
		} else {
			_fVector.combineFeature(_name.getFeatureVector());
			_fVector.combineFeature(_sname.getFeatureVector());
		}
	}

}
