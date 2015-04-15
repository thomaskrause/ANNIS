/*
 * Copyright 2013 Corpuslinguistic working group Humboldt University Berlin.
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
package annis.gui.components;

import static annis.gui.components.FrequencyWhiteboard.ADDTIONAL_PIXEL_WIDTH;
import static annis.gui.components.FrequencyWhiteboard.PIXEL_PER_VALUE;
import annis.gui.frequency.FrequencyResultPanel;
import annis.service.objects.FrequencyTable;
import annis.service.objects.FrequencyTableEntry;
import annis.service.objects.FrequencyTableQuery;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
@JavaScript(value =
{
  "flotr2.js", "vaadin://jquery.js", "scatterplot.js"
})
public class ScatterplotWhiteboard extends AbstractJavaScriptComponent implements OnLoadCallbackExtension.Callback
{
  public static final int PIXEL_PER_VALUE = 45;
  public static final int ADDTIONAL_PIXEL_WIDTH = 100;

  
  private Map<String, List<Long[]>> values;
  private String lastFont;
  private float lastFontSize = 10.0f;
  
  public ScatterplotWhiteboard(final FrequencyResultPanel freqPanel)
  {  
    setHeight("100%");
    setWidth("200px");
    addStyleName("scatterplot-chart");
    
    addFunction("selectRow", new JavaScriptFunction() {

      @Override
      public void call(JSONArray arguments) throws JSONException
      {
        freqPanel.selectRow(arguments.getInt(0));
      }
    });
    
    
    OnLoadCallbackExtension ext = new OnLoadCallbackExtension(this);
    ext.extend((ScatterplotWhiteboard) this);
    
  }
  @Override
  public void beforeClientResponse(boolean initial)
  {
    super.beforeClientResponse(initial);
    if(values != null && lastFont != null)
    {
      callFunction("showData", values, lastFont, lastFontSize);
    }
  }
  
  
  
  public void drawGraph(FrequencyTable table, String font, 
    float fontSize, FrequencyTableQuery query, FrequencyTableEntry timeEntry)
  {
    values = new LinkedHashMap<>();
    
    int timeIdx = query.indexOf(timeEntry);

    long minX = Long.MAX_VALUE;
    long maxX = Long.MIN_VALUE;
    
    for (FrequencyTable.Entry e : table.getEntries())
    {
      String label = getLabelForEntry(e, timeIdx);
      List<Long[]> series = values.get(label);
      if(series == null)
      {
        series = new LinkedList<>();
        values.put(label, series);
      }
      long x = (long) series.size();
      minX = Math.min(x, minX);
      maxX = Math.max(x, maxX);
      
      series.add(new Long[]{x, e.getCount()});
    }
    lastFont = font;
    lastFontSize = fontSize;
    
    setWidth(ADDTIONAL_PIXEL_WIDTH + (PIXEL_PER_VALUE * (maxX-minX)), Unit.PIXELS);
    
    callFunction("showData", values, lastFont, lastFontSize);
  }
  
  private String getLabelForEntry(FrequencyTable.Entry e, int timeIdx)
  {
    ArrayList<String> tuple = new ArrayList<>(Arrays.asList(e.getTupel()));
    if(timeIdx >= 0 && timeIdx < tuple.size())
    {
      tuple.remove(timeIdx);
    }
    return Joiner.on('/').join(tuple);
  }
  
  
  @Override
  public boolean onCompononentLoaded(AbstractClientConnector source)
  {
    if(values != null && lastFont != null)
    {
      callFunction("showData", values, lastFont, lastFontSize);
    }
    return true;
  }
  
  
}
