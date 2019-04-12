/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;

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

    public boolean same(MethodDeclaration method) {
        String retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
        String name = method.getName().getIdentifier();
        if (Utils.safeStringEqual(_retType, retType) && _name.equals(name)) {
            List<Type> args = method.typeParameters();
            if (args.size() != _argTypes.size()) {
                return false;
            }
            for (int i = 0; i < _argTypes.size(); i++) {
                if (!_argTypes.get(i).equals(args.get(i).toString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
