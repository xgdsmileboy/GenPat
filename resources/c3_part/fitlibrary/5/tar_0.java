/*
a * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.traverse.workflow;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import fitlibrary.DefineAction;
import fitlibrary.annotation.ActionType;
import fitlibrary.annotation.AnAction;
import fitlibrary.annotation.ShowSelectedActions;
import fitlibrary.closure.ICalledMethodTarget;
import fitlibrary.domainAdapter.FileHandler;
import fitlibrary.exception.FitLibraryException;
import fitlibrary.exception.FitLibraryShowException;
import fitlibrary.exception.IgnoredException;
import fitlibrary.exception.table.MissingCellsException;
import fitlibrary.flow.DoAutoWrapper;
import fitlibrary.flow.IDoAutoWrapper;
import fitlibrary.global.PlugBoard;
import fitlibrary.log.FitLibraryLogger;
import fitlibrary.parser.Parser;
import fitlibrary.parser.graphic.GraphicParser;
import fitlibrary.parser.graphic.ObjectDotGraphic;
import fitlibrary.runResults.TestResults;
import fitlibrary.table.Cell;
import fitlibrary.table.Row;
import fitlibrary.table.Table;
import fitlibrary.traverse.FitHandler;
import fitlibrary.traverse.Traverse;
import fitlibrary.traverse.function.CalculateTraverse;
import fitlibrary.traverse.function.ConstraintTraverse;
import fitlibrary.traverse.workflow.caller.DefinedActionCaller;
import fitlibrary.traverse.workflow.caller.TwoStageSpecial;
import fitlibrary.traverse.workflow.special.PrefixSpecialAction;
import fitlibrary.traverse.workflow.special.SpecialActionContext;
import fitlibrary.typed.NonGenericTyped;
import fitlibrary.typed.TypedObject;
import fitlibrary.utility.ClassUtility;
import fitlibrary.xref.CrossReferenceFixture;

@ShowSelectedActions(rename="Work Flow")
public class DoTraverse extends Traverse implements FlowEvaluator, SpecialActionContext {
	private static Logger logger = FitLibraryLogger.getLogger(DoTraverse.class);
	private final PrefixSpecialAction prefixSpecialAction = new PrefixSpecialAction(this);
	protected IDoAutoWrapper doAutoWrapper = new DoAutoWrapper(this);
	protected final DispatchRowInFlow dispatchRowInFlow;
	protected final boolean sequencing;
	public static final String BECOMES_TIMEOUT = "becomes";
	// Methods that can be called within DoTraverse.
	// Each element is of the form "methodName/argCount"
	private final static String[] methodsThatAreVisibleAsActions = {
		"calculate/0", "start/1", "constraint/0", "failingConstraint/0",
		"addAs/2"
	}; // The rest of the methods that used to be here are now in GlobalScope
	//------------------- Methods that are visible as actions (the rest are hidden):
	@Override
	public List<String> methodsThatAreVisible() {
		return Arrays.asList(methodsThatAreVisibleAsActions);
	}
	public DoTraverse() {
		this.sequencing = false;
		this.dispatchRowInFlow = new DispatchRowInFlow(this, sequencing);
	}
	public DoTraverse(Object sut) {
		super(sut);
		this.sequencing = false;
		this.dispatchRowInFlow = new DispatchRowInFlow(this, sequencing);
	}
	public DoTraverse(TypedObject typedObject) {
		super(typedObject);
		this.sequencing = false;
		this.dispatchRowInFlow = new DispatchRowInFlow(this, sequencing);
	}
	public DoTraverse(Object sut, boolean sequencing) {
		super(sut);
		this.sequencing = sequencing;
		this.dispatchRowInFlow = new DispatchRowInFlow(this, sequencing);
	}

	@Override
	public Object interpretAfterFirstRow(Table table, TestResults testResults) {
		// Now handled by DoFlow
		return null;
	}
    @Override
	public TypedObject interpretRow(Row row, TestResults testResults) {
    	return doAutoWrapper.wrap(interpretRowBeforeWrapping(row,testResults));
    }
    final public TypedObject interpretRowBeforeWrapping(Row row, TestResults testResults) {
    	return dispatchRowInFlow.interpretRow(row, testResults);
    }
    // @Overridden in CollectionSetUpTraverse
    @Override
	public Object interpretInFlow(Table table, TestResults testResults) {
    	return null; // Leave it here, as override it.
    }
	// The following is needed for its obligation to the interface SpecialActionContext, which is called by specials
	@Override
	public ICalledMethodTarget findMethodFromRow(Row row, int from, int extrasCellsOnEnd) throws Exception {
		int upTo = row.size() - extrasCellsOnEnd;
		return PlugBoard.lookupTarget.findMethodByArity(row, from, upTo, !dispatchRowInFlow.isDynamicSequencing(), this);
	}
	public ICalledMethodTarget findMethodFromRow222(Row row, int from, int less) throws Exception {
		int extrasCellsOnEnd = less-from-1;
		int upTo = row.size() - extrasCellsOnEnd;
		return PlugBoard.lookupTarget.findMethodByArity(row, from, upTo, !dispatchRowInFlow.isDynamicSequencing(), this);
	}
	protected Object callMethodInRow(Row row, TestResults testResults, boolean catchError, Cell operatorCell) throws Exception {
		return findMethodFromRow222(row,1, 2).invokeForSpecial(row.fromAt(2),testResults,catchError,operatorCell);
	}
//	--- FIXTURE WRAPPERS FOR THIS (and so not available in GlobalScope):
	/** To allow for a CalculateTraverse to be used for the rest of the table.
     */
	@AnAction(wiki="",actionType=ActionType.SIMPLE,
			tooltip="Treat the rest of the table as a calculate table.")
	public CalculateTraverse calculate() {
		CalculateTraverse traverse;
		if (this.getClass() == DoTraverse.class)
			traverse = new CalculateTraverse(getTypedSystemUnderTest());
		else
			traverse = new CalculateTraverse(this);
		return traverse;
	}
    /** To allow for DoTraverse to be used without writing any fixturing code.
     */
	public void start(String className) {
		try {
		    setSystemUnderTest(ClassUtility.newInstance(className));
		} catch (Exception e) {
		    throw new FitLibraryException("Unknown class: "+className);
		}
	}
	/** To allow for a ConstraintTraverse to be used for the rest of the table.
     */
	@AnAction(wiki="",actionType=ActionType.SIMPLE,
			tooltip="Treat the rest of the table as a constraint table.")
	public ConstraintTraverse constraint() {
		return new ConstraintTraverse(this);
	}
	/** To allow for a failing ConstraintTraverse to be used for the rest of the table.
     */
	@AnAction(wiki="",actionType=ActionType.SIMPLE,
			tooltip="Treat the rest of the table as a failing constraint table.")
	public ConstraintTraverse failingConstraint() {
		ConstraintTraverse traverse = new ConstraintTraverse(this,false);
		return traverse;
	}

	//------ THE FOLLOWING ARE HERE SO THAT THEY'RE STILL ACCESSIBLE FROM A SUBCLASS:
	
	//--- BECOMES, ETC TIMEOUTS:
	public void becomesTimeout(int timeout) {
		global().becomesTimeout(timeout);
	}
	public int becomesTimeout() {
		return global().becomesTimeout();
	}
	public int getTimeout(String name) {
		return global().getTimeout(name);
	}
	public void putTimeout(String name, int timeout) {
		global().putTimeout(name,timeout);
	}
	//--- STOP ON ERROR AND ABANDON:
	/** When (stopOnError), don't continue interpreting a table if there's been a problem */
	public void setStopOnError(boolean stopOnError) {
		global().setStopOnError(stopOnError);
	}
	public void abandonStorytest() {
		global().abandonStorytest();
	}
	//--- DYNAMIC VARIABLES:
	public boolean addDynamicVariablesFromFile(String fileName) {
		return global().addDynamicVariablesFromFile(fileName);
	}
	public void addDynamicVariablesFromUnicodeFile(String fileName) throws IOException {
		global().addDynamicVariablesFromUnicodeFile(fileName);
	}
	public boolean clearDynamicVariables() {
		return global().clearDynamicVariables();
	}
	public boolean setSystemPropertyTo(String property, String value) {
		return global().setSystemPropertyTo(property, value);
	}
	@Override
	public void setFitVariable(String variableName, Object result) {
		global().setFitVariable(variableName, result);
	}
	public Object getSymbolNamed(String fitSymbolName) {
		return global().getSymbolNamed(fitSymbolName);
	}
	//--- SLEEP & STOPWATCH:
	public boolean sleepFor(int milliseconds) {
		return global().sleepFor(milliseconds);
	}
	public void startStopWatch() {
		global().startStopWatch();
	}
	public long stopWatch() {
		return global().stopWatch();
	}
	//--- FIXTURE SELECTION
	public SetVariableTraverse setVariables() {
		return global().setVariables();
	}
	public FileHandler file(String fileName) {
		return global().file(fileName);
	}
	public CrossReferenceFixture xref(String suiteName) {
		return global().xref(suiteName);
	}
	//--- DEFINED ACTIONS
	public DefineAction defineAction(String wikiClassName) {
		return global().defineAction(wikiClassName);
	}
	public DefineAction defineAction() {
		return global().defineAction();
	}
	public void defineActionsSlowlyAt(String pageName) throws Exception {
		global().defineActionsSlowlyAt(pageName);
	}
	public void defineActionsAt(String pageName) throws Exception {
		global().defineActionsAt(pageName);
	}
	public void defineActionsAtFrom(String pageName, String rootLocation) throws Exception {
		global().defineActionsAtFrom(pageName,rootLocation);
	}
	public void clearDefinedActions() {
		global().clearDefinedActions();
	}
	public boolean toExpandDefinedActions() {
		return global().toExpandDefinedActions();
	}
	public void setExpandDefinedActions(boolean expandDefinedActions) {
		global().setExpandDefinedActions(expandDefinedActions);
	}
	//--- RANDOM, TO, GET, FILE, HARVEST
	public RandomSelectTraverse selectRandomly(String var) {
		return global().selectRandomly(var);
	}
	public boolean harvestUsingPatternFrom(String[] vars, String pattern, String text) {
		return global().harvestUsingPatternFrom(vars, pattern, text);
	}
	//--- FILE LOGGING
	public void recordToFile(String fileName) {
		global().recordToFile(fileName);
	}
	public void startLogging(String fileName) {
		global().startLogging(fileName);
	}
	@Override
	public void logMessage(String s) {
		global().logMessage(s);
	}
	//--- SHOW
	@Override
	public void show(Row row, String text) {
		global().show(row, text);
	}
	public void logText(String s) {
		global().logText(s);
	}

	//------------------- Postfix Special Actions:
	/** Check that the result of the action in the first part of the row is the same as
	 *  the expected value in the last cell of the row.
	 */
	@AnAction(wiki="|action...|'''<b>is</b>'''|expected value|",actionType=ActionType.SUFFIX,
			tooltip="Check if the result of the action is the expected value.")
	public void is(TestResults testResults, Row row) throws Exception {
		int less = 3;
		if (row.size() < less)
			throw new MissingCellsException("DoTraverseIs");
		ICalledMethodTarget target = findMethodFromRow222(row,0,less);
		Cell expectedCell = row.last();
		target.invokeAndCheckForSpecial(row.fromTo(1,row.size()-2),expectedCell,testResults,row,operatorCell(row));
	}
	public void equals(TestResults testResults, Row row) throws Exception {
		is(testResults,row);
	}
	/** Check that the result of the action in the first part of the row is not the same as
	 *  the expected value in the last cell of the row.
	 */
	@AnAction(wiki="|action...|'''<b>is not</b>'''|unexpected value|",actionType=ActionType.SUFFIX,
			tooltip="Check if the result of the action is not the unexpected value.")
	public void isNot(TestResults testResults, Row row) throws Exception {
		int less = 3;
		if (row.size() < less)
			throw new MissingCellsException("DoTraverseIs");
		Cell specialCell = operatorCell(row);
		Cell expectedCell = row.last();
		try {
			ICalledMethodTarget target = findMethodFromRow222(row,0,less);
			Object result = target.invoke(row.fromTo(1,row.size()-2),testResults,true);
			target.notResult(expectedCell, result, testResults);
        } catch (IgnoredException e) {
            //
        } catch (InvocationTargetException e) {
        	Throwable embedded = e.getTargetException();
        	if (embedded instanceof FitLibraryShowException) {
        		specialCell.error(testResults);
        		row.error(testResults, e);
        	} else
        		expectedCell.exceptionExpected(false, e, testResults);
        } catch (Exception e) {
        	expectedCell.exceptionExpected(false, e, testResults);
        }
	}
	private Cell operatorCell(Row row) {
		return row.at(row.size()-2);
	}
	/** Check that the result of the action in the first part of the row, as a string becomes equals
	 *  to the given value within the timeout period.
	 */
	@AnAction(wiki="|action...|'''<b>becomes</b>'''|expected value|",actionType=ActionType.SUFFIX,
			tooltip="Check if the result of the action eventually becomes the expected value. It fails after the timeout period otherwise.")
	public void becomes(TestResults testResults, Row row) throws Exception {
		int less = 3;
		if (row.size() < less)
			throw new MissingCellsException("DoTraverseMatches");
		ICalledMethodTarget target = findMethodFromRow222(row,0,less);
		Cell expectedCell = row.last();
		Row actionPartOfRow = row.fromTo(1,row.size()-2);
		long start = System.currentTimeMillis();
		int becomesTimeout = getTimeout(BECOMES_TIMEOUT);
		boolean matched = false;
		while (System.currentTimeMillis() - start < becomesTimeout ) {
			Object result = target.invokeForSpecial(actionPartOfRow, testResults, false,operatorCell(row));
			if (target.getResultParser().matches(expectedCell, result, testResults)) {
				matched = true;
				break;
			}
			try {
				Thread.sleep(Math.min(100,becomesTimeout/10));
			} catch (Exception e) {
				//
			}
		}
		long delay = System.currentTimeMillis() - start;
		if (!matched && delay > 0)
			logger.trace("becomes failed after "+delay+" milliseconds");
		target.invokeAndCheckForSpecial(actionPartOfRow,expectedCell,testResults,row,operatorCell(row));
	}

	//------------------- Prefix Special Actions:
	/** Check that the result of the action in the rest of the row matches
	 *  the expected value in the last cell of the row.
	 */
	@AnAction(wiki="|'''<b>check</b>'''|action...|expected value|",actionType=ActionType.SELF_FORMAT,
			tooltip="Check if the result of the action is the expected value.")
	public TwoStageSpecial check(Row row) throws Exception {
		return prefixSpecialAction.check(row);
	}
	/** Set the dynamic variable name to the result of the action, or to the string if there's no action.
	 */
	@AnAction(wiki="|'''<b>set</b>'''|dynamic variable name|",actionType=ActionType.PREFIX,
			tooltip="Set the dynamic variable to the result of the action (or the expression when the action starts with |=|).")
	public TwoStageSpecial set(Row row) throws Exception {
		return prefixSpecialAction.set(row);
	}
	/** Set the named FIT symbol to the result of the action, or to the string if there's no action.
	 */
	@AnAction(wiki="|'''<b>set symbol named</b>'''|symbol name|",actionType=ActionType.PREFIX,
			tooltip="Set the Fit symbol to the result of the action.")
	public TwoStageSpecial setSymbolNamed(Row row) throws Exception {
		return prefixSpecialAction.setSymbolNamed(row);
	}
	/** Add a cell containing the result of the rest of the row,
     *  shown as a Dot graphic.
	 * @param testResults 
     */
	@AnAction(wiki="",actionType=ActionType.PREFIX,
			tooltip="Treat the result of the action as a Dot specification, use Dot to create an image, and include it in the report.")
	public void showDot(Row row, TestResults testResults) throws Exception {
		Parser adapter = new GraphicParser(new NonGenericTyped(ObjectDotGraphic.class));
		try {
		    Object result = callMethodInRow(row,testResults, true,row.at(0));
		    row.addCell(adapter.show(new ObjectDotGraphic(result)));
		} catch (IgnoredException e) { // No result, so ignore
		}
	}

	/** The rest of the row is ignored. 
     */
	@SuppressWarnings("unused")
	@AnAction(wiki="",actionType=ActionType.PREFIX,
			tooltip="Ignore the rest of the row.")
	public void note(Row row, TestResults testResults) throws Exception {
		//		Nothing to do
	}
	/** To allow for example storytests in user guide to pass overall, even if they have failures within them. */
	public void expectedTestResults(Row row, TestResults testResults) throws Exception {
		if (testResults.matches(row.text(1,this),row.text(3,this),row.text(5,this),row.text(7,this))) {
			testResults.clear();
			row.at(0).pass(testResults);
		} else {
			String results = testResults.toString();
			testResults.clear();
			row.at(0).fail(testResults,results,this);
		}
	}
	public Object oo(Row row, TestResults testResults) throws Exception {
		if (row.size() < 3)
			throw new MissingCellsException("DoTraverseOO");
		String object = row.text(1,this);
		Object className = getDynamicVariable(object+".class");
		if (className == null || "".equals(className))
			className = object; // then use the object name as a class name
		Row macroRow = row.fromAt(2);
		TypedObject typedObject = new DefinedActionCaller(object,className.toString(),macroRow,getRuntimeContext()).run(row, testResults);
		return typedObject.getSubject();
	}
	/** Don't mind that the action succeeds or not, just as long as it's not a FitLibraryException (such as action unknown) 
     */
//	@AnAction(wiki="",actionType=ActionType.PREFIX,
//			tooltip="Ignore the result of the action, just as long as it's not a FitLibraryException.")
	public void optionally(Row row, TestResults testResults) throws Exception {
		try {
		    Object result = callMethodInRow(row,testResults, true,row.at(0));
		    if (result instanceof Boolean && !((Boolean)result).booleanValue()) {
		    	row.addCell("false").shown();
		    	getRuntimeContext().getDefinedActionCallManager().addShow(row);
		    }
		} catch (FitLibraryException e) {
			row.at(0).error(testResults,e);
		} catch (Exception e) {
			row.addCell(PlugBoard.exceptionHandling.exceptionMessage(e)).shown();
			getRuntimeContext().getDefinedActionCallManager().addShow(row);
		}
		row.at(0).pass(testResults);
	}
	/*
	 * |''add named''|name|...action or fixture|
	 */
	@AnAction(wiki="|''<i>add named</i>''|name|action... or class name|",actionType=ActionType.SELF_FORMAT,
			tooltip="Take the result of the action (or an instance of the class) and add it the current scope, with the given name. "+
			"This is one way of allowing several objects to be used in a storytest at the same time.")
	public void addNamed(Row row, TestResults testResults) throws Exception {
		int less = 3;
		if (row.size() < less)
			throw new MissingCellsException("addNamed");
		TypedObject typedObject = interpretRow(row.fromAt(2), testResults);
		getRuntimeContext().getTableEvaluator().addNamedObject(row.text(1,this),typedObject,row,testResults);
	}
	/*
	 * |''add global''|...action or fixture|
	 */
	@AnAction(wiki="",actionType=ActionType.PREFIX,
			tooltip="Take the result of the action (or an instance of the class) and add it as a new global to the scope.")
	public void addGlobal(Row row, TestResults testResults) throws Exception {
		int less = 2;
		if (row.size() < less)
			throw new MissingCellsException("addGlobal");
		TypedObject typedObject = interpretRow(row.fromAt(1), testResults);
		if (typedObject == null || typedObject.getSubject() == null)
			return;
		if (typedObject.classType() == DoTraverse.class)
			typedObject = typedObject.getTypedSystemUnderTest();
		typedObject.injectRuntime(getRuntimeContext());
		getRuntimeContext().getScope().addGlobal(typedObject);
		row.at(0).pass(testResults);
	}
	@Override
	public FitHandler fitHandler() {
		return getFitHandler();
	}
}
