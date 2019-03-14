/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.java.FakeSubject;
import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.locator.AbstractFaultLocalization;
import mfix.core.locator.FaultLocalizationFactory;
import mfix.core.locator.Location;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-02-03
 */
public class Repair {


    private final static String COMMAND = "<command> (-bf <arg> | -bp <arg> | -xml) -pf <arg>";
    private Options options() {
        Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(new Option("bp", "path", true, "Directory of source code for repair."));
        optionGroup.addOption(new Option("bf", "file", true, "Single file of source code for repair."));
        optionGroup.addOption(new Option("xml", "useXml", false, "Read subject from project.xml."));
        options.addOptionGroup(optionGroup);

        Option option = new Option("pf", "PatternFile", true,
                "Pattern record file which record all pattern paths.");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    private enum ValidateResult{
        COMPILE_FAILED,
        TEST_FAILED,
        PASS
    }

    private ValidateResult validate(Subject subject, String clazzName, String source) {
        if (subject.compileFile()) {
            boolean compile = new JCompiler().compile(subject, clazzName, source);
            return compile ? ValidateResult.PASS : ValidateResult.COMPILE_FAILED;
        } else if (subject.compile()){
            boolean test = subject.test();
            return test ? ValidateResult.PASS : ValidateResult.TEST_FAILED;
        } else {
            return ValidateResult.COMPILE_FAILED;
        }
    }

    private Pattern readPattern(String patternFile) {
        LevelLogger.debug("Deserialize pattern from file : " + patternFile);
        try {
            Pattern fixPattern = (Pattern) Utils.deserialize(patternFile);
            fixPattern.setPatternName(patternFile);
            return fixPattern;
        } catch (IOException | ClassNotFoundException e) {
            LevelLogger.error("Deserialize pattern failed!", e);
        }
        return null;
    }

    private List<String> filterPatterns(String patternRecords, Set<String> keys, int topK) throws IOException {
        List<Pair<String, Integer>> patterns = new LinkedList<>();
        Set<String> set = new HashSet<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(patternRecords), "UTF-8"));
        String line;
        while((line = br.readLine()) != null) {
            if (line.startsWith(Constant.DEFAULT_PATTERN_HOME)) {
                if (line.contains("#")) {
                    boolean match = true;
                    for (String s : set) {
                        if (!keys.contains(s)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        String[] info = line.split("#");
                        if (info.length != 2) {
                            LevelLogger.error("Record file format error : " + line);
                        } else {
                            patterns.add(new Pair<>(info[0], Integer.parseInt(info[1])));
                            set.clear();
                        }
                    }
                }
            } else {
                set.add(line);
            }
        }
        topK = topK > patterns.size() ? patterns.size() : topK;
        List<String> result = patterns.stream()
                .sorted(Comparator.comparingInt(Pair<String, Integer>::getSecond).reversed())
                .limit(topK).map(pair -> pair.getFirst()).collect(Collectors.toList());
        return result;
    }

    private Set<String> getKeys(Node node) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        Set<String> keys = new HashSet<>();
        while(!queue.isEmpty()) {
            Node n = queue.poll();
            queue.addAll(n.getAllChildren());
            if (NodeUtils.isSimpleExpr(n)) {
                keys.add(n.toSrcString().toString());
            }
        }
        return keys;
    }

    private void writeLog(String patternName, String buggyFile, String original, Set<String> imports,
                          String fixed, int startLine, int endLine, String logFile) {
        LevelLogger.debug(patternName);
        LevelLogger.debug("------------ Origin ---------------");
        LevelLogger.debug(original);
        LevelLogger.debug("------------ Solution ---------------");
        LevelLogger.debug(fixed);
        LevelLogger.debug("------------ End ---------------");
        LevelLogger.info("Find a solution!");

        StringBuffer b = new StringBuffer();
        for (String s : imports) {
            b.append(s).append(Constant.NEW_LINE);
        }
        b.append(fixed);

        TextDiff diff = new TextDiff(original, b.toString());
        StringBuffer buffer = new StringBuffer();
        buffer.append("FILE : ").append(buggyFile)
                .append('[')
                .append(startLine)
                .append(',')
                .append(endLine)
                .append(']')
                .append(Constant.NEW_LINE)
                .append("------------ Solution ---------------")
                .append(diff.toString())
                .append(Constant.NEW_LINE)
                .append("PATTERN : ")
                .append(patternName);

        JavaFile.writeStringToFile(logFile, buffer.toString(), true);
    }

    private int tryFix(Subject subject, Node bNode, Pattern pattern,
                       Set<String> buggyMethodVar, int successRepair, Timer timer, String logFile) {
        if (bNode == null || pattern == null || timer.timeout()) {
            return successRepair;
        }
        String origin = bNode.toSrcString().toString();
        String buggyFile = bNode.getFileName();
        List<String> sources = JavaFile.readFileToStringList(buggyFile);
        int startLine = bNode.getStartLine();
        int endLine = bNode.getEndLine();

        Set<MatchInstance> fixPositions = new Matcher().tryMatch(bNode, pattern);
        Set<String> alreadyGenerated = new HashSet<>();

        for (MatchInstance matchInstance : fixPositions) {
            if (timer.timeout()) {
                break;
            }
            matchInstance.apply();
            StringBuffer fixedCode;
            try{
                fixedCode = bNode.adaptModifications(buggyMethodVar, matchInstance.getStrMap());
            } catch (Exception e) {
                matchInstance.reset();
                LevelLogger.error("AdaptModification causes exception ....", e);
                continue;
            }
            if (fixedCode == null) {
                matchInstance.reset();
                continue;
            }

            String fixed = fixedCode.toString();
            if (origin.equals(fixed) || alreadyGenerated.contains(fixed)) {
                matchInstance.reset();
                continue;
            }

            alreadyGenerated.add(fixed);
            String code = JavaFile.sourceReplace(buggyFile, pattern.getImports(), sources, startLine, endLine, fixed);

            switch (validate(subject, buggyFile, code)) {
                case PASS:
                    writeLog(pattern.getPatternName(), buggyFile, origin, pattern.getImports(),
                            fixed, startLine, endLine, logFile);
                    if ((++successRepair) >= Constant.MAX_PATCH_NUMBER) {
                        return successRepair;
                    }
                    break;
                case TEST_FAILED:
                case COMPILE_FAILED:
                default:
            }
            matchInstance.reset();
        }
        return successRepair;
    }


    private Node getBuggyNode(String file, final int line) {
        final CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final List<MethodDeclaration> lst = new ArrayList<>(1);
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                int start = unit.getLineNumber(node.getStartPosition());
                int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
                if (start <= line && line <= end) {
                    lst.add(node);
                    return false;
                }
                return true;
            }
        });
        if (lst.isEmpty()) return null;
        NodeParser parser = new NodeParser();
        return parser.process(lst.get(0));
    }

    private void tryFix(String patternRecords, Subject subject) {
        Timer timer = new Timer(Constant.MAX_REPAIR_TIME);
        String start = timer.start();
        LevelLogger.info("Repair : " + subject.getHome() + "\n" + start);
        final Set<String> emptySet = new HashSet<>();
        AbstractFaultLocalization locator = FaultLocalizationFactory.dispatch(subject);
        List<Location> locations = locator.getLocations(Constant.MAX_REPAIR_LOCATION);
        Map<String, Map<Integer, Set<String>>> buggyFileVarMap = new HashMap<>();
        int totalFix = 0;
        for (Location location : locations) {
            if (timer.timeout() || totalFix > Constant.MAX_PATCH_NUMBER) {
                break;
            }
            LevelLogger.info("Location : " + location.getRelClazzFile() + "#" + location.getLine());
            final String file = subject.getHome() + subject.getSsrc() + location.getRelClazzFile() + ".java";
            final String logFile = Constant.PATCH_PATH + location.getRelClazzFile() + ".patch";
            Map<Integer, Set<String>> varMaps = buggyFileVarMap.get(file);
            if (varMaps == null) {
                varMaps = NodeUtils.getUsableVarTypes(file);
                buggyFileVarMap.put(file, varMaps);
            }
            Node node = getBuggyNode(file, location.getLine());

            List<String> patterns;
            try {
                patterns = filterPatterns(patternRecords, getKeys(node), Constant.TOP_K_PATTERN_EACH_LOCATION);
            } catch (IOException e) {
                LevelLogger.error("Filter patterns failed!", e);
                continue;
            }
            for (String s : patterns) {
                Pattern p = readPattern(s);
                Set<String> vars = varMaps.getOrDefault(node.getStartLine(), emptySet);
                totalFix += tryFix(subject, node, p, vars, totalFix, timer, logFile);
                if (totalFix > Constant.MAX_PATCH_NUMBER) {
                    break;
                }
            }
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        String message = "Finish : " + subject.getName() + " > patch : " + totalFix
                + " | Start : " + start + " | End : " + simpleDateFormat.format(new Date());
        System.out.println(message);
        LevelLogger.info(message);
    }

    private Pair<String, List<Subject>> parseCommandLine(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp(COMMAND, options);
            System.exit(1);
        }

        String patternFile = cmd.getOptionValue("pf");
        List<Subject> subjects = new LinkedList<>();
        if (cmd.hasOption("xml")) {
            subjects = Utils.getSubjectFromXML(Constant.DEFAULT_SUBJECT_XML);
        } else if (cmd.hasOption("f")) {
            String file = cmd.getOptionValue("f");
            String base = new File(file).getParentFile().getAbsolutePath();
            List<String> files = new LinkedList<>(Arrays.asList(file));
            FakeSubject fakeSubject = new FakeSubject(base, files);
            subjects.add(fakeSubject);
        } else if (cmd.hasOption("p")) {
            String base = cmd.getOptionValue("p");
            List<String> files = JavaFile.ergodic(base, new LinkedList<>());
            FakeSubject fakeSubject = new FakeSubject(base, files);
            subjects.add(fakeSubject);
        } else {
            return null;
        }
        return new Pair<>(patternFile, subjects);
    }

    public void repair(String[] args) {
        Pair<String, List<Subject>> pair = parseCommandLine(args);
        if (pair == null) {
            LevelLogger.error("Wrong command line input!");
            return;
        }

        String patternRecords = pair.getFirst();
        List<Subject> subjects = pair.getSecond();

        for (Subject subject : subjects) {
            tryFix(patternRecords, subject);
        }
    }

    public static void main(String[] args) {
        Repair repair = new Repair();
        repair.repair(args);
    }
}
