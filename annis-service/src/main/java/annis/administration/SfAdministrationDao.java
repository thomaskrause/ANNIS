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
package annis.administration;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author thomas
 */
public class SfAdministrationDao extends DefaultAdministrationDao
{

  private static final Logger log = LoggerFactory.getLogger(AdministrationDao.class);

  @Override
  protected void adjustIDs()
  {
    adjustRankPrePost();
    adjustTextId();
    adjustNodeId();
    adjustComponentId();
  }
  
  protected void adjustComponentId()
  { 
    log.info("adjusting component id");
    executeSqlFromScript("adjustcomponentid.sql");
    log.debug("analyzing _component and _rank");
    getJdbcTemplate().execute("ANALYZE " + tableInStagingArea("component"));
    getJdbcTemplate().execute("ANALYZE " + tableInStagingArea("rank"));
  }

  protected void adjustNodeId()
  { 
    log.info("adjusting node id");
    executeSqlFromScript("adjustnodeid.sql");
    log.debug("analyzing _node, _node_annotation and _rank");
    getJdbcTemplate().execute("ANALYZE " + tableInStagingArea("node"));
    getJdbcTemplate().execute("ANALYZE " + tableInStagingArea("node_annotation"));
    getJdbcTemplate().execute("ANALYZE " + tableInStagingArea("rank"));
  }
  
  @Override
  void analyzeFacts(long corpusID)
  {
    log.info("analyzing imported tables for corpus with ID " + corpusID);
    
    log.debug("analyzing node table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE node_" + corpusID);

    log.debug("analyzing node_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE node_annotation_" + corpusID);
    
    log.debug("analyzing edge_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE edge_annotation_" + corpusID);
    
    log.debug("analyzing rank table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE rank_" + corpusID);

    log.debug("analyzing component table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE component_" + corpusID);
    
    // general parent tables
    log.debug("analyzing general node table");
    getJdbcTemplate().execute("ANALYZE node");
    
    log.debug("analyzing general node_annotation table");
    getJdbcTemplate().execute("ANALYZE node_annotation");
    
    log.debug("analyzing general edge_annotation table");
    getJdbcTemplate().execute("ANALYZE edge_annotation");
    
    log.debug("analyzing general rank table");
    getJdbcTemplate().execute("ANALYZE rank");

    log.debug("analyzing general component table");
    getJdbcTemplate().execute("ANALYZE component");
  }

  @Override
  public void deleteCorpora(List<Long> ids)
  {
    for (long l : ids)
    {
      log.debug("dropping annotations table for corpus " + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS annotations_" + l);
      
      log.debug("dropping partitioned tables for corpus " + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS edge_annotation_" + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS node_annotation_" + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS rank_" + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS component_" + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS node_" + l);
    }
    
    log.debug("recursivly deleting corpora: " + ids);
    executeSqlFromScript("delete_corpus.sql", makeArgs().addValue(":ids",
      StringUtils.join(ids, ", ")));
  }
  
  
  
}
