/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.comp;


import mfix.core.parse.node.Node;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
@Deprecated
public class Insertion extends Modification {
	
	/**
	 * the position before which {@code _tarNode} should be inserted. 
	 */
	private int _index;
	private Node _tarNode;
	private Node _preBro;
	private Node _nextBro;
	
	public Insertion(Node parent, int index, Node tarNode) {
		_parent = parent;
		_index = index;
		_tarNode = tarNode;
	}
	
	public void setPreBro(Node node) {
		_preBro = node;
	}
	
	public Node getPreBro() {
		return _preBro;
	}
	
	public void setNextBro(Node node) {
		_nextBro = node;
	}
	
	public Node getNextBro() {
		return _nextBro;
	}
	
	public int getIndex() {
		return _index;
	}
	
	public Node getTarget() {
		return _tarNode;
	}
	
	@Override
	public Set<String> getNewVars() {
		Set<String> newVars = new HashSet<>();
		if(_tarNode != null){
			newVars.addAll(_tarNode.getNewVars());
		}
		return newVars;
	}
	
	@Override
	public String toString() {
		return "[INS] : (" + _index + ")" + _tarNode.toSrcString().toString();
	}
	
}
