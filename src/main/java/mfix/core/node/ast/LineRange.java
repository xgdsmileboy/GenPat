/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.ast;

/**
 * @author: Jiajun
 * @date: 2019-03-18
 */
public class LineRange {

    private int _start;
    private int _end;

    public LineRange(int start, int end) {
        _start = start;
        _end = end;
    }

    public boolean contains(int line) {
        return _start <= line && line <= _end;
    }

    @Override
    public int hashCode() {
        return _start + _end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LineRange)) {
            return false;
        }

        LineRange range = (LineRange) obj;
        return _start == range._start && _end == range._end;
    }
}
