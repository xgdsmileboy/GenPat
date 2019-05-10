/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.pattern.Pattern;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author: Jiajun
 * @date: 2019-05-10
 */
public class RepairMatcherImpl implements Callable<List<MatchInstance>> {

    private Node _bNode;
    private Pattern _pattern;
    private int _timeoutMin;
    private MatchLevel _level;

    private RepairMatcherImpl(Node bNode, Pattern pattern, MatchLevel level, int timeoutMin) {
        _bNode = bNode;
        _pattern = pattern;
        _level = level;
        _timeoutMin = timeoutMin;
    }

    public static List<MatchInstance> tryMatch(Node bNode, Pattern pattern, int timeoutMin) {
        return tryMatch(bNode, pattern, MatchLevel.ALL, timeoutMin);
    }

    public static List<MatchInstance> tryMatch(Node bNode, Pattern pattern, MatchLevel level, int timeoutMin) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        RepairMatcherImpl matcher = new RepairMatcherImpl(bNode, pattern, level, timeoutMin);
        Future<List<MatchInstance>> task = service.submit(matcher);
        List<MatchInstance> fixPositions = null;
        try {
            fixPositions = task.get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            LevelLogger.debug("Repair match failed!");
            task.cancel(true);
            LevelLogger.debug("Cancel task now!");
            fixPositions = new LinkedList<>();
        }
        LevelLogger.debug("Try to shut down server.");
        service.shutdownNow();
        LevelLogger.debug("Finish shutting down server.");
        return fixPositions;
    }

    @Override
    public List<MatchInstance> call() {
        return new RepairMatcher().tryMatch(_bNode, _pattern, null, _level);
    }
}
