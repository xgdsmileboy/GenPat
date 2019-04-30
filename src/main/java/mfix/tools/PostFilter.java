/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.modify.Wrap;
import mfix.core.pattern.Pattern;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-04-29
 */
public class PostFilter {

    public static void main(String[] args) throws Exception {
        String file = Constant.HOME + Constant.SEP + args[0];
        List<String> contents = JavaFile.readFileToStringList(file);
        List<String> filtered = new LinkedList<>();
        int filter = 0, left = 0;
        for (String s : contents) {
            if (s.startsWith(Constant.DEFAULT_PATTERN_HOME)
                    &&s.contains("#")) {
                String string = s.substring(0, s.indexOf('#'));
                Pattern p = (Pattern) Utils.deserialize(string);
                Set<Modification> modifications = p.getAllModifications();
                if (modifications.size() == 1) {
                    Modification modification = modifications.iterator().next();
                    if (!(modification instanceof Deletion)) {
                        left ++;
                        System.out.println("LEFT : " + left);
                        filtered.add(string);
                    } else {
                        filter ++;
                        System.out.println(">>>>>>> FILTER : " + filter);
                    }
                } else {
                    Set<Modification> wraps = modifications.stream().filter(m -> m instanceof Wrap)
                            .collect(Collectors.toSet());
                    if (wraps.size() == 1) {
                        boolean yes = true;
                        for (Modification m : modifications) {
                            if (m instanceof Wrap) {
                                continue;
                            }
                            if (m instanceof Update || m instanceof Insertion) {
                                yes = false;
                                break;
                            }
                            if (m instanceof Deletion) {
                                if(((Deletion) m).getDelNode().noBinding()) {
                                    yes = false;
                                    break;
                                }
                            }
                        }
                        if (yes) {
                            left ++;
                            System.out.println("LEFT : " + left);
                            filtered.add(string);
                        } else {
                            filter ++;
                            System.out.println(">>>>>>> FILTER : " + filter);
                        }
                    }
                }
            }
        }
        System.out.println("FILTER : " + filter + "\tLEFFT : " + left);
        JavaFile.writeStringToFile("Filtered.txt", "", false);
        for (String s : filtered) {
            JavaFile.writeStringToFile("Filtered.txt", s + "\n", true);
        }
    }

}
