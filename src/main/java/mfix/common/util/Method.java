/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-04-11
 */
public class Method {

    private String _retType;
    private String _name;
    private List<String> _argTypes;

    public Method(String retType, String name, List<String> args) {
        _retType = retType;
        _name = name;
        _argTypes = args;
    }

    public String getRetType() {
        return _retType;
    }

    public String getName() {
        return _name;
    }

    public List<String> getArgTypes() {
        return _argTypes;
    }

    public Method toMethod(MethodDeclaration method) {
        String retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
        String name = method.getName().getIdentifier();
        List<SingleVariableDeclaration> args = method.parameters();
        List<String> arguments = new ArrayList<>(args.size());
        for (int i = 0; i < _argTypes.size(); i++) {
            arguments.add(args.get(i).getType().toString());
        }
        return new Method(retType, name, arguments);
    }


    public boolean same(MethodDeclaration method) {
        String retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
        String name = method.getName().getIdentifier();
        if (Utils.safeStringEqual(_retType, retType) && _name.equals(name)) {
            List<SingleVariableDeclaration> args = method.parameters();
            if (args.size() != _argTypes.size()) {
                return false;
            }
            for (int i = 0; i < _argTypes.size(); i++) {
                if (!_argTypes.get(i).equals(args.get(i).getType().toString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Method)) {
            return false;
        }
        Method m = (Method) obj;
        List<String> args = m.getArgTypes();
        if (Utils.safeStringEqual(_retType, m.getRetType()) && _name.equals(m.getName())
                && _argTypes.size() == args.size()) {
            for (int i = 0; i < _argTypes.size(); i++) {
                if (!Utils.safeStringEqual(_argTypes.get(i), args.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
