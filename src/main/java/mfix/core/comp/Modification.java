/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.comp;

import mfix.core.parse.node.Node;

import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Modification {
	
	protected Node _parent;
	
	public Node getParent() {
		return _parent;
	}
	
	public abstract Set<String> getNewVars();
	
}
