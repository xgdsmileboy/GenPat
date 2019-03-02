/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.diff;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Diff<T> {

	protected Node _src;
	protected Node _tar;
	protected List<T> _source;
	
	public Diff(Node src, Node tar) {
		_src = src;
		_tar = tar;
		extractDiff();
	}
	
	public boolean exist() {
		return _source != null && _source.size() > 0;
	}
	
	public List<T> getFullDiff() {
		return _source;
	}

	public List<T> getMiniDiff() {
		List<T> miniDiff = new ArrayList<>(_source.size());
		for(T t : _source) {
			if(t instanceof Add || t instanceof Delete) {
				miniDiff.add(t);
			}
		}
		return miniDiff;
	}
	
	protected abstract void extractDiff(); 
	public abstract void accurateMatch();
	
	public String miniDiff() {
		StringBuffer stringBuffer = new StringBuffer();
		for(T t : _source) {
			if(t instanceof Add || t instanceof Delete) {
				stringBuffer.append(t.toString());
				stringBuffer.append(Constant.NEW_LINE);
			}
		}
		return stringBuffer.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_source != null) {
			for(T t : _source) {
				stringBuffer.append(t.toString());
				stringBuffer.append(Constant.NEW_LINE);
			}
		}
		return stringBuffer.toString();
	}
	
	public static List<Diff> extractFileDiff(String srcFile, String tarFile, Class<? extends Diff> clazz) {
		List<Diff> diffs = new LinkedList<>();
		CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
		CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
		Matcher matcher = new Matcher();
		List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = matcher.match(srcUnit, tarUnit);
		NodeParser nodePaser = new NodeParser();
		for(Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
			nodePaser.setCompilationUnit(srcFile, srcUnit);
			Node srcNode = nodePaser.process(pair.getFirst()); 
			String src = srcNode.toSrcString().toString();
			nodePaser.setCompilationUnit(tarFile, tarUnit);
			Node tarNode = nodePaser.process(pair.getSecond()); 
			String tar = tarNode.toSrcString().toString();
			
			if(src.equals(tar)) {
				continue;
			}
			
			try {
				Diff diff = clazz.getConstructor(Node.class, Node.class).newInstance(srcNode, tarNode);
				diffs.add(diff);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return diffs;
	}
	
}
