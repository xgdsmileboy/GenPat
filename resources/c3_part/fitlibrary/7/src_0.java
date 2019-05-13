/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
 */
package fitlibrary.spec;

import fit.Fixture;
import fit.Parse;
import fit.exception.FitParseException;
import fitlibrary.suite.BatchFitLibrary;
import fitlibrary.table.TableFactory;
import fitlibrary.utility.ParseUtility;
import fitlibrary.utility.StringUtility;

/**
 * Uses embedded tables to specify how fixtures work, based on simple
 * subclasses of those fixtures.
 * The test and the report can be in two separate rows, or in a single row.
 */
public class SpecifyFixture extends Fixture {
	@Override
	public void doTable(Parse table) {
		final Parse firstRow = table.parts.more;
		final Parse actual = firstRow.parts.parts;
		final Parse secondRow = firstRow.more;
		Parse expectedCell;
		if (secondRow != null)
			expectedCell = secondRow.parts;
		else
			expectedCell = firstRow.parts.more;
		Parse expected = expectedCell.parts;

		new BatchFitLibrary().doStorytest(TableFactory.tables(actual));
		if (reportsEqual(actual,expected)) {
			right(expectedCell);
			counts.right += cellCount(actual) - 1;
		} else {
			wrong(expectedCell);
			ParseUtility.printParse(actual,"actual");
			addTableToBetterShowDifferences(table,actual,expected);
		}
	}
	private int cellCount(Parse parse) {
		if (parse == null)
			return 0;
		int count = 0;
		if (parse.parts == null)
			count = 1;
		return count + cellCount(parse.parts) + cellCount(parse.more);
	}
	protected void addTableToBetterShowDifferences(Parse table, Parse actual, Parse expected) {
		Parse end = table.last();
		Parse cells1 = new Parse("td","fitlibrary.CommentFixture",null,null);
		Parse cells2 = new Parse("td","actual",null,new Parse("td","expected",null,null));
		Parse cells3 = new Parse("td",show(actual),null,new Parse("td",show(expected),null,null));
		Parse rows = new Parse("tr","",cells1 ,new Parse("tr","",cells2 ,new Parse("tr","",cells3 ,null)));
		end.more = new Parse("table","",rows ,null);
	}
	private static String show(Parse parse) {
		if (parse == null)
			return "null";
		String result = "&lt;"+parse.tag.substring(1,parse.tag.length()-1)+"&gt;<ul>";
		result += showField("leader", parse.leader);
		if (parse.parts != null)
			result += show(parse.parts);
		else
			result += showField("body", parse.body);
		result += showField("trailer", parse.trailer);
		result += "</ul>";
		if (parse.more != null)
			result += show(parse.more);
		return result;
	}
	private static String showField(String field, String value) {
		if (value != null && !value.trim().equals(""))
			return "<li>"+field+": '"+noTags(value)+"'";
		return "";
	}
	private static String noTags(String s) {
		String value = s;
		while (true) {
			int index = value.indexOf("<");
			if (index < 0)
				break;
			value = value.substring(0,index)+"&lt;"+value.substring(index+1);
		}
		return value;
	}
	public static boolean reportsEqual(Parse actual, Parse expected) {
		if (actual == null)
			return expected == null;
		if (expected == null)
			return false;
		massageBodyToTable(actual);
		boolean result = equalTags(actual, expected) &&
		equalStrings(actual.leader, expected.leader) &&
		equalBodies(actual,expected) &&
		equalStrings(actual.trailer, expected.trailer) &&
		reportsEqual(actual.more, expected.more) &&
		reportsEqual(actual.parts, expected.parts);
		return result;
	}
	private static void massageBodyToTable(Parse actual) {
		if (actual.body == null || actual.body.indexOf("<table") < 0)
			return;
		if (actual.parts == null) {
			try {
				actual.parts = new Parse(actual.body);
			} catch (FitParseException e) {
				//
			}
		}
		actual.body = "";
	}
	private static boolean equalBodies(Parse actual, Parse expected) {
		boolean result = equalBodies22(actual,expected);
		if (!result) {
			System.out.println("!SpecifyFixture.equalBodies(\"\n"+actual.body+"\",\"\n"+expected.body+"\")");
		}
		return result;
	}
	private static boolean equalBodies22(Parse actual, Parse expected) {
		String expectedBody = canonicalString(expected.body);
		String actualBody = canonicalString(actual.body);
		if (expectedBody.equals("IGNORE"))
			return true;
		if (actualBody.equals(expectedBody))
			return true;
		String stackTrace = "class=\"fit_stacktrace\">";
		int start = expectedBody.indexOf(stackTrace);
		if (start >= 0) {
			String pattern = expectedBody.substring(0,start+stackTrace.length());
			return actual.body.startsWith(pattern);
		}
		start = errorMessage(expectedBody);
		if (start >= 0) {
			int endSpan = expectedBody.indexOf("</span>",start);
			if (endSpan >= 0) {
				String pattern = expectedBody.substring(0,endSpan-1);
				return actual.body.startsWith(pattern);
			}
		}
		return false;
	}
	private static int errorMessage(String expected) {
		if (expected == null)
			return -1;
		return expected.indexOf("<span class=\"fit_label\">");
	}
	private static String canonicalString(String s) {
		if (s == null)
			return "";
		return s.trim().replaceAll("\t"," ").replaceAll("\r","");
	}
	private static boolean equalTags(Parse p1, Parse p2) {
		return p1.tag.equals(p2.tag);
	}
	private static boolean equalStrings(String actualInitially, String expected) {
		String actual = actualInitially;
		if (actual != null && expected != null) {
			if (expected.indexOf("<hr>") >= 0) {
				actual = StringUtility.replaceString(actual,"<hr/>\r\n","<hr>");
				actual = StringUtility.replaceString(actual,"<hr/>\n","<hr>");
				actual = StringUtility.replaceString(actual,"<hr/>\r","<hr>");
			}
			if (expected.indexOf("<br>") >= 0)
				actual = StringUtility.replaceString(actual,"<br/>","<br>"); 
		}
		boolean result = equalStrings22(actual,expected);
		if (result)
			return true;
		if (!result && actual != null && expected != null && 
				aBreak(actual) && expected.equals(""))
			return true;
		int start = errorMessage(expected);
		if (start >= 0) {
			int endSpan = expected.indexOf("</span>",start);
			if (endSpan >= 0) {
				String pattern = expected.substring(0,endSpan-1);
				result = actual.startsWith(pattern);
			}
		}
		if (!result) {
			System.out.println("!SpecifyFixture.equalStrings(\n\""+showWhiteSpace(actual)+"\",\n\""+showWhiteSpace(expected)+"\")");
		}
		return result;
	}
	private static boolean aBreak(String actual) {
		return actual.trim().equals("<br>") || actual.trim().equals("<br/>");
	}
	private static String showWhiteSpace(String actual) {
		if (actual == null)
			return "null";
		return actual.replaceAll(" ","{SPACE}").replaceAll("\t","{TAB}");
	}
	private static boolean equalStrings22(String actual, String expectedInitially) {
		String expected = canonicalString(expectedInitially);
		if (expected.equals("IGNORE"))
			return true;
		return canonicalString(actual).equals(canonicalString(expected));
	}
}
