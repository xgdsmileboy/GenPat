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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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


public class rlytest2 {
    final static String LOCAL_DATASET = Constant.RES_DIR + Constant.SEP + "SysEdit-part1";
    final static int defaultWaitMinuate = 1;

    static int correctCaseNum = 0, incorrectCaseNum = 0, caseNum;
    static List<Integer> correctCase;


    static MethodDeclaration getKthMethodFromFile(String file, int index) {
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

    static Set<Integer> selectSet = null;

    static void readXML(String xmlPath) {
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
            String index = ind.item(i).getTextContent();
            String[] indexArr = index.split(",");

        }
    }

    static String outFile = "/Users/luyaoren/Desktop/ase-data/msign_log.txt";
    static String cluFolder = "/Users/luyaoren/Downloads/cluster";
    // static String cluFolder = "/Users/luyaoren/Downloads/c3_download/checkstyle_cluster";


    public static void extractAndRepair(JSONObject methods,int x, int y) {

    }

    public static void runc3() {
        int cntTotal = 0, cntEqual = 0;
        correctCaseNum = 0;
        incorrectCaseNum = 0;
        correctCase = new LinkedList<>();

        int sameCnt = 0;
//        Integer[] arr = new Integer[]{2, 12, 19, 20, 23, 34, 38, 51, 76, 81, 84, 90, 95, 98, 101, 107, 130, 143, 147, 152, 154, 160, 171, 174, 180, 184, 187, 199, 205, 207, 209, 210, 223, 228, 247, 250, 260, 261, 265, 266, 269, 270, 280, 294, 302, 328, 333, 349, 370, 373, 374, 393, 399, 406, 409, 421, 424, 436, 439, 451, 463, 479, 495, 517, 532, 534, 549, 552, 563, 565, 566, 594, 602, 618, 625, 644, 645, 649, 651, 655, 669, 675, 678, 680, 686, 688, 689, 707, 728, 743, 746, 757, 777, 798, 807, 814, 820, 830, 839, 883, 892, 942, 953, 957, 975, 976, 978, 984, 987, 1001, 1003, 1011, 1015, 1025, 1040, 1044, 1065, 1079, 1082, 1090, 1091, 1097, 1104, 1106, 1107, 1121, 1132, 1134, 1138, 1145, 1155, 1157, 1204, 1215, 1221, 1237, 1239, 1254, 1268, 1270, 1271, 1281, 1287, 1292, 1300, 1321, 1337, 1340, 1344, 1348, 1359, 1373, 1386, 1392, 1393, 1394, 1397, 1419, 1427, 1442, 1472, 1491, 1505, 1507, 1508, 1520, 1527, 1533, 1543, 1545, 1558, 1572, 1583, 1626, 1641, 1644, 1646, 1654, 1660, 1670, 1672, 1686, 1715, 1716, 1723, 1739, 1748, 1776, 1778, 1787, 1789, 1823, 1824, 1834, 1861, 1865, 1868, 1872, 1873, 1876, 1916, 1921, 1928, 1937, 1948, 1980, 1985, 1988, 1991, 1997, 2007, 2011, 2028, 2031, 2049, 2050, 2051, 2073, 2088, 2096, 2118, 2121, 2125, 2129, 2132, 2136, 2140, 2154, 2168, 2205, 2217, 2219, 2223, 2228, 2230, 2236, 2244, 2252, 2255, 2271, 2273, 2297, 2298, 2303, 2333, 2348, 2354, 2359, 2360, 2362, 2371, 2374, 2376, 2378, 2389, 2396, 2401, 2404, 2407, 2449, 2466, 2475, 2485, 2504, 2508, 2509, 2513, 2517, 2521, 2527, 2534, 2540, 2544, 2548, 2550, 2552, 2563, 2575, 2648, 2654, 2662, 2678, 2701, 2703, 2713, 2719, 2720, 2783, 2784, 2791, 2796, 2806, 2809, 2820, 2832, 2837, 2841, 2875, 2878, 2885, 2900, 2916, 2919, 2932, 2944, 2946, 2952, 2981, 2989, 3002, 3016, 3039, 3055, 3060, 3087, 3089, 3092, 3095, 3123, 3124, 3130, 3147, 3148, 3155, 3157, 3170, 3173, 3178, 3185, 3209, 3214, 3216, 3220, 3243, 3252, 3265, 3267, 3274, 3287, 3304, 3305, 3314, 3320, 3323, 3350, 3353, 3361, 3371, 3382, 3384, 3391, 3393, 3398, 3401, 3411, 3414, 3429, 3443, 3460, 3469, 3472, 3484, 3501, 3507, 3518, 3528, 3542, 3553, 3564, 3566, 3579, 3583, 3593, 3594, 3598, 3600, 3628, 3632, 3646, 3650, 3653, 3670, 3709, 3716, 3719, 3723, 3740, 3741, 3745, 3754, 3763, 3766, 3768, 3775, 3778, 3798, 3802, 3803, 3804, 3813, 3822, 3828, 3841, 3847, 3865, 3874, 3882, 3898};
//        Integer[] arr = new Integer[]{191, 425};
//        selectSet = new HashSet(Arrays.asList(arr));

        FileWriter fileWritter = null;
        try {
            fileWritter = new FileWriter(outFile,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fileWritter);

        for (caseNum = 1; caseNum <= 1000000; ++caseNum) {
            if ((selectSet != null) && (!selectSet.contains(caseNum))) {
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
//            for (int j = 0; j < 2; ++j) {
                String src0 = path + "/" + "src_0.java";
                String tar0 = path + "/" + "tar_0.java";

                String src = path + "/" + "src_" + j + ".java";
                String tar = path + "/" + "tar_" + j + ".java";

                String src_method = (String)((JSONObject)methods.get(j)).get("signatureBeforeChange");
                String tar_method = (String)((JSONObject)methods.get(j)).get("signatureAfterChange");
                int src_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberBeforeChange");
                int tar_method_num = (int)(long)((JSONObject)methods.get(j)).get("methodNumberAfterChange");

//                if (j > 0) {
//                    String p1 = ((JSONObject) methods.get(j)).get("diff").toString();
//                    if (cmpbyc(p0, p1)) {
//                        sameCnt ++;
//                        System.out.println("same:" + caseNum);
//                    }
//                } else {
//                    p0 = ((JSONObject) methods.get(j)).get("diff").toString();
//                }


                /*
                if (j == 0) {
                    System.out.println("PATTERN:");
                    System.out.println(src_method);
                    System.out.println(tar_method);

                    JSONArray diff = (JSONArray) (((JSONObject) methods.get(j)).get("diff"));
                    for (int d = 0; d < diff.size(); ++d) {
                        System.out.println(diff.get(d));
                    }
                    System.out.println("--end--");
                }
                */

                if ((new File(src).exists()) && (new File(tar).exists())) {
//                    src_methods.add(new Method(findMethodFromFile(src, parseMethodFromString(src_method))));
                    MethodDeclaration point_src_method = matchMethodWithSign(src, src_method);
                    if (point_src_method == null) {
                        point_src_method = getKthMethodFromFile(src, src_method_num);
                    }

//                    System.out.println(point_src_method.getName());
//                    System.out.println(src_method);
                    src_methods.add(new Method(point_src_method));
                    if (point_src_method == null || (!src_method.contains(point_src_method.getName().toString()))) {
                        System.err.println("method ind err" + src);
                        System.out.println(src_method);
                    }

//                    tar_methods.add(new Method(findMethodFromFile(tar, parseMethodFromString(tar_method))));
                    MethodDeclaration point_tar_method = matchMethodWithSign(tar, tar_method);
                    if (point_tar_method == null) {
                        point_tar_method = getKthMethodFromFile(tar, tar_method_num);
                    }
//                    System.out.println(point_tar_method.getName());
//                    System.out.println(tar_method);
                    tar_methods.add(new Method(point_tar_method));

                    if (point_tar_method == null || (!tar_method.contains(point_tar_method.getName().toString()))) {
                        System.err.println("method ind err" + tar);
                        System.out.println(tar_method);
                    }
//
                    // output method sign
//                    if (point_src_method != null) {
//                        try {
//                            bw.write("msign:" + caseNum + "/" + j + "/src/" + getPrettySign(point_src_method));
//                            bw.newLine();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                    if (point_tar_method != null) {
//                        try {
//                            bw.write("msign:" + caseNum + "/" + j + "/tar/" + getPrettySign(point_tar_method));
//                            bw.newLine();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }

                } else {
                    System.err.println("File not exist!");
                }


                cntTotal += 1;
                if (src_methods.get(j).equals(tar_methods.get(j))) {
                    cntEqual += 1;
                } else {
//                    if (j <= 5) {
//                        System.out.println(src_methods.get(j) + "->" + tar_methods.get(j));
//                        String diff = "";
//                        for (Object x : (JSONArray) ((JSONObject) methods.get(j)).get("diff")) {
//                            diff += x.toString() + "\n";
//                        }
//                        System.out.println(diff);
//                    }
                }

                if (j > 0) {
                    if (src_methods.get(0).equals(tar_methods.get(0)) &&
                            src_methods.get(j).equals(tar_methods.get(j))) {
                        try {
//                            tryApply(new Pair<>(src0, tar0), src_methods.get(0), new Pair<>(src, tar), src_methods.get(j));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // System.err.println("Method signature is different: ");
                    }
                }
            }
        }
//        System.out.println(cntEqual);
//        System.out.println(cntTotal);
//        System.out.println(1.0 * cntEqual / cntTotal);


        System.out.println("sameCnt=" + sameCnt);

        System.out.println("correctCaseNum=" + correctCaseNum);
        System.out.println("incorrectCaseNum=" + incorrectCaseNum);
        System.out.println("correctCase:" + correctCase.toString());

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        runc3();


//        readXML("/Users/luyaoren/Downloads/junit_all.xml");

        // matchMethodFromFile("/Users/luyaoren/Downloads/cluster/7/src_0.java", "");
    }
}

