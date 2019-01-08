/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.diff;

import mfix.core.parse.Matcher;
import mfix.core.parse.diff.ast.AddTree;
import mfix.core.parse.diff.ast.DelTree;
import mfix.core.parse.diff.ast.KeepTree;
import mfix.core.parse.diff.ast.Tree;
import mfix.core.parse.node.MethDecl;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.stmt.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AstDiff extends mfix.core.parse.diff.Diff<Tree> {
	
	public AstDiff(Node src, Node tar) {
		super(src, tar);
	}
	
	@Override
	public void extractDiff() {
		if(_src instanceof MethDecl && _tar instanceof MethDecl) {
			MethDecl srcMethod = (MethDecl) _src;
			MethDecl tarMethod = (MethDecl) _tar;
			if(srcMethod.getBody() == null || tarMethod.getBody() == null) {
				return;
			}
			List<Stmt> srcNodes = srcMethod.getBody().getChildren();
			List<Stmt> tarNodes = tarMethod.getBody().getChildren();
			Map<Integer, Integer> map = Matcher.match(srcNodes, tarNodes);
			int rightCursor = 0;
			int srcLen = srcNodes.size();
			int tarLen = tarNodes.size();
			_source = new ArrayList<>(srcLen + tarLen);
			for(int i = 0; i < srcLen; i++) {
				Integer cursor = map.get(i);
				if(cursor == null) {
					_source.add(new DelTree(srcNodes.get(i)));
				} else {
					for(; rightCursor < cursor && rightCursor < tarLen; rightCursor ++) {
						_source.add(new AddTree(tarNodes.get(rightCursor)));
					}
					rightCursor = cursor + 1;
					_source.add(new KeepTree(srcNodes.get(i)));
				}
			}
			for(; rightCursor < tarLen; rightCursor ++) {
				_source.add(new AddTree(tarNodes.get(rightCursor)));
			}
		}
	}
	
	@Override
	public void accurateMatch() {
//		List<Integer> addTrees = new ArrayList<>(_source.size());
//		List<Integer> delTrees = new ArrayList<>(_source.size());
//		Tree tree = null;
//		for(int i = 0; i < _source.size(); i++) {
//			tree = _source.get(i);
//			if(tree instanceof KeepTree) {
//				tree.getNode().setAllNodeTypeMatch();
//			} else if(tree instanceof mfix.core.parse.diff.Add) {
//				addTrees.add(i);
//			} else if(tree instanceof mfix.core.parse.diff.Delete) {
//				delTrees.add(i);
//			}
//		}
//
//		int last = 0;
//		Map<Integer, Integer> simMap = new HashMap<>();
//		for(int i = 0; i < delTrees.size(); i++) {
//			Object[] delTokens = _source.get(delTrees.get(i)).getNode().tokens().toArray();
//			Object[] addTokens = null;
//			for(int j = last; j < addTrees.size(); j ++) {
//				 addTokens = _source.get(addTrees.get(j)).getNode().tokens().toArray();
//				 Map<Integer, Integer> map = Matcher.match(delTokens, addTokens);
//				 double value = ((double) map.size()) / ((double) delTokens.length);
//				 if(value > 0.5) {
//					 simMap.put(delTrees.get(i), addTrees.get(j));
//					 last = j + 1;
//					 break;
//				 }
//			}
//		}
//		for(Entry<Integer, Integer> entry : simMap.entrySet()) {
//			System.out.println("-------match----------");
//			System.out.println(_source.get(entry.getKey()));
//			System.out.println(_source.get(entry.getValue()));
//			System.out.println("-----------------");
//		}
//		for(Integer index : delTrees) {
//			if(simMap.containsKey(index)) {
//				continue;
//			} else {
//				System.out.println("---------no match - delete---------");
//				System.out.println(_source.get(index));
//			}
//		}
//		for(Integer index : addTrees) {
//			if(simMap.containsValue(index)) {
//				continue;
//			} else {
//				System.out.println("--------no match - add------------");
//				System.out.println(_source.get(index));
//			}
//		}
		
	}
	
}
