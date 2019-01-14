/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.match;

import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.parser.PatternExtraction;
import mfix.core.pattern.relation.Relation;
import mfix.core.pattern.solver.Z3Solver;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class Matcher {

	public static List<Pair<MethodDeclaration, MethodDeclaration>> match(CompilationUnit src, CompilationUnit tar) {
		List<Pair<MethodDeclaration, MethodDeclaration>> matchPair = new LinkedList<>();
		MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
		methodDeclCollector.init();
		src.accept(methodDeclCollector);
		List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();
		methodDeclCollector.init();
		tar.accept(methodDeclCollector);
		List<MethodDeclaration> tarMethods = methodDeclCollector.getAllMethDecl();
		
		if(srcMethods.size() != tarMethods.size()) {
			LevelLogger.warn("Different numbers of method declarations for two source files.");
			return matchPair;
		}
		
		for(MethodDeclaration sm : srcMethods) {
			boolean noMatch = true;
			for(int i = 0; noMatch && i < tarMethods.size(); i++) {
				MethodDeclaration tm = tarMethods.get(i);
				final DiffType diff = sameSignature(sm, tm);
				switch(diff) {
				case SAME:
					matchPair.add(new Pair<MethodDeclaration, MethodDeclaration>(sm, tm));
					tarMethods.remove(tm);
					noMatch = false;
					break;
				default :
					LevelLogger.info(diff.toString());
				}
			}
			if(noMatch) {
				LevelLogger.warn("No match for method declaration : \n" + sm.toString());
				return new LinkedList<>();
			}
		}
		
		return matchPair;
	}
	
	enum DiffType{
		DIFF_MODIFIER("different modifiers"),
		DIFF_NAME("different names"),
		DIFF_RETURN("different return types"),
		DIFF_PARAM("different parameters"),
		SAME("same");
		
		private String message;
		DiffType(String msg) {
			message = msg;
		}
		
		public String toString() {return message;}
	}
	
	@SuppressWarnings("unchecked")
	private static DiffType sameSignature(MethodDeclaration sm, MethodDeclaration tm) {
		int smdf = sm.getModifiers();
		int tmdf = tm.getModifiers();
		if((smdf & tmdf) != smdf) return DiffType.DIFF_MODIFIER;
		if(!sm.getName().getFullyQualifiedName().equals(tm.getName().getFullyQualifiedName())) return DiffType.DIFF_NAME;
		String sType = sm.getReturnType2() == null ? "?" : sm.getReturnType2().toString();
		String tType = tm.getReturnType2() == null ? "?" : tm.getReturnType2().toString(); 
		if(!sType.equals(tType)) return DiffType.DIFF_RETURN; 
		List<Object> sp = sm.typeParameters();
		List<Object> tp = tm.typeParameters();
		if(sp.size() != tp.size()) return DiffType.DIFF_PARAM;
		for(int i = 0; i < sp.size(); i++){
			if(!sp.get(i).toString().equals(tp.get(i).toString()))
				return DiffType.DIFF_PARAM;
		}
		return DiffType.SAME;
	}
	
	
	static class MethodDeclCollector extends ASTVisitor {
		
		List<MethodDeclaration> methodDeclarations;
		
		public MethodDeclCollector() {
		}
		
		public void init() {
			methodDeclarations = new LinkedList<>();
		}
		
		public List<MethodDeclaration> getAllMethDecl() {
			return methodDeclarations;
		}
		
		public boolean visit(MethodDeclaration md) {
			methodDeclarations.add(md);
			return true;
		}
	}

	private static Map<Relation, Integer> mapRelation2LstIndex(List<Relation> relations) {
		Map<Relation, Integer> map = new HashMap<>();
		if (relations != null) {
			for (int i = 0; i < relations.size(); i++) {
				map.put(relations.get(i), i);
			}
		}
		return map;
	}

	public static boolean greedyMatch(MethDecl src, MethDecl tar) {
		Pattern p = PatternExtraction.extract(src, true);
		List<Relation> srcRelations = p.getOldRelations();
		p = PatternExtraction.extract(tar, true);
		List<Relation> tarRelations = p.getOldRelations();
		Map<Relation, Integer> oldR2index = mapRelation2LstIndex(srcRelations);
		Map<Relation, Integer> newR2index = mapRelation2LstIndex(tarRelations);

		int oldLen = srcRelations.size();
		int newLen = tarRelations.size();

		int[][] matrix = new int[oldLen][newLen];
		Map<String, Set<Pair<Integer, Integer>>> loc2dependencies = new HashMap<>();
		Set<Pair<Relation, Relation>> dependencies = new HashSet<>();
		Set<Pair<Integer, Integer>> set;
		for (int i = 0; i < oldLen; i++) {
			for (int j = 0; j < newLen; j++) {
				dependencies.clear();
				if (srcRelations.get(i).match(tarRelations.get(j), dependencies)) {
					matrix[i][j] = 1;
					String key = i + "_" + j;
					set = new HashSet<>();
					for (Pair<Relation, Relation> pair : dependencies) {
						set.add(new Pair<>(oldR2index.get(pair.getFirst()), newR2index.get(pair.getSecond())));
					}
					loc2dependencies.put(key, set);
				}
			}
		}
		Z3Solver solver = new Z3Solver();
		Map<Integer, Integer> oldR2newRidxMap = solver.maxOptimize(matrix, loc2dependencies);
		if(oldR2newRidxMap.size() * 2 == (srcRelations.size() + tarRelations.size())) {
			return false;
		}

		Relation l,r;
		for (Map.Entry<Integer, Integer> entry : oldR2newRidxMap.entrySet()) {
			l = srcRelations.get(entry.getKey());
			l.setMatched(true);
			r = tarRelations.get(entry.getValue());
			r.setMatched(true);
			l.getAstNode().setBindingNode(r.getAstNode());
		}
		src.postAccurateMatch(tar);
		return true;
	}

	public static Map<Integer, Integer> simMatch(List<Node> src, List<Node> tar, double similar) {
		Map<Integer, Integer> map = match(src, tar, new Comparator<Node>() {
			@Override
			public int compare(Node o1, Node o2) {
				if(o1.compare(o2)) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		
		return simMatch(map, src, tar, similar);
	}
	
	public static <T extends Node> Map<Integer, Integer> simMatch(Map<Integer, Integer> exactMatchMap, List<T> src, List<T> tar, double similar) {
		
		Map<Integer, Integer> result = new HashMap<>();
		List<Integer> left = new ArrayList<>(src.size());
		for(int i = 0; i < src.size(); i++) {
			if(!exactMatchMap.containsKey(i)) {
				left.add(i);
			}
		}
		List<Integer> right = new ArrayList<>(tar.size());
		for(int i = 0; i < tar.size(); i++) {
			if(!exactMatchMap.containsValue(i)) {
				right.add(i);
			}
		}
		
		int last = 0;
		for(int i = 0; i < left.size(); i++) {
			Object[] delTokens = src.get(left.get(i)).tokens().toArray();
			Object[] addTokens = null;
			for(int j = last; j < right.size(); j ++) {
				 addTokens = tar.get(right.get(j)).tokens().toArray();
				 Map<Integer, Integer> tmpMap = Matcher.match(delTokens, addTokens);
				 double value = ((double) tmpMap.size()) / ((double) delTokens.length);
				 if(value > similar) {
					 result.put(left.get(i), right.get(j));
					 last = j + 1;
					 break;
				 }
			}
		}
		
		return result;
	}
	
	public static Map<Integer, Integer> match(Object[] src, Object[] tar) {
		return match(Arrays.asList(src), Arrays.asList(tar), new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if(o1.equals(o2)) {
					return 1;
				} else {
					return 0;
				}
			};
		});
	}
	
	public static Map<Integer, Integer> match(List<Stmt> src, List<Stmt> tar) {
		return match(src, tar, new Comparator<Stmt>() {
			@Override
			public int compare(Stmt o1, Stmt o2) {
				if(o1.compare(o2)) {
					return 1;
				} else {
					return 0;
				}
			}
		});
	}
	
	enum Direction {
		LEFT,
		UP,
		ANDGLE
	}
	
	public static <T> Map<Integer, Integer> match(List<T> src, List<T> tar, Comparator<T> comparator) {
		Map<Integer, Integer> map = new HashMap<>();
		int srcLen = src.size();
		int tarLen = tar.size();
		if(srcLen == 0 || tarLen == 0) {
			return map;
		}
		int[][] score = new int[srcLen + 1][tarLen + 1];

		// LCS matching with path retrieval
		Direction[][] path = new Direction[srcLen + 1][tarLen + 1];
		for(int i = 0; i < srcLen; i++){
			for(int j = 0; j < tarLen; j++){
				if(comparator.compare(src.get(i), tar.get(j)) > 0){
					score[i + 1][j + 1] = score[i][j] + 1;
					path[i + 1][j + 1] = Direction.ANDGLE;
				} else {
					int left = score[i + 1][j];
					int up = score[i][j + 1];
					if(left >= up) {
						score[i + 1][j + 1] = left;
						path[i + 1][j + 1] = Direction.LEFT;
					} else {
						score[i + 1][j + 1] = up;
						path[i + 1][j + 1] = Direction.UP;
					}
				}
			}
		}
		
		for(int i = srcLen, j = tarLen; i > 0 && j > 0;) {
			switch(path[i][j]){
			case ANDGLE:
				map.put(i-1, j-1);
				i --;
				j --;
				break;
			case LEFT:
				j --;
				break;
			case UP:
				i --;
				break;
			default:
				LevelLogger.error("should not happen!");
				System.exit(0);
			}
		}
		
		assert map.size() == score[srcLen][tarLen];
		return map;
	}

}
