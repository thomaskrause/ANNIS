/*
 * Copyright 2013 SFB 632.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.sqlgen.splitfacts;

import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import annis.sqlgen.DefaultWhereClauseGenerator;
import annis.sqlgen.WhereClauseSqlGenerator;
import java.util.List;
import static annis.sqlgen.SqlConstraints.in;
import static annis.sqlgen.SqlConstraints.join;
import static annis.sqlgen.SqlConstraints.sqlString;

/**
 * {@link WhereClauseSqlGenerator} for the splitfacts scheme.
 * @author Thomas Krause <thomas.krause@alumni.hu-berlin.de>
 */
import annis.sqlgen.TableAccessStrategy;
import annis.sqlgen.model.CommonAncestor;
import annis.sqlgen.model.RankTableJoin;
import org.apache.commons.lang3.StringUtils;
public class SfWhereClauseGenerator extends DefaultWhereClauseGenerator
{

  @Override
  protected void addComponentPredicates(List<String> conditions, QueryNode node,
    String edgeType, String componentName,  List<Long> corpora)
  {
    conditions.add(
      join("=", 
      tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "type_ref"), 
      "getComponentTypeNameOnly(" + sqlString(edgeType) + ", " 
        + (componentName == null ? "NULL" : sqlString(componentName)) + ", "
        + "ARRAY[" + StringUtils.join(corpora, ", ") + "]"
        + ")"
      )
    );    
  }

  @Override
  protected void addComponentPredicates(List<String> conditions, QueryNode node,
    QueryNode target, String componentName, String edgeType,
    List<Long> corpora)
  {
    if ("lhs".equals(getComponentPredicates()) || "both".equals(getComponentPredicates()))
    {
      addComponentPredicates(conditions, node, edgeType, componentName, corpora);
    }
    if ("rhs".equals(getComponentPredicates()) || "both".equals(getComponentPredicates()))
    {
      addComponentPredicates(conditions, target, edgeType, componentName, corpora);
    }
  }
  
    // FIXME: why not in addSingleEdgeConditions() ?
  protected void addLeftOrRightDominance(List<String> conditions, QueryNode node,
    QueryNode target, QueryData queryData, RankTableJoin join,
    String aggregationFunction, String tokenBoarder)
  {
    RankTableJoin rankTableJoin = (RankTableJoin) join;
    String componentName = rankTableJoin.getName();
    addComponentPredicates(conditions, node, target, componentName, "d", queryData.getCorpusList());

    TableAccessStrategy tas = tables(null);

    conditions.add(join("=", tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "pre"),
      tables(target).aliasedColumn(TableAccessStrategy.RANK_TABLE, "parent")));

    List<Long> corpusList = queryData.getCorpusList();
    
    
    boolean doJoin = !tas.isMaterialized(TableAccessStrategy.NODE_TABLE, TableAccessStrategy.RANK_TABLE);
    
    String innerSelect = 
       "SELECT "
      + aggregationFunction
      + "(lrsub."
      + tokenBoarder
      + ") FROM ";
    if(doJoin)
    {
      innerSelect +=
          tas.tableName(TableAccessStrategy.NODE_TABLE) + " AS lrsub, "
        + tas.tableName(TableAccessStrategy.RANK_TABLE) + " AS lrsub_rank ";
    }
    else
    {
      innerSelect += 
        tas.tableName(TableAccessStrategy.RANK_TABLE)
        + " as lrsub ";
    }
    
    innerSelect +=
        "WHERE parent="
      + tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "pre")
      + " AND lrsub_rank.corpus_ref="
      + tables(target).aliasedColumn(TableAccessStrategy.RANK_TABLE, "corpus_ref")
      + " AND lrsub.corpus_ref="
      + tables(target).aliasedColumn(TableAccessStrategy.NODE_TABLE, "corpus_ref")
      + " AND lrsub.toplevel_corpus IN("
      + (corpusList == null || corpusList.isEmpty() ? "NULL"
      : StringUtils.join(corpusList, ",")) + ")";
    
    if(doJoin)
    {
      innerSelect +=
        " AND lrsub_rank.toplevel_corpus IN("
        + (corpusList == null || corpusList.isEmpty() ? "NULL"
        : StringUtils.join(corpusList, ",")) + ")"
        + " AND lrsub_rank.node_ref = lrsub.id";
    }
    
    conditions.add(in(
      tables(target).aliasedColumn(TableAccessStrategy.NODE_TABLE, tokenBoarder), innerSelect));
  }

  @Override
  protected void addCommonAncestorConditions(List<String> conditions,
    QueryNode node, QueryNode target, CommonAncestor join, QueryData queryData)
  {
    List<Long> corpusList = queryData.getCorpusList();
    String componentName = join.getName();
    addComponentPredicates(conditions, node, target, componentName, "d", queryData.getCorpusList());

    if (!isAllowIdenticalSibling())
    {
      joinOnNode(conditions, node, target, "<>", "id", "id");
    }

    // fugly
    TableAccessStrategy tas = tables(null);
    String pre1 = tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "pre");
    String pre2 = tables(target).aliasedColumn(TableAccessStrategy.RANK_TABLE, "pre");
    
    String corpusref1 = tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "corpus_ref");
    String corpusref2 = tables(target).aliasedColumn(TableAccessStrategy.RANK_TABLE, "corpus_ref");
    
    String typeref1 = tables(node).aliasedColumn(TableAccessStrategy.RANK_TABLE, "type_ref");
    String typeref2 = tables(target).aliasedColumn(TableAccessStrategy.RANK_TABLE, "type_ref");
    
    String pre = tas.column("ancestor", tas.columnName(TableAccessStrategy.RANK_TABLE, "pre"));
    String post = tas.column("ancestor", tas.columnName(TableAccessStrategy.RANK_TABLE, "post"));
    
    StringBuffer sb = new StringBuffer();
    sb.append("EXISTS (SELECT 1 FROM " + tas.tableName(TableAccessStrategy.RANK_TABLE)
      + " AS ancestor WHERE\n");
    
    sb.append("\t" + pre + " < " + pre1 + " AND " + pre1 + " < " + post
      + " AND\n");
    sb.append("\t"
      + pre
      + " < "
      + pre2
      + " AND "
      + pre2
      + " < "
      + post + " AND\n");
    sb.append("\tancestor.corpus_ref=").append(corpusref1)
      .append(" AND ancestor.corpus_ref=").append(corpusref2).append(" AND \n");
    
    sb.append("\tancestor.type_ref=").append(typeref1)
      .append(" AND ancestor.type_ref=").append(typeref2).append("\n");
    
    if (isUseToplevelCorpusPredicateInCommonAncestorSubquery() && !(corpusList
      == null || corpusList.isEmpty()))
    {
      sb.append(" AND toplevel_corpus IN(");
      sb.append(StringUtils.join(corpusList, ","));
      sb.append(")");
    }
    sb.append(")");
    conditions.add(sb.toString());
  }
  
  
  
  
  
}
