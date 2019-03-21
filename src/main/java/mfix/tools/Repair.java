/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.java.FakeSubject;
import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.locator.AbstractFaultLocator;
import mfix.core.locator.FaultLocatorFactory;
import mfix.core.locator.Location;
import mfix.core.locator.purify.CommentTestCase;
import mfix.core.locator.purify.Purification;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
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
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-02-03
 */
public class Repair {

    private Subject _subject;
    private String _logfile;
    private String _patchFile;
    private int _patchNum;
    private String _patternRecords;

    private Timer _timer;

    private Set<String> _alreadyGenerated = new HashSet<>();

    private Set<String> _allFailedTests = new HashSet<>();
    private Set<String> _alreadyFixedTests = new HashSet<>();
    private List<String> _currentFailedTests = new LinkedList<>();

    public Repair(Subject subject, String patternRecords) {
        _subject = subject;
        _patchNum = 0;
        _patternRecords = patternRecords;
        _patchFile = _subject.getPatchFile();
        _logfile = _subject.getLogFile();
        _timer = new Timer(Constant.MAX_REPAIR_TIME);
    }

    private boolean shouldStop() {
        return _patchNum > Constant.MAX_PATCH_NUMBER || _timer.timeout();
    }

    protected void setTimer(Timer timer) {
        _timer = timer;
    }

    private enum ValidateResult{
        COMPILE_FAILED,
        TEST_FAILED,
        PASS
    }

    private ValidateResult validate(String clazzName, String source) {
        if (_subject.compileFile()) {
            LevelLogger.debug("Compile single file : " + clazzName);
            boolean compile = new JCompiler().compile(_subject, clazzName, source);
            if (!compile) {
                LevelLogger.debug("Compiling single file failed!");
                return ValidateResult.COMPILE_FAILED;
            }
            LevelLogger.debug("Compiling single file success!");
        }
        if (_subject.compileProject()){
            LevelLogger.debug("Compile subject : " + _subject.getName());
            boolean compile = _subject.compile();
            if (!compile){
                LevelLogger.debug("Compiling subject failed!");
                return ValidateResult.COMPILE_FAILED;
            }
            LevelLogger.debug("Compiling subject success!");
        }

        for (String string : _currentFailedTests) {
            LevelLogger.debug("Test : " + string);
            if (!_subject.test(string)) {
                return ValidateResult.TEST_FAILED;
            }
        }

        if (_subject.testProject()) {
            LevelLogger.debug("Test project : " + _subject.getName());
            boolean test = _subject.test();
            if (!test) {
                LevelLogger.debug("Testing project failed!");
                return ValidateResult.TEST_FAILED;
            }
            LevelLogger.debug("Testing project success!");
        }

        _alreadyFixedTests.addAll(_currentFailedTests);

        try {
            _subject.restorePurifiedTest();
            for (String s : _allFailedTests) {
                if (_allFailedTests.contains(s)) {
                    continue;
                }
                if (_subject.test(s)) {
                    _alreadyFixedTests.add(s);
                }
            }
        } catch(IOException e) {}

        return ValidateResult.PASS;
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

    private List<String> filterPatterns(Set<String> keys, int topK) throws IOException {
        List<Pair<String, Integer>> patterns = new LinkedList<>();
        Set<String> set = new HashSet<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(_patternRecords),
                StandardCharsets.UTF_8));
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
                        }
                    }
                }
                set.clear();
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
        Set<String> keys = new HashSet<>();
        if (node == null) return keys;
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        while(!queue.isEmpty()) {
            Node n = queue.poll();
            queue.addAll(n.getAllChildren());
            if (NodeUtils.isSimpleExpr(n)) {
                keys.add(n.toSrcString().toString());
            }
        }
        return keys;
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
        return parser.setCompilationUnit(file, unit).process(lst.get(0));
    }


    private void writeLog(String patternName, String buggyFile, String original, Set<String> imports,
                          String fixed, int startLine, int endLine, boolean patch) {
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
                .append("------------")
                .append(patch ? "Solution" : "Candidate")
                .append("---------------")
                .append(Constant.NEW_LINE)
                .append(diff.toString())
                .append(Constant.NEW_LINE)
                .append("PATTERN : ")
                .append(patternName)
                .append(Constant.NEW_LINE)
                .append("--------------- END -----------------")
                .append(Constant.NEW_LINE);

        JavaFile.writeStringToFile(_logfile, buffer.toString(), true);
        if (patch) {
            JavaFile.writeStringToFile(_patchFile, buffer.toString(), true);
        }
    }

    protected void tryFix(Node bNode, Pattern pattern, VarScope scope, String clazzFile) {
        if (bNode == null || pattern == null || shouldStop()) {
            return;
        }
        String origin = bNode.toSrcString().toString();
        String buggyFile = bNode.getFileName();
        String oriSrcCode = JavaFile.readFileToString(buggyFile);
        List<String> sources = JavaFile.readFileToStringList(buggyFile);
        sources.add(0, "");
        int startLine = bNode.getStartLine();
        int endLine = bNode.getEndLine();

        Set<MatchInstance> fixPositions = Matcher.tryMatch(bNode, pattern);

        for (MatchInstance matchInstance : fixPositions) {
            if (shouldStop()) { break; }

            matchInstance.apply();
            StringBuffer fixedCode;
            try{
                fixedCode = bNode.adaptModifications(scope, matchInstance.getStrMap());
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
            if (origin.equals(fixed) || _alreadyGenerated.contains(fixed)) {
                matchInstance.reset();
                continue;
            }

            _alreadyGenerated.add(fixed);
            String code = JavaFile.sourceReplace(buggyFile, pattern.getImports(),
                    sources, startLine, endLine, fixed);

            TextDiff diff = new TextDiff(origin, fixed);
            LevelLogger.debug("Repair code :\n" + diff.toString());
            try {
                FileUtils.forceDeleteOnExit(new File(clazzFile));
            } catch (IOException e) {}
            switch (validate(buggyFile, code)) {
                case PASS:
                    writeLog(pattern.getPatternName(), buggyFile, origin,
                            pattern.getImports(), fixed, startLine, endLine, true);
                    _patchNum ++;
                    break;
                case TEST_FAILED:
                    writeLog(pattern.getPatternName(), buggyFile, origin,
                            pattern.getImports(), fixed, startLine, endLine, false);
                case COMPILE_FAILED:
                default:
            }
            matchInstance.reset();
        }
        JavaFile.writeStringToFile(buggyFile, oriSrcCode);
    }

    private void repair0(List<Location> locations, Map<String, Map<Integer, VarScope>> buggyFileVarMap) {
        final String srcSrc = _subject.getHome() + _subject.getSsrc();
        final String srcBin = _subject.getHome() + _subject.getSbin();

        for (Location location : locations) {
            if (shouldStop()) { break; }
            _alreadyGenerated.clear();
            LevelLogger.info("Location : " + location.getRelClazzFile() + "#" + location.getLine());

            final String file = Utils.join(Constant.SEP, srcSrc, location.getRelClazzFile());
            final String clazzFile = Utils.join(Constant.SEP, srcBin,
                    location.getRelClazzFile().replace(".java", ".class"));

            Map<Integer, VarScope> varMaps = buggyFileVarMap.get(file);
            if (varMaps == null) {
                varMaps = NodeUtils.getUsableVariables(file);
                buggyFileVarMap.put(file, varMaps);
            }
            Node node = getBuggyNode(file, location.getLine());
            if (node == null) {
                LevelLogger.error("Get faulty node failed ! " + file + "#" + location.getLine());
                continue;
            }

            List<String> patterns;
            try {
                patterns = filterPatterns(getKeys(node), Constant.TOP_K_PATTERN_EACH_LOCATION);
            } catch (IOException e) {
                LevelLogger.error("Filter patterns failed!", e);
                continue;
            }
            VarScope scope = varMaps.getOrDefault(node.getStartLine(), new VarScope());
            for (String s : patterns) {
                if (shouldStop()) { break; }
                Pattern p = readPattern(s);
                scope.reset(p.getNewVars());
                tryFix(node, p, scope, clazzFile);
            }
        }
    }

    public void repair() throws IOException {
        LevelLogger.info("Repair : " + _subject.getHome());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        _subject.backup();

        String srcSrc = _subject.getHome() + _subject.getSsrc();
        String testSrc = _subject.getHome() + _subject.getTsrc();
        String srcBin = _subject.getHome() + _subject.getSbin();
        String testBin = _subject.getHome() + _subject.getTbin();

        FileUtils.deleteDirectory(new File(srcBin));
        FileUtils.deleteDirectory(new File(testBin));

        Purification purification = new Purification(_subject);
        List<String> purifiedFailedTestCases = purification.purify(_subject.purify());
        if(purifiedFailedTestCases == null || purifiedFailedTestCases.size() == 0){
            purifiedFailedTestCases = purification.getFailedTest();
        }
        _allFailedTests.addAll(purifiedFailedTestCases);
        _subject.backupPurifiedTest();

        Map<String, Map<Integer, VarScope>> buggyFileVarMap = new HashMap<>();

        String start = _timer.start();
        LevelLogger.info(start);

        for(int currentTry = 0; currentTry < purifiedFailedTestCases.size(); currentTry ++) {
            String teString = purifiedFailedTestCases.get(currentTry);
            JavaFile.writeStringToFile(_logfile, "Current failed test : " +
                    teString + " | " + simpleDateFormat.format(new Date()) + "\n", true);
            if (_alreadyFixedTests.contains(teString)) {
                JavaFile.writeStringToFile(_logfile, "Already fixed : " + teString + "\n", true);
                continue;
            }

            _subject.restore(srcSrc);
            _subject.restorePurifiedTest();
            FileUtils.deleteDirectory(new File(srcBin));
            FileUtils.deleteDirectory(new File(testBin));

            _currentFailedTests.clear();
            if (_subject.purify()) {
                _currentFailedTests.add(teString);
            } else {
                _currentFailedTests.addAll(_allFailedTests);
            }
            CommentTestCase.comment(testSrc, purifiedFailedTestCases, new HashSet<>(_currentFailedTests));

            AbstractFaultLocator locator = FaultLocatorFactory.dispatch(_subject);
            locator.setFailedTests(_currentFailedTests);
            List<Location> locations = locator.getLocations(Constant.MAX_REPAIR_LOCATION);

            repair0(locations, buggyFileVarMap);
        }

        _subject.restore();

        String message = "Finish : " + _subject.getName() + " > patch : " + _patchNum
                + " | Start : " + start + " | End : " + simpleDateFormat.format(new Date());
        System.out.println(message);
        LevelLogger.info(message);
    }

    private final static String COMMAND = "<command> (-bf <arg> | -bp <arg> | -xml | -d4j <arg>) " +
            "-pf <arg> [-d4jhome <arg>]";

    private static Options options() {
        Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(new Option("bp", "path", true, "Directory of source code for repair."));
        optionGroup.addOption(new Option("bf", "file", true, "Single file of source code for repair."));
        optionGroup.addOption(new Option("d4j", "d4jBug", true, "Bug id in defects4j, e.g., chart_1"));
        optionGroup.addOption(new Option("xml", "useXml", false, "Read subject from project.xml."));
        options.addOptionGroup(optionGroup);

        Option option = new Option("pf", "PatternFile", true,
                "Pattern record file which record all pattern paths.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("d4jhome", "defects4jHome", true, "Home directory of defects4j buggy code.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    private static Pair<String, List<Subject>> parseCommandLine(String[] args) {
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
        } else if (cmd.hasOption("bf")) {
            String file = cmd.getOptionValue("bf");
            String base = new File(file).getParentFile().getAbsolutePath();
            List<String> files = new LinkedList<>(Arrays.asList(file));
            FakeSubject fakeSubject = new FakeSubject(base, files);
            subjects.add(fakeSubject);
        } else if (cmd.hasOption("bp")) {
            String base = cmd.getOptionValue("bp");
            List<String> files = JavaFile.ergodic(base, new LinkedList<>());
            FakeSubject fakeSubject = new FakeSubject(base, files);
            subjects.add(fakeSubject);
        } else if (cmd.hasOption("d4j")) {
            String[] ids = cmd.getOptionValue("d4j").split(",");
            String base = cmd.hasOption("d4jhome") ? cmd.getOptionValue("d4jhome") : Constant.D4J_PROJ_DEFAULT_HOME;
            for (String id : ids) {
                String name = id.split("_")[0];
                int number = Integer.parseInt(id.split("_")[1]);
                D4jSubject subject = new D4jSubject(base, name, number);
                subjects.add(subject);
            }
        } else {
            return null;
        }
        return new Pair<>(patternFile, subjects);
    }

    public static void repairAPI(String[] args) {
        Pair<String, List<Subject>> pair = parseCommandLine(args);
        if (pair == null) {
            LevelLogger.error("Wrong command line input!");
            return;
        }

        String patternRecords = pair.getFirst();
        List<Subject> subjects = pair.getSecond();
        for (Subject subject : subjects) {
            LevelLogger.info(subject.getHome() + ", " + subject.toString());
            Repair repair = new Repair(subject, patternRecords);
            try {
                repair.repair();
            } catch (IOException e) {}
        }
    }

    public static void main(String[] args) {
        Repair.repairAPI(args);
    }
}
