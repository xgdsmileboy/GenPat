/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public enum Status {

    SUCCESS("Successfully repair!"),
    FAILED("Failed to repair!"),
    TIMEOUT("Timeout!");

    private String _msg;
    private Status(String message){
        _msg = message;
    }

    @Override
    public String toString() {
        return _msg;
    }
}
