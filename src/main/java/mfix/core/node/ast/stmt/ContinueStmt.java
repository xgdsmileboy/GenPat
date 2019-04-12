/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
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
public class ContinueStmt extends Stmt {

    private static final long serialVersionUID = -4634975771051671527L;
    private SName _identifier = null;

    /**
     * ContinueStatement:
     * continue [ Identifier ] ;
     */
    public ContinueStmt(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ContinueStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.CONTINUE;
        _fIndex = VIndex.STMT_CONTINUE;
    }

    public void setIdentifier(SName identifier) {
        _identifier = identifier;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("continue");
        if (_identifier != null) {
            stringBuffer.append(" ");
            stringBuffer.append(_identifier.toSrcString());
        }
        stringBuffer.append(";");
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (isAbstract() && !isConsidered()) return null;
        StringBuffer identifier = _identifier == null ? null : _identifier.formalForm(nameMapping, isConsidered(),
				keywords);
        if (identifier == null) {
            if (isConsidered()) {
                return new StringBuffer("continue ")
                        .append(_identifier == null ? "" : nameMapping.getExprID(_identifier)).append(';');
            } else {
                return null;
            }
        }
        return new StringBuffer("continue ").append(identifier).append(';');
    }

    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("continue");
        if (_identifier != null) {
            _tokens.addAll(_identifier.tokens());
        }
        _tokens.add(";");
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(1);
        if (_identifier != null) {
            children.add(_identifier);
        }
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof ContinueStmt) {
            ContinueStmt continueStmt = (ContinueStmt) other;
            match = (_identifier == null) ? (continueStmt._identifier == null)
                    : _identifier.compare(continueStmt._identifier);
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _selfFVector.inc(FVector.KEY_CONTINUE);

        _completeFVector = new FVector();
        _completeFVector.inc(FVector.KEY_CONTINUE);
        if (_identifier != null) {
            _completeFVector.combineFeature(_identifier.getFeatureVector());
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        ContinueStmt continueStmt = null;
        if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
            continueStmt = (ContinueStmt) getBindingNode();
            match = false;
        } else if (canBinding(node)) {
            continueStmt = (ContinueStmt) node;
            setBindingNode(node);
            match = true;
        }
        if (continueStmt != null && _identifier != null) {
            _identifier.postAccurateMatch(continueStmt._identifier);
        }
        return match;
    }

    @Override
    public boolean genModifications() {
        if (super.genModifications()) {
            ContinueStmt continueStmt = (ContinueStmt) getBindingNode();
            if (_identifier == null) {
                if (continueStmt._identifier != null) {
                    Update update = new Update(this, _identifier, continueStmt._identifier);
                    _modifications.add(update);
                }
            } else if (_identifier.getBindingNode() != continueStmt._identifier) {
                Update update = new Update(this, _identifier, continueStmt._identifier);
                _modifications.add(update);
            } else {
                _identifier.genModifications();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
        if (node instanceof ContinueStmt) {
            ContinueStmt continueStmt = (ContinueStmt) node;
            if (super.ifMatch(node, matchedNode, matchedStrings, level)) {
                if (_identifier != null && continueStmt._identifier != null) {
                    matchedNode.put(_identifier, continueStmt._identifier);
                    matchedStrings.put(_identifier.toString(), continueStmt._identifier.toString());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer("continue");
            if (_identifier != null) {
                stringBuffer.append(" ");
                StringBuffer tmp = _identifier.transfer(vars, exprMap, retType, exceptions);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
            stringBuffer.append(";");
            return stringBuffer;
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
        StringBuffer identifier = null;
        Node pnode = NodeUtils.checkModification(this);
        if (pnode != null) {
            ContinueStmt continueStmt = (ContinueStmt) pnode;
            for (Modification modification : continueStmt.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    if (update.getSrcNode() == continueStmt._identifier) {
                        identifier = update.apply(vars, exprMap, retType, exceptions);
                        if (identifier == null) return null;
                    } else {
                        LevelLogger.error("@ContinueStmt ERROR");
                    }
                } else {
                    LevelLogger.error("@ContinueStmt Should not be this kind of modification : " + modification);
                }
            }
        }
        StringBuffer stringBuffer = new StringBuffer("continue");
        if (identifier == null) {
            if (_identifier != null) {
                stringBuffer.append(" ");
                StringBuffer tmp = _identifier.adaptModifications(vars, exprMap, retType, exceptions);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
        } else {
            stringBuffer.append(" " + identifier);
        }
        stringBuffer.append(";");
        return stringBuffer;

    }
}
