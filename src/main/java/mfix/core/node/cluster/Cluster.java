/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.cluster;

import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.ast.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author: Jiajun
 * @date: 2019-02-14
 */
public class Cluster {

    private int _maxThreadCount = 10;
    private String _path;
    private volatile Set<Set<Node>> _returnedNodes;

    public void cluster(String path) {
        _path = path;
        _returnedNodes = new HashSet<>();
    }

    public void callback(Set<Node> set) {
        update(set, true);
    }

    public Set<Set<Node>> clusteredNodeSet() {
        return update(null, false);
    }

    private synchronized Set<Set<Node>> update(Set<Node> set, boolean isSubThread) {
        Set<Set<Node>> result = null;
        if (isSubThread) {
            _returnedNodes.add(set);
        } else {
            result = _returnedNodes;
            _returnedNodes = new HashSet<>();
        }
        return result;
    }

    private Set<Node> cluster(Set<Node> patterns) {
        Set<Node> results = new HashSet<>();
        Set<Pair<Set<Node>, Set<Node>>> compareClusters = initClusterPair(patterns);
        int currTaskCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(_maxThreadCount);
        List<Future<Void>> threadResultList = new LinkedList<>();
        while (true) {
            if (threadResultList.isEmpty() && compareClusters.size() == 1) {
                Pair<Set<Node>, Set<Node>> pair = compareClusters.iterator().next();
                if (pair.getFirst() == null || pair.getSecond() == null) {
                    if (pair.getFirst() != null) {
                        results = pair.getFirst();
                    } else if (pair.getSecond() != null) {
                        results = pair.getSecond();
                    } else {
                        LevelLogger.fatal("Empty pattern nodes after clustering!");
                    }
                    break;
                }
            }
            Iterator<Pair<Set<Node>, Set<Node>>> iter = compareClusters.iterator();
            while (iter.hasNext()) {
                Pair<Set<Node>, Set<Node>> pair = iter.next();
                Future<Void> future = threadPool.submit(new NodeCluster(pair.getFirst(), pair.getSecond(),
                        this));
                threadResultList.add(future);
                currTaskCount++;
            }
            compareClusters.clear();
            if (currTaskCount >= _maxThreadCount) {
                try {
                    for (Future<Void> future : threadResultList) {
                        future.get();
                        currTaskCount--;
                    }
                    threadResultList.clear();
                    currTaskCount = 0;

                } catch (Exception e) {
                    LevelLogger.error("Clustering error !", e);
                }
            }
            Set<Set<Node>> sets = clusteredNodeSet();
            compareClusters.addAll(buildClusterPair(sets));
        }

        return results;
    }


    /**
     * group patterns as pairs for clustering
     * @param patterns : patterns to group
     * @return : a set of pairs for clustering
     */
    private Set<Pair<Set<Node>, Set<Node>>> initClusterPair(Set<Node> patterns) {
        Set<Pair<Set<Node>, Set<Node>>> result = new HashSet<>();
        Node pre = null;
        for (Node node : patterns) {
            if (pre == null) {
                pre = node;
            } else {
                Set<Node> s1 = new HashSet<>();
                s1.add(pre);
                Set<Node> s2 = new HashSet<>();
                s2.add(node);
                result.add(new Pair<>(s1, s2));
                pre = null;
            }
        }
        if (pre != null) {
            Set<Node> s1 = new HashSet<>();
            s1.add(pre);
            result.add(new Pair<>(s1, null));
        }
        return result;
    }

    /**
     * group pattern sets as pairs for clustering
     * @param patternSets : a set of pattern sets to group
     * @return : a set of pairs for clustering
     */
    private Set<Pair<Set<Node>, Set<Node>>> buildClusterPair(Set<Set<Node>> patternSets) {
        Set<Pair<Set<Node>, Set<Node>>> result = new HashSet<>();
        Set<Node> pre = null;
        for (Set<Node> nodes : patternSets) {
            if (pre == null) {
                pre = nodes;
            } else {
                result.add(new Pair<>(pre, nodes));
                pre = null;
            }
        }
        if (pre != null) {
            result.add(new Pair<>(pre, null));
        }
        return result;
    }

    /**
     * Class takes the responsibility for node comparision.
     *
     * Given two sets of pattern nodes, this class assumes that
     * the pattern node is distinct within each set. Then,
     * it compares each node in the first set with the nodes in the
     * second set. In each comparison, if two nodes are the same
     * pattern, one will be removed and the other one will be survived
     * with the pattern frequency increased.
     *
     * Finally, after comparing, the class combine two sets of
     * pattern nodes to one set with merging the same pattern nodes.
     */
    private static class NodeCluster implements Callable<Void> {

        private Set<Node> _fstPatterns;
        private Set<Node> _sndPatterns;
        private Cluster _callback;

        public NodeCluster(Set<Node> fstPatterns, Set<Node> sndPatterns, Cluster callback) {
            _fstPatterns = fstPatterns;
            _sndPatterns = sndPatterns;
            _callback = callback;
        }

        @Override
        public Void call() {
            if (_fstPatterns != null && _sndPatterns != null) {
                for (Node fstNode : _fstPatterns) {
                    for (Iterator<Node> iter = _sndPatterns.iterator(); iter.hasNext(); ) {
                        Node sndNode = iter.next();
                        Vector fstVec = fstNode.getPatternVector();
                        Vector sndVec = sndNode.getPatternVector();
                        boolean same = false;
                        if (fstVec.equals(sndVec)) {
                            // TODO: here the abstracted node should be used,
                            //  can use String comparison directly?

                        }
                        if (same) {
                            fstNode.incFrequency(sndNode.getFrequency());
                            iter.remove();
                        }
                    }
                }
                Set<Node> result = new HashSet<>();
                result.addAll(_fstPatterns);
                result.addAll(_sndPatterns);
                _callback.callback(result);
            } else if (_fstPatterns != null) {
                _callback.callback(new HashSet<>(_fstPatterns));
            } else if (_sndPatterns != null) {
                _callback.callback(new HashSet<>(_sndPatterns));
            }
            return null;
        }
    }

}
