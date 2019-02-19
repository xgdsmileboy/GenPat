/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.match;

import mfix.core.pattern.Pattern;

/**
 * @author: Jiajun
 * @date: 2018-12-25
 */
public interface PatternMatcher {

    boolean matches(Pattern p);
}
