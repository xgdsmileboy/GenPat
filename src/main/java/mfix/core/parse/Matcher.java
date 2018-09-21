/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.comp.Deletion;
import mfix.core.comp.Insertion;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.stmt.Blk;
import mfix.core.parse.node.stmt.Stmt;
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
 * @author Jiajun
 * @date Oct 9, 2017
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
	
	static enum DiffType{
		DIFF_MODIFIER("different modifiers"),
		DIFF_NAME("different names"),
		DIFF_RETURN("different return types"),
		DIFF_PARAM("different parameters"),
		SAME("same");
		
		private String message;
		private DiffType(String msg) {
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

	public static boolean applyStmtList(List<Modification> modifications, List<? extends Node> statements, Node currentNode, Map<String, String> exprMap,
			Map<Node, List<StringBuffer>> insertionPositionMap, Map<Node, StringBuffer> changeNodeMap, Set<String> allUsableVars) {
		StringBuffer tmp = null;
		for (Modification modification : modifications) {
			if (modification instanceof Update) {
				Update update = (Update) modification;
				Node node = update.getSrcNode().getBinding();
				assert node != null;
				// map current node to the updated node string
				tmp = update.getTarString(exprMap, allUsableVars);
				if(tmp == null) return false;
				changeNodeMap.put(node, tmp);
			} else if(modification instanceof Deletion) {
				Deletion deletion = (Deletion) modification;
				Node node = deletion.getSrcNode().getBinding();
				// node to be deleted to should be completely matched
				if(!node.isAllBinding(Constant.INGORE_OPERATOR_FOR_DELETE_MATCH)) {
					return false;
				}
				assert node != null;
				// map deleted node to null
				changeNodeMap.put(node, null);
			} else {
				Insertion insertion = (Insertion) modification;
				int index = insertion.getIndex();
				if(index == -1) {
					// insert at last position
					List<StringBuffer> list = insertionPositionMap.get(currentNode);
					if(list == null) {
						list = new LinkedList<>();
					}
					tmp = insertion.getTarString(exprMap, allUsableVars);
					if(tmp == null) return false;;
					list.add(tmp);
					insertionPositionMap.put(currentNode, list);
				} else {
					Blk old = (Blk) insertion.getParent();
					List<Stmt> bro = old.getChildren();
					// find next match node
					Node nextNode = null;
					for(int i = index; nextNode == null && i < bro.size(); i++) {
						Node binding = bro.get(i).getBinding();
						while(nextNode == null && binding != null) {
							for(Node node : statements) {
								if(node == binding) {
									nextNode = binding;
									break;
								}
							}
							binding = binding.getParent();
						}
					}
					// if find next match node, insert at the position of matching node
					if(nextNode != null) {
						List<StringBuffer> list = insertionPositionMap.get(nextNode);
						if(list == null) {
							list = new LinkedList<>();
						}
						tmp = insertion.getTarString(exprMap, allUsableVars);
						if(tmp == null) return false;
						list.add(tmp);
						insertionPositionMap.put(nextNode, list);
					} else {
						// failed to find a next node, try to find a previous matching node
						for(int i = index - 1; nextNode == null && i >= 0; i--) {
							Node binding = bro.get(i).getBinding();
							while(binding != null) {
								for(int j = 0; nextNode == null && j < statements.size(); j ++) {
									Node node = statements.get(j);
									if(node == binding) {
										if((j + 1) < statements.size()) {
											nextNode = statements.get(j + 1);
										} else {
											nextNode = currentNode;
										}
									}
								}
								binding = binding.getParent();
							}
						}
						// if does not find previous node, just insert it to the last
						if(nextNode == null) {
							nextNode = currentNode;
						}
						List<StringBuffer> list = insertionPositionMap.get(nextNode);
						if(list == null) {
							list = new LinkedList<>();
						}
						tmp = insertion.getTarString(exprMap, allUsableVars);
						if(tmp == null) return false;
						list.add(tmp);
						insertionPositionMap.put(nextNode, list);
					}
				}
			}
		}
		return true;
	}
	
	public static <T extends Node> List<Modification> matchNodeList(Node parent, List<T> src, List<T> tar) {
		List<Modification> modifications = new LinkedList<>();
		Map<Integer, Integer> map = match(src, tar, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if(o1.compare(o2)) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		
		map.putAll(simMatch(map, src, tar, 0.5));
		Set<Integer> modifyBro = new HashSet<>();
		
		int last = 0;
		for(int i = 0; i < src.size(); i++) {
			Integer matchRight = map.get(i);
			if(matchRight == null) {
				modifyBro.add(i);
				src.get(i).resetAllNodeTypeMatch();
				Deletion deletion = new Deletion(parent, src.get(i));
				modifications.add(deletion);
			} else {
				for(; last < matchRight; last ++) {
					Insertion insertion = new Insertion(parent, i, tar.get(last));
					modifications.add(insertion);
				}
				last = matchRight + 1;
				src.get(i).deepMatch(tar.get(matchRight));
				if(!src.get(i).isNodeTypeMatch()) {
					modifyBro.add(i);
					Update update = new Update(parent, src.get(i), tar.get(matchRight));
					modifications.add(update);
				}
			}
		}
		
		for(; last < tar.size(); last ++) {
			Insertion insertion = new Insertion(parent, -1, tar.get(last));
			modifications.add(insertion);
		}
		
		for(Modification modification : modifications) {
			if(modification instanceof Insertion) {
				Insertion insertion = (Insertion) modification;
				int index = insertion.getIndex();
				if(index == -1) {
					index = src.size();
				}
				for(int i = insertion.getIndex() - 1; i >= 0; i --) {
					if(modifyBro.contains(i)) {
						insertion.setPreBro(src.get(i));
						break;
					}
				}
				for(int i = insertion.getIndex(); i < src.size(); i++) {
					if(modifyBro.contains(i)) {
						insertion.setNextBro(src.get(i));
						break;
					}
				}
			}
		}
		
		return modifications;
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
