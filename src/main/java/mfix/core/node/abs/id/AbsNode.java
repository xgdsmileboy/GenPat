/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs.id;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2019-02-20
 */
public abstract class AbsNode implements Serializable {

    private static final long serialVersionUID = 8148656586590047176L;

    private int _id;
    private String _name;

    protected AbsNode(int id, String name) {
        _id = id;
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public int getId() {
        return _id;
    }

    @Override
    public String toString() {
        return _name;
    }
}
