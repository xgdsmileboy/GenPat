/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-03-12
 */
public class FakeSubject extends Subject {

    public final static String NAME = "FakeSubject";
    private List<String> _filesToRepair;

    public FakeSubject(String base, List<String> filesToRepair) {
        super(base, NAME, "", "", "", "");
        _type = NAME;
        _filesToRepair = filesToRepair;
        _compile_file = true;
    }

    public List<String> getBuggyFiles() {
        return _filesToRepair;
    }

}
