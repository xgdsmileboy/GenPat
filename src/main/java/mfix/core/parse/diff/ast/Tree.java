package mfix.core.parse.diff.ast;

import mfix.core.parse.node.Node;

public abstract class Tree {

	protected Node _node;
	protected String _leading = "";
	
	public Tree(Node node) {
		_node = node;
	}
	
	public Node getNode() {
		return _node;
	}
	
	public StringBuffer toSrcString() {
		return _node.toSrcString();
	}
	
	@Override
	public String toString() {
		String[] text = _node.toSrcString().toString().split("\n");
		StringBuffer stringBuffer = new StringBuffer();
		for(String string : text) {
			stringBuffer.append(_leading + string);
			stringBuffer.append("\n");
		}
		stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		return stringBuffer.toString();
	}
	
}
