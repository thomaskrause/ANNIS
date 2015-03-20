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

import annis.gui.SearchUI;
import annis.gui.frequency.FrequencyResultPanel;
import static annis.gui.frequency.FrequencyResultPanel.MAX_NUMBER_OF_CHART_ITEMS;
import annis.gui.objects.FrequencyQuery;
import annis.libgui.Helper;
import annis.libgui.InstanceConfig;
import annis.service.objects.FrequencyTable;
import annis.service.objects.FrequencyTableEntry;
import annis.service.objects.FrequencyTableEntryType;
import com.vaadin.data.Property;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class FrequencyChart extends VerticalLayout
{

  public static final org.slf4j.Logger log = LoggerFactory.getLogger(
    FrequencyChart.class);
  
  public static enum VisType {
    histogram("Histogram"), trend("Trend");
    
    public final String shortName;
    
    VisType(String shortName)
    {
      this.shortName = shortName;
    }    
  }

  public static final int MAX_ITEMS = 25;

  private FrequencyWhiteboard whiteboard;
  
  private final HorizontalLayout toolLayout;
  private final HorizontalLayout optionLayout;
  
  private final ComboBox typeSelection;
  private final OptionGroup histogramOptions;
  private final ComboBox trendOptions;
  private FrequencyTable lastTable;
  private FrequencyQuery lastQuery;

  public FrequencyChart(FrequencyResultPanel freqPanel)
  {
    setSizeFull();
    
    typeSelection = new ComboBox("Visualization type  ");
    typeSelection.addItem(VisType.histogram);
    typeSelection.setItemCaption(VisType.histogram, VisType.histogram.shortName);
    typeSelection.addItem(VisType.trend);
    typeSelection.setItemCaption(VisType.trend, VisType.trend.shortName);
    typeSelection.setNullSelectionAllowed(false);
    typeSelection.setValue(VisType.histogram);
    typeSelection.setNewItemsAllowed(false);
    typeSelection.setWidth("200px");
    typeSelection.addValueChangeListener(new Property.ValueChangeListener()
    {
      @Override
      public void valueChange(Property.ValueChangeEvent event)
      {
        updateType();
      }
    });
    
    optionLayout = new HorizontalLayout();
    optionLayout.setWidth("100%");
    optionLayout.setHeight("-1");
    optionLayout.setMargin(new MarginInfo(false, false, false, true));
    
    
    histogramOptions = new OptionGroup();
    histogramOptions.setHeight("100%");
    histogramOptions.setWidth("-1px");
    histogramOptions.addItem(FrequencyWhiteboard.Scale.LINEAR);
    histogramOptions.addItem(FrequencyWhiteboard.Scale.LOG10);
    histogramOptions.setItemCaption(FrequencyWhiteboard.Scale.LINEAR, "linear scale");
    histogramOptions.setItemCaption(FrequencyWhiteboard.Scale.LOG10, "log<sub>10</sub> scale");
    
    histogramOptions.setHtmlContentAllowed(true);
    histogramOptions.setImmediate(true);
    histogramOptions.setValue(FrequencyWhiteboard.Scale.LINEAR);
   
    histogramOptions.addValueChangeListener(new Property.ValueChangeListener()
    {
      @Override
      public void valueChange(Property.ValueChangeEvent event)
      {
        // redraw graph with right scale
        if (lastTable != null)
        {
          setFrequencyData(lastTable, lastQuery);
        }
      }
    });
    
    trendOptions = new ComboBox("time variable");
    trendOptions.setHeight("100%");
    trendOptions.setWidth("400px");
    trendOptions.setNewItemsAllowed(false);
    trendOptions.setNullSelectionAllowed(false);
    
    toolLayout = new HorizontalLayout(typeSelection, optionLayout);
    toolLayout.setWidth("100%");
    toolLayout.setHeight("-1px");
    toolLayout.setExpandRatio(optionLayout, 1.0f);
    
    addComponent(toolLayout);
    InnerPanel panel = new InnerPanel(freqPanel);
    addComponent(panel);

    setExpandRatio(panel, 1.0f);

  }

  public void setFrequencyData(FrequencyTable table, FrequencyQuery query)
  {
    FrequencyTable clippedTable = table;
    if (clippedTable.getEntries().size() > MAX_NUMBER_OF_CHART_ITEMS)
    {
      List<FrequencyTable.Entry> entries
        = new ArrayList<>(clippedTable.getEntries());

      clippedTable = new FrequencyTable();
      clippedTable.setEntries(entries.subList(0,
        MAX_NUMBER_OF_CHART_ITEMS));
      clippedTable.setSum(table.getSum());
      setCaption(
        "Showing histogram of top " + MAX_NUMBER_OF_CHART_ITEMS + " results, see table below for complete dataset.");
    }
    
    String font = "sans-serif";
    float fontSize = 7.0f; // in pixel
    UI ui = UI.getCurrent();
    if(ui instanceof SearchUI)
    {
      InstanceConfig cfg = ((SearchUI) ui).getInstanceConfig();
      if(cfg != null && cfg.getFont() != null)
      {
        if(cfg.getFrequencyFont() != null)
        {
          font = cfg.getFrequencyFont().getName();
          // only use the font size if given in pixel (since flotr2 can only use this kind of information)
          String size = cfg.getFrequencyFont().getSize();
          if(size != null && size.trim().endsWith("px"))
          {
            fontSize = Float.parseFloat(size.replace("px", "").trim());
            // the label sizes will be multiplied by 1.3 in the Flotr2 library, thus
            // divide here to get the correct size
            fontSize = fontSize/1.3f;
          }
          else
          {
            log.warn("No valid font size (must in in \"px\" unit) given for frequency font configuration. "
              + "The value is {}", fontSize);
          }
        }
        else if(cfg.getFont() != null)
        {
          font = cfg.getFont().getName();
          // only use the font size if given in pixel (since flotr2 can only use this kind of information)
          String size = cfg.getFont().getSize();
          if(size != null && size.trim().endsWith("px"))
          {
            fontSize = Float.parseFloat(size.replace("px", "").trim());
            // the label sizes will be multiplied by 1.3 in the Flotr2 library, thus
            // divide here to get the correct size
            fontSize = fontSize/1.3f;
          }
        }
      }
    }
    
    lastTable = clippedTable;
    lastQuery = query;
    whiteboard.setFrequencyData(clippedTable, (FrequencyWhiteboard.Scale) histogramOptions.
      getValue(), font, fontSize);
    
    updateType();
  }
  
  private void updateType()
  {
    optionLayout.removeAllComponents();
    
    if(typeSelection.getValue() == VisType.histogram)
    {
      optionLayout.addComponent(histogramOptions);
      optionLayout.setComponentAlignment(histogramOptions, Alignment.BOTTOM_LEFT);
    }
    else if(typeSelection.getValue() == VisType.trend)
    {
      optionLayout.addComponent(trendOptions);
      optionLayout.setComponentAlignment(trendOptions, Alignment.BOTTOM_LEFT);
      
      trendOptions.removeAllItems();
      if(lastQuery != null)
      {
        for(FrequencyTableEntry e : lastQuery.getFrequencyDefinition())
        {
          if(e.getType() == FrequencyTableEntryType.meta)
          {
            trendOptions.addItem(e);
          }
        }
        // also add the other ones after the metadata
        for(FrequencyTableEntry e : lastQuery.getFrequencyDefinition())
        {
          if(e.getType() != FrequencyTableEntryType.meta)
          {
            trendOptions.addItem(e);
          }
        }
      }
      if(trendOptions.size() > 0)
      {
        trendOptions.setValue(trendOptions.getItemIds().iterator().next());
      }
    }
  }

  /**
   * This panel allows us to scroll the chart.
   */
  private class InnerPanel extends Panel
  {

    public InnerPanel(FrequencyResultPanel freqPanel)
    {
      setSizeFull();
      
      whiteboard = new FrequencyWhiteboard(freqPanel);
      whiteboard.addStyleName(Helper.CORPUS_FONT_FORCE);
      
      setContent(whiteboard);
    }
  }
}
