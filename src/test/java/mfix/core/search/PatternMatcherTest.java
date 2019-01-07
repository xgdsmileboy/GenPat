/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.TestCase;
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeParser;
import mfix.core.parse.Pattern;
import mfix.core.parse.PatternExtraction;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018-12-25
 */
public class PatternMatcherTest extends TestCase {

    @Test
    public void test_search_demo() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        Set<Pattern> patterns = new HashSet<>();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());
            Pattern pattern = PatternExtraction.extract(srcNode, tarNode);
            if (pattern != null) {
                pattern.minimize(1, 50);
                if(!pattern.getMinimizedOldRelations(true).isEmpty()) {
                    pattern.doAbstraction();
                    patterns.add(pattern);
                }
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

        nodeParser.setCompilationUnit(buggy, unit);
        for(MethodDeclaration m : methods) {
            Node node = nodeParser.process(m);
            Pattern bp = PatternExtraction.extract(node, true);
            Set<Pattern> matched = PatternMatcher.filter(bp, patterns);
            for(Pattern p : matched) {
                p.foldMatching(bp, new HashMap<>());
            }
        }

    }

    @Test
    public void test_search_add_try_example() {
        String srcFile = testbase + Constant.SEP + "examples" + Constant.SEP + "src_Track.java";
        String tarFile = testbase + Constant.SEP + "examples" + Constant.SEP + "tar_Track.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        Set<Pattern> patterns = new HashSet<>();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());
            Pattern pattern = PatternExtraction.extract(srcNode, tarNode);
            if (pattern != null) {
                pattern.minimize(1, 50);
                if(!pattern.getMinimizedOldRelations(true).isEmpty()) {
                    pattern.doAbstraction();
                    patterns.add(pattern);
                }
            }
        }

        String buggy = testbase + Constant.SEP + "buggy_RtcpReceivedEvent.java";
        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        nodeParser.setCompilationUnit(buggy, unit);
        for(MethodDeclaration m : methods) {
            Node node = nodeParser.process(m);
            Pattern bp = PatternExtraction.extract(node, true);
            Set<Pattern> matched = PatternMatcher.filter(bp, patterns);
            System.out.println(matched.size());
        }

    }

}
