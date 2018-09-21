package mfix.core.parse.diff.text;

import pfix.common.config.Constant;
import mfix.core.parse.diff.Delete;

public class DelLine extends Line implements Delete {

	public DelLine(String text) {
		super(text);
		_leading = Constant.PATCH_DEL_LEADING;
	}
	
}
