package mfix.core.stats;

import mfix.common.util.JavaFile;
import mfix.core.stats.element.*;
import org.eclipse.jdt.core.dom.*;

/**
 * @author: Luyao Ren
 * @date: 2018/12/06
 */
public class Analyzer {
    private static Analyzer _instance;
    private String _fileName;
    private ElementCounter _elementCounter = null;

    public static Analyzer getInstance() {
        if (_instance == null) {
            _instance = new Analyzer();
            _instance.open();
        }
        return _instance;
    }

    public void open() {
        _elementCounter = new ElementCounter();
        _elementCounter.open();
    }

    public void finish() {
        _elementCounter.close();
    }

    public void runFile(String srcFile) {
        _fileName = srcFile;

        CompilationUnit _cunit = JavaFile.genASTFromFileWithType(srcFile, null);
        _cunit.accept(new Collector());
    }

    private String getExprTypeOrNull(Expression expr) { // help function
        if (expr == null) {
            return null;
        }
        ITypeBinding type = expr.resolveTypeBinding();
        if (type == null) {
            return null;
        }
        return type.getName();
    }

    private class Collector extends ASTVisitor {
        public boolean visit(MethodInvocation method) {
            String callFuncName = method.getName().getFullyQualifiedName();
            MethodElement element = new MethodElement(callFuncName, _fileName);

            if (method.getExpression() != null) {
                element.setObjType(getExprTypeOrNull(method.getExpression()));
            }
            element.setRetType(getExprTypeOrNull(method));
            element.setArgsNumber(method.arguments().size());
            String argsType = "";
            for (Object object : method.arguments()) {
                argsType += getExprTypeOrNull((Expression) object) + ",";
            }
            element.setArgsType(argsType);

            _elementCounter.add(element);

            return true;
        }
    }
}
