/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.pattern.cluster.NameMapping;
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
public class Comment extends Expr {

    private static final long serialVersionUID = -6085082564199071574L;
    private String _comment;

    /**
     * Annotation:
     * NormalAnnotation
     * MarkerAnnotation
     * SingleMemberAnnotation
     */
    public Comment(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.COMMENT;
        _comment = node.toString();
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_comment);
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        return null;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(_comment);
    }

    @Override
    public boolean compare(Node other) {
        if (other instanceof Comment) {
            return _comment.equals(((Comment) other)._comment);
        }
        return false;
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _completeFVector = new FVector();
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
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
        return toSrcString();
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
        return toSrcString();
    }
}
