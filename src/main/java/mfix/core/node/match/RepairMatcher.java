/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.match.metric.IScore;
import mfix.core.node.match.metric.LocationScore;
import mfix.core.node.match.metric.NodeSimilarity;
import mfix.core.node.match.metric.TokenSimilarity;
import mfix.core.pattern.Pattern;
import mfix.tools.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-04-05
 */
public class RepairMatcher implements Callable<List<MatchInstance>> {

    private Node _bNode;
    private Pattern _pattern;
    private List<Integer> _buggyLines;
    private MatchLevel _level;
    private Timer _timer;

    public RepairMatcher() {
        this(null, null, new LinkedList<>(), 30);
    }

    public RepairMatcher(Node bNode, Pattern pattern, List<Integer> buggyLines, int minutes) {
        _bNode = bNode;
        _pattern = pattern;
        _buggyLines = buggyLines;
        _timer = new Timer(minutes);
        _timer.start();
    }

    public MatchLevel getMatchLevel() {
        return _level;
    }

    @Override
    public List<MatchInstance> call() {
        _timer.start();
        _level = MatchLevel.ALL;
        List<MatchInstance> fixPositions = tryMatch(_bNode, _pattern, _buggyLines);
        if (fixPositions.isEmpty()) {
            _level = MatchLevel.TYPE;
            fixPositions = tryMatch(_bNode, _pattern, _buggyLines, MatchLevel.TYPE);
            if (fixPositions.isEmpty()) {
                _level = MatchLevel.FUZZY;
                fixPositions = tryMatch(_bNode, _pattern, _buggyLines, MatchLevel.FUZZY);
            }
        }
        return fixPositions;
    }

    /**
     * Try to figure out all possible matching solutions between
     * buggy node {@code buggy} and _pattern node {@code _pattern}.
     *
     * @param buggy   : buggy node
     * @param pattern : _pattern node
     * @return : a set of possible solutions
     */
    public List<MatchInstance> tryMatch(Node buggy, Pattern pattern) {
        return tryMatch(buggy, pattern, null);
    }

    public List<MatchInstance> tryMatch(Node buggy, Pattern pattern, List<Integer> buggyLines) {
        return tryMatch(buggy, pattern, buggyLines, Constant.MAX_INSTANCE_PER_PATTERN, MatchLevel.ALL);
    }

    public List<MatchInstance> tryMatch(Node buggy, Pattern pattern, List<Integer> buggyLines,
                                               MatchLevel level) {
        return tryMatch(buggy, pattern, buggyLines, Constant.MAX_INSTANCE_PER_PATTERN, level);
    }

    public List<MatchInstance> tryMatch(Node buggy, Pattern pattern, List<Integer> buggyLines,
                                               int topk, MatchLevel level) {
        LevelLogger.info("Try match with _level : " + level.name());
        List<Node> bNodes = new ArrayList<>(buggy.flattenTreeNode(new LinkedList<>()));
        List<Node> pNodes = new ArrayList<>(pattern.getConsideredNodes());

        int bSize = bNodes.size();
        int pSize = pNodes.size();

        // similarity metrics used for match instance sorting
        List<IScore> similarities = new LinkedList<>();
        similarities.add(new NodeSimilarity(0.33));
        similarities.add(new LocationScore(0.33, buggyLines));
        similarities.add(new TokenSimilarity(0.33));

        String buggyMethodName = NodeUtils.decorateMethodName(distilMethodName(buggy));
        String patternMethodName = NodeUtils.decorateMethodName(distilMethodName(pattern.getPatternNode()));
        ArrayList<MatchList> matchLists = new ArrayList<>(pSize);
        Map<Node, Node> nodeMap;
        Map<String, String> strMap;
        for (int i = 0; i < pSize; i++) {
            List<MatchNode> matchNodes = new LinkedList<>();
            for (int j = 0; j < bSize; j++) {
                nodeMap = new HashMap<>();
                strMap = new HashMap<>();
                strMap.put(patternMethodName, buggyMethodName);
                if (pNodes.get(i).ifMatch(bNodes.get(j), nodeMap, strMap, level)) {
                    pNodes.get(i).greedyMatchBinding(bNodes.get(j), nodeMap, strMap);
                    matchNodes.add(new MatchNode(bNodes.get(j), nodeMap, strMap, similarities));
                }
            }
            if (matchNodes.isEmpty()) {
                return new LinkedList<>();
            }
            matchLists.add(new MatchList(pNodes.get(i)).setMatchedNodes(matchNodes));
        }

        // match instance with fewer matching node first for better performance
        // fewer back-tracking
        matchLists.sort(Comparator.comparing(MatchList::nodeSize).reversed().thenComparing(MatchList::matchSize));

        List<MatchInstance> matches = permutePossibleMatches(matchLists, similarities);
        if (buggyLines != null && !buggyLines.isEmpty()) {
            Set<Integer> needToMatch = new HashSet<>(buggyLines);
            // matched node exists in the given lines (line _level fault localization)
            matches = matches.stream().filter(in -> in.modifyAny(needToMatch))
                    .sorted(Comparator.comparingDouble(MatchInstance::similarity).reversed())
                    .limit(topk).collect(Collectors.toList());
        } else {
            // sort matching instance descending by similarity
            matches = matches.stream().sorted(Comparator.comparingDouble(MatchInstance::similarity).reversed())
                    .limit(topk).collect(Collectors.toList());
        }
        LevelLogger.info("Finish match!");
        if (matches.isEmpty()) {
            System.err.println("No matches !");
        }

        return matches;
    }

    /**
     * Given the node matching result {@code matchLists},
     * permute all possible matching solutions.
     *
     * @param matchLists : node matching result
     * @return : all possible matching solutions
     */
    private List<MatchInstance> permutePossibleMatches(ArrayList<MatchList> matchLists,
                                                             List<IScore> similarities) {
        List<MatchInstance> results = new LinkedList<>();
        matchNext(new HashMap<>(), matchLists, 0, new HashSet<>(), results, similarities);
        return results;
    }

    /**
     * recursively match each node in the _pattern based on the matching result in {@code list}
     *
     * @param matchedNodeMap : already matched node maps
     * @param list           : contains all nodes can be matched for each node in the _pattern
     * @param i              : index of _pattern node in {@code list} to match
     * @param alreadyMatched : a set of node to contain already matched node in the buggy code
     *                       to avoid duplicate matching.
     * @param instances      : contains possible matching solutions
     */
    private void matchNext(Map<Node, MatchNode> matchedNodeMap, List<MatchList> list, int i,
                                  Set<Node> alreadyMatched, List<MatchInstance> instances,
                                  List<IScore> similarities) {
        if (_timer.timeout() || instances.size() >= 2 * Constant.MAX_INSTANCE_PER_PATTERN) {
            return;
        }
        if (i == list.size()) {
            Map<Node, Node> nodeMap = new HashMap<>();
            Map<String, String> strMap = new HashMap<>();
            for (Map.Entry<Node, MatchNode> entry : matchedNodeMap.entrySet()) {
                nodeMap.putAll(entry.getValue().getNodeMap());
                strMap.putAll(entry.getValue().getStrMap());
            }
            MatchInstance matchInstance = new MatchInstance(nodeMap, strMap);
            matchInstance.computeSimilarity(similarities);
            instances.add(matchInstance);
        } else {
            Iterator<MatchNode> itor = list.get(i).getIterator();
            Node toMatch, curNode = list.get(i).getNode();
            MatchNode curMatchNode;
            while (itor.hasNext()) {
                curMatchNode = itor.next();
                toMatch = curMatchNode.getNode();
                if (alreadyMatched.contains(toMatch)) {
                    continue;
                }
                if (checkCompatibility(matchedNodeMap, curNode, curMatchNode)) {
                    matchedNodeMap.put(curNode, curMatchNode);
                    alreadyMatched.add(toMatch);
                    matchNext(matchedNodeMap, list, i + 1, alreadyMatched, instances, similarities);
                    matchedNodeMap.remove(curNode);
                    alreadyMatched.remove(toMatch);
                }
            }
        }
    }

    /**
     * Given already matched node pairs {@code matchedNodeMap},
     * checking syntax relation and dependency relation compatibility
     * for the new match between {@code curNode} and {@code curMatchNode}.
     *
     * @param matchedNodeMap : already matched node pairs
     * @param curNode        : current node under match (in _pattern)
     * @param curMatchNode   : the node to match (in buggy code)
     * @return : {@code true} if node {@code curNode} can match {@code curMatchNode},
     * otherwise {@code false}
     */
    private boolean checkCompatibility(Map<Node, MatchNode> matchedNodeMap, Node curNode,
                                              MatchNode curMatchNode) {
        Node node, previous;
        MatchNode matchNode;
        Node toMatch = curMatchNode.getNode();
        for (Map.Entry<Node, MatchNode> entry : matchedNodeMap.entrySet()) {
            if (_timer.timeout()) return false;
            node = entry.getKey();
            matchNode = entry.getValue();
            previous = matchNode.getNode();
            if ((node.isParentOf(curNode) && !previous.isParentOf(toMatch))
                    || (curNode.isParentOf(node) && !toMatch.isParentOf(previous))
                    || (dataDependOn(node, curNode) && !dataDependOn(previous, toMatch))
                    || (dataDependOn(curNode, node) && !dataDependOn(toMatch, previous))) {
//                    || (node.isDataDependOn(curNode) && !previous.isDataDependOn(toMatch))
//                    || (curNode.isDataDependOn(node) && !toMatch.isDataDependOn(previous))) {
//                    || node.isControlDependOn(curNode) != previous.isControlDependOn(toMatch)){
                return false;
            }
            Map<Node, Node> nodeMap = matchNode.getNodeMap();
            for (Map.Entry<Node, Node> inner : curMatchNode.getNodeMap().entrySet()) {
                if (nodeMap.containsKey(inner.getKey()) && nodeMap.get(inner.getKey()) != inner.getValue()) {
                    return false;
                }
            }
            Map<String, String> strMap = matchNode.getStrMap();
            for (Map.Entry<String, String> inner : curMatchNode.getStrMap().entrySet()) {
                if (strMap.containsKey(inner.getKey()) && !strMap.get(inner.getKey()).equals(inner.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean dataDependOn(Node src, Node dep) {
        Set<Node> nodes = src.recursivelyGetDataDependency(new HashSet<>());
        return nodes.contains(dep);
    }

    private SName distilMethodName(Node node) {
        if (node != null && node instanceof MethDecl) {
            return ((MethDecl) node).getName();
        }
        return null;
    }
}
