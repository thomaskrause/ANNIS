/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package annis.sqlgen;

import static annis.sqlgen.TableAccessStrategy.*;

import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.SaltProject;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpanningRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SFeature;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SProcessingAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.springframework.dao.DataAccessException;
import static annis.model.AnnisConstants.*;

/**
 *
 * @author Thomas Krause <thomas.krause@alumni.hu-berlin.de>
 */
public class SaltAnnotateSqlGenerator extends AnnotateSqlGenerator<SaltProject>
{

  public SaltAnnotateSqlGenerator()
  {
  }

  @Override
  public SaltProject extractData(ResultSet resultSet)
    throws SQLException, DataAccessException
  {
    SaltProject project = SaltFactory.eINSTANCE.createSaltProject();

    SCorpusGraph corpusGraph = null;

    SDocumentGraph graph = null;

    // fn: edge pre order value -> edge
    Map<Long, SNode> nodeByPre = new HashMap<Long, SNode>();

    TreeMap<Long, String> tokenTexts = new TreeMap<Long, String>();
    TreeMap<Long, SToken> tokenByIndex = new TreeMap<Long, SToken>();

    // clear mapping functions for this graph
    // assumes that the result set is sorted by key, pre
    nodeByPre.clear();

    SDocument document = null;

    List<String> lastKey = new LinkedList<String>();

    int match_index = 0;

    while (resultSet.next())
    {
      Array sqlKey = resultSet.getArray("key_names");
      Validate.isTrue(!resultSet.wasNull(),
        "Match group identifier must not be null");
      Validate.isTrue(sqlKey.getBaseType() == Types.BIGINT,
        "Key in database must be from the type \"bigint\" but was \""
        + sqlKey.getBaseTypeName() + "\"");

      String[] keyArray = (String[]) sqlKey.getArray();
      List<String> key = Arrays.asList(keyArray);

      if (!lastKey.equals(key))
      {

        // create the text for the last graph
        if (graph != null)
        {
          createPrimaryText(graph, tokenTexts, tokenByIndex);
        }

        lastKey = key;

        // new match, reset everything        
        nodeByPre.clear();
        tokenTexts.clear();
        tokenByIndex.clear();


        Integer matchstart = resultSet.getInt("matchstart");
        corpusGraph = SaltFactory.eINSTANCE.createSCorpusGraph();
        corpusGraph.setSName("match_" + (match_index + matchstart));

        project.getSCorpusGraphs().add(corpusGraph);

        graph = SaltFactory.eINSTANCE.createSDocumentGraph();
        document = SaltFactory.eINSTANCE.createSDocument();
        
        // set the matched keys
        SFeature feature = SaltFactory.eINSTANCE.createSFeature();
        feature.setSNS(ANNIS_NS);
        feature.setSName(FEAT_MATCHEDIDS);
        feature.setSValue(StringUtils.join(keyArray, ","));
        document.addSFeature(feature);

        
        ArrayList<String> path =
          new ArrayList<String>(Arrays.asList((String[]) resultSet.getArray(
          "path").getArray()));
        Collections.reverse(path);

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
        corpusGraph.addSDocument(corpus, document);

        document.setSDocumentGraph(graph);
        match_index++;
      } // end if new key

      // get node data
      SNode node = createOrFindNewNode(resultSet, graph, tokenTexts,
        tokenByIndex, key);
      long pre = longValue(resultSet, RANK_TABLE, "pre");
      if (!resultSet.wasNull())
      {
        nodeByPre.put(pre, node);
        createRelation(resultSet, graph, nodeByPre, node);
      }
    } // end while new result row

    // the last match needs a primary text, too
    if (graph != null)
    {
      createPrimaryText(graph, tokenTexts, tokenByIndex);
    }


    return project;
  }

  private void createPrimaryText(SDocumentGraph graph,
    TreeMap<Long, String> tokenTexts, TreeMap<Long, SToken> tokenByIndex)
  {
    STextualDS textDataSource = SaltFactory.eINSTANCE.createSTextualDS();
    graph.addSNode(textDataSource);

    StringBuilder sbText = new StringBuilder();
    Iterator<Map.Entry<Long, String>> itToken = tokenTexts.entrySet().
      iterator();
    while (itToken.hasNext())
    {
      Map.Entry<Long, String> e = itToken.next();
      SToken tok = tokenByIndex.get(e.getKey());

      STextualRelation textRel = SaltFactory.eINSTANCE.createSTextualRelation();
      textRel.setSSource(tok);
      textRel.setSTarget(textDataSource);
      textRel.setSStart(sbText.length());
      textRel.setSEnd(sbText.length() + e.getValue().length());

      textRel.setSTextualDS(textDataSource);
      graph.addSRelation(textRel);

      sbText.append(e.getValue());
      if (itToken.hasNext())
      {
        sbText.append(" ");
      }
    }

    textDataSource.setSText(sbText.toString());
  }

  private SNode createOrFindNewNode(ResultSet resultSet,
    SDocumentGraph graph, TreeMap<Long, String> tokenTexts,
    TreeMap<Long, SToken> tokenByIndex, List<String> key) throws SQLException
  {
    String id = stringValue(resultSet, NODE_TABLE, "node_name");
    long internalID = longValue(resultSet, "node", "id");

    long tokenIndex = longValue(resultSet, NODE_TABLE, "token_index");
    boolean isToken = !resultSet.wasNull();

    URI nodeURI = graph.getSElementPath();
    nodeURI = nodeURI.appendFragment(id);
    SStructuredNode node = (SStructuredNode) graph.getSNode(nodeURI.toString());
    if (node == null)
    {
      // create new node
      if (isToken)
      {
        SToken tok = SaltFactory.eINSTANCE.createSToken();
        node = tok;

        // get spanned text of token
        tokenTexts.put(tokenIndex, stringValue(resultSet, NODE_TABLE, "span"));
        tokenByIndex.put(tokenIndex, tok);
      }
      else
      {
        SStructure struct = SaltFactory.eINSTANCE.createSStructure();
        node = struct;
      }

      moveNodeProperties(null, node, graph, id);

      addLongSProcessing(node, PROC_INTERNALID, internalID);
      addLongSProcessing(node, resultSet, PROC_CORPUSREF, "node", "corpus_ref");
      addLongSProcessing(node, resultSet, PROC_LEFT, "node", "left");
      addLongSProcessing(node, resultSet, PROC_LEFTTOKEN, "node", "left_token");
      addLongSProcessing(node, resultSet, PROC_RIGHT, "node", "right");
      addLongSProcessing(node, resultSet, PROC_RIGHTTOKEN, "node", "right_token");
      addLongSProcessing(node, resultSet, PROC_TOKENINDEX, "node", "token_index");

      int matchedNode = key.indexOf(id);
      if (matchedNode > -1)
      {
        addLongSFeature(node, FEAT_MATCHEDNODE, matchedNode);
      }

      String namespace = stringValue(resultSet, NODE_TABLE, "namespace");
      EList<SLayer> layerList = graph.getSLayerByName(namespace);
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

    String nodeAnnoValue =
      stringValue(resultSet, NODE_ANNOTATION_TABLE, "value");
    String nodeAnnoNameSpace = stringValue(resultSet, NODE_ANNOTATION_TABLE,
      "namespace");
    String nodeAnnoName = stringValue(resultSet, NODE_ANNOTATION_TABLE, "name");
    if (!resultSet.wasNull())
    {
      String fullName = nodeAnnoNameSpace == null ? "" : nodeAnnoNameSpace
        + "::" + nodeAnnoName;
      SAnnotation anno = node.getSAnnotation(fullName);
      if (anno == null)
      {
        anno = SaltFactory.eINSTANCE.createSAnnotation();
        anno.setSNS(nodeAnnoNameSpace == null ? "" : nodeAnnoNameSpace);
        anno.setSName(nodeAnnoName);
        anno.setSValue(nodeAnnoValue);
        node.addSAnnotation(anno);
      }
    }

    // TODO: what more do we have to do?
    return node;
  }

  private void addLongSProcessing(SNode node, String name,
    long value) throws SQLException
  {
    SProcessingAnnotation proc = SaltFactory.eINSTANCE.
      createSProcessingAnnotation();
    proc.setSNS(ANNIS_NS);
    proc.setSName(name);
    proc.setSValue(value);
    node.addSProcessingAnnotation(proc);
  }

  private void addLongSProcessing(SNode node, ResultSet resultSet, String name,
    String table, String tupleName) throws SQLException
  {
    addLongSProcessing(node, name, longValue(resultSet, table, tupleName));
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

  private void addLongSFeature(SNode node, ResultSet resultSet, String name,
    String table, String tupleName) throws SQLException
  {
    addLongSFeature(node, name, longValue(resultSet, table, tupleName));
  }

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
      throw new NotImplementedException("no node creation possible for class: "
        + clazz.getName());
    }
    moveNodeProperties(oldNode, node, oldNode.getSGraph(), null);

    return node;
  }

  private void moveNodeProperties(SStructuredNode from, SStructuredNode to,
    SGraph graph, String fallbackName)
  {

    if (from == null)
    {
      to.setSName(fallbackName);
    }
    else
    {
      to.setSName(from.getSName());
      for (SLayer l : from.getSLayers())
      {
        to.getSLayers().add(l);
      }
      from.getSLayers().clear();

      Validate.isTrue(graph.removeNode(from));
    }

    graph.addNode(to);

    if (from != null)
    {
      for (SAnnotation anno : from.getSAnnotations())
      {
        to.addSAnnotation(anno);
      }
      for (SFeature feat : from.getSFeatures())
      {
        to.addSFeature(feat);
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
  }

  private SRelation createRelation(ResultSet resultSet, SDocumentGraph graph,
    Map<Long, SNode> nodeByPre, SNode targetNode) throws
    SQLException
  {
    long parent = longValue(resultSet, RANK_TABLE, "parent");
    if (resultSet.wasNull())
    {
      return null;
    }

    long pre = longValue(resultSet, RANK_TABLE, "pre");
    String edgeNamespace = stringValue(resultSet, COMPONENT_TABLE, "namespace");
    String edgeName = stringValue(resultSet, COMPONENT_TABLE, "name");

    String type = stringValue(resultSet, COMPONENT_TABLE, "type");

    SStructuredNode sourceNode = (SStructuredNode) nodeByPre.get(parent);

    if (sourceNode == null)
    {
      // the edge is not fully included in the result
      return null;
    }

    EList<SLayer> layerList = graph.getSLayerByName(edgeNamespace);
    SLayer layer = (layerList != null && layerList.size() > 0)
      ? layerList.get(0) : null;
    if (layer == null)
    {
      layer = SaltFactory.eINSTANCE.createSLayer();
      layer.setSName(edgeNamespace);
      graph.addSLayer(layer);
    }

    SRelation rel = null;
    if (!resultSet.wasNull())
    {

      EList<Edge> existingEdges = graph.getEdges(sourceNode.getSId(),
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

            boolean noType = existingRel.getSTypes() == null || existingRel.
              getSTypes().size() == 0;
            if (((noType && edgeName == null) || (!noType && existingRel.
              getSTypes().
              contains(edgeName)))
              && existingRel.getSLayers().contains(layer))
            {
              rel = existingRel;
              break;
            }

          }
        }
      }


      if (rel == null)
      {
        // create new relation
        if ("d".equals(type))
        {
          SDominanceRelation domrel = SaltFactory.eINSTANCE.
            createSDominanceRelation();
          rel = domrel;

          if (sourceNode != null && !(sourceNode instanceof SStructure))
          {
            sourceNode = recreateNode(SStructure.class, sourceNode);
            nodeByPre.put(parent, sourceNode);
          }
        }
        else if ("c".equals(type))
        {
          SSpanningRelation spanrel = SaltFactory.eINSTANCE.
            createSSpanningRelation();
          rel = spanrel;

          if (sourceNode != null && !(sourceNode instanceof SSpan))
          {
            sourceNode = recreateNode(SSpan.class, sourceNode);
            nodeByPre.put(parent, sourceNode);
          }
        }
        else if ("p".equals(type))
        {
          SPointingRelation pointingrel = SaltFactory.eINSTANCE.
            createSPointingRelation();
          rel = pointingrel;
        }

        rel.setSSource(nodeByPre.get(parent));
        rel.setSTarget(targetNode);
        rel.getSLayers().add(layer);
        rel.addSType(edgeName);

        SProcessingAnnotation procInternalID = SaltFactory.eINSTANCE.
          createSProcessingAnnotation();
        procInternalID.setSNS(ANNIS_NS);
        procInternalID.setSName(PROC_INTERNALID);
        procInternalID.setSValue(Long.valueOf(pre));
        rel.addSProcessingAnnotation(procInternalID);

        graph.addSRelation(rel);
      } // end if no existing relation

      // add edge annotations
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
    return rel;
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

  protected long longValue(ResultSet resultSet, String table, String column)
    throws SQLException
  {
    return resultSet.getLong(getFactsTas().columnName(table, column));
  }

  protected String stringValue(ResultSet resultSet, String table, String column)
    throws SQLException
  {
    return resultSet.getString(getFactsTas().columnName(table, column));
  }
}
