/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff;

import mfix.common.util.Constant;
import mfix.core.node.match.Matcher;
import mfix.core.node.diff.text.AddLine;
import mfix.core.node.diff.text.DelLine;
import mfix.core.node.diff.text.KeepLine;
import mfix.core.node.diff.text.Line;
import mfix.core.node.ast.Node;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class TextDiff extends Diff<Line> {
	
	public TextDiff(Node src, Node tar) {
		super(src, tar);
	}
	
	@Override
	protected void extractDiff() {
		if(_source == null || _source.isEmpty()) {
			String[] srcText = _src.toSrcString().toString().split(Constant.NEW_LINE);
			String[] tarText = _tar.toSrcString().toString().split(Constant.NEW_LINE);
			int srcLen = srcText.length;
			int tarLen = tarText.length;
			_source = new ArrayList<>(srcLen + tarLen);
			int rightCursor = 0;
			Map<Integer, Integer> lineMap = Matcher.match(srcText, tarText);
			for(int i = 0; i < srcLen; i++) {
				Integer cursor = lineMap.get(i);
				if(cursor == null) {
					_source.add(new DelLine(srcText[i]));
				} else {
					for(; rightCursor < cursor && rightCursor < tarLen; rightCursor ++) {
						_source.add(new AddLine(tarText[rightCursor]));
					}
					rightCursor = cursor + 1;
					_source.add(new KeepLine(srcText[i]));
				}
			}
			for(; rightCursor < tarLen; rightCursor ++) {
				_source.add(new AddLine(tarText[rightCursor]));
			}
		}		
	}
	
	@Override
	public void accurateMatch() {
		
	}
	
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		for(Line line : _source) {
			stringBuffer.append(line.toString());
			stringBuffer.append(Constant.NEW_LINE);
		}
		return stringBuffer.toString();
	}

}
