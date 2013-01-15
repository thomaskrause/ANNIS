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
  public void populateSchema()
  {
    super.populateSchema();

    log.info(
      "creating immutable functions for getting annotations from the annotation pool");
    executeSqlFromScript(getDbLayout() + "/functions_get.sql");
  }

  @Override
  void analyzeFacts(long corpusID)
  {
    log.info("analyzing node table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE node_" + corpusID);

    log.info("analyzing node_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE node_annotation_" + corpusID);
    
    log.info("analyzing edge_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE edge_annotation_" + corpusID);
    
    log.info("analyzing rank table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE rank_" + corpusID);

    log.info("analyzing component table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE component_" + corpusID);
    
    // general parent tables
    log.info("analyzing general node table");
    getJdbcTemplate().execute("ANALYZE node");
    
    log.info("analyzing general node_annotation table");
    getJdbcTemplate().execute("ANALYZE node_annotation");
    
    log.info("analyzing general edge_annotation table");
    getJdbcTemplate().execute("ANALYZE edge_annotation");
    
    log.info("analyzing general rank table");
    getJdbcTemplate().execute("ANALYZE rank");

    log.info("analyzing general component table");
    getJdbcTemplate().execute("ANALYZE component");
  }
}
