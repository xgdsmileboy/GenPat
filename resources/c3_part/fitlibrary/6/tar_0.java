/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.table;

import fit.Parse;
import fitlibrary.dynamicVariable.VariableResolver;
import fitlibrary.runResults.TestResults;

public interface Cell extends Tables {
	String text();
	String text(VariableResolver resolver);
	String textLower(VariableResolver resolver);
	String fullText();
	void setText(String text);
	void setUnvisitedEscapedText(String s);
	boolean isBlank(VariableResolver resolver);
	boolean matchesText(String text, VariableResolver resolver);
	String camelledText(VariableResolver resolver);
	
	boolean hasEmbeddedTables();
	Tables getEmbeddedTables();
	Table getEmbeddedTable();
	void setInnerTables(Tables tables);
	
	void passOrFail(TestResults testResults, boolean right);
	void pass(TestResults testResults);
	void pass(TestResults testResults, String msg);
	void passIfNotEmbedded(TestResults testResults);
	void passIfBlank(TestResults testResults, VariableResolver resolver);
	void fail(TestResults testResults);
	void fail(TestResults testResults, String message, VariableResolver resolver);
	void failWithStringEquals(TestResults testResults, String message, VariableResolver resolver);
	void failHtml(TestResults testResults, String html);
	void exceptionExpected(boolean exceptionExpected, Exception e, TestResults testResults);
	void error(TestResults testResults);
	void error(TestResults testResults, Throwable e);
	void error(TestResults testResults, String s);
	void ignore(TestResults testResults);
	void unexpected(TestResults testResults, String string);
	void wrongHtml(TestResults testResults, String show);
	void expectedElementMissing(TestResults testResults);
	void actualElementMissing(TestResults testResults);
	void actualElementMissing(TestResults testResults, String s);
	
	boolean didPass();
	boolean didFail();
	boolean wasIgnored();
	boolean hadError();
	void shown();
	
	boolean unresolved(VariableResolver resolver);
	int getColumnSpan();
	void setColumnSpan(int span);
	void setIsHidden();
	Parse parse();
	void setUnvisitedText(String s);
}
