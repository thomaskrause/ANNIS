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

import annis.model.QueryAnnotation;
import annis.model.QueryNode;
import annis.model.QueryNode.TextMatching;
import annis.ql.parser.QueryData;
import annis.sqlgen.AnnotationConditionProvider;
import annis.sqlgen.TableAccessStrategy;
import java.util.List;
import static annis.sqlgen.SqlConstraints.sqlString;
import static annis.sqlgen.SqlConstraints.join;

/**
 *
 * @author thomas
 */
public class SfAnnotationConditionProvider implements
  AnnotationConditionProvider
{

  @Override
  public void addAnnotationConditions(List<String> conditions, QueryNode node,
    int index, QueryAnnotation annotation, String table, QueryData queryData, TableAccessStrategy tas)
  {
    if (annotation.getNamespace() != null)
    {
      if(annotation.getValue() == null)
      {
        conditions.add(join("LIKE",
          tas.aliasedColumn(table, "val_ns", index),
          sqlString(annotation.getNamespace() + ":" + annotation.getName() + ":%")));
      }
      else
      {
        
        TextMatching textMatching = annotation.getTextMatching();
        conditions.add(join(textMatching.sqlOperator(), tas.aliasedColumn(table,
          "val_ns", index),
          sqlString(annotation.getNamespace() 
          + ":" + annotation.getName()
          + ":" + annotation.getValue(), textMatching)));
      }
    }
    else
    {
      if(annotation.getValue() == null)
      {
        conditions.add(join("LIKE",
          tas.aliasedColumn(table, "val", index),
          sqlString(annotation.getName() + ":%")));
      }
      else
      {
        TextMatching textMatching = annotation.getTextMatching();
      conditions.add(join(textMatching.sqlOperator(), tas.aliasedColumn(table,
        "val", index),
        sqlString(annotation.getName() 
            + ":" + annotation.getValue(), textMatching)));       
       }
    }
  }
  
}
