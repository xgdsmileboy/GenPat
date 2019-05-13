/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.flow;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;

import fit.Parse;
import fit.exception.FitParseException;
import fitlibrary.flow.TestDoFlowWithFixture.MockFixture;
import fitlibrary.runResults.ITableListener;
import fitlibrary.runResults.TestResults;
import fitlibrary.runResults.TestResultsFactory;
import fitlibrary.runtime.RuntimeContextInternal;
import fitlibrary.suite.SuiteEvaluator;
import fitlibrary.table.Row;
import fitlibrary.table.Table;
import fitlibrary.table.Tables;
import fitlibrary.traverse.Evaluator;
import fitlibrary.traverse.FitHandler;
import fitlibrary.traverse.RuntimeContextual;
import fitlibrary.traverse.workflow.FlowEvaluator;
import fitlibrary.typed.TypedObject;
import fitlibraryGeneric.typed.GenericTypedObject;

public class DoFlowDriver {
	final Mockery context;
	final FlowEvaluator flowEvaluator;
	final IScopeStack scopeStack;
	final TestResults testResults;
	final ITableListener tableListener;
	final IScopeState scopeState;
	final RuntimeContextInternal runtime;
	final RuntimeContextInternal runtimeCopy;
	final SetUpTearDown setUpTearDown;
	
	final States state;
	final DoFlow doFlow;
	final static String BEGIN_STATE = "begin";
	String currentState = BEGIN_STATE;
	int tableNo = 0;
	int rowNo = 0;
	
	public boolean showTearDown = false;

	public DoFlowDriver(Mockery context) {
		this.context = context;
		flowEvaluator = context.mock(FlowEvaluator.class);
		scopeStack = context.mock(IScopeStack.class);
		testResults = TestResultsFactory.testResults();
		tableListener = context.mock(ITableListener.class);
		scopeState = context.mock(IScopeState.class);
		runtime = context.mock(RuntimeContextInternal.class);
		runtimeCopy = context.mock(RuntimeContextInternal.class,"runtimeCopy");
		setUpTearDown = context.mock(SetUpTearDown.class);
		state = context.states("doFlow").startsAs(BEGIN_STATE);
		doFlow = new DoFlow(flowEvaluator,scopeStack,runtime,setUpTearDown);
		startingStorytest();
	}
	public void runStorytest(Tables storytest) {
		endingStorytest();
		doFlow.runStorytest(storytest,tableListener);
	}
	public void exit() {
		doFlow.exit();
	}
	private void startingStorytest() {
		final String endState = "startingStorytest";
		context.checking(new Expectations() {{
			allowing(tableListener).getTestResults();
			   will(returnValue(testResults));       when(state.is(currentState));
			oneOf(scopeStack).setAbandon(false);     when(state.is(currentState));
			oneOf(scopeStack).setStopOnError(false); when(state.is(currentState));
			oneOf(scopeStack).clearAllButSuite();    when(state.is(currentState));
			oneOf(runtime).reset();                  when(state.is(currentState));
			                                         then(state.is(endState));
		}});
		currentState = endState;
	}
	private void endingStorytest() {
		final String endState = "endingStorytest";
		context.checking(new Expectations() {{
			oneOf(tableListener).storytestFinished(); when(state.is(currentState));
			                                          then(state.is(endState));
		}});
		currentState = endState;
	}
	public RuntimeContextInternal startingStorytestWithSuite(final SuiteEvaluator suiteEvaluator) {
		final String endState = "startingStorytestWithSuite";
		context.checking(new Expectations() {{
			allowing(tableListener).getTestResults();
			   will(returnValue(testResults));       when(state.is(currentState));
			oneOf(scopeStack).setAbandon(false);     when(state.is(currentState));
			oneOf(scopeStack).setStopOnError(false); when(state.is(currentState));
			oneOf(scopeStack).clearAllButSuite();    when(state.is(currentState));
			oneOf(suiteEvaluator).getCopyOfRuntimeContext();
			  will(returnValue(runtimeCopy));        when(state.is(currentState));
			oneOf(flowEvaluator).setRuntimeContext(runtimeCopy);
                                                     when(state.is(currentState));
			oneOf(flowEvaluator).getSystemUnderTest();
			                                         when(state.is(currentState));
			                                         then(state.is(endState));
		}});
		currentState = endState;
		return runtimeCopy;
	}
	public void startingOnTable(final Table table) {
		startingOnTable(table, runtime);
	}
	public void startingOnTable(final Table table, final RuntimeContextInternal runtimeLocal) {
		tableNo++;
		final String endState = "startingTable"+tableNo;
		context.checking(new Expectations() {{
			allowing(table).isPlainTextTable(); will(returnValue(false));      when(state.is(currentState));
			oneOf(runtimeLocal).setCurrentTable(table);                             when(state.is(currentState));
			allowing(tableListener).getTestResults();
			   will(returnValue(testResults));                                 when(state.is(currentState));
			oneOf(runtimeLocal).pushTestResults(with(any(TestResults.class))); when(state.is(currentState));
			                                                                   then(state.is(endState));
		}});
		currentState = endState;
	}
	public void poppingAtEndOfTable() {
		poppingScopeStackAtEndOfTable(runtime);
	}
	public void poppingScopeStackAtEndOfTable(List<Object> popObjects) {
		poppingScopeStackAtEndOfTable(runtime, popObjects);
	}
	public void poppingScopeStackAtEndOfTable(final RuntimeContextInternal runtimeLocal) {
		poppingScopeStackAtEndOfTable(runtimeLocal, new ArrayList<Object>());
	}
	public void poppingScopeStackAtEndOfTable(final RuntimeContextInternal runtimeLocal, final List<Object> popObjects) {
		final List<TypedObject> popList = asTypedObjects(popObjects);
		final String endState = "poppingScopeStackAtEndOfTable"+tableNo;
		context.checking(new Expectations() {{
			oneOf(runtimeLocal).popTestResults();                 when(state.is(currentState));
			oneOf(scopeStack).poppedAtEndOfTable();
			  will(returnValue(popList));                         when(state.is(currentState));
			                                                      then(state.is(endState));
		}});
		currentState = endState;
	}
	public void poppingAtEndOfLastTable() {
		poppingAtEndOfLastTable(runtime);
	}
	public void poppingAtEndOfLastTable(final RuntimeContextInternal runtimeLocal) {
		poppingScopeStackAtEndOfLastTable(runtimeLocal, new ArrayList<Object>());
	}
	public void poppingScopeStackAtEndOfLastTable(List<Object> popObjects) {
		poppingScopeStackAtEndOfLastTable(runtime,popObjects);
	}
	public void poppingScopeStackAtEndOfLastTable(final RuntimeContextInternal runtimeLocal, final List<Object> popObjects) {
		final List<TypedObject> popList = asTypedObjects(popObjects);
		final String endState = "poppingScopeStackAtEndOfLastTable"+tableNo;
		context.checking(new Expectations() {{
			oneOf(runtimeLocal).popTestResults();                 when(state.is(currentState));
			oneOf(scopeStack).poppedAtEndOfStorytest();
			  will(returnValue(popList));                         when(state.is(currentState));
			                                                      then(state.is(endState));
		}});
		currentState = endState;
	}
	public void finishingTable(final Table table) {
		finishingTable(table, runtime);
	}
	public void finishingTable(final Table table, final RuntimeContextInternal runtimeLocal) {
		final String endState = "finishingTable"+tableNo;
		tableNo--;
		context.checking(new Expectations() {{
			oneOf(runtimeLocal).addAccumulatedFoldingText(table); when(state.is(currentState));
			oneOf(tableListener).tableFinished(table);  when(state.is(currentState));
			                                            then(state.is(endState));
		}});
		currentState = endState;
	}
	public void interpretingRowReturning(final Row row, final Object object) {
		interpretingRowReturning(row, object, runtime);
	}
	public void interpretingRowReturning(final Row row, final Object object, final RuntimeContextInternal runtimeLocal) {
		rowNo++;
		final String endState = "interpretingRowReturning"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			allowing(runtimeLocal).isAbandoned(with(any(TestResults.class)));
			  will(returnValue(false));                           when(state.is(currentState));
			oneOf(runtimeLocal).setCurrentRow(row);               when(state.is(currentState));
			oneOf(flowEvaluator).interpretRow(row,testResults);
			  will(returnValue(new GenericTypedObject(object)));  when(state.is(currentState));
			                                                      then(state.is(endState));
		}});
		currentState = endState;
	}
	public void pushingObjectOnScopeStack(final Object sut) {
		final String endState = "pushingObjectOnScopeStack"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(scopeStack).push(new GenericTypedObject(sut)); when(state.is(currentState));
			                                                     then(state.is(endState));
		}});
		currentState = endState;
	}
	public void callingSuiteSetUpOn(final Object sut, final Row row) {
		final String endState = "callingSuiteSetUpOn"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(setUpTearDown).callSuiteSetUp(sut, row, testResults); when(state.is(currentState));
			                                                               then(state.is(endState));
		}});
		currentState = endState;
	}
	public void callingSuiteTearDownOn(final Object sut) {
		final String endState = "callingSuiteTearDownOn"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(setUpTearDown).callSuiteTearDown(with(sut), with(any(TestResults.class))); when(state.is(currentState));
			                                                          then(state.is(endState));
		}});
		currentState = endState;
	}
	public void callingSetUpOn(final Object sut, final Row row) {
		final String endState = "callingSetUpOn"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(setUpTearDown).callSetUpSutChain(sut, row, testResults); when(state.is(currentState));
			                                                               then(state.is(endState));
		}});
		currentState = endState;
	}
	public void callingTearDownOn(final Object sut, final Row row) {
		final String endState = "callingTearDownOn"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(setUpTearDown).callTearDownSutChain(sut, row, testResults); when(state.is(currentState));
			                                                                  then(state.is(endState));
		}});
		currentState = endState;
	}
	private List<TypedObject> asTypedObjects(final List<Object> popObjects) {
		final List<TypedObject> popList = new ArrayList<TypedObject>();
		for (Object object : popObjects)
			popList.add(new GenericTypedObject(object));
		return popList;
	}
	public void interpretingFixture(final MockFixture mockFixture, final Table table) throws FitParseException {
		final Parse parse = new Parse("<table><tr><td>1</td></tr></table>");
		final String endState = "interpretingFixture"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(table).asTableOnParse(); will(returnValue(table)); when(state.is(currentState));
			oneOf(flowEvaluator).fitHandler();
			  will(returnValue(new FitHandler()));                   when(state.is(currentState));
			oneOf(table).asParse(); will(returnValue(parse));        when(state.is(currentState));
			oneOf(mockFixture).doTable(parse);                       when(state.is(currentState));
			oneOf(table).replaceAt(0, table.at(0));                  when(state.is(currentState));
			oneOf(table).replaceAt(1, table.at(1));                  when(state.is(currentState));
			                                                         then(state.is(endState));
		}});
		currentState = endState;
	}
	public void interpretingEvaluator(final Evaluator mockEvaluator, final Table table) {
		final String endState = "interpretingEvaluator"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(mockEvaluator).interpretAfterFirstRow(table, testResults); 
			                                                     when(state.is(currentState));
			                                                     then(state.is(endState));
		}});
		currentState = endState;
	}
	public RuntimeContextInternal getRuntime() {
		return runtime;
	}
	public void injectingWithRuntime(final RuntimeContextual runtimeContextual) {
		final String endState = "injectingWithRuntime"+tableNo+","+rowNo;
		context.checking(new Expectations() {{
			oneOf(runtimeContextual).setRuntimeContext(runtime);  when(state.is(currentState));
			                                                      then(state.is(endState));
		}});
		currentState = endState;
	}
}
