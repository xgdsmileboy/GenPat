/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.TestCase;
import mfix.core.parse.node.Node;
import mfix.core.parse.relation.Pattern;
import mfix.core.parse.relation.Relation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/7
 */
public class PatternExtractionTest extends TestCase {

    @Test
    public void test_parse() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        Set<String> changedMethod = new HashSet<>();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());
            Pattern pattern = PatternExtraction.extract(srcNode, tarNode);
            if (pattern != null) {
                changedMethod.add(pair.getFirst().getName().getFullyQualifiedName());
            }
        }

        Assert.assertTrue(changedMethod.contains("fireBuildStarted"));
        Assert.assertTrue(changedMethod.contains("fireBuildFinished"));
        Assert.assertTrue(changedMethod.contains("fireTargetStarted"));
        Assert.assertTrue(changedMethod.contains("fireTargetFinished"));
        Assert.assertTrue(changedMethod.contains("fireTaskStarted"));
        Assert.assertTrue(changedMethod.contains("fireTaskFinished"));
        Assert.assertTrue(changedMethod.contains("fireMessageLoggedEvent"));
    }

    @Test
    public void test_relation() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            if (pair.getFirst().getName().getIdentifier().equals("fireBuildStarted")) {
                nodeParser.setCompilationUnit(srcFile, srcUnit);
                Node srcNode = nodeParser.process(pair.getFirst());
                nodeParser.setCompilationUnit(tarFile, tarUnit);
                Node tarNode = nodeParser.process(pair.getSecond());
                Pattern pattern = PatternExtraction.extract(srcNode, tarNode);
                List<Relation> relations = pattern.getOldRelations();
                // totally, there should be 31 relations
                Assert.assertTrue(relations.size() == 31);
                // the first one should be the method structure
                Assert.assertTrue(relations.get(0).getRelationKind() == Relation.RelationKind.STRUCTURE);

                //the new relations should be the same since this method was not modified
                relations = pattern.getNewRelations();
                Assert.assertTrue(relations.size() == 33);
                Assert.assertTrue(relations.get(0).getRelationKind() == Relation.RelationKind.STRUCTURE);
                break;
            }
        }

    }

    @Test
    public void test_minimization() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            if (pair.getFirst().getName().getIdentifier().equals("fireBuildStarted")) {
                nodeParser.setCompilationUnit(srcFile, srcUnit);
                Node srcNode = nodeParser.process(pair.getFirst());
                nodeParser.setCompilationUnit(tarFile, tarUnit);
                Node tarNode = nodeParser.process(pair.getSecond());
                Pattern pattern = PatternExtraction.extract(srcNode, tarNode);

                pattern.minimize(0);
                Assert.assertTrue(pattern.getMinimizedOldRelations(false).size() == 1);
                Assert.assertTrue(pattern.getMinimizedNewRelations(false).size() == 3);
                Assert.assertTrue(pattern.getMinimizedOldRelations(true).size() == 1);
                Assert.assertTrue(pattern.getMinimizedNewRelations(true).size() == 3);

                pattern.minimize(1, 100, true);
                // the minimal changed relations should be unchanged regardless of the expansion
                Assert.assertTrue(pattern.getMinimizedOldRelations(false).size() == 1);
                Assert.assertTrue(pattern.getMinimizedNewRelations(false).size() == 3);
                // two more concerned relations after expanding one level
                // "<" operation and "listeners.size()" method invocation
                Assert.assertTrue(pattern.getMinimizedOldRelations(true).size() == 1 + 2);
                Assert.assertTrue(pattern.getMinimizedNewRelations(true).size() == 3 + 2);

                pattern.minimize(2, 100, true);
                Assert.assertTrue(pattern.getMinimizedOldRelations(false).size() == 1);
                Assert.assertTrue(pattern.getMinimizedNewRelations(false).size() == 3);
                // six more concerned relations after expanding two level
                Assert.assertTrue(pattern.getMinimizedOldRelations(true).size() == 1 + 6);
                Assert.assertTrue(pattern.getMinimizedNewRelations(true).size() == 3 + 6);

                pattern.minimize(2, 10, true);
                Assert.assertTrue(pattern.getMinimizedOldRelations(false).size() == 1);
                Assert.assertTrue(pattern.getMinimizedNewRelations(false).size() == 3);
                // two more concerned relations even expand level is 2,
                // since the max relation number is no more than 10.
                Assert.assertTrue(pattern.getMinimizedOldRelations(true).size() == 1 + 2);
                Assert.assertTrue(pattern.getMinimizedNewRelations(true).size() == 3 + 2);

            }
        }

    }

}
