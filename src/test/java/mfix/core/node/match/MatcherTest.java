/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.TestCase;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class MatcherTest extends TestCase {

    @Test
    public void test_match_dont() {
        String srcFile_change_retType = testbase + Constant.SEP + "src_Intersect.java";
        String tarFile_change_retType = testbase + Constant.SEP + "tar_Intersect.java";
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile_change_retType);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile_change_retType);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // The method signature cannot match
        // TODO: should permit some methods failing to match
        Assert.assertTrue(matchMap.isEmpty());
    }

    @Test
    public void test_match_do() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // all methods are well matched
        Assert.assertTrue(matchMap.size() == 108);
    }

    @Test
    public void test_match_demo() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        Set<Node> patterns = new HashSet<>();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());

            if(srcNode.toSrcString().toString().equals(tarNode.toSrcString().toString())) {
                continue;
            }

            if(Matcher.greedyMatch((MethDecl) srcNode, (MethDecl) tarNode)) {
                patterns.add(srcNode);
            }
        }

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";
        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

    }
}
