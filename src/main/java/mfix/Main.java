/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.node.MatchInstance;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.match.Matcher;
import mfix.core.node.modify.Modification;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class Main {

    static void savePatternToFile(String outputFile, Node obj){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
            oos.writeObject(obj);

            System.out.println("save for" + outputFile);

            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Node loadPatternFromFile(String inputFile) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFile));
            Node node = (Node)ois.readObject();
            ois.close();
            return node;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }

    static void printPattern(Set<Node> pattern) {
        System.out.println("-------start---------");
        for (Node node : pattern) {
            System.out.println(node.getAllModifications(new HashSet<Modification>()));
        }

        System.out.println("--------------------");
        System.out.println("-------end----------");
    }


    static void timeoutMethod(int timeout, String srcFile, String tarFile, String savePatternFolder) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return extract(srcFile, tarFile, savePatternFolder);
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(futureTask);

        try {
            boolean result = futureTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            System.out.println("Timeout or other error for" + srcFile);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    static boolean extract(String srcFile, String tarFile, String savePatternFilePrefix) {
    	
		long start1 = System.currentTimeMillis();

		Set<Node> patterns = PatternExtractor.extractPattern(srcFile, tarFile);
		
		for (Node pattern : patterns) {
			MethDecl methDecl = (MethDecl) pattern;
			String patternFuncName = methDecl.getName().getName();
			
			String savePath = savePatternFilePrefix + '-' + patternFuncName + ".pattern";
			
			savePatternToFile(savePath, pattern);
			
			Set<MethodInv> methods = pattern.getUniversalAPIs(new HashSet<MethodInv>(), true);
			for (MethodInv method : methods) {
				String methodName = method.getName().getName();
				Integer methodExprSize = method.getArguments().getExpr().size();

				JavaFile.writeStringToFile(System.getProperty("user.dir") + "/API_Mapping.txt",
						methodName + " " + methodExprSize + " " + savePath + "\n", true);
				
			}
			
			// Node newp = loadPatternFromFile(saveFile);

	        // System.out.println("-----before save-------");
	        // printPattern(patterns);
	        
	        // System.out.println("-----after save-------");
	        // printPattern(newp);
		}
		
		JavaFile.writeStringToFile(System.getProperty("user.dir") + "/time.log",
				srcFile + "[" + (System.currentTimeMillis() - start1) / 1000 + " s]\n", true);

        return true;
    }

    static void runFolder(String folderPath) throws Exception {
        File buggyFolder = new File(folderPath + "/buggy-version");
        File fixedFolder = new File(folderPath + "/fixed-version");

        if ((!buggyFolder.exists()) || (!fixedFolder.exists())) {
            throw new Exception("Files not exist!");
        }

        List<File> buggyFiles = JavaFile.ergodic(buggyFolder, new LinkedList<>());
        List<File> fixedFiles = JavaFile.ergodic(fixedFolder, new LinkedList<>());
        HashMap<String, File> buggyFilesMap = new HashMap<String, File>();

        for (File file : buggyFiles) {
            buggyFilesMap.put(file.getName(), file);
        }

        for (File fixedFile : fixedFiles) {
            File buggyFile = buggyFilesMap.getOrDefault(fixedFile.getName(), null);

            File saveFolder = new File(folderPath + "/pattern-ver4-serial");
            if (!saveFolder.exists()) {
                saveFolder.mkdirs();
            } else {
            	continue;
            }

            String savePatternFolder = saveFolder.getAbsolutePath() + "/" + fixedFile.getName();

            if (buggyFile != null) {
                try {
                    timeoutMethod(60 * 2, buggyFile.getAbsolutePath(), fixedFile.getAbsolutePath(), savePatternFolder);
                } catch (Exception e) {
                    // TODO: handle exception
                }

                // extract(buggyFile.getAbsolutePath(), fixedFile.getAbsolutePath(), saveFile);
            }
        }

        //System.out.println(buggyFiles.toString());
        //System.out.println(fixedFiles.toString());
    }

    static void work(String fileListFilePath) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileListFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Integer cnt = 0;
        String filePath = null;
        while(true)
        {
            try {
                filePath = bufferedReader.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (filePath == null) {
                break;
            }

            cnt += 1;
            System.out.println(cnt.toString() + ":" + filePath);
            try {
                runFolder(filePath);
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
    }
    
    
    // static String APIMappingFile = "/home/jack/Desktop/rly/api-test.txt";
    static String APIMappingFile = "/home/jack/Desktop/rly/API_Mapping.txt";
    // TODO(rly): is hashset could avoid repeat??
    static Map<Pair<String, Integer>, Set<String>> method2PatternFiles;
    
    static Set<String> fixedRet = new HashSet<String>();
    
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
                
                
                if (cnt >= 100000) {
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


    public static void tryMatchAndFix(Node buggy, Node pattern, String patternFile) throws Exception {
		Set<MatchInstance> set = Matcher.tryMatch(buggy, pattern);
		
		String origin = buggy.toString();
		
        for (MatchInstance matchInstance : set) {
            matchInstance.apply();
            
            StringBuffer fixedProg = buggy.adaptModifications();
            
            if (fixedProg != null) {
                String fixed = fixedProg.toString().replaceAll(" ", "");
                
                if (!fixed.equals(origin.replaceAll(" ", ""))) {
                	if (fixedRet.contains(fixed)) {
                		continue;
                	}
                	fixedRet.add(fixed);
                	
                	System.out.println(patternFile);
                	System.out.println("------------ Solution ---------------");
                	System.out.println(fixedProg);
                	
                	JavaFile.writeStringToFile("/home/jack/Desktop/rly/fix_result.txt",
                			fixedProg + "\n---------------\n", true);
                	
                	System.out.println("------------ End ---------------");
                }
            }
            
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

        for(MethodDeclaration m : methods) {
            Node node = parser.process(m);

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
               
                
                /*
                if (!(MethodName.equals("dismiss") && MethodArgsNum == 0)) {
                	continue;
                }
                */
                
            
                /*
                if (!(MethodName.equals("isAJavaToken"))) {
                	continue;
                }
                */
                
                
                System.out.println("Start matching!");
                
            	// System.out.println("------------ Original ---------------");
            	// System.out.println(node.toString());
            	
                for (String patternFile : patternFileList) {
	                try {
	                	// System.out.println(patternFile);
	                	
	                	tryMatchAndFix(node, loadPatternFromFile(patternFile), patternFile);
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
    	
    	
    	System.out.println("Hello!");
    	
    	loadAPI();
    	
    	String buggyFilePath = "/home/jack/code/workspace/eclipse/MineFix/resources/forTest/buggy_SimpleSecureBrowser.java";
    	// String buggyFilePath = "/home/jack/Desktop/rly/cases/5/base-all.java";
    	
    	tryFix(buggyFilePath);
    	
    }
}

