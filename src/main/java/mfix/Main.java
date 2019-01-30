/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.MatchInstance;
import mfix.core.node.NodeUtils;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class Main {
    // Change the file path in API_Mapping.txt to your local path.
    static String APIMappingFile = "/home/jack/Desktop/rly/API_Mapping.txt";

    static Map<Pair<String, Integer>, HashSet<String>> method2PatternFiles; // (MethodName, ArgsNumber) -> Pattern contains this API.

    static Set<String> fixedRet = new HashSet<String>(); // Used for avoid repeat fix.

    static String versionFolder = "ver6";

    static String pointedAPI = null;
    // static String pointedAPI = "dismiss";

    static Integer cntLimit = null;

    static String resultFile = "/home/jack/Desktop/rly/fix_result.txt";

    static String buggyFilePath = "/home/jack/code/workspace/eclipse/MineFix/resources/forTest/buggy_SimpleSecureBrowser.java";
    // static String buggyFilePath = "/home/jack/Desktop/rly/cases/4/base-all.java";;

    static int timeoutForExtractOneFile = 60;

    static int timeoutForFix = 10;

    static String[] bannedAPIs = {};
    //static String[] bannedAPIs = {"length", "indexOf", "substring"};  // Skip these apis.

    static void loadAPI() {
        System.out.println("Start Load API Mappings!");
        method2PatternFiles = new HashMap<Pair<String, Integer>, HashSet<String>>();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(APIMappingFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Integer cnt = 0;

        while(true) {
            try {
                String str = bufferedReader.readLine();
                if (str == null) {
                	break;
                }
                String[] splited = str.split("\\s+");
                String MethodName = splited[0];
                Integer MethodArgsNum = Integer.parseInt(splited[1]);
                String patternFile = splited[2];

                Pair<String, Integer> key = new Pair<String, Integer>(MethodName, MethodArgsNum);
                if (!method2PatternFiles.containsKey(key)) {
                	method2PatternFiles.put(key, new HashSet<String>());
                }
                method2PatternFiles.get(key).add(patternFile);

                cnt += 1;
                if (cnt % 100000 == 0) {
                	System.out.println(cnt);
                }

                if ((cntLimit != null) && (cnt >= cntLimit)) {
                	break;
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
        	bufferedReader.close();
            inputStream.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Load API Mappings successfully!");
    }


    public static void tryMatchAndFix(Node buggy, Node pattern, Set<String> buggyMethodVar, String buggyFile, String patternFile) throws Exception {
		Set<MatchInstance> fixPositions = Matcher.tryMatch(buggy, pattern);

		String origin = buggy.toString();

        for (MatchInstance matchInstance : fixPositions) {
            matchInstance.apply();

            StringBuffer fixedProg = buggy.adaptModifications(buggyMethodVar);

            if (fixedProg != null) {
                String fixed = fixedProg.toString().replaceAll(" ", "");

                if (!fixed.equals(origin.replaceAll(" ", ""))) {
                	if (fixedRet.contains(fixed)) {
                		continue;
                	}
                	fixedRet.add(fixed);

                	System.out.println(patternFile);

                	System.out.println("------------ Origin ---------------");
                	System.out.println(origin);

                	System.out.println("------------ Solution ---------------");
                	System.out.println(fixedProg);

                	JavaFile.writeStringToFile(resultFile,
                			"FILE:" + buggyFile + "\n" + "PATTERN:" + patternFile + "\n------------ Origin ---------------\n" + origin + "\n------------ Solution --------------\n" + fixedProg + "\n---------------\n", true);

                	System.out.println("------------ End ---------------");
                }
            }

            matchInstance.reset();
        }
    }


    static void extractAndSave(String filePath, String file) throws Exception {
        Set<Node> patternCandidates = PatternExtractor.extractPattern(
                filePath + "/buggy-version/" + file,
                filePath + "/fixed-version/" + file);

        for (Node fixPattern : patternCandidates) {
            MethDecl methDecl = (MethDecl) fixPattern;
            String patternFuncName = methDecl.getName().getName();

            String savePatternPath = filePath + "/" + versionFolder + "/" + file + "-" + patternFuncName + ".pattern";
            System.out.println("save pattern: " + savePatternPath);
            Utils.serialize(fixPattern, savePatternPath);
        }
    }



    static void timeoutMethodForFix(String patternSerializePath, String buggyFile, Node node, Set<String> buggyMethodVar) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Node fixPattern = (Node) Utils.deserialize(patternSerializePath);
                if (fixPattern != null) {
                    tryMatchAndFix(node, fixPattern, buggyMethodVar, buggyFile, patternSerializePath);
                }
                return true;
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(futureTask);

        try {
            futureTask.get(timeoutForFix, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            System.out.println("Timeout or other error for fixing " + buggyFile + " using " + patternSerializePath);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    static void timeoutMethodForExtractAndFix(String filePath, String file, String patternSerializePath, String buggyFile, Node node, Set<String> buggyMethodVar) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                extractAndSave(filePath, file);
                return true;
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(futureTask);

        boolean success = false;

        try {
            futureTask.get(timeoutForExtractOneFile, TimeUnit.SECONDS);
            success = true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            System.out.println("Timeout or other error for" + filePath + " " + file);
            try {
                Utils.serialize(null, patternSerializePath);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            futureTask.cancel(true);
        }

        executorService.shutdownNow();

        if (success) {
            timeoutMethodForFix(patternSerializePath, buggyFile, node, buggyMethodVar);
        }
    }


    public static void tryFix(String buggyFile) {
        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggyFile);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(buggyFile, unit);

        Map<Integer, Set<String>> buggyFileVarMap = NodeUtils.getUsableVarTypes(buggyFile);

        for(MethodDeclaration m : methods) {

        	System.out.println("Method: " + m.getName().getFullyQualifiedName());

            Node node = parser.process(m);

            Set<String> buggyMethodVar = buggyFileVarMap.getOrDefault(node.getStartLine(), new HashSet<String>());

            Set<MethodInv> ContainMethodInvs = node.getUniversalAPIs(new HashSet<MethodInv>(), false);

            // System.out.println(" Size of ContainMethodInvs in " + m.toString() + ": " + ContainMethodInvs.size());

            Set<Pair<String, Integer>> runned = new HashSet<Pair<String, Integer>>();

            for (MethodInv containMethod : ContainMethodInvs) {
                String MethodName = containMethod.getName().getName();
                Integer MethodArgsNum = containMethod.getArguments().getExpr().size();

                if (runned.contains(new Pair<String, Integer>(MethodName, MethodArgsNum))) {
                	continue;
                }

                runned.add(new Pair<String, Integer>(MethodName, MethodArgsNum));

                System.out.println(buggyFile + " contains: " + MethodName + " " + MethodArgsNum);

                Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<String, Integer>(MethodName, MethodArgsNum), new HashSet<String>());

                System.out.println(" Size of patternList : " + patternFileList.size());


                if ((pointedAPI != null) && (!MethodName.equals(pointedAPI))) {
                    continue;
                }

                boolean skip = false;
                for (String bannedAPI : bannedAPIs) {
                    if (MethodName.equals(bannedAPI)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }

                System.out.println("Start matching!");

                for (String patternFile : patternFileList) {
                    int ind = patternFile.indexOf("pattern-ver4-serial");
                    int indLen = "pattern-ver4-serial".length();
                    String filePath = patternFile.substring(0, ind - 1);
                    String fileAndMethod = patternFile.substring(ind + indLen + 1);

                    int dashIndex = fileAndMethod.lastIndexOf("-");
                    String file = fileAndMethod.substring(0, dashIndex);
                    String method = fileAndMethod.substring(dashIndex + 1);


                    String patternSerializePath = filePath + "/" + versionFolder + "/" + fileAndMethod;

//	            	String mypattern = "/home/lee/Xia/GitHubData/MissSome/2012-2014/V15/2065" + "/" + versionFolder + "/" +
//                            "question-reply.question-reply-war.src.main.java.com.silverpeas.questionReply.servlets.QuestionReplyRequestRouter.java";
//
//	            	String mypattern = "/home/lee/Xia/GitHubData/MissSome/2012-2014/V49/776/ver6/src.co.ords.w.Endpoint.java-doGet.pattern";
//
//	            	if (!(patternSerializePath.equals(mypattern))) {
//	            		continue;
//	            	}

                    // System.out.println("current:" + patternSerializePath);

                    // Make the folder for saving patterns if it doesn't exist.
                    File versionAbsFolder = new File(filePath + "/" + versionFolder);
                    if (!versionAbsFolder.exists()) {
                        versionAbsFolder.mkdirs();
                    }

                    if (!(new File(patternSerializePath)).exists()) {

//                        // Not set timeout
//                        Set<Node> patternCandidates = PatternExtractor.extractPattern(
//                                filePath + "/buggy-version/" + file,
//                                filePath + "/fixed-version/" + file);
//
//                        for (Node fixPattern : patternCandidates) {
//                            MethDecl methDecl = (MethDecl) fixPattern;
//                            String patternFuncName = methDecl.getName().getName();
//
//                            String savePatternPath = filePath + "/" + versionFolder + "/" + file + "-" + patternFuncName + ".pattern";
//                            System.out.println("save pattern: " + savePatternPath);
//
//                            Utils.serialize(fixPattern, savePatternPath);
//                        }

                        timeoutMethodForExtractAndFix(filePath, file, patternSerializePath, buggyFile, node, buggyMethodVar);
                    } else {
                        // Already saved!
                        // System.out.println("skip for " + patternSerializePath);
                        timeoutMethodForFix(patternSerializePath, buggyFile, node, buggyMethodVar);
                    }


                }
            }


        }
    }

    public static void main(String[] args) {
        /*
        if (args.length < 1) {
            System.out.println(versionFolder + ": Input Error!");
            return;
        }
        buggyFilePath = args[0];


        if (args.length == 2) {
            pointedAPI = args[1];
            System.out.println("pointedAPI: " + pointedAPI);
        }
        */

        loadAPI();

    	tryFix(buggyFilePath);
    }
}

