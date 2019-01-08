/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.utils;

import mfix.common.util.JavaFile;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.match.metric.FVector.ALGO;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NodeUtils {

	public static Set<String> IGNORE_METHOD_INVOKE = new HashSet<String>(Arrays.asList("toString","equals","hashCode"));
	
	public static boolean matchNode(Node sketch, Node candidate) {
//		FVector fVector = sketch.getParentStmt().getFeatureVector();
//		FVector otherVector = candidate.getParentStmt().getFeatureVector();
		FVector fVector = sketch.getParent().getFeatureVector();
		FVector otherVector = candidate.getParent().getFeatureVector();
		if(fVector.computeSimilarity(otherVector, ALGO.COSINE) > 0.8 && fVector.computeSimilarity(otherVector, ALGO.NORM_2) < 0.5) {
			return true;
		}
//		Map<String, Set<Node>> map = sketch.getCalledMethods();
//		Map<String, Set<Node>> thisKeys = candidate.getCalledMethods();
//		for(Entry<String, Set<Node>> entry : map.entrySet()) {
//			if(!thisKeys.containsKey(entry.getKey())) {
//				return false;
//			}
//		}
		System.out.println("----Similarity filter");
		return false;
	}
	

	public static Type parseExprType(Expr left, String operator, Expr right){
		if(left == null){
			return parsePreExprType(right, operator);
		}
		
		if(right == null){
			return parsePostExprType(left, operator);
		}
		
		AST ast = AST.newAST(AST.JLS8);
		switch(operator){
		case "*":
		case "/":
		case "+":
		case "-":
			Type type = union(left.getType(), right.getType());
			if(type == null){
				type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
			}
			return type;
		case "%":
		case "<<":
		case ">>":
		case ">>>":
		case "^":
		case "&":
		case "|":
			return ast.newPrimitiveType(PrimitiveType.INT);
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "==":
		case "!=":
		case "&&":
		case "||":
			return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		default :
			return null;
		}
	}
	
	private static Type union(Type ty1, Type ty2){
		if(ty1 == null){
			return ty2;
		} else if(ty2 == null){
			return ty1;
		}
		
		if(!ty1.isPrimitiveType() || !ty2.isPrimitiveType()){
			return null;
		}
		
		String ty1String = ty1.toString().toLowerCase().replace("integer", "int");
		String ty2String = ty2.toString().toLowerCase().replace("integer", "int");
		
		AST ast = AST.newAST(AST.JLS8);
		if(ty1String.equals("double") || ty2String.equals("double")){
			
			return ast.newPrimitiveType(PrimitiveType.DOUBLE);
			
		} else if(ty1String.equals("float") || ty2String.equals("float")){
			
			return ast.newPrimitiveType(PrimitiveType.FLOAT);
			
		} else if(ty1String.equals("long") || ty2String.equals("long")){
			
			return ast.newPrimitiveType(PrimitiveType.LONG);
			
		} else if(ty1String.equals("int") || ty2String.equals("int")){
			
			return ast.newPrimitiveType(PrimitiveType.INT);
			
		} else if(ty1String.equals("short") || ty2String.equals("short")){
			
			return ast.newPrimitiveType(PrimitiveType.SHORT);
			
		} else {
			
			return ast.newPrimitiveType(PrimitiveType.BYTE);
			
		}
		
	}
	
	private static Type parsePostExprType(Expr expr, String operator){
		// ++/--
		AST ast = AST.newAST(AST.JLS8);
		return ast.newPrimitiveType(PrimitiveType.INT);
	}
	
	private static Type parsePreExprType(Expr expr, String operator){
		AST ast = AST.newAST(AST.JLS8);
		switch(operator){
		case "++":
		case "--":
			return ast.newPrimitiveType(PrimitiveType.INT);
		case "+":
		case "-":
			return expr.getType();
		case "~":
		case "!":
			return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		default :
			return null;
		}
	}
	
	public static Map<Integer, Set<String>> getUsableVarTypes(String file){
		CompilationUnit unit = JavaFile.genAST(file);
		VariableVisitor variableVisitor = new VariableVisitor(unit);
		unit.accept(variableVisitor);
		return variableVisitor.getVars();
	}
	
}

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
class VariableVisitor extends ASTVisitor {
	private MethodVisitor _methodVisitor = new MethodVisitor();
	private Map<Integer, Set<String>> _vars = new HashMap<>();
	private Set<String> _fields = new HashSet<>();
	private CompilationUnit _unit;
	
	public VariableVisitor(CompilationUnit unit) {
		_unit = unit;
	}
	
	public boolean visit(FieldDeclaration node) {
		for(Object object: node.fragments()){
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
			_fields.add(vdf.getName().toString());
		}
		return true;
	}
	
	public Map<Integer, Set<String>> getVars(){
		for(Entry<Integer, Set<String>> entry : _vars.entrySet()){
			entry.getValue().addAll(_fields);
		}
		return _vars;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		int start = _unit.getLineNumber(node.getStartPosition());
		Set<String> localVars = new HashSet<>();
		for(Object object : node.parameters()){
			SingleVariableDeclaration svd = (SingleVariableDeclaration) object;
			localVars.add(svd.getName().toString());
		}
			
		if(node.getBody() != null){
			_methodVisitor.reset();
			node.getBody().accept(_methodVisitor);
			localVars.addAll(_methodVisitor.getVars());
		}
		_vars.put(start, localVars);
		return true;
	}
	
	class MethodVisitor extends ASTVisitor {

		private Set<String> vars = new HashSet<>();
		
		public void reset() {
			vars.clear();
		}
		
		public Set<String> getVars() {
			return vars;
		}
		
		public boolean visit(VariableDeclarationStatement node) {
			for (Object o : node.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
				vars.add(vdf.getName().getFullyQualifiedName());
			}
			return true;
		}

		public boolean visit(VariableDeclarationExpression node) {
			for (Object o : node.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
				vars.add(vdf.getName().getFullyQualifiedName());
			}
			return true;
		}
		
		public boolean visit(SingleVariableDeclaration node){
			vars.add(node.getName().getFullyQualifiedName());
			return true;
		}
		
	}
	
}
