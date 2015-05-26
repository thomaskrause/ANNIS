/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package annis.sqlgen;

import annis.model.AnnisConstants;
import annis.model.RelannisNodeFeature;
import static annis.model.AnnisConstants.*;
import annis.model.RelannisEdgeFeature;
import annis.service.objects.Match;
import annis.service.objects.MatchGroup;
import static annis.sqlgen.TableAccessStrategy.COMPONENT_TABLE;
import static annis.sqlgen.TableAccessStrategy.EDGE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_ANNOTATION_TABLE;
import static annis.sqlgen.TableAccessStrategy.NODE_TABLE;
import static annis.sqlgen.TableAccessStrategy.RANK_TABLE;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;

import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.SaltProject;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.exceptions.SaltException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.*;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SDATATYPE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SFeature;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SProcessingAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.emf.common.util.EList;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class SaltAnnotateExtractor implements AnnotateExtractor<SaltProject>
{
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(SaltAnnotateExtractor.class);
  private TableAccessStrategy outerQueryTableAccessStrategy;
  private CorpusPathExtractor corpusPathExtractor;
  
  
  public SaltAnnotateExtractor()
  {
  }

  @Override
  public SaltProject extractData(ResultSet resultSet)
    throws SQLException, DataAccessException
  {
    SaltProject project = SaltFactory.eINSTANCE.createSaltProject();

    try
    {
      
      SCorpusGraph corpusGraph = null;

      SDocumentGraph graph = null;

      // fn: parent information (pre and component) id to node
      FastInverseMap<Long, SNode> nodeByRankID = new FastInverseMap<>();

      TreeSet<Long> allTextIDs = new TreeSet<>();
      TreeMap<Long, String> tokenTexts = new TreeMap<>();
      TreeMap<Long, SToken> tokenByIndex = new TreeMap<>();
      TreeMap<String, TreeMap<Long, String>> nodeBySegmentationPath =
        new TreeMap<>();
      Map<String, ComponentEntry> componentForSpan = 
        new HashMap<>();

      // clear mapping functions for this graph
      // assumes that the result set is sorted by key, pre
      nodeByRankID.clear();

      SDocument document = null;
      
      AtomicInteger numberOfEdges = new AtomicInteger();
      int match_index = 0;
      
      SolutionKey<?> key = createSolutionKey();

      int counter = 0;
      while (resultSet.next())
      {
        if(counter % 1000 == 0)
        {
          log.debug("handling resultset row {}", counter);
        }
        counter++;
        //List<String> annotationGraphKey = 
        key.retrieveKey(resultSet);

        if (key.isNewKey())
        {

          // create the text for the last graph
          if (graph != null && document != null)
          {
            createMissingSpanningRelations(graph, nodeByRankID, tokenByIndex, 
              componentForSpan,
              numberOfEdges);
            createPrimaryTexts(graph, allTextIDs, tokenTexts, tokenByIndex);
            addOrderingRelations(graph, nodeBySegmentationPath);
          }

          // new match, reset everything        
          nodeByRankID.clear();
          tokenTexts.clear();
          tokenByIndex.clear();
          componentForSpan.clear();

          Integer matchstart = resultSet.getInt("matchstart");
          corpusGraph = SaltFactory.eINSTANCE.createSCorpusGraph();
          corpusGraph.setSName("match_" + (match_index + matchstart));

          project.getSCorpusGraphs().add(corpusGraph);

          graph = SaltFactory.eINSTANCE.createSDocumentGraph();
          document = SaltFactory.eINSTANCE.createSDocument();


          List<String> path = corpusPathExtractor.extractCorpusPath(resultSet,
            "path");

          SCorpus toplevelCorpus = SaltFactory.eINSTANCE.createSCorpus();
          toplevelCorpus.setSName(path.get(0));
          corpusGraph.addSNode(toplevelCorpus);

          Validate.isTrue(path.size() >= 2,
            "Corpus path must be have at least two members (toplevel and document)");
          SCorpus corpus = toplevelCorpus;

          for (int i = 1; i < path.size() - 1; i++)
          {
            SCorpus subcorpus = SaltFactory.eINSTANCE.createSCorpus();
            subcorpus.setSName(path.get(i));
            corpusGraph.addSSubCorpus(corpus, subcorpus);
            corpus = subcorpus;
          }
          document.setSName(path.get(path.size() - 1));
          document.setSId("" + match_index);
          corpusGraph.addSDocument(corpus, document);

          document.setSDocumentGraph(graph);
          match_index++;
        } // end if new key

        // get node data
        SNode node = createOrFindNewNode(resultSet, graph, allTextIDs, tokenTexts,
          tokenByIndex, nodeBySegmentationPath,
          key, nodeByRankID);
        long pre = longValue(resultSet, RANK_TABLE, "pre");
        long rankID = longValue(resultSet, RANK_TABLE, "id");
        long componentID = longValue(resultSet, COMPONENT_TABLE, "id");
        if (!resultSet.wasNull())
        {
          nodeByRankID.put(rankID, node);
          createRelation(resultSet, graph, nodeByRankID, node, numberOfEdges);
          
          if(node instanceof SSpan)
          {
            componentForSpan.put(node.getSId(), new ComponentEntry(componentID,
              'c',
              stringValue(resultSet, COMPONENT_TABLE, "namespace"),
              stringValue(resultSet, COMPONENT_TABLE, "name")
            ));
          }
        }
      } // end while new result row

      // the last match needs a primary text, too
      if (graph != null)
      {
        createMissingSpanningRelations(graph, nodeByRankID, tokenByIndex, 
          componentForSpan,
          numberOfEdges);
        createPrimaryTexts(graph, allTextIDs, tokenTexts, tokenByIndex);
        addOrderingRelations(graph, nodeBySegmentationPath);
      }
    }
    catch(Exception ex)
    {
      log.error("could not map result set to SaltProject", ex);
    }

    return project;
  }
  
  
  private void addOrderingRelations(SDocumentGraph graph,
    TreeMap<String, TreeMap<Long, String>> nodeBySegmentationPath)
  {
    AtomicInteger numberOfSOrderRels = new AtomicInteger();
    
    for(Map.Entry<String, TreeMap<Long, String>> e : nodeBySegmentationPath.entrySet())
    {
      String segName = e.getKey();
      TreeMap<Long, String> nodeBySegIndex = e.getValue();
      
      // mark the first node in the chain
      if(!nodeBySegIndex.isEmpty())
      {
        String idOfFirstNode = nodeBySegIndex.firstEntry().getValue();
        SNode firstNodeInSegChain = graph.getSNode(idOfFirstNode);
        if(firstNodeInSegChain != null)
        {
          SFeature featFistSegInChain = SaltFactory.eINSTANCE.createSFeature();
          featFistSegInChain.setSNS(ANNIS_NS);
          featFistSegInChain.setSName(FEAT_FIRST_NODE_SEGMENTATION_CHAIN);
          featFistSegInChain.setSValue(segName);
          firstNodeInSegChain.addSFeature(featFistSegInChain);
        }
      }
      
      SNode lastNode = null;
      for(String nodeID : nodeBySegIndex.values())
      {
        SNode n = graph.getSNode(nodeID);
        
        if(lastNode != null && n != null)
        {
          SOrderRelation orderRel = SaltFactory.eINSTANCE.createSOrderRelation();
          orderRel.setSSource(lastNode);
          orderRel.setSTarget(n);
          orderRel.addSType(segName);
          orderRel.setSName("sOrderRel" + numberOfSOrderRels.getAndIncrement());
          graph.addSRelation(orderRel);
        }
        lastNode = n;
      }
    }
  }
  
  /**
   * Use the left/right token index of the spans to create spanning relations
   * when this did not happen yet.
   * @param graph 
   * @param nodeByRankID 
   * @param numberOfEdges 
   */
  private void createMissingSpanningRelations(SDocumentGraph graph,
    FastInverseMap<Long, SNode> nodeByRankID, 
    TreeMap<Long, SToken> tokenByIndex,
    Map<String, ComponentEntry> componentForSpan,
    AtomicInteger numberOfEdges)
  { 
    
    // add the missing spanning relations for each continuous span of the graph
    for(SSpan span : graph.getSSpans())
    {
      long pre=1;
      RelannisNodeFeature featSpan = RelannisNodeFeature.extract(span);      
      ComponentEntry spanComponent = componentForSpan.get(span.getSId());
      if(spanComponent != null && featSpan != null)
      {
        for(long i=featSpan.getLeftToken(); i <= featSpan.getRightToken(); i++)
        {
          SToken tok = tokenByIndex.get(i);
          if(tok != null)
          {
            boolean missing = true;
            EList<Edge> existingEdges = graph.getEdges(span.getSId(), tok.getSId());
            if(existingEdges != null)
            {
              for(Edge e : existingEdges)
              {
                if(e instanceof SSpanningRelation)
                {
                  missing = false;
                  break;
                }
              }
            } // end if edges exist

            if(missing)
            {
              String type = "c";

              SLayer layer = findOrAddSLayer(spanComponent.getNamespace(), graph);
              
              createNewRelation(graph, span, tok, null, type, 
                spanComponent.getId(), layer, 
                pre++, nodeByRankID, numberOfEdges);
            }
          } // end if token exists
        } // end for each covered token index
      } 
    } // end for each span
  }

  private static void setMatchedIDs(SDocument document, Match match)
  {
    List<String> allUrisAsString = new LinkedList<>();
    for(URI u : match.getSaltIDs())
    {
      allUrisAsString.add(u.toASCIIString());
    }
    // set the matched keys
    SFeature featIDs = SaltFactory.eINSTANCE.createSFeature();
    featIDs.setSNS(ANNIS_NS);
    featIDs.setSName(FEAT_MATCHEDIDS);
    featIDs.setSValue(Joiner.on(",").join(allUrisAsString));
    document.addSFeature(featIDs);
    
    SFeature featAnnos = SaltFactory.eINSTANCE.createSFeature();
    featAnnos.setSNS(ANNIS_NS);
    featAnnos.setSName(FEAT_MATCHEDANNOS);
    featAnnos.setSValue(Joiner.on(",").join(match.getAnnos()));
    document.addSFeature(featAnnos);

  }

  private void createSinglePrimaryText(SDocumentGraph graph, long textID,
    TreeMap<Long, String> tokenTexts,  TreeMap<Long, SToken> tokenByIndex)
  {
    STextualDS textDataSource = SaltFactory.eINSTANCE.createSTextualDS();
    textDataSource.setSName("sText" + textID);
    graph.addSNode(textDataSource);
    
    StringBuilder sbText = new StringBuilder();
    Iterator<Map.Entry<Long, String>> itToken = tokenTexts.entrySet().
      iterator();
    long index = 0;
    while (itToken.hasNext())
    {
      Map.Entry<Long, String> e = itToken.next();
      SToken tok = tokenByIndex.get(e.getKey());
      
      SFeature rawFeature = tok.getSFeature(ANNIS_NS, FEAT_RELANNIS_NODE);
      if(rawFeature != null)
      {
        RelannisNodeFeature feat = (RelannisNodeFeature) rawFeature.getSValue();

        if(feat.getTextRef() == textID)
        {
          STextualRelation textRel = SaltFactory.eINSTANCE.createSTextualRelation();
          textRel.setSSource(tok);
          textRel.setSTarget(textDataSource);
          textRel.setSStart(sbText.length());
          textRel.setSEnd(sbText.length() + e.getValue().length());

          textRel.setSName("sTextRel" + textID + "_" + (index++));

          textRel.setSTextualDS(textDataSource);
          graph.addSRelation(textRel);

          sbText.append(e.getValue());
          if (itToken.hasNext())
          {
            sbText.append(" ");
          }
        }
      }
    }

    textDataSource.setSText(sbText.toString());
  }
  
  private void createPrimaryTexts(SDocumentGraph graph,
    TreeSet<Long> allTextIDs, TreeMap<Long, String> tokenTexts, 
    TreeMap<Long, SToken> tokenByIndex)
  {
    for(long textID : allTextIDs)
    {
      createSinglePrimaryText(graph, textID, tokenTexts, tokenByIndex);
    }
    
  }

  private SNode createOrFindNewNode(ResultSet resultSet,
    SDocumentGraph graph, TreeSet<Long> allTextIDs, TreeMap<Long, String> tokenTexts,
    TreeMap<Long, SToken> tokenByIndex, 
    TreeMap<String, TreeMap<Long, String>> nodeBySegmentationPath,
    SolutionKey<?> key,
    FastInverseMap<Long, SNode> nodeByRankID) throws SQLException
  {
    String name = stringValue(resultSet, NODE_TABLE, "node_name");
    String saltID = stringValue(resultSet, NODE_TABLE, "salt_id");
    if(saltID == null)
    {
      // fallback to the name
      saltID = name;
    }
    long internalID = longValue(resultSet, "node", "id");

    String edgeType = stringValue(resultSet, COMPONENT_TABLE, "type");
    
    long tokenIndex = longValue(resultSet, NODE_TABLE, "token_index");
    boolean isToken = !resultSet.wasNull();

    org.eclipse.emf.common.util.URI nodeURI = graph.getSDocument().getSElementPath();

    nodeURI = nodeURI.appendSegment("");
    nodeURI = nodeURI.appendFragment(saltID);
    SStructuredNode node = (SStructuredNode) graph.getSNode(nodeURI.toString());
    if (node == null)
    {
      // create new node
      if (isToken)
      {
        node = createSToken(tokenIndex, resultSet, tokenTexts, tokenByIndex);
      }
      else
      {
        node = createOtherSNode(resultSet);
      }

      node.setSName(name);
      node.setSId(nodeURI.toString());
      
      setFeaturesForNode(node, internalID, resultSet);
      
      Object nodeId = key.getNodeId(resultSet,
        outerQueryTableAccessStrategy);
      

      graph.addNode(node);
      Integer matchedNode = key.getMatchedNodeIndex(nodeId);
      if (matchedNode != null)
      {
        addLongSFeature(node, FEAT_MATCHEDNODE, matchedNode);
      }
      
      mapLayer(node, graph, resultSet);
      
      long textRef = longValue(resultSet, NODE_TABLE, "text_ref");
      allTextIDs.add(textRef);
      
    }
    else if("c".equals(edgeType) && isToken == false)
    {
      node = testAndFixNonSpan(node, nodeByRankID);
    }

    String nodeAnnoValue =
      stringValue(resultSet, NODE_ANNOTATION_TABLE, "value");
    String nodeAnnoNameSpace = stringValue(resultSet, NODE_ANNOTATION_TABLE,
      "namespace");
    String nodeAnnoName = stringValue(resultSet, NODE_ANNOTATION_TABLE, "name");
    if (!resultSet.wasNull())
    {
      String fullName = (nodeAnnoNameSpace == null || nodeAnnoNameSpace.isEmpty() ? "" : (nodeAnnoNameSpace
        + "::")) + nodeAnnoName;
      SAnnotation anno = node.getSAnnotation(fullName);
      if (anno == null)
      {
        anno = SaltFactory.eINSTANCE.createSAnnotation();
        anno.setSNS(nodeAnnoNameSpace);
        anno.setSName(nodeAnnoName);
        anno.setSValue(nodeAnnoValue);
        node.addSAnnotation(anno);
      }
    }

    // prepare SOrderingRelation if the node is part of a segmentation path
    String segName = stringValue(resultSet, "node", "seg_name");
    if(segName != null)
    {
      long left = longValue(resultSet, "node", "seg_index");
      // only nodes that might be valid leafs
      // since we are sorting everything by preorder the real leafs will be the
      // last ones
      if(!nodeBySegmentationPath.containsKey(segName))
      {
        nodeBySegmentationPath.put(segName, new TreeMap<Long, String>());
      }
      nodeBySegmentationPath.get(segName).put(left, node.getSId());

    }
    
    return node;
  }
  
  private SToken createSToken(long tokenIndex, ResultSet resultSet,  
    TreeMap<Long, String> tokenTexts,
    TreeMap<Long, SToken> tokenByIndex) throws SQLException
  {
    SToken tok = SaltFactory.eINSTANCE.createSToken();
    
    // get spanned text of token
    tokenTexts.put(tokenIndex, stringValue(resultSet, NODE_TABLE, "span"));
    tokenByIndex.put(tokenIndex, tok);
    
    return tok;
  }
  
  private SStructuredNode createOtherSNode(ResultSet resultSet) throws SQLException
  {
    // check if we have span, early detection of spans will spare
    // us calls to recreateNode() which is quite costly since it
    // removes nodes/edges and this is something Salt does not handle
    // efficiently
    String edgeType = stringValue(resultSet, COMPONENT_TABLE, "type");
    if("c".equals(edgeType))
    {
      SSpan span = SaltFactory.eINSTANCE.createSSpan();
      return span;
    }
    else
    {
      // default fallback is a SStructure
      SStructure struct = SaltFactory.eINSTANCE.createSStructure();
      return struct;
    }
  }
  
  private void setFeaturesForNode(SStructuredNode node, long internalID, ResultSet resultSet) throws SQLException
  { 
    
    SFeature feat = SaltFactory.eINSTANCE.createSFeature();
    feat.setSNS(ANNIS_NS);
    feat.setSName(FEAT_RELANNIS_NODE);
    
    RelannisNodeFeature val = new RelannisNodeFeature();
    val.setInternalID(longValue(resultSet, "node", "id"));
    val.setCorpusRef(longValue(resultSet, "node", "corpus_ref"));
    val.setTextRef(longValue(resultSet, "node", "text_ref"));
    val.setLeft(longValue(resultSet, "node", "left"));
    val.setLeftToken(longValue(resultSet, "node", "left_token"));
    val.setRight(longValue(resultSet, "node", "right"));
    val.setRightToken(longValue(resultSet, "node", "right_token"));
    val.setTokenIndex(longValue(resultSet, "node", "token_index"));
    val.setSegIndex(longValue(resultSet, "node", "seg_index"));
    val.setSegName(stringValue(resultSet, "node", "seg_name"));
    feat.setSValue(val);
    
    node.addSFeature(feat);
  }
  
  private void mapLayer(SStructuredNode node, SDocumentGraph graph, ResultSet resultSet) 
    throws SQLException
  {
    String namespace = stringValue(resultSet, NODE_TABLE, "namespace");
    List<SLayer> layerList = graph.getSLayerByName(namespace);
    SLayer layer = (layerList != null && layerList.size() > 0)
      ? layerList.get(0) : null;
    if (layer == null)
    {
      layer = SaltFactory.eINSTANCE.createSLayer();
      layer.setSName(namespace);
      graph.addSLayer(layer);
    }
    node.getSLayers().add(layer);
  }
  

  private void addLongSFeature(SNode node, String name,
    long value) throws SQLException
  {
    SFeature feat = SaltFactory.eINSTANCE.createSFeature();
    feat.setSNS(ANNIS_NS);
    feat.setSName(name);
    feat.setSValue(value);
    node.addSFeature(feat);
  }
  
  private void addStringSFeature(SNode node, String name,
    String value) throws SQLException
  {
    SFeature feat = SaltFactory.eINSTANCE.createSFeature();
    feat.setSNS(ANNIS_NS);
    feat.setSName(name);
    feat.setSValue(value);
    node.addSFeature(feat);
  }

// non used functions, commmented out in order to avoid some findbugs warnings 
//  private void addLongSFeature(SNode node, ResultSet resultSet, String name,
//    String table, String tupleName) throws SQLException
//  {
//    addLongSFeature(node, name, longValue(resultSet, table, tupleName));
//  }
//  
//  private void addStringSFeature(SNode node, ResultSet resultSet, String name,
//    String table, String tupleName) throws SQLException
//  {
//    addStringSFeature(node, name, stringValue(resultSet, table, tupleName));
//  }

  private SStructuredNode recreateNode(Class<? extends SStructuredNode> clazz,
    SStructuredNode oldNode)
  {
    if (oldNode.getClass() == clazz)
    {
      return oldNode;
    }
    
    SStructuredNode node = oldNode;

    if (clazz == SSpan.class)
    {
      node = SaltFactory.eINSTANCE.createSSpan();
    }
    else if (clazz == SStructure.class)
    {
      node = SaltFactory.eINSTANCE.createSStructure();
    }
    else
    {
      throw new UnsupportedOperationException("no node creation possible for class: "
        + clazz.getName());
    }
    moveNodeProperties(oldNode, node, oldNode.getSGraph());

    return node;
  }
  
  private void updateMapAfterRecreatingNode(SNode oldNode, SNode newNode, 
    FastInverseMap<Long, SNode> nodeByRankID)
  {
    // get *all* keys associated with this node
    List<Long> keys = nodeByRankID.getKeys(oldNode);
    for(Long id : keys)
    {
      nodeByRankID.put(id, newNode);
    }
  }

  private void moveNodeProperties(SStructuredNode from, SStructuredNode to,
    SGraph graph)
  {
    Validate.notNull(from);
    Validate.notNull(to);

    String oldID = from.getSId();
    
    to.setSName(from.getSName());
    for (SLayer l : from.getSLayers())
    {
      to.getSLayers().add(l);
    }
    from.getSLayers().clear();
 
    Multimap<SRelation, SLayer> layerOfEdge = ArrayListMultimap.create();
    
    List<Edge> inEdges =  new LinkedList<>(graph.getInEdges(from.getSId()));
    for(Edge e : inEdges)
    {
      if(e instanceof SRelation)
      {
        SRelation rel = (SRelation) e;
        if(rel.getSLayers() != null)
        {
          layerOfEdge.putAll(rel, rel.getSLayers());
        }
        Validate.isTrue(graph.removeEdge(e));
      }
    }
    List<Edge> outEdges = new LinkedList<>(graph.getOutEdges(from.getSId()));
    for(Edge e : outEdges)
    {
      if(e instanceof SRelation)
      {
        SRelation rel = (SRelation) e;
        if(rel.getSLayers() != null)
        {
          layerOfEdge.putAll(rel, rel.getSLayers());
        }
        Validate.isTrue(graph.removeEdge(e));
      }
    }
    
    Validate.isTrue(graph.removeNode(from));
    to.setSId(oldID);
    graph.addNode(to);
    
    // fix old edges
    for(Edge e : inEdges)
    {
      if(e instanceof SRelation)
      {
        SRelation rel = (SRelation) e;
        rel.setSTarget(to);
        graph.addSRelation(rel);
        if(layerOfEdge.containsKey(rel))
        {
          for(SLayer l : layerOfEdge.get(rel))
          {
            rel.getSLayers().add(l);
          }
        }
      }
    }
    
    for(Edge e : outEdges)
    {
      if(e instanceof SRelation)
      {
        SRelation rel = (SRelation) e;
        rel.setSSource(to);
        graph.addSRelation(rel);
        if(layerOfEdge.containsKey(rel))
        {
          for(SLayer l : layerOfEdge.get(rel))
          {
            rel.getSLayers().add(l);
          }
        }
      }
    }


    for (SAnnotation anno : from.getSAnnotations())
    {
      to.addSAnnotation(anno);
    }
    for (SFeature feat : from.getSFeatures())
    {
      // filter the features, do not include salt::SNAME 
      if (!(SaltFactory.SALT_CORE_NAMESPACE.equals(feat.getSNS())
        && SaltFactory.SALT_CORE_SFEATURES.SNAME.toString().equals(
        feat.getSName())))
      {
        to.addSFeature(feat);
      }
    }
    for (SProcessingAnnotation proc : from.getSProcessingAnnotations())
    {
      to.addSProcessingAnnotation(proc);
    }
    for (SMetaAnnotation meta : from.getSMetaAnnotations())
    {
      to.addSMetaAnnotation(meta);
    }

  }
  
  private SRelation findExistingRelation(SDocumentGraph graph, 
    SNode sourceNode, SNode targetNode, String edgeName, SLayer layer)
  {
    SRelation rel = null;
    
    List<Edge> existingEdges = graph.getEdges(sourceNode.getSId(),
      targetNode.getSId());
    if (existingEdges != null)
    {
      for (Edge e : existingEdges)
      {
        // only select the edge that has the same type ("edge_name" and
        // the same layer ("edge_namespace")
        if (e instanceof SRelation)
        {
          SRelation existingRel = (SRelation) e;

          boolean noType = existingRel.getSTypes() == null || existingRel.getSTypes().size() == 0;
          if (((noType && edgeName == null) || (!noType && existingRel.getSTypes().
            contains(edgeName)))
            && existingRel.getSLayers().contains(layer))
          {
            rel = existingRel;
            break;
          }

        }
      }
    }
    return rel;
  }  
  
  private SRelation createNewRelation(SDocumentGraph graph,
    SStructuredNode sourceNode,
    SNode targetNode, String edgeName, String type, long componentID,
    SLayer layer, long pre,
    FastInverseMap<Long, SNode> nodeByRankID, AtomicInteger numberOfEdges)
  {
    
    SRelation rel = null;

    if (null != type)
    // create new relation
    {
      
      switch (type)
      {
        case "d":
          SDominanceRelation domrel = SaltFactory.eINSTANCE.
            createSDominanceRelation();
          // always set a name by ourself since the SDocumentGraph#basicAddEdge()
          // functions otherwise real slow
          domrel.setSName("sDomRel" + numberOfEdges.incrementAndGet());
          rel = domrel;
          if (sourceNode != null && !(sourceNode instanceof SStructure))
          {
            log.debug("Mismatched source type: should be SStructure");
            SNode oldNode = sourceNode;
            sourceNode = recreateNode(SStructure.class, sourceNode);
            updateMapAfterRecreatingNode(oldNode, sourceNode, nodeByRankID);
          }
          
          if (edgeName == null || edgeName.isEmpty())
          {
            // check if there is an edge which connects the nodes in the same 
            // layer but has a non-empty edge name
            if(handleArtificialDominanceRelation(graph, 
              sourceNode, targetNode,
              rel, layer, componentID,
              pre))
            {
              // don't include this relation
              rel = null;
            }
          }
          
          break;
        case "c":
          SSpanningRelation spanrel = SaltFactory.eINSTANCE.
            createSSpanningRelation();
          // always set a name by ourself since the SDocumentGraph#basicAddEdge()
          // functions is real slow otherwise
          spanrel.setSName("sSpanRel" + numberOfEdges.incrementAndGet());
          rel = spanrel;
          sourceNode = testAndFixNonSpan(sourceNode, nodeByRankID);
          break;
        case "p":
          SPointingRelation pointingrel = SaltFactory.eINSTANCE.
            createSPointingRelation();
          pointingrel.setSName("sPointingRel" + numberOfEdges.incrementAndGet());
          rel = pointingrel;
          break;
        default:
          throw new IllegalArgumentException("Invalid type " + type
            + " for new Edge");
      }

      try
      {
        if(rel != null)
        {
          rel.addSType(edgeName);

          RelannisEdgeFeature featEdge = new RelannisEdgeFeature();
          featEdge.setPre(pre);
          featEdge.setComponentID(componentID);

          SFeature sfeatEdge = SaltFactory.eINSTANCE.createSFeature();
          sfeatEdge.setSNS(ANNIS_NS);
          sfeatEdge.setSName(FEAT_RELANNIS_EDGE);
          sfeatEdge.setValue(featEdge);
          rel.addSFeature(sfeatEdge);

          rel.setSSource(sourceNode);
          
          if ("c".equals(type) && !(targetNode instanceof SToken))
          {
            log.warn("invalid edge detected: target node ({}) "
              + "of a coverage relation (from: {}, internal id {}) was not a token",
              new Object[]
              {
                targetNode.getSName(), sourceNode == null ? "null" : sourceNode.
                  getSName(), "" + pre
              });
          }
          else
          {
            rel.setSTarget(targetNode);
            graph.addSRelation(rel);            
            rel.getSLayers().add(layer);
          }

        }
      }
      catch (SaltException ex)
      {
        log.warn("invalid edge detected", ex);
      }
    }

    return rel;
  }

  /**
   * In relANNIS there is a special combined dominance component which has an empty name,
   * but which should not directly be included in the Salt graph.
   * 
   * This functions checks if a dominance edge with empty name has a "mirror" edge which
   * is inside the same layer and between the same nodes but has an edge name.
   * If yes the original dominance edge is an artificial one. The function will
   * return true in this case and update the mirror edge to include information
   * about the artificial dominance edge.
   * @param graph 
   * @param rel
   * @parem layer
   * @param componentID 
   * @param pre
   * @return True if the dominance edge was an artificial one
   */
  private boolean handleArtificialDominanceRelation(SDocumentGraph graph,
    SNode source, SNode target,
    SRelation rel, SLayer layer,
    long componentID, long pre)
  {
    List<Edge> mirrorEdges = graph.getEdges(source.getSId(),
      target.getSId());
    if (mirrorEdges != null && mirrorEdges.size() > 0)
    {
      for (Edge mirror : mirrorEdges)

      {
        if (mirror != rel && mirror instanceof SRelation)
        {
          // check layer
          SRelation mirrorRel = (SRelation) mirror;

          EList<SLayer> mirrorLayers = mirrorRel.getSLayers();
          if (mirrorLayers != null)
          {
            for (SLayer mirrorLayer : mirrorLayers)
            {
              if (mirrorLayer == layer)
              {
                        // adjust the feature of the mirror edge to include
                // information about the artificial dominance edge
                RelannisEdgeFeature mirrorFeat = RelannisEdgeFeature.
                  extract(mirrorRel);
                mirrorFeat.setArtificialDominanceComponent(componentID);
                mirrorFeat.setArtificialDominancePre(pre);
                mirrorRel.removeLabel(ANNIS_NS, FEAT_RELANNIS_EDGE);
                mirrorRel.createSFeature(ANNIS_NS, FEAT_RELANNIS_EDGE,
                  mirrorFeat,
                  SDATATYPE.SOBJECT);
                
                return true;
              }
            }
          }
        }
      }
    }
    
    return false;
  }
  
  private void addEdgeAnnotations(ResultSet resultSet, SRelation rel) 
    throws SQLException
  {
    String edgeAnnoValue =
        stringValue(resultSet, EDGE_ANNOTATION_TABLE, "value");
      String edgeAnnoNameSpace = stringValue(resultSet, EDGE_ANNOTATION_TABLE,
        "namespace");
      String edgeAnnoName =
        stringValue(resultSet, EDGE_ANNOTATION_TABLE, "name");
      if (!resultSet.wasNull())
      {
        String fullName = edgeAnnoNameSpace == null ? "" : edgeAnnoNameSpace
          + "::" + edgeAnnoName;
        SAnnotation anno = rel.getSAnnotation(fullName);
        if (anno == null)
        {
          anno = SaltFactory.eINSTANCE.createSAnnotation();
          anno.setSNS(edgeAnnoNameSpace == null ? "" : edgeAnnoNameSpace);
          anno.setSName(edgeAnnoName);
          anno.setSValue(edgeAnnoValue);
          rel.addSAnnotation(anno);
        }
      } // end if edgeAnnoName exists
  }
  
  /**
   * Tests if the source node is not a span and fixes this if necessary
   * @param sourceNode The source node to check.
   * @param nodeByRankID 
   * @return Either the original span or a new created one
   */
  private SSpan testAndFixNonSpan(SStructuredNode sourceNode, FastInverseMap<Long, SNode> nodeByRankID)
  {
    if (sourceNode != null && !(sourceNode instanceof SSpan))
    {
      log.debug("Mismatched source type: should be SSpan");
      SNode oldNode = sourceNode;
      sourceNode = recreateNode(SSpan.class, sourceNode);
      updateMapAfterRecreatingNode(oldNode,  sourceNode, nodeByRankID);
    }
    return (SSpan) sourceNode;
  }

  private void createRelation(ResultSet resultSet, SDocumentGraph graph,
    FastInverseMap<Long, SNode> nodeByRankID, SNode targetNode, AtomicInteger numberOfEdges) throws
    SQLException
  {
    long parent = longValue(resultSet, RANK_TABLE, "parent");
    if (resultSet.wasNull())
    {
      return;
    }

    long pre = longValue(resultSet, RANK_TABLE, "pre");
    long componentID = longValue(resultSet, RANK_TABLE, "component_id");
    String edgeNamespace = stringValue(resultSet, COMPONENT_TABLE, "namespace");
    if(edgeNamespace == null)
    {
      edgeNamespace = "default_ns";
    }
    String edgeName = stringValue(resultSet, COMPONENT_TABLE, "name");
    String type = stringValue(resultSet, COMPONENT_TABLE, "type");

    SStructuredNode sourceNode = 
      (SStructuredNode) nodeByRankID.get(parent);

    if (sourceNode == null)
    {
      // the edge is not fully included in the result
      return;
    }

    SLayer layer = findOrAddSLayer(edgeNamespace, graph);

    SRelation rel;
    if (!resultSet.wasNull())
    {

      rel = findExistingRelation(graph, sourceNode, targetNode, edgeName, layer);

      if (rel == null)
      {
        rel = createNewRelation(graph, sourceNode, targetNode, edgeName, type, 
          componentID, layer, pre, nodeByRankID, numberOfEdges);
      } // end if no existing relation

      // add edge annotations if relation was successfully created
      if(rel != null)
      {
        addEdgeAnnotations(resultSet, rel);
      }
    }
  }
  
  /**
   * Retrieves an existing layer by it's name or creates and adds a new one if
   * not existing yet
   * @param name
   * @param graph
   * @return Either the old or the newly created layer
   */
  private SLayer findOrAddSLayer(String name, SDocumentGraph graph)
  {
    List<SLayer> layerList = graph.getSLayerByName(name);
    SLayer layer = (layerList != null && layerList.size() > 0)
      ? layerList.get(0) : null;
    if (layer == null)
    {
      layer = SaltFactory.eINSTANCE.createSLayer();
      layer.setSName(name);
      graph.addSLayer(layer);
    }
    return layer;
  }
  
  /**
   * Sets additional match (global) information about the matched nodes and annotations.
   * 
   * This will add the {@link AnnisConstants#FEAT_MATCHEDIDS) to all {@link SDocument} elements of the 
   * salt project.
   * 
   * @param p The salt project to add the features to.
   * @param matchGroup A list of matches in the same order as the corpus graphs of the salt project.
   */
  public static void addMatchInformation(SaltProject p, MatchGroup matchGroup)
  {
    int matchIndex = 0;
    for(Match m : matchGroup.getMatches())
    {
      // get the corresponding SDocument of the salt project
      if (matchIndex < p.getSCorpusGraphs().size())
      {
        SCorpusGraph corpusGraph = p.getSCorpusGraphs().get(matchIndex);
        SDocument doc = corpusGraph.getSDocuments().get(0);

        setMatchedIDs(doc, m);
      }
      else
      {
        log.error("No corpus graph in result for expected match {}", m.toString());
      }

      matchIndex++;
    }
  }
  
  protected SolutionKey<?> createSolutionKey()
  {
    throw new UnsupportedOperationException(
      "BUG: This method needs to be overwritten by ancestors or through Spring");
  }

  protected void newline(StringBuilder sb, int indentBy)
  {
    sb.append("\n");
    indent(sb, indentBy);
  }

  protected void indent(StringBuilder sb, int indentBy)
  {
    sb.append(StringUtils.repeat(AbstractSqlGenerator.TABSTOP, indentBy));
  }
  
  protected boolean booleanValue(ResultSet resultSet, String table, String column)
    throws SQLException
  {
    return resultSet.getBoolean(outerQueryTableAccessStrategy.columnName(table,
      column));
  }

  protected long longValue(ResultSet resultSet, String table, String column)
    throws SQLException
  {
    return resultSet.getLong(outerQueryTableAccessStrategy.columnName(table,
      column));
  }

  protected String stringValue(ResultSet resultSet, String table, String column)
    throws SQLException
  {
    return resultSet.getString(outerQueryTableAccessStrategy.columnName(
      table, column));
  }

  public CorpusPathExtractor getCorpusPathExtractor()
  {
    return corpusPathExtractor;
  }

  public void setCorpusPathExtractor(CorpusPathExtractor corpusPathExtractor)
  {
    this.corpusPathExtractor = corpusPathExtractor;
  }

  public TableAccessStrategy getOuterQueryTableAccessStrategy()
  {
    return outerQueryTableAccessStrategy;
  }

  @Override
  public void setOuterQueryTableAccessStrategy(TableAccessStrategy outerQueryTableAccessStrategy)
  {
    this.outerQueryTableAccessStrategy = outerQueryTableAccessStrategy;
  }
  
  public static class FastInverseMap<KeyType, ValueType>
  {
    private Map<KeyType, ValueType> key2value = new HashMap<>();
    private Map<ValueType, List<KeyType>> values2keys = new HashMap<>(); 
    
    /**
     * Wrapper for {@link Map#put(java.lang.Object, java.lang.Object) }
     * @param key
     * @param value
     * @return 
     */
    public ValueType put(KeyType key, ValueType value)
    {
      List<KeyType> inverse = values2keys.get(value);
      if(inverse == null)
      {
        inverse = new LinkedList<>();
        values2keys.put(value, inverse);
      }
      
      inverse.add(key);
      
      return key2value.put(key, value);
    }
    
    /**
     * Wrapper for {@link Map#get(java.lang.Object) }
     * @param key
     * @return 
     */
    public ValueType get(KeyType key)
    {
      return key2value.get(key);
    }
    
    /**
     * Fast inverse lookup.
     * 
     * @param value
     * @return All keys belonging to this value.
     */
    public List<KeyType> getKeys(ValueType value)
    {
      List<KeyType> result = values2keys.get(value);
      if(result == null)
      {
        result = new LinkedList<>();
        values2keys.put(value, result);
      }
      
      // always return a copy
      return new LinkedList<>(result);
    }
    
    /**
     * Wrapper for {@link  Map#clear() }
     */
    public void clear()
    {
      key2value.clear();
      values2keys.clear();
    }
  }
  
  public static class RankID
  {
    private final long componentID;
    private final long pre;

    public RankID(long componentID, long pre)
    {
      this.componentID = componentID;
      this.pre = pre;
    }

    public long getComponentID()
    {
      return componentID;
    }

    public long getPre()
    {
      return pre;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (obj == null)
      {
        return false;
      }
      if (getClass() != obj.getClass())
      {
        return false;
      }
      final RankID other = (RankID) obj;
      if (this.componentID != other.componentID)
      {
        return false;
      }
      if (this.pre != other.pre)
      {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode()
    {
      int hash = 5;
      hash = 29 * hash + (int) (this.componentID ^ (this.componentID >>> 32));
      hash = 29 * hash + (int) (this.pre ^ (this.pre >>> 32));
      return hash;
    } 
  }
  
  public static class ComponentEntry
  {
    
    private final long id;
    private final char type;
    private final String namespace;
    private final String name;

    public ComponentEntry(long id, char type, String namespace, String name)
    {
      this.id = id;
      this.type = type;
      this.namespace = namespace;
      this.name = name;
    }

    public long getId()
    {
      return id;
    }

    public char getType()
    {
      return type;
    }

    public String getNamespace()
    {
      return namespace;
    }

    public String getName()
    {
      return name;
    }

    @Override
    public String toString()
    {
      return "ComponentEntry{" + "id=" + id + ", type=" + type + ", namespace=" + namespace + ", name=" + name + '}';
    }

    
    
  }
  
}
