/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.common.java.JavaFile;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/20
 */
public class JavaFileTest {


    @Test
    public void test_generateAST() {
        String file = System.getProperty("user.dir") + "/resources/forTest/CGMonitor.java";
        CompilationUnit unit = JavaFile.genAST(file);
        Assert.assertNotNull(unit);

        final Set<String> md = new HashSet<String>();
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                md.add(node.getName().getFullyQualifiedName());
                return true;
            }
        });

        Assert.assertEquals(8, md.size());
        Assert.assertTrue(md.contains("printPrefix"));
    }


}
