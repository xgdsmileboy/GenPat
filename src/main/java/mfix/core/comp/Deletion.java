/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.comp;

import mfix.core.parse.node.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class Deletion extends Modification {

	private Node _srcNode;
	
	public Deletion(Node parent, Node srcNode) {
		_parent = parent;
		_srcNode = srcNode;
	}
	
	public Node getSrcNode() {
		return _srcNode;
	}
	
	@Override
	public Set<String> getNewVars() {
		return new HashSet<>();
	}
	
	@Override
	public String toString() {
		return "[DEL] : " + _srcNode.toSrcString().toString();
	}
}
