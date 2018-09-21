package mfix.core.parse.diff.ast;

import pfix.common.config.Constant;
import mfix.core.parse.node.Node;

public class KeepTree extends Tree {

	public KeepTree(Node node) {
		super(node);
		_leading = Constant.PATCH_KEEP_LEADING;
	}
	
}
