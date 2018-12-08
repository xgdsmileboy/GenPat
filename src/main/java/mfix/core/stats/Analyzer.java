package mfix.core.stats;

import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.SName;
import org.eclipse.jdt.core.dom.*;
import mfix.core.parse.node.*;

import java.util.Set;

/**
 * @author: Luyao Ren
 * @date: 2018/12/06
 */
public class Analyzer {
    private static Analyzer _instance;
    private CompilationUnit _cunit;
    private String _fileName;
    private ElementCounter _elementCounter = new ElementCounter<Element>();
    private ElementCounter _typedElementCounter = new ElementCounter<TypedElement>();

    public static Analyzer getInstance() {
        if (_instance == null) {
            _instance = new Analyzer();
        }
        return _instance;
    }

    public void analyze(Node curNode) {
        if (curNode instanceof MethodInv) {
            // System.out.println("code=" + curNode.toString());
            // System.out.println(curNode.getCalledMethods().toString());
            // System.out.println(curNode.getAllVars().toString());

            for (Set<Node> methodSet : curNode.getCalledMethods().values()) {
                for (Node method : methodSet) {
                    _elementCounter.add(new Element(method.toString()));
                    _typedElementCounter.add(new TypedElement(method.toString(), method.getNodeType()));
                }
            }
            for (SName var : curNode.getAllVars()) {
                _elementCounter.add(new Element(var.getName()));
                _typedElementCounter.add(new TypedElement(var));
            }
        }
        for (Node child : curNode.getAllChildren()) {
            analyze(child);
        }
    }

    public Integer getElementFrequency(Element element) {
        return _elementCounter.count(element);
    }

    public Integer getTypedElementFrequency(TypedElement element) {
        return _typedElementCounter.count(element);
    }

}
