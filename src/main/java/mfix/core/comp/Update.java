/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.comp;

import org.eclipse.jdt.core.dom.Type;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
@Deprecated
public class Update extends Modification {

	private Node _srcNode;
	private Node _tarNode;
	
	public Update(Node parent, Node srcNode, Node tarNode) {
		_parent = parent;
		_srcNode = srcNode;
		_tarNode = tarNode;
	}
	
	public String changedType() {
		if(_srcNode instanceof Expr && _tarNode instanceof Expr) {
			Expr src = (Expr) _srcNode;
			Expr tar = (Expr) _tarNode;
			Type srcType = src.getType();
			Type tarType = tar.getType();
			if(srcType != null && tarType != null) {
				if(!srcType.toString().equals(tarType.toString())) {
					return srcType.toString();
				}
			}
		}
		return null;
	}
	
	public Node getSrcNode() {
		return _srcNode;
	}
	
	public Node getTarNode() {
		return _tarNode;
	}
	
	@Override
	public Set<String> getNewVars() {
		return new HashSet<>();
	}
	
	@Override
	public String toString() {
		if(_srcNode == null){
			return "[UPD] : null => " + _tarNode.toSrcString().toString();
		} else if(_tarNode == null) {
			return "[UPD] : " + _srcNode.toSrcString().toString() + " => null";
		} else {
			return "[UPD] : " + _srcNode.toSrcString().toString() + " => " + _tarNode.toSrcString().toString();
		}
	}
}
