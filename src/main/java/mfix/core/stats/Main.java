package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.core.parse.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Main {
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

    static void runFile(String srcFile) {

        System.out.println(srcFile);

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);

//        Analyzer nodeAnalyzer = Analyzer.getInstance();
//
//        MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
//        methodDeclCollector.init();
//        srcUnit.accept(methodDeclCollector);
//        List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();
//
//        NodeParser nodePaser = NodeParser.getInstance();
//
//        for(MethodDeclaration method: srcMethods) {
//            nodePaser.setCompilationUnit(srcFile, srcUnit);
//            // nodeAnalyzer.setFileName(srcFile);
//            // nodeAnalyzer.analyze(nodePaser.process(method));
//        }
    }

    static void work() {
        // Create the table.
//        DatabaseConnector connector = new DatabaseConnector();
//        connector.open();
//        connector.createTable();

        File rootFile = new File("/Users/luyaoren/mtest");
        List<File> files = JavaFile.ergodic(rootFile, new LinkedList<>());

        System.out.println(files.size());
        files = files.subList(0, 100);

        for (File file : files) {
            runFile(file.getAbsolutePath());
        }

//        connector.close();
    }

    public static void main(String[] args) {
        work();
    }


}
