/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;

import mfix.common.util.JavaFile;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/19
 */
public class Main {

    public static void main(String[] args) {

//        CompilationUnit unit = JavaFile.genAST(System.getProperty("user.dir") + "/Pair.java");
        CompilationUnit unit = JavaFile.genASTFromFileWithType(System.getProperty("user.dir") + "/Path.java", null);
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
//                System.out.println(node.resolveMethodBinding());
//                System.out.println(node.getName().getFullyQualifiedName());
                return true;
            }

            public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
                List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
                for(VariableDeclarationFragment vdf : fragments) {
                    System.out.println(vdf.resolveBinding().getType().getQualifiedName());
                }
                return true;
            }

            public boolean visit(SingleVariableDeclaration svd) {
                System.out.println(svd.resolveBinding().getType().getQualifiedName());
                return true;
            }

            public boolean visit(FieldDeclaration fieldDeclaration) {
                List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
                for(VariableDeclarationFragment vdf : fragments) {
                    System.out.println(vdf.resolveBinding().getType().getQualifiedName());
                }
                return true;
            }

        });

    }

}
