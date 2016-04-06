/*
 * Copyright 2016 SFB 632.
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

import com.google.common.base.Preconditions;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.core.GraphTraverseHandler;
import org.corpus_tools.salt.core.SGraph;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SProcessingAnnotation;
import org.corpus_tools.salt.core.SRelation;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * A DAO to insert and delete documents to/from existing corpora in the database.
 * 
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class DocumentManagementDao extends AbstractAdminstrationDao
{
  /**
   * Deletes a document
   * @param toplevelCorpus
   * @param documentName 
   */
  @Transactional
  public void deleteDocument(String toplevelCorpus, String documentName)
  {
    Preconditions.checkNotNull(toplevelCorpus);
    Preconditions.checkNotNull(documentName);
    
    // get the current corpus graph from the database
    CorpusGraphMapper mapper = new CorpusGraphMapper(toplevelCorpus);
    SCorpusGraph corpusGraph = getJdbcTemplate().query(mapper.getSql(), mapper, mapper);
       
    SDocument docToRemove = null;
    for(SDocument doc : corpusGraph.getDocuments())
    {
      if(documentName.equals(doc.getName()))
      {
        docToRemove = doc;
        break;
      }
    }
    
    
    if(docToRemove != null)
    {
      SCorpus rootCorpus = (SCorpus) corpusGraph.getRoots().get(0);
      long topID = rootCorpus.getProcessingAnnotation("annis::origID").getValue_SNUMERIC();
   
      // remove the document from our internal representation
      corpusGraph.removeNode(docToRemove);
      
      // delete from corpus table and all connected rows in other tables
      SProcessingAnnotation origIDAnno = docToRemove.getProcessingAnnotation("annis::origID");
      if(origIDAnno != null)
      {
        long origID = origIDAnno.getValue_SNUMERIC();
        getJdbcTemplate().update("DELETE FROM facts WHERE corpus_ref=? AND toplevel_corpus=?", origID, topID);
        int changedRows = getJdbcTemplate().update("DELETE FROM corpus WHERE id=?", origID);
        Preconditions.checkState(changedRows > 0);
      }
      // write updated corpus graph in corpus table
      updatePrePost(corpusGraph);
      
      // update statistics
      updateCorpusStats(corpusGraph);
    }
  }
  
  
    /**
   * Appends a Salt document to an existing corpus.
   * @param toplevelCorpus The name of the toplevel corpus that this document should be appended to.
   * @param doc The document
   */
  @Transactional
  public void insertDocument(String toplevelCorpus, SDocument doc)
  {
    final ANNISFormatVersion version = ANNISFormatVersion.V3_3;
    
    // TODO: implement
    
//    createStagingAreaV33(temporaryStagingArea);
//    bulkImport(path, version);
//
//    String toplevelCorpusName = getTopLevelCorpusFromTmpArea();
//
//    applyConstraints();
//    createStagingAreaIndexes(version);
//
//    fixResolverVisMapTable(toplevelCorpusName, tableInStagingArea(
//      FILE_RESOLVER_VIS_MAP));
//    analyzeStagingTables();
//
//    addDocumentNameMetaData();
//
//    AdministrationDao.Offsets offsets = calculateOffsets();
//    long corpusID = getQueryDao().mapCorpusNameToId(toplevelCorpusName);
//    createNodeIdMapping();
//
//    importBinaryData(path, toplevelCorpusName);
//
//    extendStagingText(corpusID);
//    extendStagingExampleQueries(corpusID);
//
//    analyzeAutoGeneratedQueries(corpusID);
//
//    computeCorpusStatistics(path);
//
//    analyzeStagingTables();
//
//    insertCorpus(corpusID, offsets);
//
//    // TODO: re-calculate the pre/post order of the documents
//    computeCorpusPath(corpusID);
//
//    createAnnotations(corpusID);
//
//    createAnnoCategory(corpusID);
//
//    // create the new facts table partition
//    createFacts(corpusID, version, offsets);
//    
//    if(hackDistinctLeftRightToken)
//    {
//      adjustDistinctLeftRightToken(corpusID);
//    }
//    
//    if (temporaryStagingArea)
//    {
//      dropStagingArea();
//    }
//
//    // create empty corpus properties file
//    if (getQueryDao().getCorpusConfigurationSave(toplevelCorpusName) == null)
//    {
//      log.info("creating new corpus.properties file");
//      getQueryDao().setCorpusConfiguration(toplevelCorpusName, new Properties());
//    }
//
//    analyzeFacts(corpusID);
//    analyzeTextTable(toplevelCorpusName);
//    generateExampleQueries(corpusID);
//
//    if (aliasName != null && !aliasName.isEmpty())
//    {
//      addCorpusAlias(corpusID, aliasName);
//    }
//    return true; 
  }
  
  private void updatePrePost(SCorpusGraph corpusGraph)
  { 
    // re-calcuate the pre and post order and append it to the corpus nodes as 
    // processing annotation
    int maxPrePost = getJdbcTemplate().queryForObject("SELECT max(post) FROM corpus", Integer.class);
    corpusGraph.traverse(corpusGraph.getRoots(), 
      SGraph.GRAPH_TRAVERSE_TYPE.TOP_DOWN_DEPTH_FIRST, "calculatePrePost", 
      new PrePostCalculator(maxPrePost+1));
    
    // update the corpus table for each element of the corpusGraph
    for(SNode n : corpusGraph.getNodes())
    {
      SProcessingAnnotation origID = n.getProcessingAnnotation("annis::origID");
      if(origID != null)
      {
        SProcessingAnnotation pre = n.getProcessingAnnotation("annis::pre");
        if(pre != null)
        {
          getJdbcTemplate().update("UPDATE corpus SET pre=? WHERE id=?", 
            pre.getValue_SNUMERIC(), origID.getValue_SNUMERIC());
        }
        SProcessingAnnotation post = n.getProcessingAnnotation("annis::post");
        if(post != null)
        {
          getJdbcTemplate().update("UPDATE corpus SET post=? WHERE id=?", 
            post.getValue_SNUMERIC(), origID.getValue_SNUMERIC());
        }
      }
    } 
  }
  
  private void updateCorpusStats(SCorpusGraph corpusGraph)
  {
    SCorpus root = (SCorpus) corpusGraph.getRoots().get(0);
    SProcessingAnnotation topID = root.getProcessingAnnotation("annis::origID");
    if(topID != null)
    {
      String sql = 
        "WITH corpusselection AS (\n" +
        "  SELECT sub.* FROM corpus AS sub, corpus AS top \n" +
        "  WHERE top.id=? AND top.top_level IS TRUE AND sub.pre >= top.pre AND sub.post <= top.post\n" +
        ")\n" +
        "UPDATE corpus_stats SET \n" +
        "  text =  (SELECT count(*) FROM text WHERE toplevel_corpus = ?),\n" +
        "  tokens = (SELECT count(distinct id) FROM facts WHERE toplevel_corpus = ? AND is_token IS TRUE),\n" +
        "  max_corpus_id = (SELECT max(id) FROM corpusselection),\n" +
        "  max_corpus_pre = (SELECT max(pre) FROM corpusselection),\n" +
        "  max_corpus_post = (SELECT max(post) FROM corpusselection) ,\n" +
        "  max_node_id = (SELECT max(id) FROM facts WHERE toplevel_corpus = ?)\n" +
        "WHERE id = ?";
      getJdbcTemplate().update(sql, topID.getValue_SNUMERIC(), topID.getValue_SNUMERIC(), 
        topID.getValue_SNUMERIC(), topID.getValue_SNUMERIC(), topID.getValue_SNUMERIC());
    }
  }
  
  private static class PrePostCalculator implements GraphTraverseHandler
  {
    
    private int prePost = 0;
    
    public PrePostCalculator(int orderOffset)
    {
      this.prePost = orderOffset;
    }

    @Override
    public void nodeReached(SGraph.GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation<SNode, SNode> relation, SNode fromNode, long order)
    {
      currNode.createProcessingAnnotation("annis", "pre", prePost++);
    }

    @Override
    public void nodeLeft(SGraph.GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation<SNode, SNode> relation, SNode fromNode, long order)
    {
      currNode.createProcessingAnnotation("annis", "post", prePost++);
    }

    @Override
    public boolean checkConstraint(SGraph.GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SRelation<SNode, SNode> relation, SNode currNode, long order)
    {
      return true;
    }
    
  }
  
  private static class CorpusGraphMapper implements ResultSetExtractor<SCorpusGraph>,
    SqlProvider, PreparedStatementSetter
  {
    
    private final String toplevelCorpusName;

    public CorpusGraphMapper(String toplevelCorpus)
    {
      this.toplevelCorpusName = toplevelCorpus;
    }

    
    
    @Override
    public SCorpusGraph extractData(ResultSet rs) throws SQLException, DataAccessException
    {
      SCorpusGraph cg = SaltFactory.createSCorpusGraph();
      
      Preconditions.checkState(rs.next());
      
      // the first row must always contain the toplevel corpus entry
      Boolean toplevel = rs.getBoolean("top_level");
      Preconditions.checkState(Objects.equals(toplevel, Boolean.TRUE));
      Preconditions.checkState(Objects.equals(rs.getString("name"), toplevelCorpusName));

      SCorpus topCorpus = cg.createCorpus(null, toplevelCorpusName);
      int topID = rs.getInt("id");
      topCorpus.createProcessingAnnotation("annis", "origID", topID);
      Map<Integer, SCorpus> id2corpus = new HashMap<>();
      id2corpus.put(topID, topCorpus);
      
      while(rs.next())
      {
        // there should be only one top-level corpus
        Preconditions.checkState(Objects.equals(rs.getBoolean("top_level"), Boolean.FALSE));
        
        int id = rs.getInt("id");
        
        Integer[] ancestors = (Integer[]) rs.getArray("a").getArray();
        SCorpus parent = id2corpus.get(ancestors[1]);
        Preconditions.checkState(parent != null);
        if("DOCUMENT".equals(rs.getString("type")))
        {
          SDocument doc = parent.getGraph().createDocument(parent, rs.getString("name"));
          doc.createProcessingAnnotation("annis", "origID", id);
        }
        else
        {
          // subcorpus
          SCorpus subcorpus = parent.getGraph().createCorpus(parent, rs.getString("name"));
          subcorpus.createProcessingAnnotation("annis", "origID", id);
          id2corpus.put(id, subcorpus);
        }
      }
      
      return cg;
    }
    
   
    

    @Override
    public String getSql()
    {
      return "WITH subcorpora AS\n" +
        "(\n" +
        "SELECT sub.*\n" +
        "FROM corpus AS top, corpus AS sub\n" +
        "WHERE\n" +
        "  top.name=? AND top.top_level IS TRUE\n" +
        "  AND sub.pre >= top.pre AND sub.post <= top.post\n" +
        "),\n" +
        "id_with_ancestors AS (\n" +
        "SELECT array_agg(ancestors.id order by ancestors.pre DESC) AS a, o.id FROM subcorpora AS o, subcorpora AS ancestors\n" +
        "WHERE ancestors.pre <= o.pre AND ancestors.post >= o.post\n" +
        "GROUP BY o.id\n" +
        ")\n" +
        "SELECT c.id AS id, c.\"name\" AS \"name\", anc.a AS a, c.top_level AS top_level, c.\"type\" AS \"type\"\n" +
        "FROM id_with_ancestors AS anc, corpus AS c\n" +
        "WHERE c.id=anc.id\n" +
        "ORDER BY c.pre";
    }

    @Override
    public void setValues(PreparedStatement ps) throws SQLException
    {
      ps.setString(1, toplevelCorpusName);
    }

  }
}
