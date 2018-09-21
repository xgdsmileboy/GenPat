package mfix.core.parse.diff;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pfix.common.config.Constant;
import pfix.common.config.LevelLogger;
import pfix.common.run.CmdFactory;
import pfix.common.util.JavaFile;
import pfix.common.util.Pair;
import pfix.common.util.RepoSubject;

public class DiffExtractor {

	public static Set<Diff> extraceAllDiff(List<RepoSubject> subjects, Class<? extends Diff> clazz) {
		Map<RepoSubject, List<Pair<String, String>>> commitPairs = JavaFile.readCommitFromFile(Constant.FILE_ABSO_COMMIT_PAIR_XML);
		Set<Diff> allDiffs = new HashSet<>();
		for(Entry<RepoSubject, List<Pair<String, String>>> entry : commitPairs.entrySet()) {
			String base = Constant.DIR_ABSO_DISTIL + File.separator + entry.getKey().getName();
			String save = Constant.DIR_ABSO_DISTIL + File.separator + entry.getKey().getName() + Constant.FILE_REL_HIS_CHANGE;
			boolean append = false;
			for(Pair<String, String> pair : entry.getValue()) {
				String srcPath = base + File.separator + CmdFactory.buildPath(pair) + File.separator + pair.getFirst();
				String tarPath = base + File.separator + CmdFactory.buildPath(pair) + File.separator + pair.getSecond();
				Set<String> fileNames = verify(srcPath, tarPath);
				if(fileNames == null) {
					LevelLogger.warn("different files before and after commit : " + entry.getKey().getName() + " : " + pair);
				} else {
					for(String fName : fileNames) {
						List<Diff> diffs = Diff.extractFileDiff(srcPath + File.separator + fName, tarPath + File.separator + fName, clazz);
						allDiffs.addAll(diffs);
						if(diffs == null || diffs.isEmpty()) {
							continue;
						}
						StringBuffer stringBuffer = new StringBuffer();
						stringBuffer.append("COMMIT : " + pair.toString());
						stringBuffer.append(Constant.NEW_LINE);
						stringBuffer.append("FILE : " + fName);
						stringBuffer.append(Constant.NEW_LINE);
						boolean exist = false;
						for(Diff diff : diffs) {
							if(diff.exist()) {
								exist = true;
								stringBuffer.append(diff.toString());
								stringBuffer.append(Constant.NEW_LINE);
								stringBuffer.append("-----------------");
								stringBuffer.append(Constant.NEW_LINE);
							}
						}
						if(exist) {
							JavaFile.writeStringToFile(save, stringBuffer.toString(), append);
							append = true;
						}
					}
				}
			}
		}
		return allDiffs;
	}
	
	private static Set<String> verify(String srcPath, String tarPath) {
		Set<File> srcFiles = new HashSet<>(JavaFile.ergodic(new File(srcPath), new LinkedList<File>()));
		List<File> tarFiles = JavaFile.ergodic(new File(tarPath), new LinkedList<File>());
		Set<String> tarFileNames = new HashSet<>();
		for(File file : tarFiles) {
			String name = file.getName();
			if(name.endsWith(Constant.SOURCE_FILE_SUFFIX)) {
				tarFileNames.add(file.getName());
			}
		}
		Set<String> files = new HashSet<>(tarFiles.size());
		for(File file : srcFiles) {
			String name = file.getName();
			if(name.endsWith(Constant.SOURCE_FILE_SUFFIX)) {
				// minimal different file sets
				if(tarFileNames.contains(file.getName())) {
					files.add(file.getName());
				} else {
					return null;
				}
			}
		}
		return files;
	}
}
