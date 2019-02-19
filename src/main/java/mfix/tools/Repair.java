/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
import mfix.common.util.Constant;
import mfix.common.util.ExecuteCommand;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.Matcher;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
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

    private boolean validate(Subject subject, String clazzName, String source) {
        if (subject.comileFile()) {
            return new JCompiler().compile(subject, clazzName, source);
        } else {
            List<String> message = ExecuteCommand.executeCompiling(subject);
            return subject.compileSuccess(message);
        }
    }

    private Map<Pair<String, Integer>, Set<String>> method2PatternFiles;
    private Set<String> bannedAPIs;
    private String pointedAPI = null;

    private boolean extractAndSave(int timeout, String filePath, String file) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                PatternExtractor extractor = new PatternExtractor();
                Set<Pattern> patternCandidates = extractor.extractPattern(
                        filePath + "/buggy-version/" + file,
                        filePath + "/fixed-version/" + file);
                boolean sucess = false;
                for (Pattern fixPattern : patternCandidates) {
                    if (fixPattern.getAllModifications().isEmpty()
                            || fixPattern.getUniversalAPIs().isEmpty()) {
                        continue;
                    }
                    sucess = true;
                    MethDecl methDecl = (MethDecl) fixPattern.getPatternNode();
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

    private Pattern readPattern(String patternFile) {

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
            LevelLogger.info("Serialize pattern : " + patternFile);
            success = extractAndSave(Constant.PATTERN_EXTRACT_TIMEOUT, filePath, file);
        } else {
            // Already saved!
            LevelLogger.info("Existing pattern : " + patternSerializePath);
        }

        Pattern fixPattern = null;
        if (success) {
            LevelLogger.info("Try match and fix!");
            try {
                fixPattern = (Pattern) Utils.deserialize(patternSerializePath);
            } catch (IOException | ClassNotFoundException e) {
                LevelLogger.error("Deserialize pattern failed!", e);
                return null;
            }

        } else {
            LevelLogger.error("Serialization failed!");
        }

        return fixPattern;
    }

    private Set<String> listPatternFiles(Pair<String, Integer> pair) {
        String methodName = pair.getFirst();
        int methodArgsNum = pair.getSecond();

        if (pointedAPI != null && !pointedAPI.equals(methodName)) {
            return new HashSet<>();
        }

        return method2PatternFiles.getOrDefault(new Pair<>(methodName, methodArgsNum), new HashSet<>());
    }

    private int tryFix(Subject subject, Node bNode, Set<String> buggyMethodVar,
                       StringBuffer aboveBuggyNode, StringBuffer belowBuggyNode) {

        String origin = bNode.toSrcString().toString();
        String buggyFile = bNode.getFileName();
        String clazzName = new File(buggyFile).getName();
        String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                buggyFile.replace("/", "_") + ".diff");

        LevelLogger.info("Method: " + ((MethDecl) bNode).getName().getName());

        Set<String> alreadyGenerated = new HashSet<>();
        int successRepair = 0;

        Set<Pair<String, Integer>> containMethodInvs = bNode.getUniversalAPIs(false, new HashSet<>());

        for (Pair<String, Integer> pair : containMethodInvs) {
            Set<String> patternFileList = listPatternFiles(pair);

            LevelLogger.info("Size of patternList : " + patternFileList.size());
            LevelLogger.info("Start matching!");

            for (String f : patternFileList) {
                if (successRepair > Constant.FIX_NUMBER) {
                    return successRepair;
                }
                Pattern pattern = readPattern(f);
                if (pattern == null) continue;
                String patterFileName = pattern.getFileName();

                Set<MatchInstance> fixPositions = Matcher.tryMatch(bNode, pattern);

                for (MatchInstance matchInstance : fixPositions) {
                    if (successRepair > Constant.FIX_NUMBER) {
                        return successRepair;
                    }

                    matchInstance.apply();
                    StringBuffer fixedProg;
                    try{
                        fixedProg = bNode.adaptModifications(buggyMethodVar, matchInstance.getStrMap());
                    } catch (Exception e) {
                        LevelLogger.error("AdaptModification causes exception ....", e);
                        continue;
                    }

                    if (fixedProg == null) continue;
                    String fixed = fixedProg.toString();

                    if (origin.equals(fixed) || alreadyGenerated.contains(fixed)) {
                        continue;
                    }

                    alreadyGenerated.add(fixed);
                    StringBuffer replaced = new StringBuffer(aboveBuggyNode);
                    replaced.append("\n");
                    replaced.append(fixed);
                    replaced.append("\n");
                    replaced.append(belowBuggyNode);

                    if (subject == null || validate(subject, clazzName, replaced.toString())) {
                        LevelLogger.debug(pattern.getFileName());
                        LevelLogger.debug("------------ Origin ---------------");
                        LevelLogger.debug(origin);
                        LevelLogger.debug("------------ Solution ---------------");
                        LevelLogger.debug(fixedProg);
                        LevelLogger.debug("------------ End ---------------");
                        LevelLogger.info("Find a solution!");

                        TextDiff diff = new TextDiff(origin, fixedProg.toString());
                        JavaFile.writeStringToFile(tarFile, "FILE:" + buggyFile + "\n" +
                                "PATTERN:" + patterFileName + "\n" +
                                "------------ Origin ---------------\n" + origin + "\n" +
                                "------------ Solution --------------\n" + diff + "\n" +
                                "---------------\n", true);

                        if ((++successRepair) >= Constant.FIX_NUMBER) {
                            return successRepair;
                        }
                    }
                    matchInstance.reset();
                }
            }
        }
        return successRepair;
    }

    private int tryFix(Subject subject, String buggyFile) {

        Set<Node> buggyNodes = Utils.getAllMethods(buggyFile);
        Map<Integer, Set<String>> buggyFileVarMap = NodeUtils.getUsableVarTypes(buggyFile);
        List<String> source = JavaFile.readFileToStringList(buggyFile);
        int totalFix = 0;
        for (Node node : buggyNodes) {
            StringBuffer before = new StringBuffer();
            StringBuffer after = new StringBuffer();
            int start = node.getStartLine(), end = node.getEndLine();
            for (int i = 0; i < start; i++) {
                before.append(source.get(i) + "\n");
            }
            for (int i = end + 1; i < source.size(); i++) {
                after.append(source.get(i) + "\n");
            }

            Set<String> vars = buggyFileVarMap.getOrDefault(node.getStartLine(), new HashSet<>());
            totalFix += tryFix(subject, node, vars, before, after);

        }
        return totalFix;
    }

    private void repairFiles(Subject subject, List<String> bFiles) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        for (String file : bFiles) {
            // clear previous result
            String tarFile = Utils.join(Constant.SEP, Constant.RESULT_PATH, Constant.PATTERN_VERSION,
                    file.replace("/", "_") + ".diff");

            JavaFile.writeStringToFile(tarFile, "", false);

            int patch = tryFix(subject, file);

            if (patch == 0) {
                new File(tarFile).delete();
            }
            String message = "Finish : " + file + " > patch : " + patch + " | " + simpleDateFormat.format(new Date());
            System.out.println(message);
            LevelLogger.info(message);
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
            formatter.printHelp("<command> -f <arg> | -p <arg> | xml [-m <arg>] ", options);
            System.exit(1);
        }

        if (cmd.hasOption("m")) {
            pointedAPI = cmd.getOptionValue("m");
            LevelLogger.debug("pointedAPI: " + pointedAPI);
        }

        List<String> bfiles = new LinkedList<>();
        List<Subject> subjects = null;

        if (cmd.hasOption("xml")) {
            subjects = Utils.getSubjectFromXML(Constant.DEFAULT_SUBJECT_XML);
        } else if (cmd.hasOption("f")) {
            bfiles.add(cmd.getOptionValue("f"));
        } else if (cmd.hasOption("p")) {
            bfiles = JavaFile.ergodic(cmd.getOptionValue("p"), new LinkedList<>());
        }

        bannedAPIs = JavaFile.readFileToStringSet(Constant.BANNED_API_FILE);
        method2PatternFiles = Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, bannedAPIs);

        if (subjects == null) {
            repairFiles(null, bfiles);
        } else {
            for (Subject subject : subjects) {
                bfiles = JavaFile.ergodic(subject.getHome() + subject.getSsrc(),
                        new LinkedList<>());
                repairFiles(subject, bfiles);
            }
        }
    }

    public static void main(String[] args) {
        Repair repair = new Repair();
        repair.repair(args);
    }
}
