/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.ast.expr.Vdf;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class VarDeclarationStmt extends Stmt {

	private static final long serialVersionUID = 3322205918544098505L;
	private String _modifier = null;
	private MType _declType = null;
	private List<Vdf> _fragments = null;

	/**
	 * VariableDeclarationStatement: { ExtendedModifier } Type
	 * VariableDeclarationFragment { , VariableDeclarationFragment } ;
	 */
	public VarDeclarationStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public VarDeclarationStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.VARDECLSTMT;
		_fIndex = VIndex.STMT_VAR_DECL;
	}

	/**
	 * @param modifier
	 *            the modifier to set
	 */
	public void setModifier(String modifier) {
		this._modifier = modifier;
	}

	public void setDeclType(MType declType) {
		_declType = declType;
	}

	public void setFragments(List<Vdf> fragments) {
		_fragments = fragments;
	}

	public String getModifier() {
		return _modifier;
	}

	public MType getDeclType() {
		return _declType;
	}

	public List<Vdf> getFragments() {
		return _fragments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_modifier != null) {
			stringBuffer.append(_modifier).append(' ');
		}
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(' ');
		stringBuffer.append(_fragments.get(0).toSrcString());
		for (int i = 1; i < _fragments.size(); i++) {
			stringBuffer.append(',');
			stringBuffer.append(_fragments.get(i).toSrcString());
		}
		stringBuffer.append(';');
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer dec = _declType.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer frag = new StringBuffer();
		StringBuffer b = _fragments.get(0).formalForm(nameMapping, isConsidered(), keywords);
		boolean contain = false;
		if (b == null) {
			frag.append(nameMapping.getExprID(_fragments.get(0)));
		} else {
			contain = true;
			frag.append(b);
		}
		for (int i = 1; i < _fragments.size(); i++) {
			b = _fragments.get(i).formalForm(nameMapping, isConsidered(), keywords);
			if (b == null) {
				frag.append(',').append(nameMapping.getExprID(_fragments.get(i)));
			} else {
				contain = true;
				frag.append(',').append(b);
			}

		}
		if (isConsidered() || dec != null || contain) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(dec == null ? nameMapping.getTypeID(_declType) : dec)
					.append(' ').append(frag).append(';');
			return buffer;
		}
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_modifier != null) {
			_tokens.add(_modifier);
		}
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_fragments.get(0).tokens());
		for (int i = 1; i < _fragments.size(); i++) {
			_tokens.add(",");
			_tokens.addAll(_fragments.get(i).tokens());
		}
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_fragments.size() + 1);
		children.add(_declType);
		children.addAll(_fragments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof VarDeclarationStmt) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) other;
			match = _declType.compare(varDeclarationStmt._declType);
			if (_modifier == null) {
				match = match && (varDeclarationStmt._modifier == null);
			} else {
				if (varDeclarationStmt._modifier == null) {
					match = false;
				} else {
					match = match && _modifier.equals(varDeclarationStmt._modifier);
				}
			}
			match = match && (_fragments.size() == varDeclarationStmt._fragments.size());
			for (int i = 0; match && i < _fragments.size(); i++) {
				match = match && _fragments.get(i).compare(varDeclarationStmt._fragments.get(i));
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_declType.getFeatureVector());
		for(Vdf vdf : _fragments) {
			_fVector.combineFeature(vdf.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		VarDeclarationStmt vds = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			vds = (VarDeclarationStmt) getBindingNode();
			match = (vds == node);
		} else if(canBinding(node)) {
			vds = (VarDeclarationStmt) node;
			setBindingNode(node);
			match = true;
		}
		if(vds == null) {
			continueTopDownMatchNull();
		} else {
			_declType.postAccurateMatch(vds.getDeclType());
			NodeUtils.greedyMatchListNode(_fragments, vds.getFragments());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) getBindingNode();
			if (!_declType.compare(varDeclarationStmt.getDeclType())) {
				Update update = new Update(this, _declType, varDeclarationStmt.getDeclType());
				_modifications.add(update);
			}
			_modifications.addAll(NodeUtils.genModificationList(this, _fragments, varDeclarationStmt.getFragments()));
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof VarDeclarationStmt) {
			return super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			StringBuffer tmp = _declType.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" ");
			tmp = _fragments.get(0).transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _fragments.size(); i++) {
				stringBuffer.append(",");
				tmp = _fragments.get(i).transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(";");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) pnode;
			StringBuffer declType = null;
			List<Modification> modifications = new LinkedList<>();
			for (Modification modification : varDeclarationStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == varDeclarationStmt._declType) {
						declType = update.apply(vars, exprMap);
						if (declType == null) return null;
					} else {
						modifications.add(update);
					}
				} else {
					modifications.add(modification);
				}
			}

			Map<Node, List<StringBuffer>> insertionBefore = new HashMap<>();
			Map<Node, List<StringBuffer>> insertionAfter = new HashMap<>();
			Map<Integer, List<StringBuffer>> insertionAt = new HashMap<>();
			Map<Node, StringBuffer> map = new HashMap<>(_fragments.size());
			if (!Matcher.applyNodeListModifications(modifications, _fragments, insertionBefore, insertionAfter,
				insertionAt, map, vars, exprMap)) {
				return null;
			}

			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			if(declType == null) {
				tmp = _declType.adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(declType);
			}
			stringBuffer.append(" ");

			boolean first = true;
			for(int index = 0; index < _fragments.size(); index ++) {
				Node node = _fragments.get(index);
				List<StringBuffer> list = insertionBefore.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						if(!first) {
							stringBuffer.append(",");
						}
						first = false;
						stringBuffer.append(list.get(i));
					}
				}
				list = insertionAt.get(index);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						if (!first) {
							stringBuffer.append(',');
						}
						first = false;
						stringBuffer.append(list.get(i));
					}
					insertionAt.remove(index);
				}

				if (map.containsKey(node)) {
					StringBuffer update = map.get(node);
					if (update != null) {
						if(!first) {
							stringBuffer.append(",");
						}
						first = false;
						stringBuffer.append(update);
					}
				} else {
					if(!first) {
						stringBuffer.append(",");
					}
					first = false;
					tmp = node.adaptModifications(vars, exprMap);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
				}
				list = insertionAfter.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						if(!first) {
							stringBuffer.append(",");
						}
						first = false;
						stringBuffer.append(list.get(i));
					}
				}
			}
			if (!insertionAt.isEmpty()) {
				List<Map.Entry<Integer, List<StringBuffer>>> list = new ArrayList<>(insertionAt.entrySet());
				list.stream().sorted(Comparator.comparingInt(Map.Entry::getKey));
				for (Map.Entry<Integer, List<StringBuffer>> entry : list) {
					for (StringBuffer s : entry.getValue()) {
						stringBuffer.append(',').append(s);
					}
				}
			}
			stringBuffer.append(";");
			return stringBuffer;
		} else {
			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			tmp = _declType.adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" ");
			tmp = _fragments.get(0).adaptModifications(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _fragments.size(); i++) {
				stringBuffer.append(",");
				tmp = _fragments.get(i).adaptModifications(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(";");
			return stringBuffer;
		}
	}
}
