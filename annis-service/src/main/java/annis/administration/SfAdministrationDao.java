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

import annis.sqlgen.SqlConstraints;
import java.util.List;
import javax.sql.RowSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import static annis.sqlgen.SqlConstraints.sqlString;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
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
  @Transactional(readOnly = false)
  public void exportCorpus(long id, String outputPath)
  {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue(":id", id);
    log.info("Exporting corpus with id " + id + " to temporary tables.");
    executeSqlFromScript(getDbLayout() + "/export.sql", param);
    
    // copy the created tables to database files
    File parent = new File(outputPath);
    if(parent.isDirectory() || parent.mkdirs())
    {
      storeTableToResource("_export_corpus", new File(parent, "corpus.tab"));
      storeTableToResource("_export_corpus_annotation", new File(parent, "corpus_annotation.tab"));
    }
    
  }
  
  
  

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
      "CREATE INDEX testidx__rank_level ON _rank(component_ref, \"level\");");
    
    while(rows > 0)
    {
      level++;
      log.info("computing level {}", level);
      rows = getJdbcTemplate().update(
        "UPDATE _rank AS p SET \"level\" = ?\n" +
        "FROM _rank AS c\n" +
        "WHERE\n" +
        "  c.component_ref = p.component_ref AND\n" +
        "  c.parent = p.pre AND\n" +
        "  c.\"level\" = ?;", level, level-1);
    }
  }

  @Override
  @Transactional(readOnly=false)
  protected void createFacts(long corpusID)
  {
    super.createFacts(corpusID);
    
    // query for all component types
    List<Short> types = 
      getJdbcTemplate().queryForList("SELECT id FROM component_type_" + corpusID, Short.class);
    
    for(short t : types)
    {
      log.info("indexing the new rank table for type {} (corpus with ID {})", t, corpusID);
      MapSqlParameterSource args = makeArgs().addValue(":id", corpusID).addValue(":type", t);    
      executeSqlFromScript(getDbLayout() + "/indexes_rank.sql", args);    
    }
    
    // query for all annotation categories and their borders
    SqlRowSet annos =
      getJdbcTemplate().queryForRowSet(
      "SELECT \"type\", namespace, \"name\", min(\"value\") AS lower, max(value) AS upper \n" +
      "FROM annotations_" + corpusID + "\n" +
      "GROUP BY \"type\", namespace, \"name\"\n" +
      "ORDER BY lower");
    
    Set<String> alreadyDefinedNames = new HashSet<String>();
    
    while(annos.next())
    {    
      
      String type = annos.getString("type");
      String name = annos.getString("name");
      String namespace = annos.getString("namespace");
      String lower = annos.getString("lower");
      String upper = annos.getString("upper");

      String typeAndName = type + ":" + name;
      
      log.info("{}: indexing {}:{} annotation category ({})", 
        new String[] {typeAndName, namespace, name, type});
      
      if(!alreadyDefinedNames.contains(typeAndName))
      {
        getJdbcTemplate().execute("CREATE INDEX \"idx__anno_" + type
          + "_" + name + "__" + corpusID + "\"" + 
          " ON facts_" + type + "_" + corpusID
          + "(val) WITH(FILLFACTOR=100) WHERE val ~>=~ " + sqlString(name + ":" + lower) + " AND "
          + "val ~<=~ " + sqlString(name + ":" + upper));
        
        alreadyDefinedNames.add(typeAndName);
      }
      
      getJdbcTemplate().execute("CREATE INDEX \"idx__anno_" + type
        + "_" + namespace+ "_" + name + "__" + corpusID + "\"" + 
        " ON facts_" + type + "_" + corpusID
        + "(val) WITH(FILLFACTOR=100) WHERE val ~>=~ " + sqlString(namespace + ":" + name + ":" + lower) + " AND "
        + "val ~<=~ " + sqlString(namespace + ":" + name + ":" + upper));
    }
  }

  
  
  
  @Override
  protected void adjustIDs()
  {
    adjustNodeId();
    adjustRankPrePost();
    adjustTextId();
    addComponentType();
    
//    adjustTextId();
//    adjustNodeId();
//    if(true) return;
//    adjustComponentId();
  }
  
  protected void addComponentType()
  {
    log.info("adding component type");
    if(!executeSqlFromScript(getDbLayout() + "/rank_component_type.sql"))
    {
      executeSqlFromScript("rank_component_type.sql");
    }
  }
  
  protected void adjustComponentId()
  {     
    log.info("adjusting component id");
    executeSqlFromScript("adjustcomponentid.sql");
    log.debug("analyzing _component and _rank");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("component"));
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("rank"));
  }

  protected void adjustNodeId()
  { 
    log.info("adjusting node id");
    executeSqlFromScript("adjustnodeid.sql");
    log.debug("analyzing _node, _node_annotation and _rank");
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("node"));
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("node_annotation"));
    getJdbcTemplate().execute("VACUUM FULL ANALYZE " + tableInStagingArea("rank"));
  }
  
  @Override
  void analyzeFacts(long corpusID)
  {
    log.info("analyzing newly imported tables for corpus with ID " + corpusID);
    
    log.debug("analyzing node table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ALTER TABLE facts_node_" + corpusID + " ALTER COLUMN span SET STATISTICS 200");
    
    getJdbcTemplate().execute("ANALYZE facts_node_" + corpusID);

    log.debug("analyzing node_annotation table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE facts_edge_" + corpusID);

    log.debug("analyzing component_type table for corpus with ID " + corpusID);
    getJdbcTemplate().execute("ANALYZE component_type_" + corpusID);
    
    // general parent tables
    log.debug("analyzing general node table");
    getJdbcTemplate().execute("ANALYZE facts_edge");
    
    log.debug("analyzing general node_annotation table");
    getJdbcTemplate().execute("ANALYZE facts_node");

    log.debug("analyzing general component_type table");
    getJdbcTemplate().execute("ANALYZE component_type");
  }

  @Override
  public void deleteCorpora(List<Long> ids)
  {
    for (long l : ids)
    {
      log.debug("dropping annotations table for corpus " + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS annotations_" + l + " CASCADE");
      
      log.debug("dropping partitioned tables for corpus " + l);
      getJdbcTemplate().execute("DROP TABLE IF EXISTS facts_edge_" + l + " CASCADE");
      getJdbcTemplate().execute("DROP TABLE IF EXISTS facts_node_" + l + " CASCADE");
      getJdbcTemplate().execute("DROP TABLE IF EXISTS component_type_" + l + " CASCADE");
      getJdbcTemplate().execute("DROP TABLE IF EXISTS text_" + l + " CASCADE");
    }
    
    log.debug("recursivly deleting corpora: " + ids);
    executeSqlFromScript("delete_corpus.sql", makeArgs().addValue(":ids",
      StringUtils.join(ids, ", ")));
    
    getJdbcTemplate().execute("VACUUM FULL ANALYZE corpus");
  }

  @Override
  protected void populateSchema()
  {
    super.populateSchema();
    
    log.info(
      "creating immutable functions for getting component types");
    executeSqlFromScript(getDbLayout() + "/functions_get.sql"); 
  }
  
  
  
}
