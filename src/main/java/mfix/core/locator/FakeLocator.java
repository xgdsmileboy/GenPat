/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.locator;

import mfix.common.java.FakeSubject;
import mfix.common.util.JavaFile;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-03-12
 */
public class FakeLocator extends AbstractFaultLocalization {

    public FakeLocator(FakeSubject subject) {
        super(subject);
    }

    @Override
    protected void locateFault(double threshold) {

    }

    @Override
    public List<Location> getLocations(int topK) {
        if (_faultyLocations == null) {
            _faultyLocations = new LinkedList<>();
            List<String> buggyFiles = ((FakeSubject)_subject).getBuggyFiles();
            int relStart = _subject.getHome().length() + 1;
            for (String f : buggyFiles) {
                final String fileName = f.substring(relStart, f.length() - 5/*".java".length()*/);
                final CompilationUnit unit = JavaFile.genASTFromFileWithType(f);
                unit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        int start = unit.getLineNumber(node.getStartPosition());
                        int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
                        Location location = new Location(fileName, null, node.getName().getFullyQualifiedName(),
                                (start + end) / 2, 1.0);
                        _faultyLocations.add(location);
                        return true;
                    }
                });
            }

        }
        return _faultyLocations;
    }
}
