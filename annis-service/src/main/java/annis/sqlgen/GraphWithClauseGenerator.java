/*
 * Copyright 2012 SFB 632.
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
package annis.sqlgen;

import annis.CommonHelper;
import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import annis.service.objects.Match;
import annis.service.objects.MatchGroup;
import static annis.sqlgen.AbstractSqlGenerator.TABSTOP;
import static annis.sqlgen.SqlConstraints.sqlString;
import static annis.sqlgen.TableAccessStrategy.CORPUS_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import annis.sqlgen.extensions.AnnotateQueryData;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Generates a WITH clause sql statement for a list of salt ids.
 *
 * Salt ids are simple URI and are defined like this:
 *
 * <p>{@code salt:/corp1/corp2/doc1#node}</p>.
 *
 * The leading / of the URI is a must, // would cause an error, because
 * authorities are currently not supported.
 *
 * @author Benjamin Weißenfels <b.pixeldrama@gmail.com>
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class GraphWithClauseGenerator extends TableAccessStrategyFactory
  implements WithClauseSqlGenerator<QueryData>
{
  private IslandsPolicy islandsPolicy;
  
  /**
   * Create a solution key to be used inside a single call to
   * {@code extractData}.
   *
   * This method must be overridden in child classes or by Spring.
   * @return 
   */
  protected SolutionKey<?> createSolutionKey()
  {
    throw new UnsupportedOperationException(
      "BUG: This method needs to be overwritten by ancestors or through Spring");
  }

  @Override
  public List<String> withClauses(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    List<Long> corpusList = queryData.getCorpusList();
    HashMap<Long, Properties> corpusProperties =
      queryData.getCorpusConfiguration();
    IslandsPolicy.IslandPolicies policy = getIslandsPolicy().getMostRestrictivePolicy(corpusList,
      corpusProperties);

    List<String> result = new LinkedList<>();

    List<AnnotateQueryData> ext = queryData.getExtensions(
      AnnotateQueryData.class);
    if (!ext.isEmpty())
    {
      AnnotateQueryData annoQueryData = ext.get(0);

      if (annoQueryData.getSegmentationLayer() == null)
      {
        // token index based method

        // first get the raw matches
        result.addAll(getMatchesWithClause(queryData, alternative, indent));
        result.add(getKeyWithClause(indent));

        // break the columns down in a way that every matched node has it's own
        // row
        result.add(getSolutionFromMatchesWithClause(policy, "matches", indent));

      }
      else
      {
        // segmentation layer based method

        result.addAll(getMatchesWithClause(queryData, alternative, indent));
        result.add(getKeyWithClause(indent));
        result.add(getNearestSeqWithClause(queryData, annoQueryData, alternative,
          "matches", indent));
        result.add(getSolutionFromNearestSegWithClause(queryData, annoQueryData,
          alternative, policy, "nearestseg", indent));

      }
    }

    return result;
  }
  
  protected String getKeyWithClause(String indent)
  {
    String indent2 = indent + TABSTOP;
    
    return indent + "keys AS (\n" +
     indent2 + "SELECT n, array_agg(id ORDER BY nodenr ASC) AS \"key\" FROM matches\n" +
     indent2 + "GROUP BY n\n" +
     indent + ")";
  }

  /**
   * Breaks down the matches table, so that each node of each match has it's own
   * row.
   * @param islandPolicy
   * @param matchesName
   * @param indent
   * @return 
   */
  protected String getSolutionFromMatchesWithClause(
    IslandsPolicy.IslandPolicies islandPolicy,
    String matchesName,
    String indent)
  {
    String indent2 = indent + TABSTOP;
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(indent).append("solutions AS\n");
    sb.append(indent).append("(\n");
    
    sb.append(indent2).append("SELECT ");
   
    
    if(islandPolicy == IslandsPolicy.IslandPolicies.none)
    {
      sb.append("min(keys.key) AS key, ")
        .append(matchesName).append(".n AS n, ")
        .append(matchesName).append(".text, ")
        .append(matchesName).append(".corpus, ");
      sb.append("min(").append(matchesName).append(".min").append("), ");
      sb.append("max(").append(matchesName).append(".max").append(")");
    }
    else if(islandPolicy == IslandsPolicy.IslandPolicies.context)
    {
      sb.append("keys.key AS key, ")
        .append(matchesName).append(".n AS n, ")
        .append(matchesName).append(".text, ")
        .append(matchesName).append(".corpus, ");
      sb.append(matchesName).append(".min, ");
      sb.append(matchesName).append(".max");
    }
    else
    {
      throw new UnsupportedOperationException(
        "No implementation for island policy "
        + islandsPolicy.toString());
    }
    
    sb.append("\n").append(indent2);
    sb.append("FROM ").append(matchesName).append(", keys\n").append(indent2);
    sb.append("WHERE keys.n = ").append(matchesName).append(".n\n");
    if(islandPolicy == IslandsPolicy.IslandPolicies.none)
    {
      sb.append(indent2)
        .append("GROUP BY ")
        .append(matchesName).append(".n, ")
        .append(matchesName).append(".text, ")
        .append(matchesName).append(".corpus").append("\n");
    }
    
    sb.append(indent).append(")");
    
    return sb.toString();

  }

  
  protected String getNearestSeqWithClause(
    QueryData queryData, AnnotateQueryData annoQueryData,
    List<QueryNode> alternative, String matchesName, String indent)
  {
    String indent2 = indent + TABSTOP;
    String indent3 = indent2 + TABSTOP;
    
    // use copy constructor in order not to mess up the global TableAccessStrategy bean
    TableAccessStrategy tas = new TableAccessStrategy(createTableAccessStrategy());
    tas.addTableAlias("solutions", matchesName);
   
    StringBuilder sb = new StringBuilder();
    sb.append(indent).append("nearestseg AS\n");
    sb.append(indent).append("(\n");

    
    sb.append(indent2).append("SELECT\n");

    sb.append(indent3);
    sb.append("matches.nodeNr AS nodeNr");
    sb.append(", matches.n AS n");
    sb.append(", matches.id AS id,\n");
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "seg_index")).append(" - ").append(annoQueryData.
      getLeft()).append(" AS \"min\",\n");
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE,
      "seg_index")).append(" + ").append(annoQueryData.getRight()).append(
      " AS \"max\",\n");
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(
      " AS \"text\", \n");
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(
      " AS \"corpus\", \n");

    String distLeft = "min"
      + " - " 
      + tas.aliasedColumn(NODE_TABLE, "left_token");
    String distRight = tas.aliasedColumn(NODE_TABLE, "right_token") 
      + " - max" ;

    // create ordered window partition
    // values are ordered by their distance to the min or max token index
    // NULLIF( dist+1, -abs(dist+1) will give negative entries NULL which means
    // their are put last in the ordered list
    // +1 is there to ensure that positive equal values (thus dist=0) are not ignored
    sb.append(indent3).append("row_number() OVER (PARTITION BY ")
      .append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(", ")
      .append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(", ")
      .append(matchesName).append(".n").append(", ")
      .append(matchesName).append(".nodeNr")
      .append(" ORDER BY NULLIF(")
      .append(distLeft).append("+ 1, -abs(").append(distLeft)
      .append(" + 1)) ASC) AS rank_left,\n");

    sb.append(indent3).append("row_number() OVER (PARTITION BY ")
      .append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(", ")
      .append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(", ")
      .append(matchesName).append(".n").append(", ")
      .append(matchesName).append(".nodeNr")
      .append(" ORDER BY NULLIF(")
      .append(distRight).append(" + 1, -abs(").append(distRight)
      .append(" + 1)) ASC) AS rank_right\n");


    sb.append(indent2).append("FROM ").append(tas.tableName(NODE_TABLE)).append(
      ", matches\n");
    sb.append(indent2).append("WHERE\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "toplevel_corpus")).
      append(" IN (").append(StringUtils.join(queryData.getCorpusList(), ",")).
      append(") AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "n_sample")).append(
      " IS TRUE AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "seg_name"))
      .append(" = ").append(sqlString(annoQueryData.getSegmentationLayer()))
      .append(" AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "text_ref"))
      .append(" = matches.text").append(" AND\n");

     sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "corpus_ref"))
      .append(" = matches.corpus").append("\n");

    sb.append(indent).append(")");

    return sb.toString();
  }

  /**
   * Get solution from a covered segmentation using a WITH clause.
   *
   * @param queryData
   * @param annoQueryData
   * @param alternative
   * @param islandsPolicy
   * @param coveredName
   * @param indent
   * @return
   *
   */
  protected String getSolutionFromNearestSegWithClause(
    QueryData queryData, AnnotateQueryData annoQueryData,
    List<QueryNode> alternative, IslandsPolicy.IslandPolicies islandsPolicy,
    String coveredName, String indent)
  {

    TableAccessStrategy tas = createTableAccessStrategy();

    String indent2 = indent + TABSTOP;
    String indent3 = indent2 + TABSTOP;

    List<Long> corpusList = queryData.getCorpusList();

    StringBuilder sb = new StringBuilder();

    sb.append(indent).append("solutions AS\n");
    sb.append(indent).append("(\n");

    sb.append(indent2).append("SELECT DISTINCT ");


    sb.append("min(").append("keys.key) AS key, ")
      .append(coveredName).append(".n AS n, ")
      .append("min(").append(tas.aliasedColumn(NODE_TABLE, "left_token")).append(") AS \"min\", ")
      .append("max(").append(tas.aliasedColumn(NODE_TABLE, "right_token")).append(") AS \"max\", ")
      .append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(" AS \"text\", ")
      .append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(" AS \"corpus\"\n");


    sb.append(indent2).append("FROM ")
      .append(coveredName).append(", ")
      .append(tas.tableName(NODE_TABLE)).append(", ")
      .append("keys")
      .append("\n");

    sb.append(indent2).append("WHERE\n");
    
    sb.append(indent3).append("keys.n = ").append(coveredName).append(".n AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "toplevel_corpus")).
      append(" IN (").append(StringUtils.join(corpusList, ",")).append(") AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "n_sample")).append(
      " IS TRUE AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "seg_name")).append(
      " = ")
      .append(sqlString(annoQueryData.getSegmentationLayer())).append(" AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(
      " = ").append(coveredName).append(".\"text\" AND\n");
    
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(
      " = ").append(coveredName).append(".\"corpus\" AND\n");

    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "seg_index")).append(
      " <= ").append(coveredName).append(".\"max\" AND\n");
    sb.append(indent3).append(tas.aliasedColumn(NODE_TABLE, "seg_index")).append(
      " >= ").append(coveredName).append(".\"min\" AND\n");
    
    sb.append(indent3).append("(").append(coveredName)
      .append(".rank_left = 1 OR ").append(coveredName)
      .append(".rank_right = 1)\n");

    if (islandsPolicy == IslandsPolicy.IslandPolicies.none)
    {
      sb.append(indent2).append("GROUP BY ")
        .append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(", ")
        .append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(", ")
        .append(coveredName).append(".n\n");
    }
    else if(islandsPolicy == IslandsPolicy.IslandPolicies.context)
    {
      sb.append(indent2).append("GROUP BY ")
        .append(tas.aliasedColumn(NODE_TABLE, "corpus_ref")).append(", ")
        .append(tas.aliasedColumn(NODE_TABLE, "text_ref")).append(", ")
        .append(coveredName).append(".n, ")
        .append(coveredName).append(".nodeNr\n");
    }
    else
    {
      throw new UnsupportedOperationException(
        "No implementation for island policy "
        + islandsPolicy.toString());
    }

    sb.append(indent).append(")\n");

    return sb.toString();
  }
  
  
  public static String overlapForOneRange(String indent,
    String rangeMin, String rangeMax, String textRef, String corpus_ref, TableAccessStrategy tables)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(indent).append("(");

    sb.append(tables.aliasedColumn(NODE_TABLE, "left_token")).append(" <= ").append(rangeMax)
      .append(" AND ")
      .append(tables.aliasedColumn(NODE_TABLE, "right_token")).append(" >= ").append(rangeMin)
      .append(" AND ")
      .append(tables.aliasedColumn(NODE_TABLE, "text_ref")).append(" = ").append(textRef)
      .append(" AND ")
      .append(tables.aliasedColumn(NODE_TABLE, "corpus_ref")).append(" = ").append(corpus_ref);

    sb.append(")");

    return sb.toString();
  }
  
  /**
   * @return the islandsPolicy
   */
  public IslandsPolicy getIslandsPolicy()
  {
    return islandsPolicy;
  }

  /**
   * @param islandsPolicy the islandsPolicy to set
   */
  public void setIslandsPolicy(IslandsPolicy islandsPolicy)
  {
    this.islandsPolicy = islandsPolicy;
  }
  
  private String selectForNode(
    TableAccessStrategy tas, AnnotateQueryData annotateQueryData,
    int match,
    int nodeNr, 
    String indent)
  {
    StringBuilder sb = new StringBuilder();

    
    sb.append(match).append(" AS n, ");
    sb.append(nodeNr).append(" AS nodeNr,\n").append(indent);
    
    sb.append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".")
      .append(tas.columnName(NODE_TABLE, "id")).append(" AS ")
      .append("id, ");

    sb.append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".")
      .append(tas.columnName(NODE_TABLE, "text_ref")).append(" AS ")
      .append("text, ");

    sb.append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".")
      .append(tas.columnName(NODE_TABLE, "left_token"));

    if (annotateQueryData.getSegmentationLayer() == null)
    {
      sb.append(" - ").append(annotateQueryData.getLeft());
    }
    sb.append(" AS ").append("min, ");

    sb.append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".")
      .append(tas.columnName(NODE_TABLE, "right_token"));
    if (annotateQueryData.getSegmentationLayer() == null)
    {
      sb.append(" + ").append(annotateQueryData.getRight());
    }
    sb.append(" AS ").append("max, ");

    sb.append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".")
      .append(tas.columnName(NODE_TABLE, "corpus_ref"))
      .append(" AS ").append("corpus");

    return sb.toString();
  }
  
  private String fromForNode(
    TableAccessStrategy tas, String indent,
      int nodeNr, List<Long> corpusList)
  {
    String factsSQL = SelectedFactsFromClauseGenerator.selectedFactsSQL(corpusList, indent);
    StringBuilder sb = new StringBuilder();
    sb.append(indent)
        .append(factsSQL).append(" AS ")
        .append(tas.tableName(NODE_TABLE)).append(nodeNr).append(", ")
        .append(tas.tableName(CORPUS_TABLE)).append(" AS ")
        .append(tas.tableName(CORPUS_TABLE)).append(nodeNr);
    
    return sb.toString();
  }
  
  private String whereForNode(URI uri,
    TableAccessStrategy tas, List<Long> corpusList, String indent,
      int nodeNr)
  {
    StringBuilder sb = new StringBuilder();
    // check for corpus/document by it's path
      sb.append(indent)
        .append(tas.tableName(CORPUS_TABLE)).append(nodeNr).append(".path_name = ")
        .append(generatePathName(uri)).append(" AND\n");

      // join the found corpus/document to the facts table
      sb.append(indent)
        .append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".corpus_ref = ")
        .append(tas.tableName(CORPUS_TABLE)).append(nodeNr).append(".id AND\n");

      // filter the node with the right name
      sb.append(indent)
        .append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".salt_id = ")
        .append("'").append(generateNodeID(uri)).append("'").append(" AND\n");

      // use the toplevel partioning
      sb.append(indent)
        .append(tas.tableName(NODE_TABLE)).append(nodeNr).append(".toplevel_corpus IN ( ")
        .append(StringUtils.join(corpusList, ",")).append(") ");
      return sb.toString();
  }
  
  
  private String subselectForMatch(int match, int nodeNr, URI uri, 
    TableAccessStrategy tas, AnnotateQueryData annoQueryData, List<Long> corpusList, 
    String indent)
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(indent).append("SELECT ").append(
      selectForNode(tas, annoQueryData, match, nodeNr, indent+TABSTOP)).append("\n");
    sb.append(indent).append("FROM\n").append(fromForNode(tas, indent+TABSTOP, nodeNr, corpusList)).append("\n");
    sb.append(indent).append("WHERE\n").append(whereForNode(uri, tas, corpusList , indent+TABSTOP, nodeNr)).append("\n");
    sb.append(indent).append("LIMIT 1\n");
  
    return sb.toString();
  }

  protected List<String> getMatchesWithClause(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    TableAccessStrategy tas = createTableAccessStrategy();
    
    List<AnnotateQueryData> extensions =
      queryData.getExtensions(AnnotateQueryData.class);
    AnnotateQueryData annotateQueryData = extensions.isEmpty()
      ? new AnnotateQueryData(5, 5) : extensions.get(0);
    List<MatchGroup> listOfSaltURIs = queryData.getExtensions(MatchGroup.class);
    // only work with the first element
    Validate.isTrue(!listOfSaltURIs.isEmpty());
    
    List<String> subselects = new LinkedList<>();
    
    
    String indent2 = indent + TABSTOP;
    
    MatchGroup groupSet = listOfSaltURIs.get(0);
    int matchNr = 1;
    for(Match match : groupSet.getMatches())
    {
      List<URI> uriList = match.getSaltIDs();
      int nodeNr = 1;
      for (URI uri : uriList)
      {
        String sub
          = indent2 + "(\n"
          + subselectForMatch(matchNr, nodeNr, uri, tas, annotateQueryData,
            queryData.getCorpusList(),
            indent2)
          + indent2 + ")";

        subselects.add(0, sub);
        nodeNr++;
      }
      matchNr++;
    }
    String result =
      indent + "matches AS\n" + indent + "(\n" 
      + Joiner.on("\n" + indent2 +"UNION ALL\n").join(subselects) 
      + "\n" + indent + ")";
    
    return Lists.newArrayList(result);
  }

  private String generatePathName(URI uri)
  {
    StringBuilder sb = new StringBuilder();
    
    List<String> path = CommonHelper.getCorpusPath(uri);
    Collections.reverse(path);

    sb.append("{");
    sb.append(StringUtils.join(path, ", "));
    sb.append("}");
    
    return  sqlString(sb.toString());
  }
  
  private String generateNodeID(URI uri)
  { 
    return uri.getFragment();
  }
}
