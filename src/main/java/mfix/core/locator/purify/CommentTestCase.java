/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator.purify;

import mfix.common.util.JavaFile;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-19
 */
public class CommentTestCase {
	
	public static void comment(String fileBasePath, List<String> testcases, Set<String> avoid){
		Map<String, Set<String>> clazzAndMethods = new HashMap<>();
		for(String test : testcases){
			if(avoid.contains(test)){
				continue;
			}
			String[] testInfo = test.split("::");
			if(testInfo.length != 2){
				System.err.println("Test case format error : " + test);
				continue;
			}
			Set<String> methods = clazzAndMethods.get(testInfo[0]);
			if(methods == null){
				methods = new HashSet<>();
			}
			methods.add(testInfo[1]);
			clazzAndMethods.put(testInfo[0], methods);
		}
		
		for(Entry<String, Set<String>> entry : clazzAndMethods.entrySet()){
			String fileName = fileBasePath + "/" + entry.getKey().replace(".", "/") + ".java";
			CompilationUnit cUnit = JavaFile.genASTFromFileWithType(fileName);
			CommentTestCaseVisitor visitor = new CommentTestCaseVisitor(entry.getValue());
			cUnit.accept(visitor);
			JavaFile.writeStringToFile(fileName, cUnit.toString());
		}
	}
	
	private static class CommentTestCaseVisitor extends ASTVisitor {

		private Set<String> _testsToBeCommented = null;
		
		public CommentTestCaseVisitor(Set<String> testcases) {
			_testsToBeCommented = testcases;
		}
		
		
		@Override
		public boolean visit(MethodDeclaration node) {
			String name = node.getName().getFullyQualifiedName();
			if(_testsToBeCommented.contains(name)){
				Block emptyBody = node.getAST().newBlock();
				node.setBody(emptyBody);
				for(int i = 0; i < node.modifiers().size(); i++){
					if(node.modifiers().get(i) instanceof NormalAnnotation){
						NormalAnnotation normalAnnotation = (NormalAnnotation) node.modifiers().get(i);
						AST ast = node.getAST();
						MarkerAnnotation annotation = ast.newMarkerAnnotation();
						annotation.setTypeName((Name) ASTNode.copySubtree(ast, normalAnnotation.getTypeName()));
						node.modifiers().set(i, annotation);
					} else if(node.modifiers().get(i) instanceof SingleMemberAnnotation){
						SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) node.modifiers().get(i);
						AST ast = node.getAST();
						MarkerAnnotation annotation = ast.newMarkerAnnotation();
						annotation.setTypeName((Name) ASTNode.copySubtree(ast, singleMemberAnnotation.getTypeName()));
						node.modifiers().set(i, annotation);
					}
				}
			}
			return true;
		}
	}
}
