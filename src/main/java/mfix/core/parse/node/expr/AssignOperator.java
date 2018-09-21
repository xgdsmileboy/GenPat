package mfix.core.parse.node.expr;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;

import mfix.core.parse.node.Node;

public class AssignOperator extends Operator {

	private Assignment.Operator _operator;
	
	public AssignOperator(int startLine, int endLine, ASTNode oriNode) {
		super(startLine, endLine, oriNode);
		_nodeType = TYPE.ASSIGNOPERATOR;
	}
	
	public void setOperator(Assignment.Operator operator) {
		this._operator = operator;
	}
	
	public Assignment.Operator getOperator() {
		return _operator;
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof AssignOperator) {
			return _operator.toString().equals(((AssignOperator) other)._operator.toString());
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(_operator.toString());
		if(result != null) {
			return new StringBuffer(result);
		} else {
			return toSrcString();
		}
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof AssignOperator && _operator.toString().equals(((AssignOperator) other)._operator.toString())) {
			_matchNodeType = true;
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof AssignOperator) {
			match = true;
			AssignOperator assignOperator = (AssignOperator) sketch;
			assignOperator._binding = this;
			_binding = assignOperator;
		}
		return match;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operator.toString());
	}

}
