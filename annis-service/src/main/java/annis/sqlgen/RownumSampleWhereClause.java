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

import static annis.sqlgen.SqlConstraints.isTrue;
import static annis.sqlgen.SqlConstraints.join;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import static annis.sqlgen.TableAccessStrategy.COMPONENT_TABLE;
import static annis.sqlgen.TableAccessStrategy.EDGE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.RANK_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.FACTS_TABLE;

import annis.model.QueryNode;
import annis.ql.parser.QueryData;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author thomas
 */
public class RownumSampleWhereClause extends TableAccessStrategyFactory
  implements WhereClauseSqlGenerator<QueryData>
{

  @Override
  public Set<String> whereConditions(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    Set<String> conditions = new HashSet<String>();
    List<Long> corpusList = queryData.getCorpusList();
    List<Long> documents = queryData.getDocuments();

    for (QueryNode node : alternative)
    {
      conditions.addAll(whereConditions(node, corpusList, documents));
    }

    return conditions;
  }

  // VR: inline
  @Deprecated
  public List<String> whereConditions(QueryNode node, List<Long> corpusList,
    List<Long> documents)
  {
    LinkedList<String> conditions = new LinkedList<String>();

    TableAccessStrategy t = tables(node);

    // only apply if we have two materialized views
    if (t.isMaterialized(NODE_TABLE, NODE_ANNOTATION_TABLE)
      && t.isMaterialized(RANK_TABLE, EDGE_ANNOTATION_TABLE))
    {

      if (!t.usesRankTable() && !t.usesComponentTable() && !t.
        usesNodeAnnotationTable() && !t.usesEdgeAnnotationTable())
      {
        conditions.add(join("=", t.aliasedColumn(NODE_TABLE, "n_na_rownum"),
          "1"));
      }
      else if (t.usesRankTable() && !t.usesEdgeAnnotationTable())
      {
        conditions.add(join("=", t.aliasedColumn(RANK_TABLE, "r_ea_rownum"),
          "1"));
      }
    }
    return conditions;
  }
}
