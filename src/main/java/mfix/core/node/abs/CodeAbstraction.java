/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs;

import mfix.core.node.ast.Node;

/**
 * @author: Jiajun
 * @date: 2019-02-14
 */
public interface CodeAbstraction {

    enum Category{
        API_TOKEN,
        TYPE_TOKEN,
        NAME_TOKEN
    }

    boolean shouldAbstract(String string);

    boolean shouldAbstract(Node node);

    boolean shouldAbstract(String string, Category category);

    boolean shouldAbstract(Node node, Category category);

    CodeAbstraction lazyInit();

}
