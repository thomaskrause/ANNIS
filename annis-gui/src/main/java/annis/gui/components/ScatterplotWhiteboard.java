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

import annis.gui.frequency.FrequencyResultPanel;
import annis.service.objects.FrequencyTable;
import annis.service.objects.FrequencyTableEntry;
import annis.service.objects.FrequencyTableQuery;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
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

  private Table<String, DateTime, Long> values;
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
      callFunction("showData", dateFormattedMap(values), lastFont, lastFontSize);
    }
  }
  
  
  
  public void drawGraph(FrequencyTable table, String font, 
    float fontSize, FrequencyTableQuery query, FrequencyTableEntry timeEntry)
  {
    values = TreeBasedTable.create();
    
    final int timeColumn = query.indexOf(timeEntry);
    

    for (FrequencyTable.Entry e : table.getEntries())
    {
      String label = getLabelForEntry(e, timeColumn);
      DateTime time = DateTime.parse(e.getTupel()[timeColumn]);
      
      values.put(label, time, e.getCount());
    }
    lastFont = font;
    lastFontSize = fontSize;
    
    setWidth(ADDTIONAL_PIXEL_WIDTH + (PIXEL_PER_VALUE * (values.columnKeySet().size())), Unit.PIXELS);
    
    callFunction("showData", dateFormattedMap(values), lastFont, lastFontSize);
  }
  
  private Map<String, Map<String, Long>> dateFormattedMap(Table<String, DateTime, Long> val)
  {
    DateTimeFormatter format = ISODateTimeFormat.dateTime().withZoneUTC();
    Map<String, Map<String, Long>> result = new LinkedHashMap<>();
    
    for(Table.Cell<String, DateTime, Long> cell : val.cellSet())
    {
      Map<String, Long> rowMap = result.get(cell.getRowKey());
      if(rowMap == null)
      {
        rowMap = new LinkedHashMap<>();
        result.put(cell.getRowKey(), rowMap);
      }
      rowMap.put(cell.getColumnKey().toString(format), cell.getValue());
    }
    
    return result;
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
      callFunction("showData", dateFormattedMap(values), lastFont, lastFontSize);
    }
    return true;
  }
  
  
}
