/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.spec;

import java.util.Iterator;

import fit.Parse;
import fit.exception.FitParseException;
import fitlibrary.exception.FitLibraryException;
import fitlibrary.global.PlugBoard;
import fitlibrary.runResults.TestResults;
import fitlibrary.suite.BatchFitLibrary;
import fitlibrary.suite.StorytestRunner;
import fitlibrary.table.Cell;
import fitlibrary.table.Row;
import fitlibrary.table.Table;
import fitlibrary.table.TableElement;
import fitlibrary.table.TableFactory;
import fitlibrary.table.Tables;
import fitlibrary.traverse.Traverse;
import fitlibrary.utility.HtmlUtils;
import fitlibrary.utility.ParseUtility;

public class SpecifyFixture extends Traverse {
	private final StorytestRunner runner;
	private final SpecifyErrorReport errorReport;
	
	public SpecifyFixture() {
		this.runner = new BatchFitLibrary();
		this.errorReport = new SpecifyErrorReported();
	}
	public SpecifyFixture(StorytestRunner runner, SpecifyErrorReport errorReport) {
		this.runner = runner;
		this.errorReport = errorReport;
	}
	@Override
	public Object interpretAfterFirstRow(Table table, TestResults testResults) {
		try {
			Cell actualCell = table.at(1).at(0);
			if (actualCell.isEmpty())
				throw new FitLibraryException("Missing nested tables to be run");
			Cell expectedCell = expectedOf(table);
			Tables expectedTables = expectedCell.getEmbeddedTables();
			Tables actualTables = actualCell.getEmbeddedTables();
			runner.doStorytest(actualTables);
			if (tablesEqual("",actualTables,expectedTables)) {
				expectedCell.pass(testResults);
				testResults.addRights(cellCount(actualTables) - 1);
			} else {
				expectedCell.fail(testResults);
				errorReport.actualResult(actualTables);
			}
		} catch (Exception e) {
			table.error(testResults, e);
		}
		return null;
	}

	public boolean tablesEqual(String path, TableElement actual, TableElement expected) {
		boolean actualContainsHtmlDueToShow = false;
		if (actual.getClass() != expected.getClass())
			throw new RuntimeException("In SpecifyFixture, the classes don't match: "+
					actual.getClass()+" and "+expected.getClass());
		if (actual instanceof Cell) {
			Cell actualCell = (Cell) actual;
			Cell expectedCell = (Cell) expected;
			if (!equals(actualCell.fullText(),expectedCell.fullText())) {
				if (!actualCell.hasEmbeddedTables() && expectedCell.hasEmbeddedTables()) {
					try {
//						showAfterTable("Actual: "+escape(actualCell.fullText())+"\n");
						Tables actualTables = TableFactory.tables(new Parse(actualCell.fullText()));
						if (!tablesEqual(path,actualTables,expectedCell.getEmbeddedTables()))
							return false;
						actualContainsHtmlDueToShow = true;
					} catch (FitParseException e) {
						errorReport.cellTextWrong(path,actualCell.fullText(),expectedCell.fullText());
						return false;
					}
				} else {
					errorReport.cellTextWrong(path,actualCell.fullText(),expectedCell.fullText());
					return false;
				}
			}
			actual = actualCell.getEmbeddedTables();
			expected = expectedCell.getEmbeddedTables();
		}
		if (!actualContainsHtmlDueToShow) {
			if (!equals(actual.getLeader(),expected.getLeader())) {
				errorReport.leaderWrong(path, actual.getLeader(), expected.getLeader());
				return false;
			}
			if (!equals(actual.getTrailer(),expected.getTrailer())) {
				errorReport.trailerWrong(path, actual.getTrailer(), expected.getTrailer());
				return false;
			}
			if (!actual.getTagLine().equals(expected.getTagLine())) {
				errorReport.tagLineWrong(path, actual.getTagLine(), expected.getTagLine());
				return false;
			}
			if (actual.size() != expected.size()) {
				errorReport.sizeWrong(path,actual.size(),expected.size());
				return false;
			}
		}
		Iterator<TableElement> actuals = actual.iterator();
		Iterator<TableElement> expecteds = expected.iterator();
		int count = 0;
		while (actuals.hasNext()) {
			TableElement act = actuals.next();
			String nameOfElement = act.getType()+"["+count+"]";
			String pathFurther = path.isEmpty() ? nameOfElement : path + "." + nameOfElement;
			if (!tablesEqual(pathFurther,act,expecteds.next()))
				return false;
			count++;
		}
		return true;
	}

	private boolean equals(String actual, String expected) {
		String canonicalActual = canonical(actual);
		String canonicalExpected = canonical(expected);
		
		if ("IGNORE".equals(canonicalExpected))
			return true;
		String stackTrace = "class=\"fit_stacktrace\">";
		int start = canonicalExpected.indexOf(stackTrace);
		if (start >= 0)
			return canonicalActual.startsWith(canonicalExpected.substring(0,start+stackTrace.length()));
		String fitLabel = "<span class=\"fit_label\">";
		start = canonicalExpected.indexOf(fitLabel);
		if (start >= 0)
			return canonicalActual.startsWith(canonicalExpected.substring(0,start+fitLabel.length()));
		return canonicalActual.equals(canonicalExpected);
	}
	private String canonical(String s) {
		return s.replaceAll("\t"," ").replaceAll("\r","").replaceAll("<hr>","<hr/>\n").replaceAll("<br>","").replaceAll("<br/>","").trim();
	}
	private Cell expectedOf(Table table) {
		if (table.size() == 2 && table.at(1).size() == 2)
			return table.at(1).at(1);
		if (table.size() == 3 && table.at(1).size() == 1 && table.at(2).size() == 1)
			return table.at(2).at(0);
		throw new FitLibraryException("Table must have one row with two cells or two rows with one cell");
	}
	public static int cellCount(Tables tables) {
		int count = 0;
		for (Table table: tables)
			for (Row row: table)
				for (Cell cell: row) {
					count++;
					count += cellCount(cell);
				}
		return count;
	}
	protected String escape(String text) {
		return HtmlUtils.escape(text);
	}
	
	interface SpecifyErrorReport {
		void sizeWrong(String path, int actualSize, int expectedSize);
		void cellTextWrong(String path, String actualText, String expectedText);
		void leaderWrong(String path, String actualLeader, String expectedLeader);
		void trailerWrong(String path, String actualTrailer, String expectedTrailer);
		void tagLineWrong(String path, String actualTagLine, String expectedTagLine);
		void actualResult(Tables expectedTables);
	}
	class SpecifyErrorReported implements SpecifyErrorReport {
		@Override
		public void actualResult(Tables actualTables) {
			ParseUtility.printParse(actualTables.parse(),"actual");
		}
		@Override
		public void sizeWrong(String path, int actualSize, int expectedSize) {
			showAfterTable("Size differs at "+path+
					". Was "+actualSize+". Expected " + expectedSize);
		}
		@Override
		public void cellTextWrong(String path, String actual, String expected) {
			showAfterTable("Cell text differs at " + path + wasExpected(actual, expected));
		}
		@Override
		public void leaderWrong(String path, String actual, String expected) {
			showAfterTable("Leader differs at " + path + wasExpected(actual, expected));
		}
		@Override
		public void tagLineWrong(String path, String actual, String expected) {
			showAfterTable("Tag line differs at " + path + wasExpected(actual, expected));
		}
		@Override
		public void trailerWrong(String path, String actual, String expected) {
			showAfterTable("Trailer differs at "+ path + wasExpected(actual, expected));
		}
		private String wasExpected(String actualText, String expectedText) {
			return ". <table>"+
			plainRow("Actual","Expected")+
			row(actualText,expectedText)+
			optionalEscapedRow(actualText, expectedText)+
			"</table>";
		}
		private String optionalEscapedRow(String actualText, String expectedText) {
			String actualEscaped = escape(actualText);
			String expectedEscaped = escape(expectedText);
			if (actualEscaped.equals(actualText) && expectedEscaped.equals(expectedText))
				return "";
			return row(actualEscaped,expectedEscaped);
		}
		private String row(String actual, String expected) {
			return "<tr><td>"+actual+PlugBoard.stringDifferencing.differences(actual, expected)+
				   "</td><td>"+expected+"</td></tr>";
		}
		private String plainRow(String actual, String expected) {
			return "<tr><td>"+actual+"</td><td>"+expected+"</td></tr>";
		}
	}
}
