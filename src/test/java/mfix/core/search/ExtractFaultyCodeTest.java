/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.java.D4jSubject;
import mfix.common.conf.Constant;
import mfix.common.util.Utils;
import mfix.TestCase;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class ExtractFaultyCodeTest extends TestCase {

    @Test
    public void test_lang_1() {
        D4jSubject subject = new D4jSubject(testbase, "lang", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        for (Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            MethodDeclaration md = ExtractFaultyCode.extractFaultyMethod(file, location.getLine());
            Assert.assertTrue(md.getName().getIdentifier().equals("createNumber"));
        }
    }

    @Test
    public void test_lang_1_minimal() {
        D4jSubject subject = new D4jSubject(testbase, "lang", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        for (Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            ASTNode node = ExtractFaultyCode.extractMinimalASTNode(file, location.getLine());
            Assert.assertTrue(node instanceof IfStatement);
        }
    }

    @Test
    public void test_chart_1_minimal() {
        D4jSubject subject = new D4jSubject(testbase, "chart", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        for (Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome(), subject.getSsrc(), location.getRelClazzFile());
            ASTNode node = ExtractFaultyCode.extractMinimalASTNode(file, location.getLine());
            Assert.assertTrue("The faulty location is an if statement.", node instanceof IfStatement);
        }
    }

}
