/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.pattern.Pattern;

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
    private volatile Set<Set<Pattern>> _returnedNodes;

    public Cluster() {
        _returnedNodes = new HashSet<>();
    }

    public void cluster(String path) {
        _path = path;
        _returnedNodes = new HashSet<>();
    }

    public void callback(Set<Pattern> set) {
        update(set, true);
    }

    public Set<Set<Pattern>> clusteredNodeSet() {
        return update(null, false);
    }

    private synchronized Set<Set<Pattern>> update(Set<Pattern> set, boolean isSubThread) {
        Set<Set<Pattern>> result = null;
        if (isSubThread) {
            _returnedNodes.add(set);
        } else {
            result = _returnedNodes;
            _returnedNodes = new HashSet<>();
        }
        return result;
    }

    protected Set<Pattern> cluster(Set<Pattern> patterns) {
        Set<Pattern> results = new HashSet<>();
        Set<Pair<Set<Pattern>, Set<Pattern>>> compareClusters = initClusterPair(patterns);
        int currTaskCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(_maxThreadCount);
        List<Future<Void>> threadResultList = new LinkedList<>();
        while (true) {
            if (threadResultList.isEmpty() && compareClusters.size() == 1) {
                Pair<Set<Pattern>, Set<Pattern>> pair = compareClusters.iterator().next();
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
            Iterator<Pair<Set<Pattern>, Set<Pattern>>> iter = compareClusters.iterator();
            while (iter.hasNext()) {
                Pair<Set<Pattern>, Set<Pattern>> pair = iter.next();
                Future<Void> future = threadPool.submit(new NodeCluster(pair.getFirst(), pair.getSecond(),
                        this));
                threadResultList.add(future);
                currTaskCount++;
            }
            compareClusters.clear();
            Set<Set<Pattern>> sets = clusteredNodeSet();
            if (currTaskCount >= _maxThreadCount || sets.isEmpty()) {
                currTaskCount -= waitSubThreads(threadResultList);
            }
            compareClusters.addAll(buildClusterPair(sets));
        }

        return results;
    }

    private int waitSubThreads(List<Future<Void>> threadResultList) {
        int size = 0;
        try {
            LevelLogger.debug("Clear existing thread ...");
            for (Future<Void> future : threadResultList) {
                future.get();
                size ++;
            }
            LevelLogger.debug("Finish clear ....");
            size = threadResultList.size();
            threadResultList.clear();
        } catch (Exception e) {
            LevelLogger.warn("Clustering error !", e);
        }
        return size;
    }


    /**
     * group patterns as pairs for clustering
     * @param patterns : patterns to group
     * @return : a set of pairs for clustering
     */
    private Set<Pair<Set<Pattern>, Set<Pattern>>> initClusterPair(Set<Pattern> patterns) {
        Set<Pair<Set<Pattern>, Set<Pattern>>> result = new HashSet<>();
        Pattern pre = null;
        for (Pattern pattern : patterns) {
            if (pre == null) {
                pre = pattern;
            } else {
                Set<Pattern> s1 = new HashSet<>();
                s1.add(pre);
                Set<Pattern> s2 = new HashSet<>();
                s2.add(pattern);
                result.add(new Pair<>(s1, s2));
                pre = null;
            }
        }
        if (pre != null) {
            Set<Pattern> s1 = new HashSet<>();
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
    private Set<Pair<Set<Pattern>, Set<Pattern>>> buildClusterPair(Set<Set<Pattern>> patternSets) {
        Set<Pair<Set<Pattern>, Set<Pattern>>> result = new HashSet<>();
        Set<Pattern> pre = null;
        for (Set<Pattern> nodes : patternSets) {
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

        private Set<Pattern> _fstPatterns;
        private Set<Pattern> _sndPatterns;
        private Cluster _callback;

        public NodeCluster(Set<Pattern> fstPatterns, Set<Pattern> sndPatterns, Cluster callback) {
            _fstPatterns = fstPatterns;
            _sndPatterns = sndPatterns;
            _callback = callback;
        }

        @Override
        public Void call() {
            LevelLogger.debug(Thread.currentThread().getName() + " : " );
            if (_fstPatterns != null && _sndPatterns != null) {
                LevelLogger.debug(" >>>> " + (_fstPatterns.size() + _sndPatterns.size()));
                for (Pattern fstPattern : _fstPatterns) {
                    for (Iterator<Pattern> iter = _sndPatterns.iterator(); iter.hasNext(); ) {
                        Pattern sndPattern = iter.next();
                        Vector fstVec = fstPattern.getPatternVector();
                        Vector sndVec = sndPattern.getPatternVector();
                        boolean same = false;
                        if (fstVec.equals(sndVec)) {
                            // TODO: here the abstracted node should be used,
                            //  can use String comparison directly?

                        }
                        if (same) {
                            fstPattern.incFrequency(sndPattern.getFrequency());
                            iter.remove();
                        }
                    }
                }
                Set<Pattern> result = new HashSet<>();
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
