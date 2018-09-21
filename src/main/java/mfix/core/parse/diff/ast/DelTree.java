package mfix.core.parse.diff.ast;

import pfix.common.config.Constant;
import mfix.core.parse.diff.Delete;
import mfix.core.parse.node.Node;

public class DelTree extends Tree implements Delete {

	public DelTree(Node node) {
		super(node);
		_leading = Constant.PATCH_DEL_LEADING;
	}
	
}
