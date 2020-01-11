package mfix.tools;


import mfix.common.conf.Constant;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.abs.TermFrequency;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.Variable;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.RepairMatcher;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

import static mfix.common.util.JavaFile.genASTFromFileWithType;
import static mfix.common.util.JavaFile.genASTFromSourceWithType;

public class Transformer {
    ASTNode srcASTNode, tarASTNode;
    Node srcNode, tarNode;
    Set<String> imports;
    int maxChangeLine = 100;
    Pattern pattern;

    // find method with same name
    static MethodDeclaration findMethodFromCU(CompilationUnit unit, String targetMethod) {
        if (targetMethod == null) {
            return null;
        }
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (targetMethod.equals(node.getName().getIdentifier())) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });
        if (methods.size() == 0) {
            return null;
        }
        return methods.iterator().next();
    }

    public Transformer() {
        imports = new HashSet<>();
        pattern = null;
    }

    private Set<String> parseImports(CompilationUnit srcUnit) {
        Set<String> imports = new HashSet<>();
        Set<String> srcImports = new HashSet<>();
        for (Object o : srcUnit.imports()) {
            srcImports.add(o.toString());
        }
        return imports;
    }

    public void loadPatternSrc(String patternBlockCode, String patternFileCode, String patternFilePath) {
        CompilationUnit patternFileCodeCU = (CompilationUnit) genASTFromSourceWithType(patternFileCode, JavaCore.VERSION_1_7, AST.JLS8,
                ASTParser.K_COMPILATION_UNIT, patternFilePath, null);
        imports.addAll(parseImports(patternFileCodeCU));

//        this.srcASTNode = genASTFromSourceWithType(patternBlockCode, JavaCore.VERSION_1_7, AST.JLS8,
//                ASTParser.K_CLASS_BODY_DECLARATIONS, patternFilePath, null);
        this.srcASTNode = findMethodFromCU(patternFileCodeCU, "test1");

        NodeParser nodeParser = new NodeParser();
        nodeParser.setCompilationUnit(patternFilePath, patternFileCodeCU);
        this.srcNode = nodeParser.process(this.srcASTNode);
    }

    public void loadPatternTar(String patternTarCode, String patternFileCode, String patternFilePath) {
        CompilationUnit patternFileCodeCU = (CompilationUnit) genASTFromSourceWithType(patternFileCode, JavaCore.VERSION_1_7,
                AST.JLS8, ASTParser.K_COMPILATION_UNIT, patternFilePath, null);
        imports.addAll(parseImports(patternFileCodeCU));

//        this.tarASTNode = genASTFromSourceWithType(patternTarCode, JavaCore.VERSION_1_7, AST.JLS8,
//                ASTParser.K_CLASS_BODY_DECLARATIONS, patternFilePath, null);
        this.tarASTNode = findMethodFromCU(patternFileCodeCU, "test1");

        NodeParser nodeParser = new NodeParser();
        nodeParser.setCompilationUnit(patternFilePath, patternFileCodeCU);
        this.tarNode = nodeParser.process(this.tarASTNode);
    }

    public void extractPattern() {
        if (srcNode == null || tarNode == null) {
            return;
        }
//        CodeAbstraction abstraction = new TF_IDF(srcFile, Constant.TF_IDF_FREQUENCY);
        CodeAbstraction abstraction = new TermFrequency(Constant.TOKEN_FREQENCY);

        Pair<Set<Variable>, Set<String>> fields = PatternExtractor.parseFields(srcASTNode, tarASTNode);

        if(srcNode.toSrcString().toString().equals(tarNode.toSrcString().toString())) {
            return;
        }

        TextDiff diff = new TextDiff(srcNode, tarNode);
        if (diff.getMiniDiff().size() > maxChangeLine) {
            return;
        }

        if (Matcher.greedyMatch((MethDecl) srcNode, (MethDecl) tarNode)) {
            Set<Node> nodes = tarNode.getConsideredNodesRec(new HashSet<>(), false);
            Set<Variable> newVars = PatternExtractor.getVars(nodes);
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

            srcNode.doAbstraction(abstraction.lazyInit());
            tarNode.doAbstraction(abstraction);

            this.pattern = new Pattern(srcNode, fields.getSecond(), imports);
        }
    }

    public String apply(String buggyBlockCode, String buggyFilePath) {
        if (pattern == null) {
            System.err.println("No pattern!");
            return null;
        }


        CompilationUnit buggyFilePathCodeCU = (CompilationUnit)genASTFromFileWithType(buggyFilePath);
//        CompilationUnit patternFileCodeCU = (CompilationUnit) genASTFromSourceWithType(patternFileCode, JavaCore.VERSION_1_7, AST.JLS8,
//                ASTParser.K_COMPILATION_UNIT, buggyFilePath, null);

        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggyFilePath);
        ASTNode buggyASTNode = findMethodFromCU(buggyFilePathCodeCU, "test2");

//        ASTNode buggyASTNode = genASTFromSourceWithType(buggyBlockCode, JavaCore.VERSION_1_7,
//                AST.JLS8, ASTParser.K_CLASS_BODY_DECLARATIONS, buggyFilePath, null);

        NodeParser nodeParser = new NodeParser();
        nodeParser.setCompilationUnit(buggyFilePath, buggyFilePathCodeCU);
        MethDecl buggyNode = (MethDecl) nodeParser.process(buggyASTNode);

        List<MatchInstance> set = new RepairMatcher().tryMatch(buggyNode, pattern);
        VarScope scope = varMaps.get(buggyNode.getStartLine());
        scope.setDisable(true);
        scope.reset(pattern.getNewVars());
        Set<String> transformedCandidates = new HashSet<>();

        List<TextDiff> diffRet = new LinkedList<>();

        for (MatchInstance instance : set) {
            instance.apply();
            StringBuffer buffer = buggyNode.adaptModifications(scope, instance.getStrMap(), buggyNode.getRetTypeStr(),
                    new HashSet<>(buggyNode.getThrows()));

            instance.reset();

            if (buffer == null) {
                System.err.println("Adaptation failed!");
                continue;
            }

            String transformed = buffer.toString();
            if (transformedCandidates.contains(transformed)) {
                continue;
            }
            transformedCandidates.add(transformed);
            TextDiff diff = new TextDiff(buggyBlockCode, transformed);
            System.out.println(diff.toString());
            diffRet.add(diff);
        }

        if (transformedCandidates.isEmpty()) {
            return null;
        }
        return transformedCandidates.iterator().next();
    }
}
