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
import mfix.common.java.Subject;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Triple;
import mfix.common.util.Utils;
import mfix.core.locator.AbstractFaultLocator;
import mfix.core.locator.FaultLocatorFactory;
import mfix.core.locator.Location;
import mfix.core.locator.purify.CommentTestCase;
import mfix.core.locator.purify.Purification;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Modification;
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-04-29
 */
public class PatternRanking {

    private Subject _subject;
    private String _logfile;
    private String _patchFile;
    private int _patchNum;
    private Set<String> _patternRecords;
    private String _singlePattern;

    private Timer _timer;

    private Set<String> _alreadyGenerated = new HashSet<>();

    private Set<String> _allFailedTests = new HashSet<>();
    private Set<String> _alreadyFixedTests = new HashSet<>();
    private List<String> _currentFailedTests = new ArrayList<>();

    private Set<String> _priorityPattern = new HashSet<>();

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");

    public PatternRanking(Subject subject, Set<String> patternRecords, String singlePattern) {
        _subject = subject;
        _patchNum = 0;
        _patternRecords = patternRecords;
        _singlePattern = singlePattern;
        _patchFile = _subject.getPatchFile();
        _logfile = _subject.getLogFile();
        _timer = new Timer(Constant.MAX_REPAIR_TIME);
    }

    private boolean shouldStop() {
        return _patchNum >= Constant.MAX_PATCH_NUMBER || _timer.timeout();
    }

    protected void setTimer(Timer timer) {
        _timer = timer;
    }

    private Pattern readPattern(String patternFile) {
        LevelLogger.debug("Deserialize pattern from file : " + patternFile);
        try {
            Pattern fixPattern = (Pattern) Utils.deserialize(patternFile);
            fixPattern.setPatternName(patternFile);
            Set<Modification> modifications = fixPattern.getAllModifications();
            for (Modification modification : modifications) {
                if (Constant.FILTER_DELETION && modification instanceof Deletion) {
                    Deletion del = (Deletion) modification;
                    if (del.getDelNode() != null && del.getDelNode().noBinding()) {
                        return null;
                    }
                }
            }
            return fixPattern;
        } catch (IOException | ClassNotFoundException e) {
            LevelLogger.error("Deserialize pattern failed!", e);
        }
        return null;
    }

    private List<String> filterPatterns(Set<String> keys, int topK) throws IOException {
        List<Pair<String, Integer>> patterns = new LinkedList<>();
        Set<String> set = new HashSet<>();
        for (String file : _patternRecords) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                    StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
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
            br.close();
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
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            queue.addAll(n.getAllChildren());
            String type = n.getTypeStr();
            if (type != null && !"?".equals(type)) {
                keys.add(type);
            }
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


    private void repair0(List<Location> locations, Map<String, Map<Integer, VarScope>> buggyFileVarMap) {
        final String srcSrc = _subject.getHome() + _subject.getSsrc();
        final String srcBin = _subject.getHome() + _subject.getSbin();

        for (Location location : locations) {
            if (shouldStop()) {
                break;
            }
            _alreadyGenerated.clear();
            String message = "Location : " + location.toString();
            LevelLogger.info(message);
            JavaFile.writeStringToFile(_logfile, message + "\n", true);

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
                String err = "Get faulty node failed ! " + file + "#" + location.getLine();
                LevelLogger.error(err);
                JavaFile.writeStringToFile(_logfile, err + "\n", true);
                continue;
            }
            String retType = "void";
            Set<String> exceptions = new HashSet<>();
            if (node instanceof MethDecl) {
                MethDecl decl = (MethDecl) node;
                retType = decl.getRetTypeStr();
                exceptions.addAll(decl.getThrows());
            }

            List<String> patterns;
            if (_singlePattern != null) {
                patterns = new LinkedList<>();
                patterns.add(_singlePattern);
            } else {
                try {
                    patterns = filterPatterns(getKeys(node), Constant.TOP_K_PATTERN_EACH_LOCATION);
                } catch (IOException e) {
                    LevelLogger.error("Filter patterns failed!", e);
                    JavaFile.writeStringToFile(_logfile, "Filter patterns failed!\n", true);
                    continue;
                }
            }
            VarScope scope = varMaps.getOrDefault(node.getStartLine(), new VarScope());
            List<Integer> buggyLines = location.getConsideredLines();
            if (node instanceof MethDecl) {
                for (Node n : ((MethDecl) node).getArguments()) {
                    buggyLines.add(n.getStartLine());
                }
            }
            for (String s : _priorityPattern) {
                if (shouldStop()) {
                    break;
                }
                Pattern p = readPattern(s);
                if (p == null) {
                    continue;
                }
                scope.reset(p.getNewVars());
            }
            for (String s : patterns) {
                if (shouldStop()) {
                    break;
                }
                if (_priorityPattern.contains(s)) {
                    continue;
                }
                Pattern p = readPattern(s);
                if (p == null) {
                    continue;
                }
                scope.reset(p.getNewVars());
            }
        }
    }

    public void repair() {
        String message = "Repair : " + _subject.getName() + "_" + _subject.getId();
        JavaFile.writeStringToFile(_logfile, message + "\n", false);
        LevelLogger.info(message);
        _subject.backup();
        _priorityPattern = new HashSet<>();

        String srcSrc = _subject.getHome() + _subject.getSsrc();
        String testSrc = _subject.getHome() + _subject.getTsrc();
        String srcBin = _subject.getHome() + _subject.getSbin();
        String testBin = _subject.getHome() + _subject.getTbin();

        Utils.deleteDirs(srcBin, testBin);
        if (_subject instanceof D4jSubject) {
            // first check compilable
            if (!_subject.compile()) {
                JavaFile.writeStringToFile(_logfile, "Compile failed at the beginning!" + "\n", true);
                return;
            }
        }

        Purification purification = new Purification(_subject);
        List<String> purifiedFailedTestCases = purification.purify(_subject.purify());
        if (purifiedFailedTestCases == null || purifiedFailedTestCases.size() == 0) {
            purifiedFailedTestCases = purification.getFailedTest();
        }
        _allFailedTests.addAll(purifiedFailedTestCases);
        _subject.backupPurifiedTest();

        Map<String, Map<Integer, VarScope>> buggyFileVarMap = new HashMap<>();

        String start = _timer.start();
        LevelLogger.info(start);

        int all = 0;
        for (int currentTry = 0; currentTry < purifiedFailedTestCases.size(); currentTry++) {
            _patchNum = 0;
            String teString = purifiedFailedTestCases.get(currentTry);
            JavaFile.writeStringToFile(_logfile, "Current failed test : " +
                    teString + " | " + simpleDateFormat.format(new Date()) + "\n", true);
            LevelLogger.debug("Current failed test : " + teString);
            if (_alreadyFixedTests.contains(teString)) {
                JavaFile.writeStringToFile(_logfile, "Already fixed : " + teString + "\n", true);
                continue;
            }

            _subject.restore(srcSrc);
            _subject.restorePurifiedTest();
            Utils.deleteDirs(srcBin, testBin);

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
            all += _patchNum;
        }

        _subject.restore();

        message = "Finish : " + _subject.getName() + "-" + _subject.getId() + " > patch : " + all
                + " | Start : " + start + " | End : " + simpleDateFormat.format(new Date());
        JavaFile.writeStringToFile(_logfile, message + "\n", true);
        LevelLogger.info(message);
    }

    private final static String COMMAND = "<command> (-bf <arg> | -bp <arg> | -xml | -d4j <arg>) " +
            "(-pf <arg> | -pattern <arg>) [-d4jhome <arg>]";

    private static Options options() {
        Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(new Option("bp", "path", true, "Directory of source code for repair."));
        optionGroup.addOption(new Option("bf", "file", true, "Single file of source code for repair."));
        optionGroup.addOption(new Option("d4j", "d4jBug", true, "Bug id in defects4j, e.g., chart_1"));
        optionGroup.addOption(new Option("xml", "useXml", false, "Read subject from project.xml."));
        options.addOptionGroup(optionGroup);

        optionGroup = new OptionGroup();
        optionGroup.setRequired(true);

        optionGroup.addOption(new Option("pf", "PatternFile", true,
                "Pattern record file which record all pattern paths."));
        optionGroup.addOption(new Option("pattern", "pattern", true,
                "Single pattern file"));
        options.addOptionGroup(optionGroup);

        Option option = new Option("d4jhome", "defects4jHome", true, "Home directory of defects4j buggy code.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    private static Triple<String, Set<String>, List<Subject>> parseCommandLine(String[] args) {
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

        String singlePattern = null;
        if (cmd.hasOption("pattern")) {
            singlePattern = cmd.getOptionValue("pattern");
        }
        Set<String> patternFile = null;
        if (cmd.hasOption("pf")) {
            patternFile = new HashSet<>(Arrays.asList(cmd.getOptionValue("pf").split(",")));
        }
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
            boolean memCompile = true;
            // math_1,lang_1-10,
            for (String id : ids) {
                String[] info = id.split("_");
                if (info.length != 2) {
                    LevelLogger.error("Input format error : " + id);
                    continue;
                }
                String name = info[0];
                String[] seqs = info[1].split("-");
                D4jSubject subject;
                if (seqs.length == 1) {
                    int number = Integer.parseInt(seqs[0]);
                    subject = new D4jSubject(base, name, number, memCompile);
                    subjects.add(subject);
                } else if (seqs.length == 2) {
                    int start = Integer.parseInt(seqs[0]);
                    int end = Integer.parseInt(seqs[1]);
                    for (; start <= end; start++) {
                        subject = new D4jSubject(base, name, start, memCompile);
                        subjects.add(subject);
                    }
                } else {
                    LevelLogger.error("Input format error : " + id);
                }
            }
        } else {
            return null;
        }
        return new Triple<>(singlePattern, patternFile, subjects);
    }

    public static void repairAPI(String[] args) {
        Triple<String, Set<String>, List<Subject>> pair = parseCommandLine(args);
        if (pair == null) {
            LevelLogger.error("Wrong command line input!");
            return;
        }

        String singlePattern = pair.getFirst();
        Set<String> patternRecords = pair.getSecond();
        List<Subject> subjects = pair.getThird();
        String file = Utils.join(Constant.SEP, Constant.HOME, "repair.rec");
        for (Subject subject : subjects) {
            JavaFile.writeStringToFile(file, subject.getName() + "_" + subject.getId() + " > PATCH : ", true);
            LevelLogger.info(subject.getHome() + ", " + subject.toString());
            mfix.tools.Repair repair = new mfix.tools.Repair(subject, patternRecords, singlePattern);
            repair.repair();
            JavaFile.writeStringToFile(file, repair.patch() + "\n", true);
        }
    }

    public static void main(String[] args) {
        mfix.tools.Repair.repairAPI(args);
    }


}
