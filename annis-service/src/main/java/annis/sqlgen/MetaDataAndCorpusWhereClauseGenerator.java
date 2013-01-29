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

import static annis.sqlgen.SqlConstraints.in;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.EDGE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.RANK_TABLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import java.util.LinkedList;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author thomas
 */
public class MetaDataAndCorpusWhereClauseGenerator
  extends TableAccessStrategyFactory
  implements WhereClauseSqlGenerator<QueryData>
{

  @Override
  public Set<String> whereConditions(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    Set<String> conditions = new HashSet<String>();
    List<Long> corpusList = queryData.getCorpusList();
    List<Long> documents = queryData.getDocuments();

    if (documents == null && corpusList == null)
    {
      return new HashSet<String>();
    }

    for (QueryNode node : alternative)
    {
      // FIXME: where condition comment should be optional 
      // conditions.add("-- select documents by metadata and toplevel corpus");
      if (documents != null)
      {
        List<String> emptyList = new LinkedList<String>();
        emptyList.add("NULL");
        
        conditions.add(in(tables(node).aliasedColumn(NODE_TABLE, "corpus_ref"),
          documents.isEmpty() ? emptyList : documents));
      }

      if (corpusList != null && !corpusList.isEmpty())
      {
        TableAccessStrategy tas = tables(node);
        conditions.add(in(tas.aliasedColumn(NODE_TABLE,
          "toplevel_corpus"),
          corpusList));
        
        if (tas.usesNodeAnnotationTable() && !tas.isMaterialized(NODE_ANNOTATION_TABLE, NODE_TABLE))
        {
          conditions.add(in(tas.aliasedColumn(NODE_ANNOTATION_TABLE,
          "toplevel_corpus"),
          corpusList));
        }
        
        if (tas.usesEdgeAnnotationTable() && !tas.isMaterialized(EDGE_ANNOTATION_TABLE, NODE_TABLE))
        {
          conditions.add(in(tas.aliasedColumn(EDGE_ANNOTATION_TABLE,
          "toplevel_corpus"),
          corpusList));
        }
        
        if(tas.usesRankTable() && !tas.isMaterialized(RANK_TABLE, NODE_TABLE))
        {
          conditions.add(in(tables(node).aliasedColumn(RANK_TABLE,
            "toplevel_corpus"),
            corpusList));
        }
      }
    }

    return conditions;
  }
}
