package mfix;

import mfix.common.util.JavaFile;
import mfix.common.util.Method;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
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
    final static String LOCAL_DATASET = "/Users/luyaoren/Desktop/SysEdit-part1";

    // find method with name & argType
    static MethodDeclaration findMethodFromFile(String file, Method method) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (method.getName().equals(node.getName().getIdentifier()) &&
                        method.argTypeSame(node)) {
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
        Node node = JavaFile.getNode(buggy, m2Func);
        List<MatchInstance> set = new RepairMatcher().tryMatch(node, patterns.iterator().next());

        for (MatchInstance instance : set) {
            instance.apply();
            StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), instance.getStrMap(), "void",
                    new HashSet<>());

            instance.reset();

            String target = m2.getSecond();
            Node tar_node = JavaFile.getNode(target, m2Func);

            if (buffer != null) {
                System.out.println(tar_node.toString());
                System.out.println("--------------");
                System.out.println(buffer.toString());
                System.out.println("-----end------");
            }

            if (buffer != null && tar_node.toString().equals(buffer.toString())) {
                System.out.println("Correct!");
            }
        }
    }

    public static String getPath(JSONObject o) {
        return  LOCAL_DATASET + (String)o.get("file");
    }

    public static Method json2method(JSONObject src) {
        Method tmp = new Method(null, (String)src.get("name"), (List<String>)src.get("argTypes"));
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
        }
    }
}

