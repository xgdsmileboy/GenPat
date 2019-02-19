/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class CreationRef extends Expr {

    private static final long serialVersionUID = 6237635456129751926L;

    private String _str;

    /**
     * CreationReference:
     *      Type ::
     *          [ < Type { , Type } > ]
     *      new
     */
    public CreationRef(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _str = node.toString();
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_str);
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered) {
        return null;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(_str);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof CreationRef) {
            CreationRef creationRef = (CreationRef) other;
            match = _str.equals(creationRef.toString());
        }
        return match;
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
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
    public boolean genModifications() {
        return true;
    }

    @Override
    public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = toSrcString();
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
        return toSrcString();
    }
}
