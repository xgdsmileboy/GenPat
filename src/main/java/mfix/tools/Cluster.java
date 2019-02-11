/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.core.node.ast.Node;
import mfix.core.node.vector.Vector;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class Cluster {

    /**
     * judging whether two nodes are the same or not
     *
     * @param p1 : the first pattern node
     * @param p2 : the second pattern node
     * @return : true if pattern {@code p1} is the same as {@code p2}
     */
    private boolean isSame(Node p1, Node p2) {
        Vector v1 = p1.getPatternVector();
        Vector v2 = p2.getPatternVector();
        if (!v1.equals(v2)) {
            return false;
        }
        // TODO: otherwise, perform more accurate pattern matching

        return false;
    }

    /**
     * cluster patterns in the given directory {@code srcDir},
     * output the patterns after clustering to the given directory {@code tarDir}
     *
     * @param srcDir : the source directory contains the patterns to cluster
     * @param tarDir : the target directory to save clustered patterns
     */
    private void cluster(String srcDir, String tarDir) {

    }

    public static void main(String[] args) {

    }

}
