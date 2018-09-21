package mfix.core.parse.diff.text;

import pfix.common.config.Constant;
import mfix.core.parse.diff.Add;

public class AddLine extends Line implements Add {

	public AddLine(String text) {
		super(text);
		_leading = Constant.PATCH_ADD_LEADING;
	}
}
