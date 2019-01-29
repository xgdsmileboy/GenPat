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
import mfix.core.node.ast.NodeVisitor;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.match.Matcher;
import mfix.core.node.modify.Modification;
import mfix.core.node.parser.NodeParser;

import org.eclipse.core.internal.resources.Folder;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.eval.VariablesEvaluator;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class Main {
    // static String APIMappingFile = "/home/jack/Desktop/rly/api-test.txt";
    static String APIMappingFile = "/home/jack/Desktop/rly/API_Mapping.txt";
    // TODO(rly): is hashset could avoid repeat??
    static Map<Pair<String, Integer>, Set<String>> method2PatternFiles;
    
    static Set<String> fixedRet = new HashSet<String>();
    
    static String versionFolder = "ver6";
    
    static void loadAPI() {
        System.out.println("Start Load API Mappings!");
        method2PatternFiles = new HashMap<Pair<String, Integer>, Set<String>>();

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
                
//                if (cnt >= 5000000) {
//                	break;	
//                }
                
                
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
                	
                	JavaFile.writeStringToFile("/home/jack/Desktop/rly/new_fix_result.txt",
                			buggyFile + ":\n" + fixedProg + "\n---------------\n", true);
                	
                	System.out.println("------------ End ---------------");
                }
            }
            
            matchInstance.reset();
            // System.out.println(matchInstance.getNodeMap());   
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
               
                
                if (!(MethodName.equals("dismiss") && MethodArgsNum == 0)) {
                	continue;
                }
                
//                if (!(MethodName.equals("getAsString") && MethodArgsNum == 0)) {
//                	continue;
//                }
                
             
                System.out.println("Start matching!");
                
            	// System.out.println("------------ Original ---------------");
            	// System.out.println(node.toString());
            	
                for (String patternFile : patternFileList) {
	                try {
	            		int ind = patternFile.indexOf("pattern-ver4-serial");
	            		int indLen = "pattern-ver4-serial".length();
	            		String filePath = patternFile.substring(0, ind - 1);
	            		String fileAndMethod = patternFile.substring(ind + indLen + 1);

	            		int dashIndex = fileAndMethod.lastIndexOf("-");
	            		String file = fileAndMethod.substring(0, dashIndex);
	            		String method = fileAndMethod.substring(dashIndex + 1);
	            		
	            		
	            		File versionAbsFolder = new File(filePath + "/" + versionFolder);
	                    if (!versionAbsFolder.exists()) {
	                    	versionAbsFolder.mkdirs();
	                    }

	            		String patternSerializePath = filePath + "/" + versionFolder + "/" + fileAndMethod;
	            		
	            		
//	            		String mypattern = "/home/lee/Xia/GitHubData/MissSome/2012-2014/V64/6552/ver6/CacheCleaner.src.main.java.com.frozendevs.cache.cleaner.activity.CleanerActivity.java-onScanStarted.pattern";
//	            		
//	            		if (!(patternSerializePath.equals(mypattern))) {
//	            			continue;
//	            		}
	            		
	            		
	            		if (!(new File(patternSerializePath)).exists()) {
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
		   
	            		} else {
	            			// System.out.println("skip for " + patternSerializePath);
	            		}
	            		

	            		// System.out.println("current:" + patternSerializePath);
	            		
	            		Node fixPattern = (Node)Utils.deserialize(patternSerializePath);
	            		
	            		tryMatchAndFix(node, fixPattern, buggyMethodVar, buggyFile, patternSerializePath);
	            		
	            		/*
	            		System.out.println(filePath);
	            		System.out.println(file);
	            		System.out.println(method);
	            		
	            		// System.out.println(patternFile);
	            		
	                	
	                	// System.out.println("-------");
	                	*/
	                
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
                }
            }


        }
    }

    public static void main(String[] args) {
        // long start = System.currentTimeMillis();
    	
    	/*
        if (args.length != 1) {
            System.out.println("version5: Input Error!");
            return;
        }
        String fileListFilePath = args[0];
        

        // String fileListFilePath = "/home/renly/test/small_files_test.txt";
        // String fileListFilePath = "/Users/luyaoren/workplace/file_list.txt";

        work(fileListFilePath);
        */
    	
    	/*
    	if (args.length != 1) {
            System.out.println("version5 (Fix): Input Error!");
            return;
        }
    	
    	String buggyFilePath = args[0];
    	*/
    	
    	
    	// Node p = loadPatternFromFile("/home/lee/Xia/GitHubData/MissSome/2015/V26/7040/pattern-ver4-serial/app.src.main.java.com.sqbnet.expressassistant.registrationActivity.java-run.pattern");
    	
		
    	/*
    	Node p = loadPatternFromFile("/home/jack/Desktop/0.pattern");
    	    	
    	p.accept(new NodeVisitor(){
    		public boolean visit(MethodInv method) {
    			System.out.println(method.getName().getName());
    			System.out.println(method.isAbstract());
    			System.out.println("----");
    			
    			return true;
    		}
    	});
    	*/
    	
		
    	loadAPI();
    	
    	String buggyFilePath = "/home/jack/code/workspace/eclipse/MineFix/resources/forTest/buggy_SimpleSecureBrowser.java";
    	// String buggyFilePath = "/home/jack/Desktop/rly/cases/5/base-all.java";
    	
    	tryFix(buggyFilePath);
    	
    	
    }
}

