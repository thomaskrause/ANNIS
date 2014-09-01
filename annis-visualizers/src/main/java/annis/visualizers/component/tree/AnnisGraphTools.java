/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.visualizers.component.tree;

import annis.CommonHelper;
import annis.libgui.Helper;
import annis.libgui.visualizers.VisualizerInput;
import annis.model.AnnisConstants;
import annis.model.AnnisNode;
import annis.model.RelannisNodeFeature;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class AnnisGraphTools implements Serializable
{

  private static final String PRIMEDGE_SUBTYPE = "edge";
  private static final String SECEDGE_SUBTYPE = "secedge";
  private final VisualizerInput input;


  public AnnisGraphTools(VisualizerInput input)
  {
      this.input = input;
  }

  public Collection<DirectedGraph<SNode, SDominanceRelation>> getSyntaxGraphs(
    VisualizerInput input)
  {
    SDocument result = input.getDocument();
    
    String namespace = input.getMappings().getProperty("node_ns", input.
      getNamespace());

    String terminalName =  input.getMappings().getProperty(
      TigerTreeVisualizer.TERMINAL_NAME_KEY);
    String terminalNamespace =  input.getMappings().getProperty(
      TigerTreeVisualizer.TERMINAL_NS_KEY);
    
    TreeMap<Long, DirectedGraph<SNode, SDominanceRelation>> resultGraphs =
      new TreeMap<>();

    Set<SNode> orphanTerminals = new HashSet<>();


    for (SNode n : result.getSDocumentGraph().getSNodes())
    {
      RelannisNodeFeature featNode = (RelannisNodeFeature) n.getSFeature(
        AnnisConstants.ANNIS_NS, AnnisConstants.FEAT_RELANNIS_NODE).getSValue();
      if(isTerminal(n, input))
      {
        orphanTerminals.add(n);
        resultGraphs.put(featNode.getLeftToken(),
          extractGraph(result.getSDocumentGraph(), n, terminalNamespace, terminalName));
      }
      
      if (isRootNode(n, namespace))
      {
        DirectedGraph<SNode, SDominanceRelation> directedGraph  = 
          extractGraph(result.getSDocumentGraph(), n, terminalNamespace, terminalName);
        resultGraphs.put(featNode.getLeftToken(), directedGraph);
      }
    }
    
    // terminals that are included in any of the graphs aren't orphans
    for(DirectedGraph<SNode,?> g : resultGraphs.values())
    {
      orphanTerminals.removeAll(g.getVertices());
    }
    
    for (SNode n : orphanTerminals)
    {
      RelannisNodeFeature featNode = (RelannisNodeFeature) n.getSFeature(
        AnnisConstants.ANNIS_NS, AnnisConstants.FEAT_RELANNIS_NODE).getSValue();
      resultGraphs.put(featNode.getLeftToken(),  extractGraph(result.getSDocumentGraph(), 
        n, terminalNamespace, terminalName));
    }
    
    return resultGraphs.values();
  }

  private boolean copyNode(DirectedGraph<SNode, SDominanceRelation> graph, SNode n,
     String terminalNamespace, String terminalName)
  {
    boolean addToGraph = AnnisGraphTools.isTerminal(n, input);

    if (!addToGraph)
    {

      for (Edge e : n.getSGraph().getOutEdges(n.getSId()))
      {
        if (e instanceof SDominanceRelation)
        {
          SDominanceRelation rel = (SDominanceRelation) e;
          if (includeEdge(rel, input)
            && copyNode(graph, rel.getSTarget(), terminalNamespace, terminalName))
          {
            addToGraph |= true;
            graph.addEdge(rel, n, rel.getSTarget());

          }
        }
      }
    }

    if (addToGraph)
    {
      graph.addVertex(n);
    }
    return addToGraph;
  }

  private boolean isRootNode(SNode n, String namespace)
  {
    if (n.getSLayers() == null || n.getSLayers().size() == 0 
      || !n.getSLayers().get(0).getSName().equals(namespace))
    {
      return false;
    }
    for (Edge e : n.getSGraph().getInEdges(n.getSId()))
    {
      if(e instanceof SDominanceRelation)
      {
        SDominanceRelation rel = (SDominanceRelation) e;
        if (hasEdgeSubtype(rel, AnnisGraphTools.PRIMEDGE_SUBTYPE) && rel.getSSource()
          != null)
        {
          return false;
        }
      }
    }
    return true;
  }
  
  public static boolean isTerminal(SNode n, VisualizerInput input)
  {
    String terminalName = (input == null ? null : input.getMappings().getProperty(
      TigerTreeVisualizer.TERMINAL_NAME_KEY));
    
    if(terminalName == null)
    {
      return n instanceof SToken;
    }
    else
    {
      String terminalNamespace = (input == null ? null : input.getMappings().getProperty(
        TigerTreeVisualizer.TERMINAL_NS_KEY));

      String annoValue = extractAnnotation(n.getSAnnotations(),
        terminalNamespace,
        terminalName);
      
      return annoValue != null;
    }
  }

  private DirectedGraph<SNode, SDominanceRelation> extractGraph(SDocumentGraph sgraph,
    SNode n, String terminalNamespace, String terminalName)
  {
    DirectedGraph<SNode, SDominanceRelation> graph =
      new DirectedSparseGraph<SNode, SDominanceRelation>();
    copyNode(graph, n, terminalNamespace, terminalName);
    for (SRelation e : sgraph.getSRelations())
    {
      if(e instanceof SDominanceRelation)
      {
        if (hasEdgeSubtype((SDominanceRelation) e, AnnisGraphTools.SECEDGE_SUBTYPE))
        {
          graph.addEdge((SDominanceRelation) e, e.getSSource(), e.getSTarget());
        }
      }
    }
    return graph;
  }

  private boolean includeEdge(SDominanceRelation e, VisualizerInput input)
  {
    return hasEdgeSubtype(e, getPrimEdgeSubType());
  }

  public boolean hasEdgeSubtype(SDominanceRelation e, String edgeSubtype)
  {
    String name = e.getSLayers() != null && e.getSLayers().size() > 0 
      ? e.getSLayers().get(0).getSName() : null;

    if (getPrimEdgeSubType().equals(edgeSubtype))
    {
      edgeSubtype = input.getMappings().getProperty("edge") != null
        ? input.getMappings().getProperty("edge") : getPrimEdgeSubType();
    }

    if (getSecEdgeSubType().equals(edgeSubtype))
    {
      edgeSubtype = input.getMappings().getProperty("secedge") != null
        ? input.getMappings().getProperty("secedge") : getSecEdgeSubType();
    }

    return name != null && name.
      equals(edgeSubtype);
  }

  public static HorizontalOrientation detectLayoutDirection(SDocumentGraph graph)
  {

    if(Helper.isRTLDisabled())
    {
      return HorizontalOrientation.LEFT_TO_RIGHT;
    }
    
    int withhRTL = 0;
    for (SToken token : graph.getSTokens())
    {
      if (CommonHelper.containsRTLText(CommonHelper.getSpannedText(token)))
      {
        withhRTL += 1;
      }
    }
    return (withhRTL > graph.getSTokens().size() / 3)
      ? HorizontalOrientation.RIGHT_TO_LEFT
      : HorizontalOrientation.LEFT_TO_RIGHT;
  }

  /**
   * Gets the edge type and takes into account the user defined mappings.
   *
   * @return the name of the edge type. Is never null.
   */
  public String getPrimEdgeSubType()
  {
    return input.getMappings().getProperty("edge_type", PRIMEDGE_SUBTYPE);
  }

  /**
   * Gets the secedge type and takes into account the user defined mappings.
   *
   * @return the name of the secedge type. Is never null.
   */
  public String getSecEdgeSubType()
  {
    return input.getMappings().getProperty("secedge_type", SECEDGE_SUBTYPE);
  }
  
  public static String extractAnnotation(List<SAnnotation> annotations,
    String namespace, String featureName)
  {
    if(annotations != null)
    {
      for (SAnnotation a : annotations)
      {
        if(namespace == null)
        {
          if (a.getName().equals(featureName))
          {
            return a.getSValueSTEXT();
          }
        }
        else
        {
          if (a.getNamespace().equals(namespace) && a.getName().equals(featureName))
          {
            return a.getSValueSTEXT();
          }
        }
      } 
    }
    return null;
  }
}