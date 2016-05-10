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
package annis.gui.exporter;

import annis.model.AnnisNode;
import annis.model.Annotation;
import annis.service.ifaces.AnnisResult;
import annis.service.ifaces.AnnisResultSet;
import annis.service.objects.SubgraphFilter;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GridExporter extends GeneralTextExporter
{

  @Override
  public void convertText(AnnisResultSet queryResult, List<String> keys, 
    Map<String,String> args, Writer out, int offset) throws IOException
  {
    
    Map<String, Map<String, Annotation>> metadataCache = 
      new HashMap<>();


    boolean showNumbers = true;
    if (args.containsKey("numbers"))
    {
      String arg = args.get("numbers");
      if (arg.equalsIgnoreCase("false")
        || arg.equalsIgnoreCase("0")
        || arg.equalsIgnoreCase("off"))
      {
        showNumbers = false;
      }
    }
    List<String> metaKeys = new LinkedList<>();
    if(args.containsKey("metakeys"))
    {
      Iterable<String> it = 
        Splitter.on(",").trimResults().split(args.get("metakeys"));
      for(String s : it)
      {
        metaKeys.add(s);
      }
    }

    int counter = 0;
    for (AnnisResult annisResult : queryResult)
    {
      HashMap<String, TreeMap<Long, Span>> annos =
        new HashMap<>();

      counter++;
      out.append((counter + offset) + ".");

      long tokenOffset = annisResult.getGraph().getTokens().get(0).getTokenIndex() - 1;
      for (AnnisNode resolveNode : annisResult.getGraph().getNodes())
      {

        for (Annotation resolveAnnotation : resolveNode.getNodeAnnotations())
        {
          String k = resolveAnnotation.getName();
          if (annos.get(k) == null)
          {
            annos.put(k, new TreeMap<Long, Span>());
          }

          // create a separate span for every annotation
          annos.get(k).put(resolveNode.getLeftToken(), new Span(resolveNode.getLeftToken(), resolveNode.getRightToken(),
            resolveAnnotation.getValue()));
          
        }
      }

      for (String k : keys)
      {

        if ("tok".equals(k))
        {
          out.append("\t" + k + "\t ");
          for (AnnisNode annisNode : annisResult.getGraph().getTokens())
          {
            out.append(annisNode.getSpannedText() + " ");
          }
          out.append("\n");
        }
        else
        {
          if(annos.get(k) != null)
          {
            out.append("\t" + k + "\t ");
            for(Span s : annos.get(k).values())
            {

              out.append(s.getValue());

              if (showNumbers)
              {
                long leftIndex = Math.max(1, s.getStart() - tokenOffset);
                long rightIndex = s.getEnd() - tokenOffset;
                out.append("[" + leftIndex
                  + "-" + rightIndex + "]");
              }
              out.append(" ");

            }
            out.append("\n");
          }
        }
      }
      
      if(!metaKeys.isEmpty())
      {
        String[] path = annisResult.getPath();
        super.appendMetaData(out, metaKeys, path[path.length-1], annisResult.getDocumentName(), metadataCache);
      }
      out.append("\n\n");
    }
  }
  
  

  @Override
  public SubgraphFilter getSubgraphFilter()
  {
    return SubgraphFilter.all;
  }
  
  


  private static class Span
  {

    private long start;
    private long end;
    private String value;

    public Span(long start, long end, String value)
    {
      this.start = start;
      this.end = end;
      this.value = value;
    }

    public long getStart()
    {
      return start;
    }

    public long getEnd()
    {
      return end;
    }


    public String getValue()
    {
      return value;
    }

  }
}
