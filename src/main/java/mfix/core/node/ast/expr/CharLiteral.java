/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class CharLiteral extends Expr {

    private static final long serialVersionUID = 995719993109521913L;
    private char _value = ' ';
    private String _valStr = null;

    /**
     * Character literal nodes.
     */
    public CharLiteral(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CLITERAL;
    }

    public void setValue(char value) {
        _value = value;
        _valStr = "" + _value;
        _valStr = _valStr.replace("\\", "\\\\").replace("\'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\b", "\\b").replace("\t", "\\t").replace("\r", "\\r").replace("\f", "\\f")
                .replace("\0", "\\0");
    }

    public String getStringValue() {
        return toString().toString();
    }

    public char getValue() {
        return _value;
    }

    @Override
    public StringBuffer toSrcString() {
        return new StringBuffer("\'" + _valStr + "\'");
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("\'" + _valStr + "\'");
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof CharLiteral) {
            CharLiteral charLiteral = (CharLiteral) other;
            match = (_value == charLiteral._value);
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.E_CHAR);
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        if (getBindingNode() == node) return true;
        if (getBindingNode() == null && canBinding(node)) {
            setBindingNode(node);
            return true;
        }
        return false;
    }

    @Override
    public boolean genModidications() {
        if (super.genModidications()) {
            CharLiteral charLiteral = (CharLiteral) getBindingNode();
            if (getValue() != charLiteral.getValue()) {
                Update update = new Update(getParent(), this, charLiteral);
                _modifications.add(update);
            }
        }
        return true;
    }

    @Override
    public StringBuffer transfer() {
        StringBuffer stringBuffer = super.transfer();
        if (stringBuffer == null) {
            stringBuffer = toSrcString();
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications() {
        Node node = checkModification();
        if (node != null) {
            return ((Update) node.getModifications().get(0)).apply();
        }
        return toSrcString();
    }
}
