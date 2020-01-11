/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Method;
import mfix.common.util.Pair;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.abs.TermFrequency;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.Variable;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.expr.Svd;
import mfix.core.node.ast.expr.Vdf;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-01-15
 */
public class PatternExtractor {

    public Set<Pattern> extractPattern(String srcFile, String tarFile) {
        return extractPattern(srcFile, tarFile, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Method srcMethod, Method tarMethod) {
        return extractPattern(srcFile, tarFile, srcMethod, tarMethod, Constant.FILTER_MAX_CHANGE_LINE);
    }
    public Set<Pattern> extractPattern(String srcFile, String tarFile, Method srcMethod,
                                       Method tarMethod, int maxChangeLine) {
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        if (srcUnit == null || tarUnit == null) {
            return Collections.emptySet();
        }
        MethodDeclaration srcDecl = JavaFile.getDeclaration(srcUnit, srcMethod);
        MethodDeclaration tarDecl = JavaFile.getDeclaration(tarUnit, tarMethod);

        if (srcDecl == null || tarDecl == null) {
            return Collections.emptySet();
        }

        Set<String> imports = parseImports(srcUnit, tarUnit);

        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = new LinkedList<>();
        matchMap.add(new Pair<>(srcDecl, tarDecl));
        return extractPattern(matchMap, srcFile, srcUnit, tarFile, tarUnit, imports, maxChangeLine);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Method focus) {
        Set<Method> set = new HashSet<>();
        set.add(focus);
        return extractPattern(srcFile, tarFile, set, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Set<Method> focus) {
        return extractPattern(srcFile, tarFile, focus, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, int maxChangeLine) {
        return extractPattern(srcFile, tarFile, null, maxChangeLine);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Set<Method> focus, int maxChangeLine) {
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        if (srcUnit == null || tarUnit == null) {
            return Collections.emptySet();
        }

        Set<String> imports = parseImports(srcUnit, tarUnit);

        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        if (focus != null) {
            List<Pair<MethodDeclaration, MethodDeclaration>> filtered = new LinkedList<>();
            for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
                for (Method method : focus) {
                    if (method.same(pair.getFirst())) {
                        filtered.add(pair);
                        break;
                    }
                }
            }
            matchMap = filtered;
        }
        return extractPattern(matchMap, srcFile, srcUnit, tarFile, tarUnit, imports, maxChangeLine);
    }

    private Set<Pattern> extractPattern(List<Pair<MethodDeclaration, MethodDeclaration>> matchMap,
                                       String srcFile, CompilationUnit srcUnit,
                                       String tarFile, CompilationUnit tarUnit,
                                       Set<String> imports, int maxChangeLine) {
        Set<Pattern> patterns = new HashSet<>();
        NodeParser nodeParser = new NodeParser();

//        CodeAbstraction abstraction = new TF_IDF(srcFile, Constant.TF_IDF_FREQUENCY);
        CodeAbstraction abstraction = new TermFrequency(Constant.TOKEN_FREQENCY);
//        ElementCounter counter = new ElementCounter();
//        counter.open();
//        counter.loadCache();

        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            Pair<Set<Variable>, Set<String>> fields = parseFields(pair.getFirst(), pair.getSecond());
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());

            if(srcNode.toSrcString().toString().equals(tarNode.toSrcString().toString())) {
                continue;
            }

            TextDiff diff = new TextDiff(srcNode, tarNode);
            if (diff.getMiniDiff().size() > maxChangeLine) {
                continue;
            }

            if(Matcher.greedyMatch((MethDecl) srcNode, (MethDecl) tarNode)) {
                Set<Node> nodes = tarNode.getConsideredNodesRec(new HashSet<>(), false);
                Set<Variable> newVars = getVars(nodes);
                Set<Node> temp;
                for(Node node : nodes) {
                    if (node.getBindingNode() != null) {
                        node.getBindingNode().setExpanded();
                    }
                    if (Constant.EXPAND_PATTERN) {
                        temp = node.expand(new HashSet<>());
                        for (Node n : temp) {
                            if (n.getBindingNode() != null) {
                                n.getBindingNode().setExpanded();
                            }
                        }
                    }
                }

//                nodes = srcNode.getConsideredNodesRec(new HashSet<>(), true);
//                for (Node n : nodes) {
//                    System.out.println(n);
//                }

//                srcNode.doAbstraction(counter);
                srcNode.doAbstraction(abstraction.lazyInit());
                tarNode.doAbstraction(abstraction);
                // serialize feature vector
                Pattern pattern = new Pattern(srcNode, fields.getSecond(), imports);
                pattern.getFeatureVector();
                pattern.addNewVars(newVars);
                pattern.addNewVars(fields.getFirst());
                patterns.add(pattern);
            }
        }
//        counter.close();
        return patterns;
    }

    public static Set<Variable> getVars(Set<Node> nodes) {
        Set<Variable> vars = new HashSet<>();
        Queue<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getNodeType()) {
                case VARDECLFRAG:
                    Vdf vdf = (Vdf) node;
                    String type = vdf.getType() == null ? "?" : vdf.getType().typeStr();
                    vars.add(new Variable(vdf.getName(), type));
                    break;
                case SINGLEVARDECL:
                    Svd svd = (Svd) node;
                    vars.add(new Variable(svd.getName().getName(), svd.getDeclType().typeStr()));
                    break;
                case SNAME:
                    Node n = node.getParent();
                    if (n.getNodeType() == Node.TYPE.VARDECLFRAG) {
                        SName name = (SName) node;
                        vars.add(new Variable(name.getName(), ((Vdf) n).getType().typeStr()));
                    } else if (n.getNodeType() == Node.TYPE.SINGLEVARDECL) {
                        SName name = (SName) node;
                        vars.add(new Variable(name.getName(), ((Svd) n).getDeclType().typeStr()));
                    }
                default:
            }
            queue.addAll(node.getAllChildren());
        }
        return vars;
    }

    private Set<String> parseImports(CompilationUnit srcUnit, CompilationUnit tarUnit) {
        Set<String> imports = new HashSet<>();
        Set<String> srcImports = new HashSet<>();
        for (Object o : srcUnit.imports()) {
            srcImports.add(o.toString());
        }
        for (Object o : tarUnit.imports()) {
            String s = o.toString();
            if (!srcImports.contains(s)) {
                imports.add(s);
            }
        }
        return imports;
    }

    public static Pair<Set<Variable>, Set<String>> parseFields(ASTNode m1, ASTNode m2) {
        // TODO : define classes for fields and imports
        TypeDeclaration srcType = null;
        TypeDeclaration tarType = null;
        while(m1 != null) {
            if (m1 instanceof TypeDeclaration) {
                srcType = (TypeDeclaration) m1;
                break;
            }
            m1 = m1.getParent();
        }
        while(m2 != null) {
            if (m2 instanceof TypeDeclaration) {
                tarType = (TypeDeclaration) m2;
                break;
            }
            m2 = m2.getParent();
        }
        if (srcType == null || tarType == null) {
            return new Pair<>(Collections.emptySet(), Collections.emptySet());
        }
        final Set<Variable> srcFields = new HashSet<>();
        for (FieldDeclaration node : srcType.getFields()) {
            String type = node.getType().toString();
            for (Object obj : node.fragments()) {
                if (obj instanceof VariableDeclarationFragment) {
                    String name = ((VariableDeclarationFragment)obj).getName().getIdentifier();
                    srcFields.add(new Variable(name, type));
                }
            }
        }

        final Set<String> addedFields = new HashSet<>();
        final Set<Variable> newVars = new HashSet<>();
        for (FieldDeclaration node : tarType.getFields()) {
            StringBuffer buffer = new StringBuffer();
            for (Object o : node.modifiers()) {
                buffer.append(o.toString()).append(" ");
            }
            String type = node.getType().toString();
            buffer.append(type).append(" ");
            for (Object obj : node.fragments()) {
                if (obj instanceof VariableDeclarationFragment) {
                    String name = ((VariableDeclarationFragment)obj).getName().getIdentifier();
                    Variable var = new Variable(name, type);
                    if (!srcFields.contains(var)) {
                        newVars.add(var);
                        addedFields.add(buffer.toString() + obj.toString() + ";");
                    }
                }
            }
        }
        return new Pair<>(newVars, addedFields);
    }
}
