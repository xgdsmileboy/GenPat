package mfix;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Method;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.RepairMatcher;
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


public class rlytest {
    final static String LOCAL_DATASET = Constant.RES_DIR + Constant.SEP + "SysEdit-part1";
    final static int defaultWaitMinuate = 1;

    static int correctCaseNum = 0, incorrectCaseNum = 0, caseNum;
    static List<Integer> correctCase;
    static List<Integer> correctCaseRunnedNum;

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
                return false;
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
                ) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        if (methods.size() == 0) {
            return null;
        }

        return methods.iterator().next();
    }
    static String removeEmpty(String s) {
        return s.replace("\n", "").replace(" ", "");
    }

    static void tryApply(Pair<String, String> m1, Method m1Func, Pair<String, String> m2, Method m2Func) {
        if (m1Func == null || m2Func == null) {
            return;
        }

        String srcFile = m1.getFirst(), tarFile = m1.getSecond();

        System.out.println("pattern:" + srcFile + " -> " + tarFile);
        System.out.println("ground:" + m2.getFirst() + " -> " + m2.getSecond());

        Set<Pattern> patterns = (new PatternExtractor()).extractPattern(srcFile, tarFile, m1Func);
        System.out.println("size = " + patterns.size());

        String buggy = m2.getFirst();

        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);
        MethDecl node = (MethDecl) JavaFile.getNode(buggy, m2Func);
        if (patterns.isEmpty()) {
            System.err.println("No pattern !");
            return;
        }
        Pattern p = patterns.iterator().next();
        List<MatchInstance> set = new RepairMatcher(defaultWaitMinuate).tryMatch(node, p); // Limit by 2 mins
        VarScope scope = varMaps.get(node.getStartLine());
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
            Node tar_node = JavaFile.getNode(target, m2Func);

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

        System.out.println("TP, FP = " + correct_cnt + "," + incorrect_cnt);

        if (correct_cnt > 0) {
            System.out.println("Correct!");
            correctCaseNum += 1;
            correctCase.add(caseNum);
            correctCaseRunnedNum.add(currentRunCases);
            if (incorrect_cnt > 0) {
                System.out.println("Also with incorrect!");
            }
        } else {
            if (incorrect_cnt > 0) {
                incorrectCaseNum += 1;
                System.out.println("Incorrect!");
            } else {
                System.out.println("Not Found!");
            }

            System.out.println("original_before=\n" + node.toString());

            for (int i = 0; i < diffRet.size(); ++i) {
                if (i >= 3) {
                    System.out.println("More....");
                    break;
                }
                System.out.println("-----------");
                System.out.println("Candidate " + i + ":");
                System.out.println(diffRet.get(i).toString());
                System.out.println("-----------");
            }
        }
        System.out.println("-----end------");
    }

    public static String getPath(JSONObject o) {
        return  LOCAL_DATASET + (String)o.get("file");
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

        System.out.println(p_m_src.toString() + " -> " + p_m_tar.toString());
        System.out.println(q_m_src.toString() + " -> " + q_m_tar.toString());

        if ((!p_m_src.equals(p_m_tar)) || (!q_m_src.equals(q_m_tar))) {
            System.out.println("method diff!");
            return;
        }

        tryApply(new Pair<>(getPath(p_src), getPath(p_tar)), p_m_src,
                new Pair<>(getPath(q_src), getPath(q_tar)), q_m_src);
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


    static String cluFolder = "/Users/luyaoren/Downloads/cluster";
    // static String cluFolder = "/Users/luyaoren/Downloads/c3_download/checkstyle_cluster";


    static Map<Integer, Pair<Integer, Integer>> selectCases = null;

    static int currentRunCases = 0;

    public static void runc3() {
        int cntTotal = 0, cntEqual = 0;
        correctCaseNum = 0;
        incorrectCaseNum = 0;
        correctCase = new LinkedList<>();
        correctCaseRunnedNum = new LinkedList<>();

        for (caseNum = 1; caseNum <= 1000000; ++caseNum) {
            if ((selectCases != null) && (!selectCases.containsKey(caseNum))) {
                continue;
            }

            currentRunCases++;

            if (caseNum != 234) {
                continue;
            }


            System.out.println("run cluster:" + caseNum);
            String path = cluFolder + "/" + caseNum;

            if (!(new File(path).exists())) {
                break;
            }

            JSONParser parser = new JSONParser();
            JSONObject ret = null;
            try {
                ret = (JSONObject) parser.parse(new FileReader(path + "/info.json"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            JSONArray methods = (JSONArray)ret.get("members");

//            if (methods.size() > 5) {
//                continue;
//            }

            List<Method> src_methods, tar_methods;
            src_methods = new ArrayList<>();
            tar_methods = new ArrayList<>();


            String p0 = null;
            for (int j = 0; j < methods.size(); ++j) {
                String src = path + "/" + "src_" + j + ".java";
                String tar = path + "/" + "tar_" + j + ".java";

                String src_method = (String)((JSONObject)methods.get(j)).get("signatureBeforeChange");
                String tar_method = (String)((JSONObject)methods.get(j)).get("signatureAfterChange");
                int src_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberBeforeChange");
                int tar_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberAfterChange");

//              src_methods.add(new Method(findMethodFromFile(src, parseMethodFromString(src_method))));
                MethodDeclaration point_src_method = matchMethodWithSign(src, src_method);
                if (point_src_method == null) {
                    point_src_method = getKthMethodFromFile(src, src_method_num);
                }

//              System.out.println(point_src_method.getName());
//              System.out.println(src_method);
                src_methods.add(new Method(point_src_method));
                if (point_src_method == null || (!src_method.contains(point_src_method.getName().toString()))) {
                    System.err.println("method ind err" + src);
                    System.out.println(src_method);
                }

//              tar_methods.add(new Method(findMethodFromFile(tar, parseMethodFromString(tar_method))));
                MethodDeclaration point_tar_method = matchMethodWithSign(tar, tar_method);
                if (point_tar_method == null) {
                    point_tar_method = getKthMethodFromFile(tar, tar_method_num);
                }
//              System.out.println(point_tar_method.getName());
//              System.out.println(tar_method);
                tar_methods.add(new Method(point_tar_method));

                if (point_tar_method == null || (!tar_method.contains(point_tar_method.getName().toString()))) {
                    System.err.println("method ind err" + tar);
                    System.out.println(tar_method);
                }

                if (j == selectCases.get(caseNum).getSecond()) {
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/luyaoren/Desktop/ase-data/junit_gd.txt", true));
                        writer.write("counter:"  + currentRunCases);
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
            // Finish all methods

            int x_ind = 0, y_ind = 1;

            x_ind = selectCases.get(caseNum).getFirst();
            y_ind = selectCases.get(caseNum).getSecond();

            String x_src = path + "/" + "src_" + x_ind + ".java";
            String x_tar = path + "/" + "tar_" + x_ind + ".java";
            String y_src = path + "/" + "src_" + y_ind + ".java";
            String y_tar = path + "/" + "tar_" + y_ind + ".java";
            Method x_src_method = src_methods.get(x_ind);
            Method x_tar_method = tar_methods.get(x_ind);
            Method y_src_method = src_methods.get(y_ind);
            Method y_tar_method = tar_methods.get(y_ind);

            if (x_src_method.equals(x_tar_method) && y_src_method.equals(y_tar_method)) {

                System.out.println("PATTERN:");
                JSONArray diff = (JSONArray) (((JSONObject) methods.get(x_ind)).get("diff"));
                for (int d = 0; d < diff.size(); ++d) {
                    System.out.println(diff.get(d));
                }
                System.out.println("--end--");


                try {
                    tryApply(new Pair<>(x_src, x_tar), x_src_method, new Pair<>(y_src, y_tar), y_src_method);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                 System.err.println("Method signature is different: " + caseNum + ":" + x_ind + "," + y_ind + "\n");
            }

        }
//        System.out.println(cntEqual);
//        System.out.println(cntTotal);
//        System.out.println(1.0 * cntEqual / cntTotal);


        System.out.println("correctCaseNum=" + correctCaseNum);
        System.out.println("incorrectCaseNum=" + incorrectCaseNum);
        System.out.println("correctCase:" + correctCase.toString());
        System.out.println("correctCaseRunnedNum:" + correctCaseRunnedNum.toString());

    }


    public static void main(String[] args) {
        /*
        for (int i = 1; i <= 50; ++i) {
            String path = LOCAL_DATASET + String.format("/%d/info.json", i);
            if (new File(path).exists()) {
                System.out.println("current: " + i);
                try {
                    work(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            break;
        }
        */



//        cluFolder = args[0];
//        outFile = args[1];
//
//        System.out.println("cluFolder=" + cluFolder + ";");
//        System.out.println("outFile=" + outFile + ";");

        readXML("/Users/luyaoren/Downloads/junit_all.xml");

        runc3();


        // matchMethodFromFile("/Users/luyaoren/Downloads/cluster/7/src_0.java", "");
    }
}

