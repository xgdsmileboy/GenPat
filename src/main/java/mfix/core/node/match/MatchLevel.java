/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

/**
 * @author: Jiajun
 * @date: 2019-04-02
 */
public enum MatchLevel {
    FUZZY, // both type and name are ignored
    TYPE,  // type info should be matched if not abstracted
    NAME,  // name info should be matched if not abstracted
    ALL,   // both type and name should be matched if not abstract
    AST    // node type in the ast should be matched (i.e., StringLiteral can only match StringLiteral)
}
