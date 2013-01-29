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
import annis.sqlgen.DefaultWhereClauseGenerator;
import annis.sqlgen.WhereClauseSqlGenerator;
import java.util.List;
import static annis.sqlgen.SqlConstraints.join;

/**
 * {@link WhereClauseSqlGenerator} for the splitfacts scheme.
 * @author Thomas Krause <thomas.krause@alumni.hu-berlin.de>
 */
import annis.sqlgen.TableAccessStrategy;
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
      "getComponentTypeNameOnly('" + edgeType + "', " 
        + (componentName == null ? "NULL" : componentName) + ", "
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
  
  
  
}
