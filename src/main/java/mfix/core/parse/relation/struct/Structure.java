/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.struct;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public abstract class Structure {
    private RSKind _rskind;
    protected Structure(RSKind rsKind) {
        _rskind = rsKind;
    }

    public RSKind rskind() {
        return _rskind;
    }


    public enum RSKind{
        RS_METHOD,
        RS_FOR,
        RS_ENHANCEDFOR,
        RS_WHILE,
        RS_DO,
        RS_IF,
        RS_SWCASE,
        RS_SWITCHSTMT,
        RS_SYNC,
        RS_TRY,
        RS_CATCH,
        RS_FINALLY,
        RS_BREAK,
        RS_CONTINUE,
        RS_RETURN,
        RS_THROW
    }
}
