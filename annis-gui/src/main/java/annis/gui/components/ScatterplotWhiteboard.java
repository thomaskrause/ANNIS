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
import org.joda.time.DateTimeConstants;
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
  "flotr2.js", "vaadin://jquery.js", "moment.js", "scatterplot.js"
})
public class ScatterplotWhiteboard extends AbstractJavaScriptComponent implements OnLoadCallbackExtension.Callback
{
  public static final int PIXEL_PER_VALUE = 45;
  public static final int ADDTIONAL_PIXEL_WIDTH = 100;

  public enum DateResolution {
    days,
    months,
    years
  }
  
  private Map<String, Map<String, Long>> lastValues;
  private DateResolution lastResolution = DateResolution.days;
  private String lastFont;
  private float lastFontSize = 10.0f;
  
  private Table<String, DateTime, Integer> selection2rownum;
  
  public ScatterplotWhiteboard(final FrequencyResultPanel freqPanel)
  {  
    setHeight("100%");
    setWidth("200px");
    addStyleName("scatterplot-chart");
    
    addFunction("selectRow", new JavaScriptFunction() {

      @Override
      public void call(JSONArray args) throws JSONException
      {
        String label = args.getString(0);
        DateTime date = DateTime.parse(args.getString(1));
        Integer rowNum = selection2rownum.get(label, date);
        if (rowNum != null)
        {
          freqPanel.selectRow(rowNum);
        }
      }
    });
    
    
    OnLoadCallbackExtension ext = new OnLoadCallbackExtension(this);
    ext.extend((ScatterplotWhiteboard) this);
    
  }
  @Override
  public void beforeClientResponse(boolean initial)
  {
    super.beforeClientResponse(initial);
    if(lastValues != null && lastFont != null)
    {
      callFunction("showData", lastValues, lastFont, lastFontSize, lastResolution.name());
    }
  }
  
  
  
  public void drawGraph(FrequencyTable table, String font, 
    float fontSize, FrequencyTableQuery query, FrequencyTableEntry timeEntry)
  {
    Table<String, DateTime, Long> values = TreeBasedTable.create();
    
    final int timeColumn = query.indexOf(timeEntry);
    
    selection2rownum = TreeBasedTable.create();

    int i=0;
    for (FrequencyTable.Entry e : table.getEntries())
    {
      String label = getLabelForEntry(e, timeColumn);
      DateTime time = DateTime.parse(e.getTupel()[timeColumn]);
      
      values.put(label, time, e.getCount());
      selection2rownum.put(label, time, i);
      i++;
    }
    lastFont = font;
    lastFontSize = fontSize;
    
    setWidth(ADDTIONAL_PIXEL_WIDTH + (PIXEL_PER_VALUE * (values.columnKeySet().size())), Unit.PIXELS);
    
    lastValues = dateFormattedMap(values);
    lastResolution = getResolution(values);
    
    callFunction("showData", lastValues, lastFont, lastFontSize, lastResolution.name());
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
  
  private DateResolution getResolution(Table<String, DateTime, Long> val)
  {
    DateResolution result = DateResolution.years;
    
    for(DateTime t : val.columnKeySet())
    {
      if(result == DateResolution.years && t.getMonthOfYear() != DateTimeConstants.JANUARY)
      {
       result = DateResolution.months;
      }
      if(result == DateResolution.months && t.getDayOfMonth() != 1)
      {
        result = DateResolution.days;
        break;
      }
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
    if(lastValues != null && lastFont != null)
    {
      callFunction("showData", lastValues, lastFont, lastFontSize, lastResolution.name());
    }
    return true;
  }
  
  
}
