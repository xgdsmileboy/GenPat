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
public class ClusterImpl {

    private int _maxThreadCount = 10;
    private volatile Set<Set<Group>> _returnedNodes;

    public ClusterImpl() {
        _returnedNodes = new HashSet<>();
    }

    public Set<Group> cluster(Set<Pattern> patterns) {
        Set<Group> results = new HashSet<>();
        Set<Pair<Set<Group>, Set<Group>>> compareClusters = initClusterPair(patterns);
        int currTaskCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(_maxThreadCount);
        List<Future<Set<Group>>> threadResultList = new LinkedList<>();
        while (true) {
            if (threadResultList.isEmpty() && compareClusters.size() == 1) {
                Pair<Set<Group>, Set<Group>> pair = compareClusters.iterator().next();
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
            Iterator<Pair<Set<Group>, Set<Group>>> iter = compareClusters.iterator();
            while (iter.hasNext() && currTaskCount < _maxThreadCount) {
                Pair<Set<Group>, Set<Group>> pair = iter.next();
                Future<Set<Group>> future = threadPool.submit(new NodeCluster(pair.getFirst(), pair.getSecond()));
                threadResultList.add(future);
                currTaskCount++;
                iter.remove();
            }
            compareClusters.addAll(buildClusterPair(_returnedNodes));
            _returnedNodes = new HashSet<>();
            if (currTaskCount >= _maxThreadCount || compareClusters.isEmpty()) {
                currTaskCount -= waitSubThreads(threadResultList);
            }
        }

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
    private Set<Pair<Set<Group>, Set<Group>>> initClusterPair(Set<Pattern> patterns) {
        Set<Pair<Set<Group>, Set<Group>>> result = new HashSet<>();
        Group pre = null;
        for (Pattern pattern : patterns) {
            if (pre == null) {
                pre = new Group(pattern);
            } else {
                Set<Group> s1 = new HashSet<>();
                s1.add(pre);
                Set<Group> s2 = new HashSet<>();
                s2.add(new Group(pattern));
                result.add(new Pair<>(s1, s2));
                pre = null;
            }
        }
        if (pre != null) {
            Set<Group> s1 = new HashSet<>();
            s1.add(pre);
            result.add(new Pair<>(s1, null));
        }
        return result;
    }

    /**
     * group pattern sets as pairs for clustering
     *
     * @param groupSets : a set of pattern sets to group
     * @return : a set of pairs for clustering
     */
    private Set<Pair<Set<Group>, Set<Group>>> buildClusterPair(Set<Set<Group>> groupSets) {
        Set<Pair<Set<Group>, Set<Group>>> result = new HashSet<>();
        Set<Group> pre = null;
        for (Set<Group> nodes : groupSets) {
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
