/*
 * Copyright 2014 SFB 632.
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
import annis.service.objects.Match;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import java.net.URI;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SolutionKey} that uses the Salt URI scheme as identifier.
 * It uses the {@link Match} class as the type of the key.
 * 
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class SaltURISolutionKey
  implements SolutionKey<Match, URI>
{
  private final static Logger log = LoggerFactory.getLogger(SaltURISolutionKey.class);
  
  private Match lastKey;
  private Match currentKey = new Match();

  @Override
  public List<String> generateInnerQueryColumns(
    TableAccessStrategy tableAccessStrategy, int index)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<String> generateOuterQueryColumns(
    TableAccessStrategy tableAccessStrategy, int size)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Match retrieveKey(ResultSet resultSet)
  {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isNewKey()
  {
    return currentKey == null || !currentKey.equals(lastKey);
  }

  @Override
  public Integer getMatchedNodeIndex(Object id)
  {
    int index = currentKey.getSaltIDs().indexOf(id);
    return index == -1 ? null : index + 1;
  }
  
  @Override
  public URI getNodeId(ResultSet resultSet,
    TableAccessStrategy tas)
  {
    try
    {
      Array pathArray = 
        resultSet.getArray(tas.columnName(TableAccessStrategy.CORPUS_TABLE, "path_name"));
      String[] path = (String[]) pathArray.getArray();
      String name = resultSet.getString(tas.columnName(NODE_TABLE, "name"));
      String nameAppendix = 
        resultSet.getString(tas.columnName(NODE_TABLE, "unique_name_appendix"));
      
      // TODO: add annotation
      return CommonHelper.buildSaltId(Arrays.asList(path), 
        name + nameAppendix, null, null);
    }
    catch (SQLException e)
    {
      log.error("Exception thrown while retrieving node ID", e);
      throw new IllegalStateException(
        "Could not retrieve node ID from JDBC results set", e);
    }
  }
  
  

  @Override
  public int getKeySize()
  {
    return currentKey.getSaltIDs().size();
  }

  @Override
  public String getCurrentKeyAsString()
  {
    return currentKey.toString();
  }

}
