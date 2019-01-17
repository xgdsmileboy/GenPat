/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff.text;

import mfix.common.util.Constant;
import mfix.core.node.diff.Add;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AddLine extends Line implements Add, Serializable {

	private static final long serialVersionUID = -5178723884749316617L;

	public AddLine(String text) {
		super(text);
		_leading = Constant.PATCH_ADD_LEADING;
	}
}
