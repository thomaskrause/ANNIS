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
package annis;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import annis.dao.objects.AnnotatedMatch;
import annis.dao.objects.AnnotatedSpan;
import annis.model.Annotation;
import au.com.bytecode.opencsv.CSVWriter;

/**
 *
 * @author thomas
 */
public class CSVHelper
{
  
  private static final Logger log = LoggerFactory.getLogger(CSVHelper.class);
  
  public static SortedMap<Integer, SortedSet<String>> exportCSVHeader(
    Iterator<AnnotatedMatch> matches, PrintWriter w)
  {
    // figure out what annotations are used at each match position
    SortedMap<Integer, SortedSet<String>> columnsByNodePos = 
      new TreeMap<>();
    while(matches.hasNext())
    {
      AnnotatedMatch match = matches.next();
      for(int j = 0; j < match.size(); ++j)
      {
        AnnotatedSpan span = match.get(j);
        if(columnsByNodePos.get(j) == null)
        {
          columnsByNodePos.put(j, new TreeSet<String>());
        }
        for(Annotation annotation : span.getAnnotations())
        {
          columnsByNodePos.get(j).add("anno_" + annotation.getQualifiedName());
        }

        for(Annotation meta : span.getMetadata())
        {
          columnsByNodePos.get(j).add("meta_" + meta.getQualifiedName());
        }

      }
    }

    // important: don't close the wrapper CSVWriter!
    @SuppressWarnings("resource")
    CSVWriter csvWriter = new CSVWriter(w, '\t', CSVWriter.NO_QUOTE_CHARACTER, '\\');
  
    // print column names and data types
    int count = columnsByNodePos.keySet().size();
    ArrayList<String> headerLine = new ArrayList<>();
    
    for(int j = 0; j < count; ++j)
    {
      headerLine.add(fullColumnName(j + 1, "id"));
      headerLine.add(fullColumnName(j + 1, "span"));
      
      SortedSet<String> annotationNames = columnsByNodePos.get(j);
      for(String name : annotationNames)
      {
        headerLine.add(fullColumnName(j + 1, name));
      }
    }
    csvWriter.writeNext(headerLine.toArray(new String[headerLine.size()]));
     
    
    return columnsByNodePos;
  }
  
  public static void exportCSVData(Iterator<AnnotatedMatch> matches,
    SortedMap<Integer, SortedSet<String>> columnsByNodePos, PrintWriter w)
  {
    int count = columnsByNodePos.keySet().size();
      
    // print values
    while(matches.hasNext())
    {
      AnnotatedMatch match = matches.next();
      
      List<String> line = new ArrayList<>();
      int k = 0;
      for(; k < match.size(); ++k)
      {
        AnnotatedSpan span = match.get(k);
        Map<String, String> valueByName = new HashMap<>();

        if(span != null)
        {
          if(span.getAnnotations() != null)
          {
            for(Annotation annotation : span.getAnnotations())
            {
              valueByName.put("anno_" + annotation.getQualifiedName(), annotation.getValue());
            }
          }
          if(span.getMetadata() != null)
          {
            for(Annotation meta : span.getMetadata())
            {
              valueByName.put("meta_" + meta.getQualifiedName(), meta.getValue());
            }
          }

          line.add("" + span.getId());
          line.add(span.getCoveredText().replace("\t", "\\t"));
        }

        for(String name : columnsByNodePos.get(k))
        {
          if(valueByName.containsKey(name))
          {
            line.add(valueByName.get(name).replace("\t", "\\t"));
          }
          else
          {
            line.add("'NULL'");
          }
        }
      }
      for(int l = k; l < count; ++l)
      {
        line.add("'NULL'");
        for(int m = 0; m <= columnsByNodePos.get(l).size(); ++m)
        {
          line.add("'NULL'");
        }
      }
      w.append(StringUtils.join(line, "\t"));
      w.append("\n");
    }
  }
  
 

  private static String fullColumnName(int i, String name)
  {
    return i + "_" + name;
  }
}
