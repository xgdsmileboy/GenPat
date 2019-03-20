/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.locator.purify;

import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.util.Utils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author Jiajun
 * @date Jul 26, 2017
 */
public class PurificationTest {
	@Test
	public void testPurify_fail(){
		String d4jHome = Utils.join(Constant.SEP, Constant.HOME, "projects");
		D4jSubject subject = new D4jSubject(d4jHome, "math", 72);

		try {
			subject.backup();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Purification purification = new Purification(subject);
		List<String> purifiedFailedTestCases = purification.purify(true);
		for(String teString : purifiedFailedTestCases){
			System.out.println(teString);
		}
		
	}
	
}
