/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
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
public class VarDeclarationExpr extends Expr {

	private static final long serialVersionUID = -5908284718888454712L;
	private MType _declType = null;
	private List<Vdf> _vdfs = null;


	/**
	 * VariableDeclarationExpression:
     *	{ ExtendedModifier } Type VariableDeclarationFragment
     *	    { , VariableDeclarationFragment }
	 */
	public VarDeclarationExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.VARDECLEXPR;
		_fIndex = VIndex.EXP_VAR_DEC;
	}

	public void setDeclType(MType declType) {
		_declType = declType;
	}

	public void setVarDeclFrags(List<Vdf> vdfs) {
		_vdfs = vdfs;
	}

	public MType getDeclType() {
		return _declType;
	}

	public List<Vdf> getFragments() {
		return _vdfs;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_vdfs.get(0).toSrcString());
		for (int i = 1; i < _vdfs.size(); i++) {
			stringBuffer.append(",");
			stringBuffer.append(_vdfs.get(i).toSrcString());
		}
		return stringBuffer;
	}

	@Override
	public void doAbstraction(CodeAbstraction abstracter) {
		if (isChanged() || isExpanded()) {
			_abstractType = abstracter.shouldAbstract(_declType, CodeAbstraction.Category.TYPE_TOKEN);
		}
		super.doAbstraction(abstracter);
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//		boolean consider = isConsidered() || parentConsidered;
		boolean consider = isConsidered();
		StringBuffer dec = _declType.formalForm(nameMapping, consider, keywords);
		if (dec == null && !_abstractType) {
			keywords.add(_declType.typeStr());
			dec = new StringBuffer(_declType.typeStr());
		}
		List<StringBuffer> buffers = new ArrayList<>(_vdfs.size());
		StringBuffer b;
		boolean contain = false;
		for (int i = 0; i < _vdfs.size(); i++) {
			b = _vdfs.get(i).formalForm(nameMapping, consider, keywords);
			if (b == null) {
				b = new StringBuffer(nameMapping.getExprID(_vdfs.get(i)));
			} else {
				contain = true;
			}
			buffers.add(b);
		}
		if (dec == null && !contain && !isConsidered()) {
			return super.toFormalForm0(nameMapping, parentConsidered, keywords);
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(dec == null ? nameMapping.getTypeID(_declType) : dec)
				.append(' ').append(buffers.get(0));
		for (int i = 1; i < buffers.size(); i++) {
			buffer.append(',').append(buffers.get(i));
		}
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_vdfs.get(0).tokens());
		for (int i = 1; i < _vdfs.size(); i++) {
			_tokens.add(",");
			_tokens.addAll(_vdfs.get(i).tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof VarDeclarationExpr) {
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) other;
			match = _declType.compare(varDeclarationExpr._declType);
			match = match && (_vdfs.size() == varDeclarationExpr._vdfs.size());
			for (int i = 0; match && i < _vdfs.size(); i++) {
				match = match && _vdfs.get(i).compare(varDeclarationExpr._vdfs.get(i));
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_vdfs.size() + 1);
		children.add(_declType);
		children.addAll(_vdfs);
		return children;
	}

	@Override
	public void computeFeatureVector() {
	    _selfFVector = new FVector();
        _selfFVector.inc(FVector.E_VARDEF);

		_completeFVector = new FVector();
		_completeFVector.combineFeature(_declType.getFeatureVector());
		for (Vdf vdf : _vdfs) {
			_completeFVector.combineFeature(vdf.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		VarDeclarationExpr vde = null;
		boolean match = false;
		if (compare(node)) {
			vde = (VarDeclarationExpr) node;
			setBindingNode(node);
			match = true;
		} else if (getBindingNode() != null) {
			vde = (VarDeclarationExpr) getBindingNode();
			match = (vde == node);
		} else if (canBinding(node)) {
			vde = (VarDeclarationExpr) node;
			setBindingNode(node);
			match = true;
		}
		if (vde == null) {
			continueTopDownMatchNull();
		} else {
			_declType.postAccurateMatch(vde.getDeclType());
			NodeUtils.greedyMatchListNode(getFragments(), vde.getFragments());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			VarDeclarationExpr vde = (VarDeclarationExpr) getBindingNode();
			if (!_declType.compare(vde.getDeclType())) {
				Update update = new Update(this, _declType, vde.getDeclType());
				_modifications.add(update);
			}
			_modifications.addAll(NodeUtils.genModificationList(this, _vdfs, vde.getFragments()));
		}
		return true;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if(node instanceof VarDeclarationExpr) {
			return NodeUtils.checkDependency(this, node, matchedNode, matchedStrings, level)
					&& NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
		} else {
			return false;
		}
	}

	@Override
	public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
		if(node instanceof VarDeclarationExpr) {
			return NodeUtils.patternMatch(this, node, matchedNode);
		} else {
			return false;
		}
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp = _declType.transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" ");
			tmp = _vdfs.get(0).transfer(vars, exprMap, retType, exceptions, metric);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _vdfs.size(); i++) {
				stringBuffer.append(",");
				tmp = _vdfs.get(i).transfer(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
		StringBuffer declType = null;
		Map<Integer, StringBuffer> insertion = new HashMap<>();
        Set<Node> deletion = new HashSet<>();
        Map<Node, StringBuffer> updates = new HashMap<>();
        Node node = NodeUtils.checkModification(this);
        if (node != null) {
            VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) node;
            for (Modification modification : varDeclarationExpr.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    if (update.getSrcNode() == varDeclarationExpr._declType) {
                        declType = update.apply(vars, exprMap, retType, exceptions, metric);
                        if (declType == null) return null;
                    } else {
                        StringBuffer buffer = update.apply(vars, exprMap, retType,exceptions, metric);
                        if (buffer == null) return null;
                        if (update.getSrcNode().getBuggyBindingNode() != null) {
                            updates.put(update.getSrcNode().getBuggyBindingNode(), buffer);
                        }
                    }
                } else {
                    if (modification instanceof Insertion) {
                        Insertion ins = (Insertion) modification;
                        StringBuffer buffer = ins.apply(vars, exprMap, retType, exceptions, metric);
                        if (buffer == null) return null;
                        insertion.put(ins.getIndex(), buffer);
                    } else if (modification instanceof Deletion) {
                        Deletion del = (Deletion) modification;
                        if (del.getDelNode().getBuggyBindingNode() != null) {
                            deletion.add(del.getDelNode().getBuggyBindingNode());
                        }
                    }
                }
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp;
        if (declType == null) {
            tmp = _declType.adaptModifications(vars, exprMap, retType, exceptions, metric);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
			stringBuffer.append(declType);
		}
		stringBuffer.append(" ");
		int start = 0;
		while(start < _vdfs.size()) {
			if (insertion.containsKey(start)) {
				stringBuffer.append(insertion.get(start));
				insertion.remove(start);
				break;
			} else if (deletion.contains(_vdfs.get(start))) {
				start ++;
			} else if (updates.containsKey(_vdfs.get(start))){
				stringBuffer.append(updates.get(_vdfs.get(start)));
				start ++;
				break;
			} else {
				tmp = _vdfs.get(start).adaptModifications(vars, exprMap, retType, exceptions, metric);
				if (tmp == null) return null;
				stringBuffer.append(tmp);
				start ++;
				break;
			}
		}
		for (int i = start; i < _vdfs.size(); i++) {
			if (insertion.containsKey(i)) {
				stringBuffer.append(",");
				stringBuffer.append(insertion.get(i));
			}
			if (deletion.contains(_vdfs.get(i))) {
				continue;
			} else if (updates.containsKey(_vdfs.get(i))) {
				stringBuffer.append(",");
				stringBuffer.append(updates.get(_vdfs.get(i)));
			} else {
				tmp = _vdfs.get(i).adaptModifications(vars, exprMap, retType, exceptions, metric);
				if(tmp == null) return null;
				stringBuffer.append(",");
				stringBuffer.append(tmp);
			}
		}
		return stringBuffer;
	}
}
