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

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SName extends Label {

	private static final long serialVersionUID = 6548845608841663421L;
	private String _name = null;
	
	/**
	 * SimpleName:
     *	Identifier
	 */
	public SName(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SNAME;
	}
	
	public void setName(String name){
		_name = name;
	}
	
	public String getName(){
		return _name;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_name);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_name);
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof SName) {
			SName sName	= (SName) other;
			return _name.equals(sName._name);
		}
		return false;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_VAR);
	}

}
