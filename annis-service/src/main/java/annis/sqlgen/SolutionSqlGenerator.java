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

import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import annis.service.internal.QueryServiceImpl;
import static annis.sqlgen.AbstractSqlGenerator.TABSTOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates identifers for salt which are needed for the
 * {@link QueryServiceImpl#subgraph(java.lang.String, java.lang.String, java.lang.String)}
 *
 * @author Benjamin Weißenfels
 */
public class SolutionSqlGenerator extends AbstractUnionSqlGenerator
  implements SelectClauseSqlGenerator<QueryData>,
  OrderByClauseSqlGenerator<QueryData>,
  GroupByClauseSqlGenerator<QueryData>
{

  private static final Logger log = LoggerFactory.getLogger(
    SolutionSqlGenerator.class);

  private boolean sortSolutions;

  private boolean outputToplevelCorpus;

  private boolean outputNodeName;

  private CorpusPathExtractor corpusPathExtractor;

  private AnnotationConditionProvider annoCondition;

  @Override
  public String selectClause(QueryData queryData, List<QueryNode> alternative,
    String indent)
  {
    int maxWidth = queryData.getMaxWidth();
    Validate.isTrue(alternative.size() <= maxWidth,
      "BUG: nodes.size() > maxWidth");

    boolean needsDistinct = false;
    List<String> cols = new ArrayList<>();
    int i = 0;

    for (QueryNode node : alternative)
    {
      ++i;

      TableAccessStrategy tblAccessStr = tables(node);
      cols.add(tblAccessStr.aliasedColumn(NODE_TABLE, "id") + " AS id" + i);
      if (annoCondition != null)
      {
        if (node.getNodeAnnotations().isEmpty())
        {
          // If a query node is not using annotations, fallback to NULL as the value.
          // This is important for the DISTINCT clause, since we don't want to match 
          // the annotation itself but the node.
          cols.add("NULL::varchar AS node_annotation_ns" + i);
          cols.add("NULL::varchar AS node_annotation_name" + i);
        }
        else
        {
          cols.add(
            annoCondition.getNodeAnnoNamespaceSQL(tblAccessStr) + " AS node_annotation_ns" + i);
          cols.add(
            annoCondition.getNodeAnnoNameSQL(tblAccessStr) + " AS node_annotation_name" + i);
        }
      }
      if (outputNodeName)
      {
        cols.add("min(" + tblAccessStr.aliasedColumn(NODE_TABLE, "node_name")
          + ") AS node_name" + i);
      }

      if (tblAccessStr.usesRankTable() || !node.getNodeAnnotations().isEmpty())
      {
        needsDistinct = true;
      }
    }

    // add additional empty columns in or clauses with different node sizes
    for (i = alternative.size() + 1; i <= maxWidth; ++i)
    {
      cols.add("NULL::bigint AS id" + i);
      cols.add("NULL::varchar AS node_annotation_ns" + i);
      cols.add("NULL::varchar AS node_annotation_name" + i);
      if (outputNodeName)
      {
        cols.add("NULL::varchar AS node_name" + i);
      }
    }

    if (!alternative.isEmpty() && outputNodeName)
    {

      TableAccessStrategy tblAccessStr = tables(alternative.get(0));

      String corpusRefAlias = tblAccessStr.aliasedColumn(NODE_TABLE,
        "corpus_ref");
      cols.add(
        "(SELECT c.path_name FROM corpus AS c WHERE c.id = min(" + corpusRefAlias
        + ") LIMIT 1) AS path_name");
    }

    if (outputToplevelCorpus)
    {
      cols.add("min(" + tables(alternative.get(0)).aliasedColumn(NODE_TABLE,
        "toplevel_corpus")
        + ") AS toplevel_corpus");
    }

    cols.add("min(" + tables(alternative.get(0)).aliasedColumn(NODE_TABLE,
      "corpus_ref")
      + ") AS corpus_ref");

    String colIndent = indent + TABSTOP + TABSTOP;

    return "\n" + colIndent + StringUtils.join(cols, ",\n" + colIndent);
  }

  @Override
  public String orderByClause(QueryData queryData, List<QueryNode> alternative,
    String indent)
  {
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= queryData.getMaxWidth(); ++i)
    {
      ids.add("id" + i);
      if (annoCondition != null)
      {
        ids.add("node_annotation_ns" + i);
        ids.add("node_annotation_name" + i);
      }
    }
    return StringUtils.join(ids, ", ");
  }

  @Override
  public String groupByAttributes(QueryData queryData,
    List<QueryNode> alternative)
  {
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= queryData.getMaxWidth(); ++i)
    {
      ids.add("id" + i);
      if (annoCondition != null)
      {
        ids.add("node_annotation_ns" + i);
        ids.add("node_annotation_name" + i);
      }
    }
    return StringUtils.join(ids, ", ");
  }

  public CorpusPathExtractor getCorpusPathExtractor()
  {
    return corpusPathExtractor;
  }

  public void setCorpusPathExtractor(CorpusPathExtractor corpusPathExtractor)
  {
    this.corpusPathExtractor = corpusPathExtractor;
  }

  public boolean isSortSolutions()
  {
    return sortSolutions;
  }

  public void setSortSolutions(boolean sortSolutions)
  {
    this.sortSolutions = sortSolutions;
  }

  public boolean isOutputToplevelCorpus()
  {
    return outputToplevelCorpus;
  }

  public void setOutputToplevelCorpus(boolean outputToplevelCorpus)
  {
    this.outputToplevelCorpus = outputToplevelCorpus;
  }

  public AnnotationConditionProvider getAnnoCondition()
  {
    return annoCondition;
  }

  public void setAnnoCondition(AnnotationConditionProvider annoCondition)
  {
    this.annoCondition = annoCondition;
  }

  public boolean isOutputNodeName()
  {
    return outputNodeName;
  }

  public void setOutputNodeName(boolean outputNodeName)
  {
    this.outputNodeName = outputNodeName;
  }
  
  
  
}