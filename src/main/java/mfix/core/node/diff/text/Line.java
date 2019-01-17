/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff.text;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Line implements Serializable {

	private static final long serialVersionUID = 1241825485382255312L;
	protected String _text;
	protected String _leading = "";
	
	public Line(String text) {
		_text = text;
	}
	
	public StringBuffer toSrcString() {
		return new StringBuffer(_text);
	}
	
	@Override
	public String toString() {
		return _leading + _text;
	}
}
