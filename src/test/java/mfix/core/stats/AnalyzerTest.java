package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.core.TestCase;
import mfix.core.parse.NodeParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTVisitor;


public class AnalyzerTest extends TestCase {
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

    @Test
    public void test_parse() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);

        Analyzer nodeAnalyzer = Analyzer.getInstance();

        MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
        methodDeclCollector.init();
        srcUnit.accept(methodDeclCollector);
        List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();

        NodeParser nodePaser = NodeParser.getInstance();

        for(MethodDeclaration method: srcMethods) {
            nodePaser.setCompilationUnit(srcFile, srcUnit);
            nodeAnalyzer.analyze(nodePaser.process(method));
        }

        // System.out.println(nodeAnalyzer);

        Assert.assertTrue(nodeAnalyzer.getCount("currentThread") == 10);
    }
}
