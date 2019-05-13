package mfix;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Method;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.RepairMatcherImpl;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.eclipse.jdt.core.dom.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;


public class Main {
    final static boolean WRITE_TO_FILE = false;
    static String currentCaseLog = "";

    static String SYDIT_DATASET = Constant.RES_DIR + Constant.SEP + "SysEdit";
    static String C3_DATASET = Constant.RES_DIR + Constant.SEP + "c3_part";
    static String currentRepo = null, cluFolder = null;
    static String currentCase = null;

    static boolean varScopeFlag = false;

    final static int defaultWaitMinuate = 1; // Limit by 1 mins

    static String matchChoice = "default";

    static String xmlFile = null;

    static String groundTruthFile = null;

    static int stNum = 1, edNum = 100000;

    static int caseNum;
    static List<String> correctCase = new LinkedList<>(), adaptCase = new LinkedList<>();

    static MethodDeclaration getKthMethodFromFile(String file, int index) {
        if (!(new File(file).exists())) {
            System.err.println("File not exist! " + file);
            return null;
        }

        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        LinkedList<MethodDeclaration> all_methods = new LinkedList<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (node instanceof MethodDeclaration) {
                    all_methods.add(node);
                }
                return true;
            }
        });

//
//        for (MethodDeclaration m : all_methods) {
//            System.out.println("found method:" + m.getName());
//        }


        if (index >= all_methods.size()) {
            return null;
        }

        return all_methods.get(index);
    }


    static String getSign(MethodDeclaration node) {
        StringBuffer buffer = new StringBuffer();

        for (Object obj : node.modifiers()) {
            buffer.append(obj).append(' ');
        }
        buffer.append(node.getReturnType2() == null ? "" : node.getReturnType2().toString())
                .append(node.getName().toString())
                .append('(');
        boolean first = true;
        for (Object object : node.parameters()) {
            if (object instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) object;
                buffer.append(first ? "" : ", ");
                for (Object obj : svd.modifiers()) {
                    buffer.append(obj).append(' ');
                }
                buffer.append(svd.getType())
                        .append(' ')
                        .append(svd.getName());
                first = false;
            }
        }
        buffer.append(')');
        if (!node.thrownExceptionTypes().isEmpty()) {
            buffer.append(" throws ");
            first = true;
            for (Object th : node.thrownExceptionTypes()) {
                buffer.append(first ? "" : ',')
                        .append(th.toString());
            }
        }

//        System.out.println(buffer);
        return buffer.toString();
    }

    static MethodDeclaration matchMethodWithSign(String file, String signature) {
        if (!(new File(file).exists())) {
            System.err.println("File not exist! " + file);
            return null;
        }
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        LinkedList<MethodDeclaration> all_methods = new LinkedList<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (node instanceof MethodDeclaration) {
                    all_methods.add(node);
                }
                return true;
            }
        });

        String pettySignature = signature.replaceAll("(\\s*\\t*\\n*)+", "");

        for (MethodDeclaration m : all_methods) {
            //System.out.println(m.getProperty("")toString().substring(0, 50));
//            System.out.println(getSign(m));
            if (pettySignature.equals(getSign(m).replaceAll("(\\s*\\t*\\n*)+", ""))) {
                return m;
            }
        }

        for (MethodDeclaration m : all_methods) {
            if (pettySignature.contains(getSign(m).replaceAll("(\\s*\\t*\\n*)+", ""))) {
                return m;
            }
        }

        return null;
    }
    static String getPrettySign(MethodDeclaration m) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(m.getName().toString());
        buffer.append("(");
        boolean first = true;
        for (Object object : m.parameters()) {
            if (object instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) object;
                buffer.append(first ? "" : ", ");
                buffer.append(svd.getType());
                first = false;
            }
        }
        buffer.append(')');
        return buffer.toString();
    }

    // find method with same name & same argType
    static MethodDeclaration findMethodFromFile(String file, Method method) {
        if (method == null) {
            return null;
        }
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final Set<MethodDeclaration> methods = new HashSet<>();

        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (method.getName().equals(node.getName().getIdentifier())
                        && method.getArgTypes().size() == node.parameters().size()
                        && method.argTypeSame(node)
                ) {
//                    System.out.println(node.parameters().toString());
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        if (methods.size() == 0) {
            System.err.println("Method Not Found!");
            return null;
        }

        return methods.iterator().next();
    }
    static String removeEmpty(String s) {
        return s.replace("\n", "").replace(" ", "");
    }
    static void tryApply(Pair<String, String> m1, Pair<Method, Method> m1Func, Pair<String, String> m2, Pair<Method, Method> m2Func) {
        Method m1_src = m1Func.getFirst(), m1_tar = m1Func.getSecond();
        Method m2_src = m2Func.getFirst(), m2_tar = m2Func.getSecond();

        if (m1_src == null || m1_tar == null || m2_src == null || m2_tar == null) {
            return;
        }

        String srcFile = m1.getFirst(), tarFile = m1.getSecond();

        System.out.println("Pattern Files:" + srcFile + " -> " + tarFile);
        System.out.println("Ground Truth Files:" + m2.getFirst() + " -> " + m2.getSecond());

        Set<Pattern> patterns = (new PatternExtractor()).extractPattern(srcFile, tarFile, m1_src, m1_tar);
//        System.out.println("size = " + patterns.size());

        String buggy = m2.getFirst();

        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);
        MethDecl node = (MethDecl) JavaFile.getNode(buggy, m2_src);
        if (patterns.isEmpty()) {
            System.err.println("No Pattern Found!");
            return;
        }
        Pattern p = patterns.iterator().next();

        List<MatchInstance> set = null;
        if (matchChoice.equals("FUZZY")) {
            System.out.println("match level: FUZZY.");
            set = RepairMatcherImpl.tryMatch(node, p, MatchLevel.FUZZY, defaultWaitMinuate);
        } else if (matchChoice.equals("TYPE")) {
            System.out.println("match level: TYPE.");
            set = RepairMatcherImpl.tryMatch(node, p, MatchLevel.TYPE, defaultWaitMinuate);
        } else if (matchChoice.equals("NAME")) {
            System.out.println("match level: NAME.");
            set = RepairMatcherImpl.tryMatch(node, p, MatchLevel.NAME, defaultWaitMinuate);
        } else {
            System.out.println("match level: default.");
            set = RepairMatcherImpl.tryMatch(node, p, defaultWaitMinuate);
        }

        VarScope scope = varMaps.get(node.getStartLine());
        scope.setDisable(varScopeFlag);
        scope.reset(p.getNewVars());
        Set<String> already = new HashSet<>();

        int correct_cnt  = 0, incorrect_cnt = 0;
        List<TextDiff> diffRet = new LinkedList<>();

        for (MatchInstance instance : set) {
            instance.apply();
            StringBuffer buffer = node.adaptModifications(scope, instance.getStrMap(), node.getRetTypeStr(),
                    new HashSet<>(node.getThrows()));

            instance.reset();

            String target = m2.getSecond();
            Node tar_node = JavaFile.getNode(target, m2_tar);

            if (buffer == null) {
                System.err.println("Adaptation failed!");
                continue;
            }

            String transformed = buffer.toString();
            if (already.contains(transformed)) {
                continue;
            }
            already.add(transformed);
            String original = tar_node.toString();

            TextDiff diff = new TextDiff(original, transformed);

            // System.out.println(diff.toString());
            diffRet.add(diff);

            if (removeEmpty(original).equals(removeEmpty(transformed))) {
                correct_cnt += 1;
            } else {
                incorrect_cnt += 1;
            }
            break;
        }

//        System.out.println("TP, FP = " + correct_cnt + "," + incorrect_cnt);
//        writeToLocal("TP, FP = " + correct_cnt + "," + incorrect_cnt);

        if (correct_cnt > 0) {
//            System.out.println("Correct!");
            correctCase.add(currentCase);
            adaptCase.add(currentCase);
            System.out.println("[Correct]Syntactical equivalent!");

        } else {
            if (incorrect_cnt > 0) {
//                System.out.println("Incorrect!");
                adaptCase.add(currentCase);
                System.out.println("[Not Sure]Not Syntactical equivalent!");
                System.out.println("Need futher check for semantically equivalent.");
            } else {
                System.out.println("Not adapted!");
            }

//            System.out.println("original_before=\n" + node.toString());
//            writeToLocal("original_before=\n" + node.toString());
            System.out.println("Here is the diff result:");
            for (int i = 0; i < diffRet.size(); ++i) {
                if (i >= 3) {
                    System.out.println("More....");
                    break;
                }
                System.out.println("-----------");
                System.out.println("Candidate " + (i + 1) + ":");
                System.out.println(diffRet.get(i).toString());
                System.out.println("-----------");

            }
        }
//        System.out.println("-----end------");
    }

    public static String getPath(JSONObject o) {
        return  SYDIT_DATASET + (String)o.get("file");
    }

    public static Method json2method(JSONObject src) {
        List<String> args = (List<String>)src.get("argTypes");
        args.remove("");
        Method tmp = new Method(null, (String)src.get("name"), args);
        return new Method(findMethodFromFile(getPath(src), tmp));
    }

    public static void work(String path) {
        JSONParser parser = new JSONParser();
        JSONArray ret = null;
        try {
            ret = (JSONArray) parser.parse(new FileReader(path));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        JSONArray p = (JSONArray)ret.get(0);
        JSONObject p_src = (JSONObject)p.get(0);
        JSONObject p_tar = (JSONObject)p.get(1);

        JSONArray q = (JSONArray)ret.get(1);
        JSONObject q_src = (JSONObject)q.get(0);
        JSONObject q_tar = (JSONObject)q.get(1);

        Method p_m_src = json2method(p_src);
        Method p_m_tar = json2method(p_tar);
        Method q_m_src = json2method(q_src);
        Method q_m_tar = json2method(q_tar);

        tryApply(new Pair<>(getPath(p_src), getPath(p_tar)), new Pair<>(p_m_src, p_m_tar),
                new Pair<>(getPath(q_src), getPath(q_tar)), new Pair<>(q_m_src, q_m_tar));
    }

    static final java.util.regex.Pattern MethodPattern = java.util.regex.Pattern.compile("\\S+\\(.*\\)");

    public static Method parseMethodFromString(String s) {
        java.util.regex.Matcher m = MethodPattern.matcher(s);
        if (m.find()) {
            String method = m.group();
//            System.out.println(method);
            String[] arr = method.substring(0, method.length() - 1).split("\\(");
            String methodName = arr[0];
            List<String> methodArgs = new ArrayList<>();
            if (arr.length > 1) {
                methodArgs = Arrays.asList(arr[1].split(","));
            }
//            System.out.println(methodName);
//            System.out.println(methodArgs);
            return new Method(null, methodName, methodArgs);
        } else {
            System.err.println("Parse method error! Method:" + s);
            return null;
        }
    }

    static boolean cmpbyc(String a, String b) {
        return a.replaceAll("(\\s*\\t*\\n*)+", "").equals(b.replaceAll("(\\s*\\t*\\n*)+", ""));
    }

    static void readXML(String xmlPath) {
        selectCases = new HashMap<>();

        File file = new File(xmlPath);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document document = null;
        try {
            document = documentBuilder.parse(file);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        NodeList ind = document.getElementsByTagName("indexInDataset");
        for (int i = 0; i < ind.getLength(); ++i) {
//        for (int i = 0; i < 50; ++i) {
            String index = ind.item(i).getTextContent();
            String[] indexArr = index.split(",");
            selectCases.put(Integer.parseInt(indexArr[1]),
                    new Pair<>(Integer.parseInt(indexArr[2]), Integer.parseInt(indexArr[3])));
        }

    }

    static Map<Integer, Pair<Integer, Integer>> selectCases = null;

    static int currentRunCases = 0;

    public static void runc3() {
        for (caseNum = stNum; caseNum <= edNum; ++caseNum) {
            if ((selectCases != null) && (!selectCases.containsKey(caseNum))) {
                continue;
            }

            currentRunCases++;

            String path = cluFolder + "/" + caseNum;

            currentCase = currentRepo + " #" + caseNum;
            System.out.println("\n\n\nCurrent: " + currentCase);

            if (!(new File(path).exists())) {
                System.err.println("C3 data is missing:" + path);
                break;
            }

//            currentCaseLog = path + "/run-v5.txt";
//            if (new File(currentCaseLog).exists()) {
//                continue;
//            }

            JSONParser parser = new JSONParser();
            JSONObject ret = null;
            try {
                ret = (JSONObject) parser.parse(new FileReader(path + "/info.json"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            JSONArray methods = (JSONArray)ret.get("members");

            List<Method> src_methods, tar_methods;
            src_methods = new ArrayList<>();
            tar_methods = new ArrayList<>();


            String p0 = null;
            for (int j = 0; j < methods.size(); ++j) {
                if ((selectCases == null) && (j > 1)) {
                    break;
                }

                String src = path + "/" + "src_" + j + ".java";
                String tar = path + "/" + "tar_" + j + ".java";

                String src_method = (String)((JSONObject)methods.get(j)).get("signatureBeforeChange");
                String tar_method = (String)((JSONObject)methods.get(j)).get("signatureAfterChange");
                int src_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberBeforeChange");
                int tar_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberAfterChange");

//              src_methods.add(new Method(findMethodFromFile(src, parseMethodFromString(src_method))));
                MethodDeclaration point_src_method = getKthMethodFromFile(src, src_method_num);
                if (point_src_method == null) {
                    point_src_method = matchMethodWithSign(src, src_method);
                }

//              System.out.println(point_src_method.getName());
//              System.out.println(src_method);
                src_methods.add(new Method(point_src_method));
                if (point_src_method == null || (!src_method.contains(point_src_method.getName().toString()))) {
                    System.err.println("method ind err" + src);
                    System.out.println(src_method);
                }

//              tar_methods.add(new Method(findMethodFromFile(tar, parseMethodFromString(tar_method))));
                MethodDeclaration point_tar_method = getKthMethodFromFile(tar, tar_method_num);;
                if (point_tar_method == null) {
                    point_tar_method = matchMethodWithSign(tar, tar_method);
                }
//              System.out.println(point_tar_method.getName());
//              System.out.println(tar_method);
                tar_methods.add(new Method(point_tar_method));

                if (point_tar_method == null || (!tar_method.contains(point_tar_method.getName().toString()))) {
                    System.err.println("[ERROR]method ind err" + tar);
                    System.out.println(tar_method);
                }

                if (groundTruthFile != null) {
                    if ((selectCases != null) && (j == selectCases.get(caseNum).getSecond())) {
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(groundTruthFile, true));
                            writer.write("counter:" + currentRunCases);
                            writer.newLine();
                            writer.write("-----start-----");
                            writer.newLine();
                            writer.write(point_tar_method.toString());
                            writer.write("-----end-----");
                            writer.newLine();

                            writer.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            // Finish all methods

            int x_ind = 0, y_ind = 1;

            if (selectCases != null) {
                x_ind = selectCases.get(caseNum).getFirst();
                y_ind = selectCases.get(caseNum).getSecond();
            }

            String x_src = path + "/" + "src_" + x_ind + ".java";
            String x_tar = path + "/" + "tar_" + x_ind + ".java";
            String y_src = path + "/" + "src_" + y_ind + ".java";
            String y_tar = path + "/" + "tar_" + y_ind + ".java";
            Method x_src_method = src_methods.get(x_ind);
            Method x_tar_method = tar_methods.get(x_ind);
            Method y_src_method = src_methods.get(y_ind);
            Method y_tar_method = tar_methods.get(y_ind);

            System.out.println("PATTERN:");

            JSONArray diff = (JSONArray) (((JSONObject) methods.get(x_ind)).get("diff"));
            StringBuffer tmp = new StringBuffer();
            for (int d = 0; d < diff.size(); ++d) {
                tmp.append(diff.get(d) + "\n");
            }
            System.out.println(tmp);
            System.out.println("\n");

            System.out.println("Try Apply!");

            try {
                tryApply(new Pair<>(x_src, x_tar), new Pair<>(x_src_method, x_tar_method),
                        new Pair<>(y_src, y_tar), new Pair<>(y_src_method, y_tar_method));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        System.out.println(cntEqual);
//        System.out.println(cntTotal);
//        System.out.println(1.0 * cntEqual / cntTotal);

        // System.out.println("AdaptNum=" + (correctCaseNum + incorrectCaseNum));
        /*
        System.out.println("correctCaseNum=" + correctCaseNum);
        System.out.println("incorrectCaseNum=" + incorrectCaseNum);
        System.out.println("correctCase=" + correctCase.toString());
        System.out.println("correctCaseRunnedNum=" + correctCaseRunnedNum.toString());
        System.out.println("totalNum=" + (correctCaseNum + incorrectCaseNum));
        */
        System.out.println("Correct Cases=" + correctCase.toString());
        System.out.println("Adapt Cases=" + adaptCase.toString());
    }

    public static void runsydit() {
        for (int i = 1; i <= 56; ++i) {
            String path = SYDIT_DATASET + String.format("/%d/info.json", i);
            if (new File(path).exists()) {
                currentCase = "Sydit#" + i;
                System.out.println("\n\n\nCurrent: " + currentCase);
                try {
                    work(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("SYDIT data is missing:" + path);
            }
        }
        System.out.println("Correct Cases=" + correctCase.toString());
        System.out.println("Adapt Cases=" + adaptCase.toString());
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Please given the dataset");
            System.exit(1);
        }

        switch (args[0]) {
            case "c3":
                String[] arr = new String[]{"junit","cobertura","jgrapht","checkstyle","ant","fitlibrary","drjava","eclipsejdt","eclipseswt"};
                for (int i = 0; i < arr.length; ++i) {
                    currentRepo = arr[i];
                    cluFolder = C3_DATASET + Constant.SEP + arr[i];

                    stNum = 1;
                    edNum = 10;
                    runc3();
                }
                break;
            case "sydit":
                varScopeFlag = true;
                
                runsydit();
                break;
            default:
                System.err.println("Error!");
        }
    }

}
