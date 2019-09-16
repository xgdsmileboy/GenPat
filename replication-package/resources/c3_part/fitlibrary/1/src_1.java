/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary;

import fit.Fixture;
import fit.Parse;
import fitlibrary.dynamicVariable.DynamicVariables;
import fitlibrary.parser.lookup.ParseDelegation;
import fitlibrary.runResults.TestResults;
import fitlibrary.runResults.TestResultsFactory;
import fitlibrary.runtime.RuntimeContextInternal;
import fitlibrary.table.Table;
import fitlibrary.traverse.Evaluator;
import fitlibrary.traverse.Traverse;
import fitlibrary.typed.TypedObject;

/**
 * An abstract superclass of all the flow-style fixtures.
 * It manages differences between the Fit and FitNesse versions of the FitLibrary,
 * by delegating to an object of class fitlibrary.FitNesseDifference.
 * This class is different in the builds between the two versions, but is
 * needed here to avoid compiletime conflicts. It also has to be created 
 * reflectively, because we can't mention its name here, except in a String.
 */
public abstract class FitLibraryFixture extends Fixture implements Evaluator {
	private Traverse traverse;
	private TypedObject typedObjectUnderTest = Traverse.asTypedObject(null);

	/** Registers a delegate, a class that will
	 * handle parsing of other types of values.
	 */
	protected void registerParseDelegate(Class<?> type, Class<?> parseDelegate) {
		ParseDelegation.registerParseDelegate(type,parseDelegate);
	}
	/** Registers a delegate object that will
	 * handle parsing of other types of values.
	 */
	protected void registerParseDelegate(Class<?> type, Object parseDelegate) {
		ParseDelegation.registerParseDelegate(type,parseDelegate);
	}
	/** Registers a delegate object that will
	 * handle parsing of the given type and any subtype.
	 */
	protected void registerSuperParseDelegate(Class<?> type, Object superParseDelegate) {
		ParseDelegation.registerSuperParseDelegate(type,superParseDelegate);
	}
    /** Set the systemUnderTest. 
	 *  If an action can't be satisfied by the fixture, the systemUnderTest
	 *  is tried instead. Thus the fixture is an adapter with methods just
	 *  when they're needed.
	 */
	public void setSystemUnderTest(Object sut) {
		typedObjectUnderTest = Traverse.asTypedObject(sut);
	}
	public Object getSystemUnderTest() {
		return typedObjectUnderTest.getSubject();
	}
	public TypedObject getTypedSystemUnderTest() {
		return typedObjectUnderTest;
	}
    public final Traverse traverse() {
    	return traverse;
    }
    protected void setTraverse(Traverse traverse) {
    	this.traverse = traverse;
    }
    @Override
    public void doTable(Parse parseTable) {
    	throw new RuntimeException("Please use FitLibraryServer instead of FitServer.");
    }
	public boolean doEmbeddedTablePasses(Table table, Evaluator evaluator, TestResults testResults) {
		return traverse().doesInnerTablePass(table,evaluator,testResults);
	}
	public TestResults createTestResults() {
		return TestResultsFactory.testResults(counts);
	}
    public Object interpretAfterFirstRow(Table table, TestResults testResults) {
    	return traverse().interpretAfterFirstRow(table,testResults);
    }
    public RuntimeContextInternal getRuntimeContext() {
		return traverse().getRuntimeContext();
	}
    public DynamicVariables getDynamicVariables() {
    	return getRuntimeContext().getDynamicVariables();
    }
	public void setRuntimeContext(RuntimeContextInternal propertyValues) {
		traverse().setRuntimeContext(propertyValues);
	}
	public void setDynamicVariable(String key, Object value) {
		traverse().setDynamicVariable(key, value);
	}
    public String resolve(String key) {
    	return traverse().resolve(key);
    }
}
