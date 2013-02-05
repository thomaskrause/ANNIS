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
import static annis.sqlgen.TableAccessStrategy.CORPUS_TABLE;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;

import annis.service.objects.Match;
import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import annis.service.internal.QueryService;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates identifers for salt which are needed for the
 * {@link QueryService#subgraph(java.lang.String, java.lang.String, java.lang.String)}
 *
 * @author Benjamin Weißenfels
 */
public class FindSqlGenerator extends AbstractUnionSqlGenerator<List<Match>>
  implements SelectClauseSqlGenerator<QueryData>, 
  OrderByClauseSqlGenerator<QueryData>,
  GroupByClauseSqlGenerator<QueryData>
{  
  
  private static final Logger log = LoggerFactory.getLogger(FindSqlGenerator.class);
  
  // optimize DISTINCT operation in SELECT clause
  private boolean optimizeDistinct;
  private boolean sortSolutions;
  private boolean verboseOutput;
  private CorpusPathExtractor corpusPathExtractor;

  @Override
  public String selectClause(QueryData queryData, List<QueryNode> alternative,
    String indent)
  {
    int maxWidth = queryData.getMaxWidth();
    Validate.isTrue(alternative.size() <= maxWidth,
      "BUG: nodes.size() > maxWidth");

    List<String> ids = new ArrayList<String>();
    int i = 0;

    boolean addDistinct = distinctNeeded(alternative);
    
    for (QueryNode node : alternative)
    {
      ++i;

      TableAccessStrategy tblAccessStr = tables(node);
      ids.add(tblAccessStr.aliasedColumn(NODE_TABLE, "id") + " AS id" + i);
      ids.add(tblAccessStr.aliasedColumn(NODE_TABLE, "corpus_ref") + " AS corpus" + i);
      if(verboseOutput)
      {
        if(addDistinct)
        {
          ids.add("min(" + tblAccessStr.aliasedColumn(NODE_TABLE, "name")
            + ") AS node_name" + i);

          ids.add("min(" + tblAccessStr.aliasedColumn(CORPUS_TABLE, "path_name")
            + ") AS path_name" + i);
        }
        else
        {
          ids.add(tblAccessStr.aliasedColumn(NODE_TABLE, "name")
            + " AS node_name" + i);

          ids.add(tblAccessStr.aliasedColumn(CORPUS_TABLE, "path_name")
            + " AS path_name" + i);
        }
      }
    } // end for each output node

    for (i = alternative.size(); i < maxWidth; ++i)
    {
      ids.add("NULL");
    }

    ids.add(tables(alternative.get(0)).aliasedColumn(NODE_TABLE,
      "corpus_ref"));

    
    if(verboseOutput)
    {
      if(addDistinct)
      {
        ids.add("min(" + tables(alternative.get(0)).aliasedColumn(NODE_TABLE,
          "toplevel_corpus") + ")");
      }
      else
      {
        ids.add(tables(alternative.get(0)).aliasedColumn(NODE_TABLE,
          "toplevel_corpus"));
      }
    } //
    
    return "\n" + indent + TABSTOP
      + StringUtils.join(ids, ",\n" + indent + TABSTOP);
  }
  
  @Override
  protected void appendOrderByClause(StringBuffer sb, QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    // only use ORDER BY clause if result has to be sorted
    if (!sortSolutions)
    {
      return;
    }
    // don't use ORDER BY clause if we are only counting saves a sort
    List<LimitOffsetQueryData> extensions =
      queryData.getExtensions(LimitOffsetQueryData.class);
    
    if (extensions.size() > 0)
    {
      super.appendOrderByClause(sb, queryData, alternative, indent);
    }
  }
  
  @Override
  public String orderByClause(QueryData queryData, List<QueryNode> alternative,
    String indent)
  {
    List<String> ids = new ArrayList<String>();
    for (int i = 1; i <= queryData.getMaxWidth(); ++i)
    {
      ids.add("corpus" + i);
      ids.add("id" + i);
    }
    return StringUtils.join(ids, ", ");
  }

  @Override
  public String groupByAttributes(QueryData queryData,
    List<QueryNode> alternative)
  {
    
    if(distinctNeeded(alternative))
    {
      List<String> ids = new ArrayList<String>();
      for (int i = 1; i <= queryData.getMaxWidth(); ++i)
      {
        ids.add("corpus" + i);
        ids.add("id" + i);
      }
      return StringUtils.join(ids, ", ");
    }
    else
    {
      return "";
    }
  }
  
  private boolean distinctNeeded(List<QueryNode> alternative)
  {
    boolean result = false || !optimizeDistinct;
    
    for (QueryNode node : alternative)
    {
      if(tables(node).usesRankTable())
      {
        result = true;
        break;
      }
    }
    return result;
  }

  @Override
  public List<Match> extractData(ResultSet rs) throws SQLException,
    DataAccessException
  {
    List<Match> matches = new ArrayList<Match>();
    int rowNum = 0;
    while (rs.next())
    {
      matches.add(mapRow(rs, ++rowNum));
    }
    return matches;
  }

  public Match mapRow(ResultSet rs, int rowNum) throws SQLException
  {
    Match match = new Match();

    // get size of solution
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();

    // the order of columns is not determined and I have to combined two
    // values, so save them here and combine later
    String node_name = null;
    List<String> corpus_path = null;

    //get path
    if(verboseOutput)
    {
      for (int column = 1; column <= columnCount; ++column)
      {
        if (corpusPathExtractor != null && metaData.getColumnName(column).startsWith("path_name"))
        {
          corpus_path = corpusPathExtractor.extractCorpusPath(rs,
            metaData.getColumnName(column));
        }
      }
    }

    // one match per column
    for (int column = 1; column <= columnCount; ++column)
    {

      if (metaData.getColumnName(column).startsWith("node_name"))
      {
        node_name = rs.getString(column);
      }
      else // no more matches in this row if an id was NULL
      if (rs.wasNull())
      {
        break;
      }

      if (verboseOutput && node_name != null)
      {
        match.setSaltId(buildSaltId(corpus_path, node_name));
        node_name = null;
      }
    }


    return match;
  }

  public boolean isOptimizeDistinct()
  {
    return optimizeDistinct;
  }

  public void setOptimizeDistinct(boolean optimizeDistinct)
  {
    this.optimizeDistinct = optimizeDistinct;
  }

  private String buildSaltId(List<String> path, String node_name)
  {
    StringBuilder sb = new StringBuilder("salt:/");

    for (String dir : path)
    {
      try
      {
        sb.append(URLEncoder.encode(dir, "UTF-8")).append("/");
      }
      catch (UnsupportedEncodingException ex)
      {
        log.error(null, ex);
        // fallback, cross fingers there are no invalid characters
        sb.append(dir).append("/");
      }
    }


    return sb.append("#").append(node_name).toString();
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

  public boolean isVerboseOutput()
  {
    return verboseOutput;
  }

  public void setVerboseOutput(boolean verboseOutput)
  {
    this.verboseOutput = verboseOutput;
  }

}
