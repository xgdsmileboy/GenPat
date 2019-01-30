/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.MatchInstance;
import mfix.core.node.NodeUtils;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.match.Matcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Main {

    private static Map<Pair<String, Integer>, Set<String>> method2PatternFiles;

    private static Set<String> fixedRet = new HashSet<>();

    private static void loadAPI() {
        LevelLogger.info("Start Load API Mappings!");
        method2PatternFiles = new HashMap<>();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(Constant.API_MAPPING_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        int cnt = 0;

        while (true) {
            try {
                String str = bufferedReader.readLine();
                if (str == null) {
                    break;
                }
                String[] splited = str.split("\\s+");
                String MethodName = splited[0];
                Integer MethodArgsNum = Integer.parseInt(splited[1]);
                String patternFile = splited[2];

                Pair<String, Integer> key = new Pair<>(MethodName, MethodArgsNum);
                if (!method2PatternFiles.containsKey(key)) {
                    method2PatternFiles.put(key, new HashSet<>());
                }
                method2PatternFiles.get(key).add(patternFile);

                if ((++cnt) % 100000 == 0) {
                    LevelLogger.debug(cnt);
                }
                if (cnt >= Constant.PATTERN_NUMBER) {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try {
            bufferedReader.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        LevelLogger.info("Load API Mappings successfully!");
    }


    private static void tryMatchAndFix(Node buggy, Node pattern, Set<String> buggyMethodVar,
                                       String buggyFile, String patternFile) {

        Set<MatchInstance> fixPositions = Matcher.tryMatch(buggy, pattern);
        String origin = buggy.toSrcString().toString();

        for (MatchInstance matchInstance : fixPositions) {
            matchInstance.apply();

            StringBuffer fixedProg = buggy.adaptModifications(buggyMethodVar);

            if (fixedProg != null) {
                String fixed = fixedProg.toString().replaceAll(" ", "");

                if (!fixed.equals(origin.replaceAll(" ", ""))) {
                    if (fixedRet.contains(fixed)) {
                        continue;
                    }
                    fixedRet.add(fixed);

                    LevelLogger.debug(patternFile);
                    LevelLogger.debug("------------ Origin ---------------");
                    LevelLogger.debug(origin);
                    LevelLogger.debug("------------ Solution ---------------");
                    LevelLogger.debug(fixedProg);
                    LevelLogger.debug("------------ End ---------------");
                    LevelLogger.info("Find a solution!");

                    JavaFile.writeStringToFile(Utils.join(Constant.SEP, Constant.RESULT_PATH, "/new_fix_result.txt"),
                            "FILE:" + buggyFile + "\n" + "PATTERN:" + patternFile + "\n" +
                                    "------------ Origin ---------------\n" + origin + "\n" +
                                    "------------ Solution --------------\n" + fixedProg + "\n" +
                                    "---------------\n", true);
                }
            }
            matchInstance.reset();
        }
    }


    private static boolean extractAndSave(String filePath, String file) throws Exception {
        Set<Node> patternCandidates = PatternExtractor.extractPattern(
                filePath + "/buggy-version/" + file,
                filePath + "/fixed-version/" + file);

        for (Node fixPattern : patternCandidates) {
            MethDecl methDecl = (MethDecl) fixPattern;
            String patternFuncName = methDecl.getName().getName();

            String savePatternPath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION,
                    file + "-" + patternFuncName + ".pattern");
            LevelLogger.info("Save pattern: " + savePatternPath);
            Utils.serialize(fixPattern, savePatternPath);
        }

        return true;
    }

    private static void timeoutMethod(int timeout, String filePath, String file) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return extractAndSave(filePath, file);
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(futureTask);

        try {
            boolean result = futureTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            LevelLogger.error("Timeout or other error for" + filePath + " " + file);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
    }


    private static void tryFix(String buggyFile) {

        Set<Node> buggyNodes = Utils.getAllMethods(buggyFile);
        Map<Integer, Set<String>> buggyFileVarMap = NodeUtils.getUsableVarTypes(buggyFile);

        for (Node node : buggyNodes) {
            fixedRet.clear();
            LevelLogger.debug("Method: " + ((MethDecl) node).getName().getName());

            Set<String> buggyMethodVar = buggyFileVarMap.getOrDefault(node.getStartLine(), new HashSet<>());
            Set<Pair<String, Integer>> containMethodInvs = node.getUniversalAPIs(false, new HashSet<>());

            for (Pair<String, Integer> pair : containMethodInvs) {
                String methodName = pair.getFirst();
                int methodArgsNum = pair.getSecond();

                LevelLogger.debug(buggyFile + " contains: " + methodName + " " + methodArgsNum);

                Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<>(methodName,
                        methodArgsNum), new HashSet<>());

                LevelLogger.debug("Size of patternList : " + patternFileList.size());
                LevelLogger.debug("Start matching!");

                for (String patternFile : patternFileList) {
                    try {
                        // TODO(jiang) : The following hard encode should be optimized finally
                        int ind = patternFile.indexOf("pattern-ver4-serial");
                        int indLen = "pattern-ver4-serial".length();
                        String filePath = patternFile.substring(0, ind - 1);
                        String fileAndMethod = patternFile.substring(ind + indLen + 1);

                        int dashIndex = fileAndMethod.lastIndexOf("-");
                        String file = fileAndMethod.substring(0, dashIndex);

                        File versionAbsFolder = new File(Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION));
                        if (!versionAbsFolder.exists()) {
                            versionAbsFolder.mkdirs();
                        }

                        String patternSerializePath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION,
                                fileAndMethod);

                        if (!(new File(patternSerializePath)).exists()) {

                            timeoutMethod(30, filePath, file);
                            // TODO(rly) mark as timeout

                        } else {
                            // Already saved!
                            LevelLogger.debug("skip for " + patternSerializePath);
                        }

                        Node fixPattern = (Node) Utils.deserialize(patternSerializePath);
                        tryMatchAndFix(node, fixPattern, buggyMethodVar, buggyFile, patternSerializePath);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Please provide the java file to detect!");
            System.exit(0);
        }
        String buggyFilePath = args[0];

        loadAPI();
        tryFix(buggyFilePath);
    }
}

