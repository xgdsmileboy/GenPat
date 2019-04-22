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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class rlytest {
    final static String LOCAL_DATASET = Constant.RES_DIR + Constant.SEP + "SysEdit-part1";

    // find method with same name & same argType
    static MethodDeclaration findMethodFromFile(String file, Method method) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (method.getName().equals(node.getName().getIdentifier()) &&
                        // method.argTypeSame(node)
                        method.getArgTypes().size() == node.parameters().size()
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

    static void tryApply(Pair<String, String> m1, Method m1Func, Pair<String, String> m2, Method m2Func) {
        String srcFile = m1.getFirst(), tarFile = m1.getSecond();

        System.out.println(srcFile + " -> " + tarFile);

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
        List<MatchInstance> set = new RepairMatcher().tryMatch(node, p);
        VarScope scope = varMaps.get(node.getStartLine());
        scope.reset(p.getNewVars());
        Set<String> already = new HashSet<>();
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

            System.out.println(diff.toString());
            System.out.println("-----end------");

            if (original.equals(transformed)) {
                System.out.println("Correct!");
            } else {
                System.out.println("Incorrect!");
            }
        }
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
        runc3();
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
            System.err.println("Parse method error!");
            return null;
        }
    }

    public static void runc3() {
        for (int i = 1; i <= 100; ++i) {
            String path = "/Users/luyaoren/Downloads/cluster/" + i;

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

            for (int j = 0; j < methods.size(); ++j) {
                String src0 = path + "/" + "src_0.java";
                String tar0 = path + "/" + "tar_0.java";

                String src = path + "/" + "src_" + j + ".java";
                String tar = path + "/" + "tar_" + j + ".java";

                String src_method = (String)((JSONObject)methods.get(j)).get("signatureBeforeChange");
                String tar_method = (String)((JSONObject)methods.get(j)).get("signatureAfterChange");

                System.out.println(src_method);
                System.out.println(tar_method);


                if ((new File(src).exists()) && (new File(tar).exists())) {
                    src_methods.add(new Method(findMethodFromFile(src, parseMethodFromString(src_method))));
                    tar_methods.add(new Method(findMethodFromFile(tar, parseMethodFromString(tar_method))));
                } else {
                    System.err.println("File not exist!");
                }

                System.out.println(src_methods.get(j));
                System.out.println(tar_methods.get(j));

                if (j > 0) {
                    if (src_methods.get(0).equals(tar_methods.get(0)) &&
                            src_methods.get(j).equals(tar_methods.get(j))) {
                        tryApply(new Pair<>(src0, tar0), src_methods.get(0), new Pair<>(src, tar), src_methods.get(j));
                    } else {
                        System.err.println("Method signature is different!");
                    }
                }
            }

        }
    }
}

