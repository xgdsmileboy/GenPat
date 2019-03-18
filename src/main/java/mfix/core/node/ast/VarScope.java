/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.ast;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-18
 */
public class VarScope {

    private Set<Variable> _globalVars;
    private Map<Variable, Set<LineRange>> _localVars;

    public VarScope() {
        _globalVars = new HashSet<>();
        _localVars = new HashMap<>();
    }

    public void setGlobalVars(final Set<Variable> globalVar) {
        _globalVars = globalVar;
    }

    public void addGlobalVar(Variable var) {
        _globalVars.add(var);
    }

    public void addGlobalVar(final String name, final String type) {
        addGlobalVar(new Variable(name, type));
    }

    public void addLocalVar(Variable variable, LineRange lineRange) {
        Set<LineRange> ranges = _localVars.get(variable);
        if (ranges == null) {
            ranges = new HashSet<>();
            _localVars.put(variable, ranges);
        }
        ranges.add(lineRange);
    }

    public void addLocalVar(final String name, final String type, final int start, final int end) {
        Variable variable = new Variable(name, type);
        LineRange range = new LineRange(start, end);
        addLocalVar(variable, range);
    }

    public boolean canUse(final String name, final String type, final int line) {
        Variable variable = new Variable(name, type);
        Set<LineRange> ranges = _localVars.get(variable);
        if (ranges != null) {
            for (LineRange r : ranges) {
                if (ranges.contains(line)) {
                    return true;
                }
            }
        }
        return _globalVars.contains(variable);
    }

}

