/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Utils {
    public static String join(char delimiter, String ... element) {
        return join(delimiter, Arrays.asList(element));
    }

    public static String join(char delimiter, List<String> elements) {
        StringBuffer buffer = new StringBuffer();
        if(elements.size() > 0) {
            buffer.append(elements.get(0));
        }
        for(int i = 1; i < elements.size(); i++) {
            buffer.append(delimiter);
            buffer.append(elements.get(i));
        }
        return buffer.toString();
    }
}
