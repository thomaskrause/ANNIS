/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.sqlgen;

import java.util.HashMap;
import java.util.Map;


import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;

import annis.model.QueryNode;


public class TableAccessStrategy {

	// default table names
	public final static String NODE_TABLE = "node";
	public final static String RANK_TABLE = "rank";
	public final static String COMPONENT_TABLE = "component";
	public final static String NODE_ANNOTATION_TABLE = "node_annotation";
	public final static String EDGE_ANNOTATION_TABLE = "edge_annotation";
   public final static String ANNOTATION_POOL_TABLE = "annotation_pool";
	public final static String FACTS_TABLE = "facts";
  	public final static String CORPUS_TABLE = "corpus";
  	public final static String CORPUS_ANNOTATION_TABLE = "corpus_annotation";
  	public final static String TEXT_TABLE = "text";

	// the wrapped node
	private QueryNode node;
	
	// table aliases
	private Map<String, String> tableAliases;
	
	// aliased column names
	private Map<String, Map<String, String>> columnAliases;
	
	public TableAccessStrategy() {
		this.tableAliases = new HashMap<String, String>();
		this.columnAliases = new HashMap<String, Map<String,String>>();
		
	}
	
	public TableAccessStrategy(QueryNode node) {
		this();
		this.node = node;
	}
  
  /** Copy constructor */
  public TableAccessStrategy(TableAccessStrategy tas)
  {
    this.tableAliases = new HashMap<String, String>(tas.getTableAliases());
    this.columnAliases = new HashMap<String, Map<String, String>>(tas.getColumnAliases());
    this.node = tas.getNode();
  }

	///// table and column aliases
	
	public String tableName(String table) {
		return tableAliases.containsKey(table) ? tableAliases.get(table) : table;
	}

	public String columnName(String table, String column) {
		if (columnAliases.containsKey(table)) {
			Map<String, String> columns = columnAliases.get(table);
			if (columns.containsKey(column)) {
				return columns.get(column);
			}
		}
		return column;
	}
	
	public String aliasedTable(String table, int count) {
		if (node != null) {
			// sanity checks
//			if (table.equals(NODE_ANNOTATION_TABLE) && count > node.getNodeAnnotations().size())
//				throw new IllegalArgumentException("access to node annotation table out of range: " + count);
			if (table.equals(EDGE_ANNOTATION_TABLE) && count > node.getEdgeAnnotations().size())
				throw new IllegalArgumentException("access to edge annotation table out of range: " + count);
			if (table.equals(NODE_TABLE) && count > 1)
				throw new IllegalArgumentException("access to struct table out of range: " + count);
			if (table.equals(RANK_TABLE) && count > 1)
				throw new IllegalArgumentException("access to rank table out of range: " + count);
			
			// offset table count for edge annotations if node and edge annotations are the same table
			if (table.equals(EDGE_ANNOTATION_TABLE) && isMaterialized(EDGE_ANNOTATION_TABLE, NODE_ANNOTATION_TABLE))
				count = count + node.getNodeAnnotations().size() - 1;
		}
		
		if (count == 0) {
			count = 1;
		}
		
		// compute table counts
		Bag tables = computeSourceTables();

		String aliasedName = tableName(table);
		String aliasCount = node != null ? String.valueOf(node.getId()) : "";
		String countSuffix = tables.getCount(aliasedName) > 1 ? "_" + count : "";

		return aliasedName + aliasCount + countSuffix;
	}

	public String aliasedColumn(String table, String column) {
		return aliasedColumn(table, column, 1);
	}
	
	public String aliasedColumn(String table, String column, int count) {
		return column(aliasedTable(table, count), columnName(table, column));
	}
	
	public String column(String table, String column) {
		return table + "." + column;
	}
	
	///// table usage
	
	protected Bag computeSourceTables() {
		Bag tables = new HashBag();
		
		// hack to support table selections for ANNOTATE query
		if (node == null) {
			String[] tableNames = {NODE_TABLE, RANK_TABLE, COMPONENT_TABLE, NODE_ANNOTATION_TABLE, EDGE_ANNOTATION_TABLE};
			for (String table : tableNames)
				tables.add(table);
			return tables;
		}
		
		tables.add(tableName(NODE_ANNOTATION_TABLE), node.getNodeAnnotations().size());
		if (node.getNodeAnnotations().isEmpty() && node.getNodeAnnotations().size() > 0)
			tables.add(tableName(NODE_ANNOTATION_TABLE));
		
		tables.add(tableName(EDGE_ANNOTATION_TABLE), node.getEdgeAnnotations().size());
		
		if ( tables.getCount(tableName(RANK_TABLE)) == 0 && usesRankTable() )
			tables.add(tableName(RANK_TABLE));
		if ( tables.getCount(tableName(COMPONENT_TABLE)) == 0 && usesRankTable() )
			tables.add(tableName(COMPONENT_TABLE));
		
		if (tables.getCount(tableName(NODE_TABLE)) == 0)
			tables.add(tableName(NODE_TABLE));
		
		return tables;
	}
	
	public boolean usesNodeAnnotationTable() {
		return node == null || ! node.getNodeAnnotations().isEmpty();
	}
	
	public boolean usesRankTable() {
		return node == null || usesComponentTable() || node.isRoot() || node.getArity() != null;
	}
	
	public boolean usesComponentTable() {
		return node == null || node.isPartOfEdge() || usesEdgeAnnotationTable();
	}
	
	public boolean usesEdgeAnnotationTable() {
		return node == null || ! node.getEdgeAnnotations().isEmpty();
	}
	
	public boolean isMaterialized(String table, String otherTable) {
		return tableName(table).equals(tableName(otherTable));
	}
	
	///// delegates
	
	public void addTableAlias(String table, String alias) {
		tableAliases.put(table, alias);
	}
	
	public void addColumnAlias(String table, String column, String alias) {
		if ( ! columnAliases.containsKey(table) )
			columnAliases.put(table, new HashMap<String, String>());
		
		Map<String, String> aliases = columnAliases.get(table);
		aliases.put(column, alias);
	}
	
	///// Getter / Setter
	
	public QueryNode getNode() {
		return node;
	}

	public void setNode(QueryNode node) {
		this.node = node;
	}

	public Map<String, String> getTableAliases() {
		return tableAliases;
	}

	public void setTableAliases(Map<String, String> tableAliases) {
		this.tableAliases = tableAliases;
	}
	

	public Map<String, Map<String, String>> getColumnAliases() {
		return columnAliases;
	}

	public void setColumnAliases(Map<String, Map<String, String>> columnAliases) {
		this.columnAliases = columnAliases;
	}

}
