/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.java.D4jSubject;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.TestCase;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.node.parser.NodeParser;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/26
 */
public class SimMethodSearchTest extends TestCase {

    @Test
    public void test_chart_1() {
        D4jSubject subject = new D4jSubject(testbase, "chart", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        List<String> files = JavaFile.ergodic(subject.getHome() + subject.getSsrc(), new LinkedList<String>());
        for(Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(file, location.getLine());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            NodeParser parser = new NodeParser();
            parser.setCompilationUnit(file, unit);
            Node fnode = parser.process(method);
            for(String f : files) {
                unit = JavaFile.genASTFromFileWithType(f);
                Map<Node, Pair<Double, Double>> nodes = SimMethodSearch.searchSimMethod(unit, fnode,0.95);
                Assert.assertTrue(nodes.isEmpty() || nodes.size() == 1);
//                for(Map.Entry<Node, Pair<Double, Double>> entry: nodes.entrySet()) {
//                    System.out.println("DIST : " + entry.getValue().getFirst() + "\tANGLE : " + entry.getValue().getSecond());
//                    System.out.println(entry);
//                }
            }

        }
    }

}
