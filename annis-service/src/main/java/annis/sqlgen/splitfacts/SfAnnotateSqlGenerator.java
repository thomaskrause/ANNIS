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
package annis.sqlgen.splitfacts;


import annis.model.QueryNode;
import annis.ql.parser.QueryData;
import annis.sqlgen.AnnotateSqlGenerator;
import annis.sqlgen.LimitOffsetQueryData;
import annis.sqlgen.TableAccessStrategy;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import static annis.sqlgen.TableAccessStrategy.COMPONENT_TYPE_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import static annis.sqlgen.TableAccessStrategy.RANK_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.EDGE_ANNOTATION_TABLE;
import static annis.sqlgen.SqlConstraints.sqlString;

/**
 *
 * @author thomas
 */
public class SfAnnotateSqlGenerator<T> extends AnnotateSqlGenerator<T>
{
  @Override
  public String getDocumentQuery(String toplevelCorpusName, String documentName)
  {
    String template = "SELECT DISTINCT \n"
      + "\tARRAY[-1::bigint] AS key, ARRAY[''::varchar] AS key_names, 0 as matchstart, \n"
      + "\t" + StringUtils.join(getSelectFields(), ",\n\t") + ", \t"
      + "\tc.path_name as path, c.path_name[1] as document_name\n"
      + "FROM\n"
      + "\tfacts_node\n"
      + "\tLEFT JOIN facts_edge ON "
      + "(facts_edge.toplevel_corpus = facts_node.toplevel_corpus AND facts_edge.node_ref = facts_node.id AND facts_edge.corpus_ref = facts_node.corpus_ref)\n"
      + "\tLEFT JOIN component_type ON (facts_edge.toplevel_corpus = component_type.toplevel_corpus AND facts_edge.type_ref = component_type.id),\n "
      + "\tcorpus as c, corpus as toplevel\n" 
      + "WHERE\n"
      + "\ttoplevel.name = :toplevel_name AND c.name = :document_name AND "
      + "facts_node.corpus_ref = c.id AND\n"
      + "\tc.pre >= toplevel.pre AND c.post <= toplevel.post\n"
      + "ORDER BY facts_node.corpus_ref, facts_edge.component_id, facts_edge.pre";
    String sql = template.replace(":toplevel_name", sqlString(toplevelCorpusName))
      .replace(":document_name", sqlString(documentName));
    return sql;
  }

  @Override
  public String selectClause(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    String innerIndent = indent + TABSTOP;
    StringBuilder sb = new StringBuilder();
    
    sb.append("DISTINCT\n");
    sb.append(innerIndent).append("solutions.\"key\",\n");
    sb.append(innerIndent);
    
    int matchStart = 0;
    List<LimitOffsetQueryData> extension =
      queryData.getExtensions(LimitOffsetQueryData.class);
    if(extension.size() > 0)
    {
      matchStart = extension.get(0).getOffset();
    }
    sb.append(innerIndent).append(matchStart).append(" AS \"matchstart\",\n");
    sb.append(indent).append(TABSTOP + "solutions.n,\n");


    sb.append(innerIndent).append(StringUtils.join(getSelectFields(), ",\n" + indent + TABSTOP));
    sb.append(",\n");


    // corpus.path_name
    sb.append(innerIndent).append("corpus.path_name AS path");

    if (isIncludeDocumentNameInAnnotateQuery())
    {
      sb.append(",\n");
      sb.append(innerIndent).append("corpus.path_name[1] AS document_name");
    }
    return sb.toString();
  }
  
  private List<String> getSelectFields()
  {
    List<String> fields = new ArrayList<String>();
    // facts.fid is never evaluated in the result set
    // addSelectClauseAttribute(fields, FACTS_TABLE, "fid");
    addSelectClauseAttribute(fields, NODE_TABLE, "id");
    addSelectClauseAttribute(fields, NODE_TABLE, "text_ref");
    addSelectClauseAttribute(fields, NODE_TABLE, "corpus_ref");
    addSelectClauseAttribute(fields, NODE_TABLE, "toplevel_corpus");
    addSelectClauseAttribute(fields, NODE_TABLE, "namespace");
    addSelectClauseAttribute(fields, NODE_TABLE, "name");
    addSelectClauseAttribute(fields, NODE_TABLE, "left");
    addSelectClauseAttribute(fields, NODE_TABLE, "right");
    addSelectClauseAttribute(fields, NODE_TABLE, "token_index");
    if (isIncludeIsTokenColumn())
    {
      addSelectClauseAttribute(fields, NODE_TABLE, "is_token");
    }
    addSelectClauseAttribute(fields, NODE_TABLE, "continuous");
    addSelectClauseAttribute(fields, NODE_TABLE, "span");
    addSelectClauseAttribute(fields, NODE_TABLE, "left_token");
    addSelectClauseAttribute(fields, NODE_TABLE, "right_token");
    addSelectClauseAttribute(fields, NODE_TABLE, "seg_name");
    addSelectClauseAttribute(fields, NODE_TABLE, "seg_index");
    addSelectClauseAttribute(fields, RANK_TABLE, "component_id");
    addSelectClauseAttribute(fields, RANK_TABLE, "pre");
    addSelectClauseAttribute(fields, RANK_TABLE, "post");
    addSelectClauseAttribute(fields, RANK_TABLE, "parent");
    addSelectClauseAttribute(fields, RANK_TABLE, "root");
    addSelectClauseAttribute(fields, RANK_TABLE, "level");
    addSelectClauseAttribute(fields, COMPONENT_TYPE_TABLE, "type");
    addSelectClauseAttribute(fields, COMPONENT_TYPE_TABLE, "name");
    addSelectClauseAttribute(fields, COMPONENT_TYPE_TABLE, "namespace");
    addSelectClauseAttribute(fields, NODE_ANNOTATION_TABLE, "val_ns");
    addSelectClauseAttribute(fields, EDGE_ANNOTATION_TABLE, "val_ns");
    
    return fields;
  }

  @Override
  public String fromClause(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(indent).append("solutions,\n");
   
    sb
      .append(indent).append(TABSTOP)
      .append("facts_node LEFT JOIN facts_edge ON (facts_node.id = facts_edge.node_ref AND facts_node.corpus_ref = facts_edge.corpus_ref) ")
      .append("LEFT JOIN component_type ON (facts_edge.type_ref = component_type.id AND facts_edge.toplevel_corpus = component_type.toplevel_corpus)");
    
    sb.append(",\n");
    sb.append(indent).append(TABSTOP).append(TableAccessStrategy.CORPUS_TABLE);

    return sb.toString();
  }

  @Override
  public String orderByClause(QueryData queryData,
    List<QueryNode> alternative, String indent)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("solutions.n, ");
    sb.append(tables(null).aliasedColumn(NODE_TABLE, "corpus_ref")).append(", ");
    sb.append(tables(null).aliasedColumn(RANK_TABLE, "component_id")).append(", ");
    String preColumn = tables(null).aliasedColumn(RANK_TABLE, "pre");
    sb.append(preColumn);
    String orderByClause = sb.toString();
    return orderByClause;
  }
  
  

  
}