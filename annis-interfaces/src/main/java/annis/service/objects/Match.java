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
package annis.service.objects;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single match of an AQL query.
 * 
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
@XmlRootElement
public class Match implements Serializable
{
  
  private final static Logger log = LoggerFactory.getLogger(Match.class);

  private final static Splitter matchSplitter = Splitter.on(" ").trimResults().omitEmptyStrings();
  
  private List<URI> saltIDs;

  public Match()
  {
    saltIDs = new ArrayList<>();
  }
  
  public Match(Collection<URI> original)
  {
    saltIDs = new ArrayList<>(original);
  }

  public void addSaltId(URI id)
  {
    if(id != null)
    {
      saltIDs.add(id);
    }
  }

  @XmlElement(name="id")
  public List<URI> getSaltIDs()
  {
    return saltIDs;
  }

  public void setSaltIDs(List<URI> saltIDs)
  {
    this.saltIDs = saltIDs;
  }
  
  /**
   * Parses the textual format for a match.
   * There is one line per match and each ID is separated by space. E.g.
   * <pre>
salt:/pcc2/11299/#tok_1 salt:/pcc2/11299/#tok_2
salt:/pcc2/11299/#tok_2 salt:/pcc2/11299/#tok_3
salt:/pcc2/11299/#tok_3 salt:/pcc2/11299/#tok_4
   * </pre>
   * @param raw
   * @return 
   */
  public static Match parseFromString(String raw)
  {
    Match match = new Match();

    for (String id : matchSplitter.split(raw))
    {
      URI uri;
      try
      {
        uri = new java.net.URI(id);

        if (!"salt".equals(uri.getScheme()) || uri.getFragment() == null)
        {
          throw new URISyntaxException("not a Salt id", uri.toString());
        }
      }
      catch (URISyntaxException ex)
      {
        log.error("Invalid syntax for ID " + id, ex);
        continue;
      }
      match.addSaltId(uri);
    }

    return match;
  }

  /**
   * Returns a space seperated list of all Salt IDs.
   * @return 
   */
  @Override
  public String toString()
  {
    Iterator<URI> it = saltIDs.iterator();
    LinkedList<String> asString = new LinkedList<>();
    while(it.hasNext())
    {
      URI u = it.next();
      if(u != null)
      {
        asString.add(u.toASCIIString());
      }
    }
    return Joiner.on(" ").join(asString);
  }

  @Override
  public int hashCode()
  {
    int hash = 3;
    hash = 41 * hash + Objects.hashCode(this.saltIDs);
    return hash;
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
    final Match other = (Match) obj;
    if (!Objects.equals(this.saltIDs, other.saltIDs))
    {
      return false;
    }
    return true;
  }
  
  
  
  
}
