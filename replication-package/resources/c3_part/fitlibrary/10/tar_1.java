/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.table;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import fitlibrary.tableOnParse.CellOnParse;
import fitlibrary.tableOnParse.RowOnParse;
import fitlibrary.tableOnParse.TableOnParse;
import fitlibrary.tableOnParse.TablesOnParse;

public class TestParseToTableOnList {
	@Test public void setTagLineOnCellOnParse() {
		TableFactory.useOnLists(false);
		Cell cell = TableFactory.cell("ab");
		TableFactory.pop();
		assertThat(cell,instanceOf(CellOnParse.class));
		assertThat(cell.toString(),is("\n<td>ab</td>"));
		assertThat(cell.getLeader(),is("\n"));
		assertThat(cell.getTrailer(),is(""));

		cell.setTagLine("extra");
		assertThat(cell.getTagLine(),is("extra"));
		assertThat(cell.toString(),is("\n<td extra>ab</td>"));
	}
	@Test public void setTagLineOnCellOnList() {
		TableFactory.useOnLists(true);
		Cell cell = TableFactory.cell("ab");
		TableFactory.pop();
		assertThat(cell,instanceOf(CellOnList.class));
		assertThat(cell.toString(),is("<td>ab</td>"));
		assertThat(cell.getLeader(),is(""));
		assertThat(cell.getTrailer(),is(""));

		cell.setTagLine("extra");
		assertThat(cell.getTagLine(),is("extra"));
		assertThat(cell.toString(),is("<td extra>ab</td>"));
	}
	@Test public void convertCellToListForm() {
		TableFactory.useOnLists(false);
		Cell cell = TableFactory.cell("ab");
		TableFactory.pop();
		assertThat(cell,instanceOf(CellOnParse.class));
		cell.addToTag("extra");
		cell.addToTag("cost");
		assertThat(cell.getTagLine(),is("extra cost"));
		assertThat(cell.toString(),is("\n<td extra cost>ab</td>"));
		assertThat(cell.getLeader(),is("\n"));
		assertThat(cell.getTrailer(),is(""));

		TableFactory.useOnLists(true);
		Cell resultingCell = TableConversion.convert(cell);
		TableFactory.pop();
		assertThat(resultingCell,instanceOf(CellOnList.class));
		assertThat(resultingCell.getTagLine(),is("extra cost"));
		assertThat(resultingCell.toString(),is("\n<td extra cost>ab</td>"));
		assertThat(cell.getLeader(),is("\n"));
		assertThat(cell.getTrailer(),is(""));
	}
	@Test public void convertCellFromListForm() {
		TableFactory.useOnLists(true);
		Cell cell = TableFactory.cell("ab");
		TableFactory.pop();
		assertThat(cell,instanceOf(CellOnList.class));
		cell.addToTag("extra");
		cell.addToTag("cost");
		assertThat(cell.getTagLine(),is("extra cost"));
		assertThat(cell.toString(),is("<td extra cost>ab</td>"));

		TableFactory.useOnLists(false);
		Cell resultingCell = TableConversion.convert(cell);
		TableFactory.pop();
		assertThat(resultingCell,instanceOf(CellOnParse.class));
		assertThat(resultingCell.getTagLine(),is("extra cost"));
		assertThat(resultingCell.toString(),is("<td extra cost>ab</td>"));
	}
	@Test public void convertTableToListForm() {
		TableFactory.useOnLists(false);
		Table table = TableFactory.table();
		TableFactory.pop();
		assertThat(table,instanceOf(TableOnParse.class));
		table.addToTag("extra");
		table.addToTag("cost");
		assertThat(table.getTagLine(),is("border=\"1\" cellspacing=\"0\" extra cost"));
		assertThat(table.toString(),is("\n<table border=\"1\" cellspacing=\"0\" extra cost></table>"));

		TableFactory.useOnLists(true);
		Table resultingTable = TableConversion.convert(table);
		TableFactory.pop();
		assertThat(resultingTable,instanceOf(TableOnList.class));
		assertThat(resultingTable.getTagLine(),is("border=\"1\" cellspacing=\"0\" extra cost"));
		assertThat(resultingTable.toString(),is("\n<table border=\"1\" cellspacing=\"0\" extra cost></table>"));
	}
	@Test public void convertTableFromListForm() {
		TableFactory.useOnLists(true);
		Table table = TableFactory.table();
		TableFactory.pop();
		assertThat(table,instanceOf(TableOnList.class));
		table.addToTag("extra");
		table.addToTag("cost");
		assertThat(table.getTagLine(),is("border=\"1\" cellspacing=\"0\" extra cost"));
		assertThat(table.toString(),is("<table border=\"1\" cellspacing=\"0\" extra cost></table>"));

		TableFactory.useOnLists(false);
		Table resultingTable = TableConversion.convert(table);
		TableFactory.pop();
		assertThat(resultingTable,instanceOf(TableOnParse.class));
		assertThat(resultingTable.getTagLine(),is("border=\"1\" cellspacing=\"0\" extra cost"));
		assertThat(resultingTable.toString(),is("<table border=\"1\" cellspacing=\"0\" extra cost></table>"));
	}
	@Test public void convertTablesToListForm() {
		TableFactory.useOnLists(false);
		Tables tables = TableFactory.tables(TableFactory.table(TableFactory.row("a","b")));
		TableFactory.pop();
		assertThat(tables,instanceOf(TablesOnParse.class));
		tables.at(0).setLeader("LL");
		tables.at(0).setTrailer("TT");
		tables.at(0).addToTag("RR");
		tables.at(0).at(0).setLeader("00LL");
		tables.at(0).at(0).setTrailer("00TT");
		tables.at(0).at(0).at(0).addToTag(" 00RR");

		TableFactory.useOnLists(true);
		Tables resultingTables = TableConversion.convert(tables);
		TableFactory.pop();
		
		assertThat(resultingTables,instanceOf(TablesOnList.class));
		Table resultingTable = resultingTables.at(0);
		assertThat(resultingTable,instanceOf(TableOnList.class));
		assertThat(resultingTable.getLeader(),is("LL"));
		assertThat(resultingTable.getTrailer(),is("TT"));
		assertThat(resultingTable.getTagLine(),is("border=\"1\" cellspacing=\"0\" RR"));
		assertThat(resultingTables.size(),is(1));
		assertThat(resultingTable.size(),is(1));
		Row resultingRow = resultingTable.at(0);
		assertThat(resultingRow,instanceOf(RowOnList.class));
		assertThat(resultingRow.getLeader(),is("00LL"));
		assertThat(resultingRow.getTrailer(),is("00TT"));
		assertThat(resultingRow.size(),is(2));
		Cell resultingFirstCell = resultingRow.at(0);
		assertThat(resultingFirstCell,instanceOf(CellOnList.class));
		assertThat(resultingFirstCell.getTagLine(),is("00RR"));
		assertThat(resultingFirstCell.text(),is("a"));
		assertThat(resultingRow.at(1).text(),is("b"));
	}
	@Test public void convertToParseForm() {
		TableFactory.useOnLists(true);
		Tables tables = TableFactory.tables(TableFactory.table(TableFactory.row("a","b")));
		assertThat(tables,instanceOf(TablesOnList.class));
		TableFactory.pop();
		tables.at(0).setLeader("LL");
		tables.at(0).setTrailer("TT");
		tables.at(0).addToTag("RR");
		tables.at(0).at(0).setLeader("00LL");
		tables.at(0).at(0).setTrailer("00TT");
		tables.at(0).at(0).at(0).addToTag(" 00RR");

		TableFactory.useOnLists(false);
		Tables resultingTables = TableConversion.convert(tables);
		TableFactory.pop();
	
		assertThat(resultingTables,instanceOf(TablesOnParse.class));
		Table resultingTable = resultingTables.at(0);
		assertThat(resultingTable,instanceOf(TableOnParse.class));
		assertThat(resultingTable.getLeader(),is("LL"));
		assertThat(resultingTable.getTrailer(),is("TT"));
		assertThat(resultingTable.getTagLine(),is("border=\"1\" cellspacing=\"0\" RR"));
		assertThat(resultingTables.size(),is(1));
		assertThat(resultingTable.size(),is(1));
		Row resultingRow = resultingTable.at(0);
		assertThat(resultingRow,instanceOf(RowOnParse.class));
		assertThat(resultingRow.getLeader(),is("00LL"));
		assertThat(resultingRow.getTrailer(),is("00TT"));
		assertThat(resultingRow.size(),is(2));
		Cell resultingFirstCell = resultingRow.at(0);
		assertThat(resultingFirstCell,instanceOf(CellOnParse.class));
		assertThat(resultingFirstCell.getTagLine(),is("00RR"));
		assertThat(resultingFirstCell.text(),is("a"));
		assertThat(resultingRow.at(1).text(),is("b"));
	}
}
