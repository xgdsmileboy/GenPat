package mfix.core.parse.diff.ast;

import pfix.common.config.Constant;
import mfix.core.parse.diff.Add;
import mfix.core.parse.node.Node;

public class AddTree extends Tree implements Add{

	public AddTree(Node node) {
		super(node);
		_leading = Constant.PATCH_ADD_LEADING;
	}
}
