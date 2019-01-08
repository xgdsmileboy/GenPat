/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff.tree;

import mfix.core.node.ast.Node;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Tree {

	protected Node _node;
	protected String _leading = "";
	
	public Tree(Node node) {
		_node = node;
	}
	
	public Node getNode() {
		return _node;
	}
	
	public StringBuffer toSrcString() {
		return _node.toSrcString();
	}
	
	@Override
	public String toString() {
		String[] text = _node.toSrcString().toString().split("\n");
		StringBuffer stringBuffer = new StringBuffer();
		for(String string : text) {
			stringBuffer.append(_leading + string);
			stringBuffer.append("\n");
		}
		stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		return stringBuffer.toString();
	}
	
}
