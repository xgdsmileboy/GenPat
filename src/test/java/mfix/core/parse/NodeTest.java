/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse;

import mfix.common.java.D4jSubject;
import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.TestCase;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.parse.node.Node;
import mfix.core.search.ExtractFaultyCode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.util.List;

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
        for(Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(unit, location.getLine());
            NodeParser parser = NodeParser.getInstance();
            parser.setCompilationUnit(unit);
            Node node = parser.process(method);
            System.out.println(node.toSrcString().toString());
            List<String> tokens = node.tokens();
            for(String string : tokens) {
                System.out.println(string);
            }
        }
    }
}
