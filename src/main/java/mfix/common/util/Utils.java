/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Utils {
    public static String join(char deliminator, String ... element) {
        StringBuffer buffer = new StringBuffer();
        if(element.length > 0) {
            buffer.append(element[0]);
        }
        for(int i = 1; i < element.length; i++) {
            buffer.append(deliminator);
            buffer.append(element[i]);
        }
        return buffer.toString();
    }
}
