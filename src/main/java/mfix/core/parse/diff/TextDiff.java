package mfix.core.parse.diff;

import java.util.ArrayList;
import java.util.Map;

import pfix.common.config.Constant;
import mfix.core.parse.Matcher;
import mfix.core.parse.diff.text.AddLine;
import mfix.core.parse.diff.text.DelLine;
import mfix.core.parse.diff.text.KeepLine;
import mfix.core.parse.diff.text.Line;
import mfix.core.parse.node.Node;

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
