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
        for(Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());
            Pattern pattern = PatternExtraction.extract(srcNode, tarNode);
            if(pattern.getNewRelations().size() != pattern.getOldRelations().size()) {
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

}
