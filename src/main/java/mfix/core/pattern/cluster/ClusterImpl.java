/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Triple;
import mfix.core.pattern.Pattern;

import java.util.Comparator;
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
public class ClusterImpl {

    private int _maxThreadCount = Constant.MAX_CLUSTUR_THREAD_NUM;
    private volatile Set<Set<Group>> _returnedNodes;

    public ClusterImpl() {
        _returnedNodes = new HashSet<>();
    }

    public ClusterImpl reset() {
        _returnedNodes = new HashSet<>();
        return this;
    }

    public Set<Group> cluster(Set<Pattern> patterns) {
        Set<Group> results = new HashSet<>();
        List<Triple<Set<Group>, Set<Group>, Integer>> compareClusters = initClusterPair(patterns);
        int currTaskCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(_maxThreadCount);
        List<Future<Set<Group>>> threadResultList = new LinkedList<>();
        while (true) {
            if (threadResultList.isEmpty() && compareClusters.size() == 1) {
                Triple<Set<Group>, Set<Group>, Integer> pair = compareClusters.iterator().next();
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
            Iterator<Triple<Set<Group>, Set<Group>, Integer>> iter = compareClusters.iterator();
            while (iter.hasNext() && currTaskCount < _maxThreadCount) {
                Triple<Set<Group>, Set<Group>, Integer> pair = iter.next();
                Future<Set<Group>> future = threadPool.submit(new NodeCluster(pair.getFirst(), pair.getSecond()));
                threadResultList.add(future);
                currTaskCount++;
                iter.remove();
            }
            compareClusters.addAll(buildClusterPair(_returnedNodes));
            // Haffman-encoding-like merge, merge small sets first
            compareClusters.sort(Comparator.comparingInt(Triple::getTag));
            _returnedNodes = new HashSet<>();
            if (currTaskCount >= _maxThreadCount || compareClusters.isEmpty()) {
                currTaskCount -= waitSubThreads(threadResultList);
            }
        }
        threadPool.shutdown();

        return results;
    }

    private int waitSubThreads(List<Future<Set<Group>>> threadResultList) {
        int size = 0;
        try {
            LevelLogger.debug("Clear existing thread ...");
            for (Future<Set<Group>> future : threadResultList) {
                Set<Group> result = future.get();
                _returnedNodes.add(result);
                size++;
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
     *
     * @param patterns : patterns to group
     * @return : a set of pairs for clustering
     */
    private List<Triple<Set<Group>, Set<Group>, Integer>> initClusterPair(Set<Pattern> patterns) {
        List<Triple<Set<Group>, Set<Group>, Integer>> result = new LinkedList<>();
        Group pre = null;
        for (Pattern pattern : patterns) {
            if (pre == null) {
                pre = new Group(pattern);
            } else {
                Set<Group> s1 = new HashSet<>();
                s1.add(pre);
                Set<Group> s2 = new HashSet<>();
                s2.add(new Group(pattern));
                result.add(new Triple<>(s1, s2, s1.size() + s2.size()));
                pre = null;
            }
        }
        if (pre != null) {
            Set<Group> s1 = new HashSet<>();
            s1.add(pre);
            result.add(new Triple<>(s1, null, s1.size()));
        }
        return result;
    }

    /**
     * group pattern sets as pairs for clustering
     *
     * @param groupSets : a set of pattern sets to group
     * @return : a set of pairs for clustering
     */
    private Set<Triple<Set<Group>, Set<Group>, Integer>> buildClusterPair(Set<Set<Group>> groupSets) {
        Set<Triple<Set<Group>, Set<Group>, Integer>> result = new HashSet<>();
        Set<Group> pre = null;
        for (Set<Group> nodes : groupSets) {
            if (pre == null) {
                pre = nodes;
            } else {
                result.add(new Triple<>(pre, nodes, pre.size() + nodes.size()));
                pre = null;
            }
        }
        if (pre != null) {
            result.add(new Triple<>(pre, null, pre.size()));
        }
        return result;
    }

    /**
     * Class takes the responsibility for node comparision.
     * <p>
     * Given two sets of pattern nodes, this class assumes that
     * the pattern node is distinct within each set. Then,
     * it compares each node in the first set with the nodes in the
     * second set. In each comparison, if two nodes are the same
     * pattern, one will be removed and the other one will be survived
     * with the pattern frequency increased.
     * <p>
     * Finally, after comparing, the class combine two sets of
     * pattern nodes to one set with merging the same pattern nodes.
     */
    private static class NodeCluster implements Callable<Set<Group>> {

        private Set<Group> _fstGroups;
        private Set<Group> _sndGroups;

        public NodeCluster(Set<Group> fstGroups, Set<Group> sndGroups) {
            _fstGroups = fstGroups;
            _sndGroups = sndGroups;
        }

        @Override
        public Set<Group> call() {
            if (_fstGroups != null && _sndGroups != null) {
                LevelLogger.debug(Thread.currentThread().getName() + " : "
                        + (_fstGroups.size() + " | " + _sndGroups.size()));
                Group next;
                for (Group fstGroup : _fstGroups) {
                    for (Iterator<Group> iter = _sndGroups.iterator(); iter.hasNext(); ) {
                        next = iter.next();
                        if (fstGroup.matches(next)) {
                            fstGroup.addIsomorphicPatternPath(next.getRepresentPatternFile());
                            fstGroup.addIsomorphicPatternPaths(next.getIsomorphicPatternPath());
                            iter.remove();
                            break;
                        }
                    }
                }
                Set<Group> result = new HashSet<>();
                result.addAll(_fstGroups);
                result.addAll(_sndGroups);
                return result;
            } else if (_fstGroups != null) {
                return new HashSet<>(_fstGroups);
            } else if (_sndGroups != null) {
                return new HashSet<>(_sndGroups);
            } else {
                return new HashSet<>();
            }
        }
    }

}
