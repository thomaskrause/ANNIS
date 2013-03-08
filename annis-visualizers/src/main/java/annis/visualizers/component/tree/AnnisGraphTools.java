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
import annis.libgui.visualizers.VisualizerInput;
import annis.model.AnnisNode;
import annis.model.AnnotationGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import java.util.ArrayList;
import java.util.List;

class AnnisGraphTools
{

  public static final String PRIMEDGE_SUBTYPE = "edge";
  public static final String SECEDGE_SUBTYPE = "secedge";

  public List<DirectedGraph<SNode, SDominanceRelation>> getSyntaxGraphs(
    VisualizerInput input)
  {
    SDocument result = input.getDocument();
    
    String namespace = input.getMappings().getProperty("node_ns", input.
      getNamespace());
    List<DirectedGraph<SNode, SDominanceRelation>> resultGraphs =
      new ArrayList<DirectedGraph<SNode, SDominanceRelation>>();

    for (SNode n : result.getSDocumentGraph().getSNodes())
    {
      if (isRootNode(n, namespace, input))
      {
        resultGraphs.add(extractGraph(result.getSDocumentGraph(), n, input));
      }
    }
    return resultGraphs;
  }

  private boolean copyNode(DirectedGraph<SNode, SDominanceRelation> graph, SNode n,
    VisualizerInput input)
  {
    boolean addToGraph = n instanceof SToken;
    for (Edge e : n.getSGraph().getOutEdges(n.getSId()))
    {
      if(e instanceof SDominanceRelation)
      {
        SDominanceRelation rel = (SDominanceRelation) e;
        if (includeEdge(rel, input) && copyNode(graph, rel.getSTarget(), input))
        {
          addToGraph |= true;
          graph.addEdge(rel, n, rel.getSTarget());
        }
      }
    }
    if (addToGraph)
    {
      graph.addVertex(n);
    }
    return addToGraph;
  }

  private boolean isRootNode(SNode n, String namespace,
    VisualizerInput input)
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
        if (hasEdgeSubtype(rel, AnnisGraphTools.PRIMEDGE_SUBTYPE, input) && rel.getSSource()
          != null)
        {
          return false;
        }
      }
    }
    return true;
  }

  private DirectedGraph<SNode, SDominanceRelation> extractGraph(SDocumentGraph sgraph,
    SNode n, VisualizerInput input)
  {
    DirectedGraph<SNode, SDominanceRelation> graph =
      new DirectedSparseGraph<SNode, SDominanceRelation>();
    copyNode(graph, n, input);
    for (SRelation e : sgraph.getSRelations())
    {
      if(e instanceof SDominanceRelation)
      {
        if (hasEdgeSubtype((SDominanceRelation) e, AnnisGraphTools.SECEDGE_SUBTYPE, input))
        {
          graph.addEdge((SDominanceRelation) e, e.getSSource(), e.getSTarget());
        }
      }
    }
    return graph;
  }

  private boolean includeEdge(SDominanceRelation e, VisualizerInput input)
  {
    return hasEdgeSubtype(e, AnnisGraphTools.PRIMEDGE_SUBTYPE, input);
  }

  public static boolean hasEdgeSubtype(SDominanceRelation e, String edgeSubtype,
    VisualizerInput input)
  {
    String name = e.getSLayers() != null && e.getSLayers().size() > 0 
      ? e.getSLayers().get(0).getSName() : null;

    if (PRIMEDGE_SUBTYPE.equals(edgeSubtype))
    {
      edgeSubtype = input.getMappings().getProperty("edge") != null
        ? input.getMappings().getProperty("edge") : PRIMEDGE_SUBTYPE;
    }

    if (SECEDGE_SUBTYPE.equals(edgeSubtype))
    {
      edgeSubtype = input.getMappings().getProperty("secedge") != null
        ? input.getMappings().getProperty("secedge") : SECEDGE_SUBTYPE;
    }

    return name != null && name.
      equals(edgeSubtype);
  }

  public static HorizontalOrientation detectLayoutDirection(SDocumentGraph graph)
  {
    int withHebrew = 0;
    for (SToken token : graph.getSTokens())
    {
      if (isHebrewToken(CommonHelper.getSpannedText(token)))
      {
        withHebrew += 1;
      }
    }
    return (withHebrew > graph.getSTokens().size() / 3)
      ? HorizontalOrientation.RIGHT_TO_LEFT
      : HorizontalOrientation.LEFT_TO_RIGHT;
  }

  private static boolean isHebrewToken(String text)
  {
    for (int i = 0; i < text.length(); ++i)
    {
      char c = text.charAt(i);
      if ((c >= 0x0590 && c <= 0x06f9) || (c >= 0xfb1e && c <= 0xfdff) || (c
        >= 0xfe70 && c <= 0xfeff))
      {
        return true;
      }
    }
    return false;
  }
}