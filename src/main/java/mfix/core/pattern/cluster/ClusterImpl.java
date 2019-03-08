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

    public void callback(Set<Group> set) {
        update(set, true);
    }

    public Set<Set<Group>> clusteredNodeSet() {
        return update(null, false);
    }

    private synchronized Set<Set<Group>> update(Set<Group> set, boolean isSubThread) {
        Set<Set<Group>> result = null;
        if (isSubThread) {
            _returnedNodes.add(set);
        } else {
            result = _returnedNodes;
            _returnedNodes = new HashSet<>();
        }
        return result;
    }

    public Set<Group> cluster(Set<Pattern> patterns) {
        Set<Group> results = new HashSet<>();
        Set<Pair<Set<Group>, Set<Group>>> compareClusters = initClusterPair(patterns);
        int currTaskCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(_maxThreadCount);
        List<Future<Void>> threadResultList = new LinkedList<>();
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
            while (iter.hasNext()) {
                Pair<Set<Group>, Set<Group>> pair = iter.next();
                Future<Void> future = threadPool.submit(new NodeCluster(pair.getFirst(), pair.getSecond(),
                        this));
                threadResultList.add(future);
                currTaskCount++;
            }
            compareClusters.clear();
            Set<Set<Group>> sets = clusteredNodeSet();
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
    private static class NodeCluster implements Callable<Void> {

        private Set<Group> _fstGroups;
        private Set<Group> _sndGroups;
        private ClusterImpl _callback;

        public NodeCluster(Set<Group> fstGroups, Set<Group> sndGroups, ClusterImpl callback) {
            _fstGroups = fstGroups;
            _sndGroups = sndGroups;
            _callback = callback;
        }

        @Override
        public Void call() {
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
                        }
                    }
                }
                Set<Group> result = new HashSet<>();
                result.addAll(_fstGroups);
                result.addAll(_sndGroups);
                _callback.callback(result);
            } else if (_fstGroups != null) {
                _callback.callback(new HashSet<>(_fstGroups));
            } else if (_sndGroups != null) {
                _callback.callback(new HashSet<>(_sndGroups));
            }
            return null;
        }
    }

}
