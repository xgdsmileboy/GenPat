/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.parser;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.TestCase;
import mfix.core.node.parser.NodeParser;
import mfix.core.node.match.Matcher;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class NodeParserTest extends TestCase {

    @Test
    public void test_parse() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = new Matcher().match(srcUnit, tarUnit);
        NodeParser nodePaser = new NodeParser();
        int modifyLocs = 0;
        for(Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodePaser.setCompilationUnit(srcFile, srcUnit);
            String src = nodePaser.process(pair.getFirst()).toSrcString().toString();
            nodePaser.setCompilationUnit(tarFile, tarUnit);
            String tar = nodePaser.process(pair.getSecond()).toSrcString().toString();
            if(!src.equals(tar)) {
                modifyLocs ++;
            }
        }
        Assert.assertTrue(modifyLocs == 7);
    }

}
