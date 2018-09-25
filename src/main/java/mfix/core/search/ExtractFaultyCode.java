/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.util.JavaFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class ExtractFaultyCode {

    /**
     * @see MethodDeclaration extractFaultyMethod(final CompilationUnit unit, final int line)
     * @param file
     * @param line
     * @return
     */
    public static MethodDeclaration extractFaultyMethod(final String file, final int line) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        return extractFaultyMethod(unit, line);
    }

    /**
     * @see ASTNode extractMinimalASTNode(final CompilationUnit unit, final int line)
     * @param file
     * @param line
     * @return
     */
    public static ASTNode extractMinimalASTNode(final String file, final int line) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        return extractMinimalASTNode(unit, line);
    }

    /**
     * extract faulty method based on given line number
     * @param unit : {@code CompilationUnit}
     * @param line : line number
     * @return method declaration including the given line number or null if not found
     */
    public static MethodDeclaration extractFaultyMethod(final CompilationUnit unit, final int line) {
        final List<MethodDeclaration> methods = new ArrayList<>(1);
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                int start = unit.getLineNumber(node.getStartPosition());
                int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
                if(start <= line && line <= end) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });
        return methods.isEmpty() ? null : methods.get(0);
    }

    /**
     * extract the faulty statement for the given line number
     * @param unit : {@code CompilationUnit}
     * @param line : line number
     * @return statement at the given line or null if not found
     */
    public static ASTNode extractMinimalASTNode(final CompilationUnit unit, final int line) {
        final List<ASTNode> nodes = new ArrayList<>(1);
        FindNode findNode = new FindNode(unit, line);
        unit.accept(findNode);
        return findNode.getNode();
    }
}


class FindNode extends ASTVisitor {

    private ASTNode _node = null;
    private final CompilationUnit _cunit;
    private final int _line;

    public FindNode(CompilationUnit unit, int line) {
        _cunit = unit;
        _line = line;
    }

    public ASTNode getNode() {
        return _node;
    }

    public boolean visit(AssertStatement node) {
        return process(node);
    }

    public boolean visit(BreakStatement node) {
        return process(node);
    }

    public boolean visit(Block node) {
        process(node);
        return true;
    }

    public boolean visit(ConstructorInvocation node) {
        return process(node);
    }

    public boolean visit(ContinueStatement node) {
        return process(node);
    }

    public boolean visit(DoStatement node) {
        process(node);
        return true;
    }

    public boolean visit(EmptyStatement node) {
        return process(node);
    }

    public boolean visit(EnhancedForStatement node) {
        return process(node);
    }

    public boolean visit(ExpressionStatement node) {
        return process(node);
    }

    public boolean visit(ForStatement node) {
        return process(node);
    }

    public boolean visit(IfStatement node) {
        return process(node);
    }

    public boolean visit(LabeledStatement node) {
        return process(node);
    }

    public boolean visit(ReturnStatement node) {
        return process(node);
    }

    public boolean visit(SuperConstructorInvocation node) {
        return process(node);
    }

    public boolean visit(SwitchCase node) {
        return process(node);
    }

    public boolean visit(SwitchStatement node) {
        return process(node);
    }

    public boolean visit(SynchronizedStatement node) {
        process(node);
        return true;
    }

    public boolean visit(ThrowStatement node) {
        return process(node);
    }

    public boolean visit(TryStatement node) {
        process(node);
        return true;
    }

    public boolean visit(CatchClause node) {
        process(node);
        return true;
    }

    public boolean visit(TypeDeclarationStatement node) {
        process(node);
        return true;
    }

    public boolean visit(VariableDeclarationStatement node) {
        return process(node);
    }

    public boolean visit(WhileStatement node) {
        process(node);
        return true;
    }

    private boolean process(ASTNode node) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        if(start <= _line && _line <= end){
            _node = node;
            return false;
        }
        return true;
    }
}