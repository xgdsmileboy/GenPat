/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff.text;

import mfix.common.conf.Constant;
import mfix.core.node.diff.Delete;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class DelLine extends Line implements Delete, Serializable {

	private static final long serialVersionUID = 7207850128129029810L;

	public DelLine(String text) {
		super(text);
		_leading = Constant.PATCH_DEL_LEADING;
	}
	
}
