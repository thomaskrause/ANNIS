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
import org.springframework.transaction.annotation.Transactional;


/**
 *
 * @author thomas
 */
public class SfAdministrationDao extends DefaultAdministrationDao
{

  private static final Logger log = LoggerFactory.getLogger(AdministrationDao.class);

  @Override
  void updateCorpusStatistic(long corpusID)
  {
    // do nothing
  }

//  @Override
//  void computeLevel()
//  {
//    // make sure to make the _rank table as small as possible
//    log.info("shrinking _rank table");
//    getJdbcTemplate().execute("VACUUM FULL " + tableInStagingArea("rank") +  ";");
//    super.computeLevel();
//  }
  
  

  @Override
  void computeLevel()
  {
    int level = 0;
    log.info("preparing level computation");    
    getJdbcTemplate().execute("ALTER TABLE _rank ADD COLUMN \"level\" integer;");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("component") +  ";");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("rank") +  ";");

    
    log.info("computing level {}", level);
    
    int rows = getJdbcTemplate().update(
      "UPDATE _rank SET \"level\" = 0\n" +
      "WHERE pre + 1 = post;");
    
    getJdbcTemplate().execute(
      "CREATE INDEX testidx__rank_level ON _rank(corpus_ref, component_ref, \"level\");");
    
    while(rows > 0)
    {
      level++;
      log.info("computing level {}", level);
      rows = getJdbcTemplate().update(
        "UPDATE _rank AS p SET \"level\" = ?\n" +
        "FROM _rank AS c\n" +
        "WHERE\n" +
        "  c.corpus_ref = p.corpus_ref AND\n" +
        "  c.component_ref = p.component_ref AND\n" +
        "  c.parent = p.pre AND\n" +
        "  c.\"level\" = ?;", level, level-1);
    }
  }

  
  
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
    log.info("analyzing and shrinking imported tables for corpus with ID " + corpusID);
    
    log.debug("analyzing node table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("VACUUM FULL ANALYZE node_" + corpusID);

    log.debug("analyzing node_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("VACUUM FULL ANALYZE node_annotation_" + corpusID);
    
    log.debug("analyzing edge_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("VACUUM FULL ANALYZE edge_annotation_" + corpusID);
    
    log.debug("analyzing rank table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("VACUUM FULL ANALYZE rank_" + corpusID);

    log.debug("analyzing component table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("VACUUM FULL ANALYZE component_" + corpusID);
    
    // general parent tables
    log.debug("analyzing general node table");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE node");
    
    log.debug("analyzing general node_annotation table");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE node_annotation");
    
    log.debug("analyzing general edge_annotation table");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE edge_annotation");
    
    log.debug("analyzing general rank table");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE rank");

    log.debug("analyzing general component table");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE component");
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
