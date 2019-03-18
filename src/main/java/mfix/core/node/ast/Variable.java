/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.ast;

import mfix.common.util.Utils;

/**
 * @author: Jiajun
 * @date: 2019-03-18
 */
public class Variable {
    private String _name;
    private String _type;

    public Variable(String name, String type) {
        _name = name;
        _type = type;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Variable)) {
            return false;
        }
        Variable var = (Variable) obj;
        // currently consider name only
        // if type is needed, update here
        return Utils.safeStringEqual(_name, var._name);
//                && Utils.safeStringEqual(_type, var._type);
    }
}
