package mfix.core.parse.match;

import mfix.common.java.JavaFile;
import mfix.common.java.Subject;
import mfix.core.parse.NodePaser;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimMatcher {

	public static Map<String, List<Node>> extractMethodDeclaration(Map<String, Set<Node>>keywords, Subject subject) {
		String path = subject.getHome() + subject.getSsrc();
		List<String> allFiles = JavaFile.ergodic(path, new LinkedList<String>());
		List<Node> lists = null;
		Map<String, List<Node>> result = new HashMap<>();
		MethodIdentifyVisitor methodIdentifyVisitor = new MethodIdentifyVisitor(keywords);
		for(String file : allFiles) {
			CompilationUnit compilationUnit = JavaFile.genASTFromFileWithType(file, null);
			compilationUnit.accept(methodIdentifyVisitor);
			lists = methodIdentifyVisitor.getCandidates();
			if(!lists.isEmpty()) {
				result.put(file, lists);
			}
		}
		return result;
	}
	
	private static class MethodIdentifyVisitor extends ASTVisitor {
		
		private Map<String, Set<Node>> _keywords;
		private NodePaser _nodePaser;
		private List<Node> _candidates;
		
		public MethodIdentifyVisitor(Map<String, Set<Node>> keywords) {
			_keywords = keywords;
			_nodePaser = new NodePaser();
		}
		
		public List<Node> getCandidates() {
			return _candidates;
		}
		
		public boolean visit(CompilationUnit unit) {
			_nodePaser.setCompilationUnit(unit);
			_candidates = new LinkedList<>();
			return true;
		}
		
		@Override
		public boolean visit(MethodDeclaration node) {
			Node parsedNode = _nodePaser.process(node);
			
			_candidates.add(parsedNode);
			
//			Map<String, Set<Node>> keywords = parsedNode.getKeywords();
//			boolean containAllKey = true;
//			for (Entry<String, Set<Node>> entry : _keywords.entrySet()) {
//				Set<Node> allNodes = keywords.get(entry.getKey());
//				if (allNodes == null) {
//					containAllKey = false;
//				} else {
//					Set<Node> changedPattern = entry.getValue();
//					boolean similar = false;
//					for (Node pattern : changedPattern) {
//						for (Node candNode : allNodes) {
//							if (pattern.getParentStmt().getFeatureVector()
//									.computeSimilarity(candNode.getParentStmt().getFeatureVector(), ALGO.COSINE) > 0.8) {
//								similar = true;
//								break;
//							}
//						}
//						if (!similar) {
//							containAllKey = false;
//							break;
//						}
//					}
//				}
//				if (!containAllKey) {
//					break;
//				}
//			}
//			if (containAllKey) {
//				_candidates.add(parsedNode);
//			}
			return true;
		}
		
	}
	
}
