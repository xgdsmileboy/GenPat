/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;

import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author: Jiajun
 * @date: 2019-02-03
 */
public class Repair {

    private Options options() {
        Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(new Option("p", "path", true, "directory of source code for repair."));
        optionGroup.addOption(new Option("f", "file", true, "single file of source code for repair."));
        optionGroup.addOption(new Option("xml", "useXml", false, "read subject from project.xml."));

        options.addOptionGroup(optionGroup);

        Option option = new Option("m", "APIName", true, "the name of API to focus on.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    private int tryMatchAndFix(Subject subject, String buggyFile, String clazzName, Node buggy,
                               Set<String> buggyMethodVar, StringBuffer aboveBuggyNode,
                               StringBuffer belowBuggyNode, String patternFile, Node pattern,
                               Set<String> alreadyGenerated, int passed) {

        if (passed > Constant.FIX_NUMBER) { return passed; }

        Set<MatchInstance> fixPositions = Matcher.tryMatch(buggy, pattern);
        String origin = buggy.toSrcString().toString();
        String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                buggyFile.replace("/", "_") + ".diff");

        JCompiler compiler = JCompiler.getInstance();
        for (MatchInstance matchInstance : fixPositions) {
            if (passed > Constant.FIX_NUMBER) { return passed; }

            matchInstance.apply();
            StringBuffer fixedProg = buggy.adaptModifications(buggyMethodVar, matchInstance.getStrMap());

            if (fixedProg != null) {
                String fixed = fixedProg.toString().replaceAll(" ", "");

                if (!fixed.equals(origin.replaceAll(" ", ""))) {
                    if (alreadyGenerated.contains(fixed)) {
                        continue;
                    }

                    alreadyGenerated.add(fixed);
                    StringBuffer replaced = new StringBuffer(aboveBuggyNode);
                    replaced.append("\n");
                    replaced.append(fixed);
                    replaced.append("\n");
                    replaced.append(belowBuggyNode);

                    if (subject == null || compiler.compile(subject, clazzName, replaced.toString())) {
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

                        if ((++ passed) >= Constant.FIX_NUMBER) { return passed; }
                    }
                }
            }
            matchInstance.reset();
        }
        return passed;
    }

    private boolean extractAndSave(int timeout, String filePath, String file) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Set<Node> patternCandidates = PatternExtractor.extractPattern(
                        filePath + "/buggy-version/" + file,
                        filePath + "/fixed-version/" + file);
                boolean sucess = false;
                for (Node fixPattern : patternCandidates) {
                    if (fixPattern.getAllModifications(new HashSet<>()).isEmpty()
                            || fixPattern.getUniversalAPIs(new HashSet<>(), true).isEmpty()) {
                        continue;
                    }
                    sucess = true;
                    MethDecl methDecl = (MethDecl) fixPattern;
                    String patternFuncName = methDecl.getName().getName();

                    String savePatternPath = Utils.join(Constant.SEP, filePath,
                            Constant.PATTERN_VERSION, file + "-" + patternFuncName + ".pattern");
                    LevelLogger.info("Save pattern: " + savePatternPath);
                    Utils.serialize(fixPattern, savePatternPath);
                }
                return sucess;
            }
        });
        return Utils.futureTaskWithin(timeout, futureTask);
    }


    private int tryFix(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String pointedAPI,
                       Subject subject, String buggyFile, String clazzName, Node node, Set<String> buggyMethodVar,
                       StringBuffer aboveBuggyNode, StringBuffer belowBuggyNode) {

        Set<String> alreadyGenerated = new HashSet<>();
        int successRepair = 0;

        LevelLogger.info("Method: " + ((MethDecl) node).getName().getName());

        Set<Pair<String, Integer>> containMethodInvs = node.getUniversalAPIs(false, new HashSet<>());

        for (Pair<String, Integer> pair : containMethodInvs) {
            String methodName = pair.getFirst();
            int methodArgsNum = pair.getSecond();

            if (pointedAPI != null && !pointedAPI.equals(methodName)) {
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
                String filePath = Constant.DATASET_PATH + patternFile.substring(0, ind - 1);
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
                if (Constant.PATTERN_EXTRACT_FORCE || !(new File(patternSerializePath)).exists()) {
                    LevelLogger.info("Serialize pattern : " + patternSerializePath);
                    success = extractAndSave(Constant.PATTERN_EXTRACT_TIMEOUT, filePath, file);
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

                    successRepair = tryMatchAndFix(subject, buggyFile, clazzName, node, buggyMethodVar,
                            aboveBuggyNode, belowBuggyNode, patternFile, fixPattern, alreadyGenerated, successRepair);

                    if (successRepair >= Constant.FIX_NUMBER) return successRepair;
                } else {
                    LevelLogger.error("Serialization failed!");
                }
            }
        }
        return successRepair;
    }

    private int tryFix(Subject subject, Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String buggyFile,
                        String pointedAPI) {

        Set<Node> buggyNodes = Utils.getAllMethods(buggyFile);
        Map<Integer, Set<String>> buggyFileVarMap = NodeUtils.getUsableVarTypes(buggyFile);
        List<String> source = JavaFile.readFileToStringList(buggyFile);
        String clazzName = new File(buggyFile).getName();
        int totalFix = 0;
        for (Node node : buggyNodes) {
            StringBuffer before = new StringBuffer();
            StringBuffer after = new StringBuffer();
            int start = node.getStartLine(), end = node.getEndLine();
            for (int i = 0; i < start; i ++) {
                before.append(source.get(i) + "\n");
            }
            for (int i = end + 1; i < source.size(); i++) {
                after.append(source.get(i) + "\n");
            }

            totalFix += tryFix(method2PatternFiles, pointedAPI, subject, buggyFile, clazzName, node,
            buggyFileVarMap.getOrDefault(node.getStartLine(), new HashSet<>()), before, after);

        }
        return totalFix;
    }

    private void repairFiles(Subject subject, List<String> bFiles, Set<String> bannedAPIs, String pointedAPI) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        for (String file : bFiles) {
            // clear previous result
            String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                    file.replace("/", "_") + ".diff");

            JavaFile.writeStringToFile(tarFile, "", false);

            int patch = tryFix(subject, Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, bannedAPIs),
                    file, pointedAPI);

            System.out.println("Finish : " + file + " > patch : " + patch + " | " + simpleDateFormat.format(new Date()));
        }
    }

    public void repair(String[] args) {

        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String pointedAPI = null;
        if (cmd.hasOption("m")) {
            pointedAPI = cmd.getOptionValue("m");
            LevelLogger.debug("pointedAPI: " + pointedAPI);
        }

        Set<String> bannedAPIs = JavaFile.readFileToStringSet(Constant.BANNED_API_FILE);

        List<String> bfiles = new LinkedList<>();
        List<Subject> subjects = null;

        if (cmd.hasOption("xml")) {
            subjects = Utils.getSubjectFromXML(Constant.DEFAULT_SUBJECT_XML);
        } else if (cmd.hasOption("f")) {
            bfiles.add(cmd.getOptionValue("f"));
        } else if (cmd.hasOption("p")) {
            bfiles = JavaFile.ergodic(cmd.getOptionValue("p"), new LinkedList<>());
        }

        if (subjects == null) {
            repairFiles(null, bfiles, bannedAPIs, pointedAPI);
        } else {
            for (Subject subject : subjects) {
                bfiles = JavaFile.ergodic(subject.getHome() + subject.getSsrc(),
                        new LinkedList<>());
                repairFiles(subject, bfiles, bannedAPIs, pointedAPI);
            }
        }
    }

    public static void main(String[] args) {
        Repair repair = new Repair();
        repair.repair(args);
    }
}
