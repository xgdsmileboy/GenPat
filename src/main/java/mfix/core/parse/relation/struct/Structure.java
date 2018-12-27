/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.struct;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public abstract class Structure implements Serializable {

    private static final long serialVersionUID = 7690928543701849397L;

    private RSKind _rskind;
    protected Structure(RSKind rsKind) {
        _rskind = rsKind;
    }

    public RSKind rskind() {
        return _rskind;
    }

    public boolean match(Structure structure) {
        return _rskind == structure.rskind();
    }


    public enum RSKind{
        RS_METHOD("METHOD"),
        RS_FOR("FOR"),
        RS_ENHANCEDFOR("ENFOR"),
        RS_WHILE("WHILE"),
        RS_DO("DO"),
        RS_IF("IF"),
        RS_SWCASE("CASE"),
        RS_SWITCHSTMT("SWITCH"),
        RS_SYNC("SYNC"),
        RS_TRY("TRY"),
        RS_CATCH("CATCH"),
        RS_FINALLY("FINALLY"),
        RS_BREAK("BREAK"),
        RS_CONTINUE("CONTINUE"),
        RS_RETURN("RETURN"),
        RS_THROW("THROW");

        private String _value;
        private RSKind(String value) {
            _value = value;
        }

        @Override
        public String toString() {
            return _value;
        }
    }

    @Override
    public String toString() {
        return _rskind.toString();
    }
}
