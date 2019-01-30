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
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.Matcher;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


public class Main {

    private static void tryMatchAndFix(Node buggy, Node pattern, Set<String> buggyMethodVar,
                                       String buggyFile, String patternFile, Set<String> alreadyGenerated) {

        Set<MatchInstance> fixPositions = Matcher.tryMatch(buggy, pattern);
        String origin = buggy.toSrcString().toString();
        String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                buggyFile.replace("/", "_") + ".diff");

        for (MatchInstance matchInstance : fixPositions) {
            matchInstance.apply();

            StringBuffer fixedProg = buggy.adaptModifications(buggyMethodVar);

            if (fixedProg != null) {
                String fixed = fixedProg.toString().replaceAll(" ", "");

                if (!fixed.equals(origin.replaceAll(" ", ""))) {
                    if (alreadyGenerated.contains(fixed)) {
                        continue;
                    }
                    alreadyGenerated.add(fixed);

                    LevelLogger.debug(patternFile);
                    LevelLogger.debug("------------ Origin ---------------");
                    LevelLogger.debug(origin);
                    LevelLogger.debug("------------ Solution ---------------");
                    LevelLogger.debug(fixedProg);
                    LevelLogger.debug("------------ End ---------------");
                    LevelLogger.info("Find a solution!");

                    TextDiff diff = new TextDiff(origin, fixedProg.toString());
                    JavaFile.writeStringToFile(tarFile, "FILE:" + buggyFile + "\n" +
                            "PATTERN:" + patternFile + "\n" +
                            "------------ Origin ---------------\n" + origin + "\n" +
                            "------------ Solution --------------\n" + diff + "\n" +
                            "---------------\n", true);
                }
            }
            matchInstance.reset();
        }
    }

    private static boolean extractAndSave(int timeout, String filePath, String file) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Set<Node> patternCandidates = PatternExtractor.extractPattern(
                        filePath + "/buggy-version/" + file,
                        filePath + "/fixed-version/" + file);

                for (Node fixPattern : patternCandidates) {
                    MethDecl methDecl = (MethDecl) fixPattern;
                    String patternFuncName = methDecl.getName().getName();

                    String savePatternPath = Utils.join(Constant.SEP, filePath,
                            Constant.PATTERN_VERSION, file + "-" + patternFuncName + ".pattern");
                    LevelLogger.info("Save pattern: " + savePatternPath);
                    Utils.serialize(fixPattern, savePatternPath);
                }
                return true;
            }
        });
        return Utils.futureTaskWithin(timeout, futureTask);
    }


    private static void tryFix(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String buggyFile,
                               String pointedAPI) {

        Set<Node> buggyNodes = Utils.getAllMethods(buggyFile);
        Map<Integer, Set<String>> buggyFileVarMap = NodeUtils.getUsableVarTypes(buggyFile);

        Set<String> alreadyGenerated = new HashSet<>();
        for (Node node : buggyNodes) {
            alreadyGenerated.clear();
            LevelLogger.info("Method: " + ((MethDecl) node).getName().getName());

            Set<String> buggyMethodVar = buggyFileVarMap.getOrDefault(node.getStartLine(), new HashSet<>());
            Set<Pair<String, Integer>> containMethodInvs = node.getUniversalAPIs(false, new HashSet<>());

            for (Pair<String, Integer> pair : containMethodInvs) {
                String methodName = pair.getFirst();
                int methodArgsNum = pair.getSecond();

                if (pointedAPI != null && !pointedAPI.equals(methodName)){
                    continue;
                }

                LevelLogger.info(buggyFile + " contains: " + methodName + " " + methodArgsNum);

                Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<>(methodName,
                        methodArgsNum), new HashSet<>());

                LevelLogger.info("Size of patternList : " + patternFileList.size());
                LevelLogger.info("Start matching!");

                for (String patternFile : patternFileList) {
                    // TODO(jiang) : The following hard encode should be optimized finally
                    int ind = patternFile.indexOf("pattern-ver4-serial");
                    int indLen = "pattern-ver4-serial".length();
                    String filePath = Constant.DATASET_PATH +  patternFile.substring(0, ind - 1);
                    String fileAndMethod = patternFile.substring(ind + indLen + 1);

                    int dashIndex = fileAndMethod.lastIndexOf("-");
                    String file = fileAndMethod.substring(0, dashIndex);

                    File versionAbsFolder = new File(Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION));
                    if (!versionAbsFolder.exists()) {
                        versionAbsFolder.mkdirs();
                    }

                    String patternSerializePath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION,
                            fileAndMethod);

                    boolean success = true;
                    if (!(new File(patternSerializePath)).exists()) {
                        LevelLogger.info("Serialize pattern : " + patternSerializePath);
                        success = extractAndSave(Constant.PATTERN_EXTRAT_TIMEOUT, filePath, file);
                    } else {
                        // Already saved!
                        LevelLogger.info("Existing pattern : " + patternSerializePath);
                    }

                    if (success) {
                        LevelLogger.info("Try match and fix!");
                        Node fixPattern;
                        try {
                             fixPattern = (Node) Utils.deserialize(patternSerializePath);
                        } catch (IOException | ClassNotFoundException e) {
                            LevelLogger.error("Deserialize pattern failed!", e);
                            continue;
                        }
                        tryMatchAndFix(node, fixPattern, buggyMethodVar, buggyFile, patternSerializePath, alreadyGenerated);
                    } else {
                        LevelLogger.error("Serialization failed!");
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


        String pointedAPI = null;
        if (args.length == 2) {
            pointedAPI = args[1];
            LevelLogger.debug("pointedAPI: " + pointedAPI);
        }

        // clear previous result
        String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                buggyFilePath.replace("/", "_") + ".diff");
        JavaFile.writeStringToFile(tarFile, "", false);

        Set<String> bannedAPIs = JavaFile.readFileToStringSet(Constant.BANNED_API_FILE);
        tryFix(Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, bannedAPIs), buggyFilePath, pointedAPI);
    }
}

