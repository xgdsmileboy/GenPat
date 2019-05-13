package fitlibrary.definedAction;

import fit.Parse;
import fitlibrary.table.TableOnParse;
import fitlibrary.table.TablesOnParse;

public class DefinedActionBodyCollector {
	// Warning: 'orrible code due to Parse!
	public void parseDefinitions(TablesOnParse tables, DefineActionBodyConsumer consumer) {
		TablesOnParse innerTables = tables;
		// Process the first and last tables differently
		// (Ignore the first & (also) handle last outside loop)
		for (int i = 1; i < tables.size(); i++) {
			TableOnParse table = tables.table(i);
			TableOnParse previousTable = tables.table(i-1);
			if (isHR(table.parse.leader) || isHR(previousTable.parse.trailer)) {
				table.parse.leader = "";
				previousTable.parse.more = null;
				previousTable.parse.trailer = "";
				consumer.addAction(innerTables);
				previousTable.parse.more = table.parse;
				innerTables = new TablesOnParse(table);
			}
		}
		consumer.addAction(innerTables);
	}
	public void parseDefinitions22222(TablesOnParse tables, DefineActionBodyConsumer consumer) {
		TablesOnParse innerTables = tables; // Assume all of the tables for now
		for (int i = 0; i < tables.size(); i++) {
			TableOnParse nextTable = tables.table(i);
			if (i > 0 && isHR(nextTable.parse.leader)) {
				TableOnParse lastTableInDefinition = tables.table(i-1);
				Parse more = lastTableInDefinition.parse.more;
				String trailer = lastTableInDefinition.parse.trailer;
				lastTableInDefinition.parse.more = null;
				lastTableInDefinition.parse.trailer = "";
				consumer.addAction(innerTables);
				lastTableInDefinition.parse.more = more;
				lastTableInDefinition.parse.trailer = trailer;
				innerTables = new TablesOnParse(nextTable);
			} else if (isHR(nextTable.parse.trailer) || i == tables.size() - 1)
				consumer.addAction(innerTables);
		}
	}
	private boolean isHR(String s) {
		return s != null && (s.contains("<hr>") || s.contains("<hr/>"));
	}
	public interface DefineActionBodyConsumer {
		void addAction(TablesOnParse innerTables);
	}
}
