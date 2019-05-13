/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.flow;

import static fitlibrary.matcher.TableBuilderForTests.cell;
import static fitlibrary.matcher.TableBuilderForTests.row;
import static fitlibrary.matcher.TableBuilderForTests.table;
import static fitlibrary.matcher.TableBuilderForTests.tables;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import fitlibrary.runtime.RuntimeContextInternal;
import fitlibrary.suite.SuiteEvaluator;
import fitlibrary.table.Table;
import fitlibrary.table.Tables;
import fitlibrary.traverse.workflow.DoTraverse;
import fitlibrary.utility.CollectionUtility;

@RunWith(JMock.class)
public class TestDoFlowWithSuiteEvaluator {
	final Mockery context = new Mockery();
	final DoFlowDriver driver = new DoFlowDriver(context);
	
	final Tables storytest1 = tables().with(
			table().with(
					row().with(cell(),cell()))
			).mock(context,"storytest1");
	
	final Tables storytest2 = tables().with(
			table().with(
					row().with(cell(),cell()))
			).mock(context,"storytest2");

	@Test
	public void runWithPlainSuiteFixture() {
		final SuiteEvaluator suiteEvaluator = context.mock(SuiteEvaluator.class);
		context.checking(new Expectations() {{
			allowing(suiteEvaluator).getSystemUnderTest();  will(returnValue(null));
		}});
		verifyWithEvaluator(suiteEvaluator);
	}

	private void verifyWithEvaluator(final SuiteEvaluator suiteEvaluator) {
		Table table1 = storytest1.at(0);
		driver.startingOnTable(table1);
		driver.interpretingRowReturning(table1.at(0), suiteEvaluator);
		driver.injectingWithRuntime(suiteEvaluator);
		driver.callingSuiteSetUpOn(suiteEvaluator,table1.at(0));
		driver.pushingObjectOnScopeStack(suiteEvaluator);
		driver.callingSetUpOn(suiteEvaluator, table1.at(0));
		driver.poppingScopeStackAtEndOfLastTableGiving(list());
		driver.finishingTable(table1);
		driver.runStorytest(storytest1);
		
		Table table2 = storytest2.at(0);
		RuntimeContextInternal runtimeCopy = driver.startingStorytestWithSuite(suiteEvaluator);
		driver.startingOnTable(table2,runtimeCopy);
		String s = "s";
		driver.interpretingRowReturning(table2.at(0),new DoTraverse(s),runtimeCopy);
		driver.pushingObjectOnScopeStack(s);
		driver.callingSetUpOn(s, table2.at(0));
		driver.poppingScopeStackAtEndOfLastTableGiving(runtimeCopy,list(s));
		driver.callingTearDownOn(s, table2.at(0));
		driver.finishingTable(table2,runtimeCopy);
		driver.runStorytest(storytest2);
		
		driver.callingSuiteTearDownOn(suiteEvaluator);
		driver.exit();
		
	}
	protected List<Object> list(Object... ss) {
		return CollectionUtility.list(ss);
	}
}
