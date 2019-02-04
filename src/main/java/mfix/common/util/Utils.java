/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.common.java.D4jSubject;
import mfix.common.java.Subject;
import mfix.core.node.ast.Node;
import mfix.core.node.parser.NodeParser;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Utils {

    public static boolean safeStringEqual(String s1, String s2) {
        if(s1 == s2) return true;
        if(s1 == null) return false;
        return s1.equals(s2);
    }

    public static String join(char delimiter, String... element) {
        return join(delimiter, Arrays.asList(element));
    }

    public static String join(char delimiter, List<String> elements) {
        StringBuffer buffer = new StringBuffer();
        if (elements.size() > 0) {
            buffer.append(elements.get(0));
        }
        for (int i = 1; i < elements.size(); i++) {
            buffer.append(delimiter);
            buffer.append(elements.get(i));
        }
        return buffer.toString();
    }

    public static boolean checkCompatiblePut(String obj1, String obj2, Map<String, String> map) {
        if(map.containsKey(obj1)) {
            if (!map.get(obj1).equals(obj2)) {
                return false;
            }
        } else {
            map.put(obj1, obj2);
        }
        return true;
    }

    /**
     * Select project {@code whichProject} with bug ids{@code whichBugs}.
     * @param base
     * @param whichProject : name of the project
     * @param whichBugs : bug ids
     * @return a set of d4j subject
     */
    public static Set<D4jSubject> select(String base, String whichProject, Set<Integer> whichBugs) {
        Set<D4jSubject> subjects = new HashSet<>();
        for(Integer whichBug : whichBugs) {
            D4jSubject subject = new D4jSubject(base, whichProject, whichBug);
            subjects.add(subject);
        }
        return subjects;
    }

    /**
     * Select project {@code whichProject} with bug id{@code whichBug}.
     * @param base
     * @param whichProject : name of the project
     * @param whichBug : bug id
     * @return single d4j subject
     */
    public static D4jSubject select(String base, String whichProject, int whichBug) {
        D4jSubject subject = new D4jSubject(base, whichProject, whichBug);
        return subject;
    }

    /**
     * Select {@code samleCount} number of projects randomly.
     * @param base : base directory of the faulty projects
     * @param sampleCount : number for sampling
     * @return : a list of randomly sampled d4j subjects
     */
    public static List<D4jSubject> randomSelect(String base, int sampleCount) {
        double [] evenDist = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        String[] names = new String[]{"chart", "closure", "time", "lang", "math"};
        Map<String, Integer> name2Ids = new HashMap<>();
        name2Ids.put("chart", 26);
        name2Ids.put("closure", 133);
        name2Ids.put("time", 27);
        name2Ids.put("lang", 65);
        name2Ids.put("math", 106);
        return randomSelect(base, evenDist, names, name2Ids, sampleCount);
    }

    /**
     * Randomly select a list of d4j subjects for testing
     * @param base : base directory of the faulty projects
     * @param prob : an array of probabilities for each projects
     * @param names : project names
     * @param name2numbers : project name to bug numbers
     * @param sampleCount : number for sampling
     * @return a list of d4j subjects randomly selected
     */
    public static List<D4jSubject> randomSelect(String base, double [] prob, String[] names,
                                                Map<String, Integer> name2numbers,
                                                int sampleCount) {
        if (prob.length != names.length || prob.length != name2numbers.size()) {
            LevelLogger.error("Number of the probabilities should be the same as the number of project.");
            return null;
        }
        double [] prefixSum = new double[names.length + 1];
        double sum = 0.0;
        for(int i = 0; i < prob.length; i++) {
            sum += prob[i];
        }
        prefixSum[0] = 0;
        for(int i = 1; i < prefixSum.length; i++) {
            prefixSum[i] = prefixSum[i - 1] + prob[i - 1] / sum;
        }
        Random random = new Random(0);
        List<D4jSubject> result = new ArrayList<>();
        Set<Integer> selectedProjects = new HashSet<>();
        for(int i = 0; i < sampleCount; i++) {
            int projectID = binarySearch(prefixSum, 0, prefixSum.length, random.nextDouble());
            String whichProject = names[projectID];
            int whichBug = random.nextInt(name2numbers.get(whichProject)) + 1;
            int key = whichBug + 1000 * projectID;
            if (selectedProjects.contains(key)) {
                i--;
                continue;
            }
            selectedProjects.add(key);
            result.add(select(base, whichProject, whichBug));
        }

        // change to java 1.7
        Collections.sort(result, new Comparator<D4jSubject>(){

            @Override
            public int compare(D4jSubject o1, D4jSubject o2) {
                if (!o1.getName().equals(o2.getName())) {
                    return o1.getName().compareTo(o2.getName());
                }
                return Integer.compare(o1.getId(), o2.getId());
            }

        });
        return result;
    }

    private static int binarySearch(double [] prob, int b, int e, double r) {
        if (b == e - 1) {
            return b;
        }
        int mid = (b + e) / 2;
        if (r < prob[mid]) {
            return binarySearch(prob, b, mid, r);
        } else {
            return binarySearch(prob, mid, e, r);
        }
    }

    public static void serialize(Serializable object, String fileName) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public static Serializable deserialize(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
        return (Serializable) objectInputStream.readObject();
    }

    public static Set<Node> getAllMethods(String file) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final Set<MethodDeclaration> methods = new HashSet<>();

        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(file, unit);
        Set<Node> nodes = new HashSet<>();
        Node node;
        for (MethodDeclaration m : methods) {
            node = parser.process(m);
            nodes.add(node);
        }
        return nodes;
    }

    public static Map<Pair<String, Integer>, Set<String>> loadAPI(String apiMappingFile, int cntLimit,
                                                                  Set<String> bannedAPIs) {
        LevelLogger.info("Start Load API Mappings!");
        Map<Pair<String, Integer>, Set<String>> method2PatternFiles = new HashMap<>();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(apiMappingFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        int cnt = 0;
        while (true) {
            try {
                String str = bufferedReader.readLine();
                if (str == null || cnt >= cntLimit) {
                    break;
                }
                String[] splited = str.split("\\s+");
                String methodName = splited[0];
                Integer methodArgsNum = Integer.parseInt(splited[1]);
                String patternFile = splited[2];
                if (bannedAPIs.contains(methodName)) {
                    continue;
                }

                Pair<String, Integer> key = new Pair<>(methodName, methodArgsNum);
                if (!method2PatternFiles.containsKey(key)) {
                    method2PatternFiles.put(key, new HashSet<>());
                }
                method2PatternFiles.get(key).add(patternFile);

                if ((++cnt) % 100000 == 0) {
                    LevelLogger.debug(cnt);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            bufferedReader.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        LevelLogger.info("Load API Mappings successfully!");
        return method2PatternFiles;
    }

    public static boolean futureTaskWithin(int timeout, FutureTask futureTask) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(futureTask);

        boolean success;
        try {
            success = ((Boolean) futureTask.get(timeout, TimeUnit.SECONDS)).booleanValue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            success = false;
            LevelLogger.error(e);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
        return success;
    }

    public static List<Subject> getSubjectFromXML(String xmlFile) throws NumberFormatException {
        List<Subject> list = new LinkedList<>();

        File inputXml = new File(xmlFile);
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(inputXml);
            Element root = document.getRootElement();

            for (Iterator iterator = root.elementIterator(); iterator.hasNext();) {
                Element element = (Element) iterator.next();
                String name = element.attributeValue("name");
                String base = element.elementText("home");
                String ssrc = element.elementText("ssrc");
                String tsrc = element.elementText("tsrc");
                String sbin = element.elementText("sbin");
                String tbin = element.elementText("tbin");
                String jversion = element.elementText("jversion");

                Element pathElem = element.element("classpath");

                List<String> classpath = new ArrayList<>();
                if (pathElem != null) {
                    for (Iterator iterInner = pathElem.elementIterator(); iterInner.hasNext();) {
                        Element path = (Element) iterInner.next();
                        String clp = path.getText();
                        if (clp != null) {
                            classpath.addAll(getJarFile(new File(clp)));
                        }
                    }
                }

                Subject subject = new Subject(base, name, ssrc, tsrc, sbin, tbin,
                        Subject.SOURCE_LEVEL.valueOf(jversion), classpath);
                list.add(subject);
            }
        } catch (DocumentException e) {
            LevelLogger.fatal("Utils#getSubjectFromXML parse xml file failed !", e);
        }
        return list;
    }

    private static List<String> getJarFile(File path) {
        List<String> jars = new ArrayList<>();
        if (path.isFile()) {
            String file = path.getAbsolutePath();
            if (file.endsWith(".jar")) {
                jars.add(file);
            }
        } else if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (File f : files) {
                jars.addAll(getJarFile(f));
            }
        }
        return jars;
    }

//    public static void log(String logFile, String content, boolean append) {
//        JavaFile.writeStringToFile(logFile, "[" + new Date().toString() +  "] " + content, append);
//    }
//
//    public static void log(String logFile, String path, int startLine, int endLine, String content,
//                           boolean append) {
//        log(logFile, path, startLine, endLine, 0, 0, content, null, append);
//    }
//
//    public static <T> void log(String logFile, String path, int startLine, int endLine, double normSim,
//                           double cosineSim, String content, Diff<T> diff,
//                           boolean append) {
//        StringBuffer buffer = new StringBuffer();
//        buffer.append("path : " + path + Constant.NEW_LINE);
//        buffer.append("range : <" + startLine + "," + endLine + ">" + Constant.NEW_LINE);
//        buffer.append("DIST : " + normSim + Constant.NEW_LINE);
//        buffer.append("COSIN : " + cosineSim + Constant.NEW_LINE);
//        buffer.append("-----------------------------" + Constant.NEW_LINE);
//        buffer.append(content);
//        if(diff != null) {
//            buffer.append("-----------------------------" + Constant.NEW_LINE);
//            buffer.append(diff.miniDiff() + Constant.NEW_LINE);
//        }
//        JavaFile.writeStringToFile(logFile, buffer.toString(), append);
//    }
}
