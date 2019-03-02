/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.ast;

import mfix.common.java.D4jSubject;
import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.TestCase;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import mfix.core.pattern.cluster.Vector;
import mfix.core.search.ExtractFaultyCode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/26
 */
public class NodeTest extends TestCase {

    @Test
    public void test_keywords() {
        D4jSubject subject = new D4jSubject(testbase, "chart", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        for (Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(unit, location.getLine());
            NodeParser parser = new NodeParser();
            parser.setCompilationUnit(file, unit);
            Node node = parser.process(method);
            List<String> tokens = node.tokens();
            Assert.assertTrue(tokens.size() == 193);
        }
    }

    @Test
    public void test_serialization_1() throws IOException, ClassNotFoundException {
        D4jSubject subject = new D4jSubject(testbase, "chart", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        Assert.assertTrue(locations.size() > 0);
        Location location = locations.get(0);
        String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(unit, location.getLine());
        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(file, unit);
        Node node = parser.process(method);
        Utils.serialize(node, "/tmp/node");
        Node recoverNode = (Node) Utils.deserialize("/tmp/node");
        Assert.assertEquals("", node.toString(), recoverNode.toString());
    }

    @Test
    public void test_node_vector() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        // there is only one method changed
        Assert.assertTrue(patterns.size() == 1);

        Pattern pattern = patterns.iterator().next();

        Vector vector = pattern.getPatternVector();
        System.out.println(vector.toString());

    }
}
