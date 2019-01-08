/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.comp;

import mfix.core.parse.node.Node;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public interface NodeComparator {

	boolean compare(Node other);
	
}
