/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff.tree;

import mfix.common.conf.Constant;
import mfix.core.node.diff.Add;
import mfix.core.node.ast.Node;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AddTree extends Tree implements Add{

	public AddTree(Node node) {
		super(node);
		_leading = Constant.PATCH_ADD_LEADING;
	}
}
