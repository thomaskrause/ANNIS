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
import annis.libgui.Helper;
import annis.libgui.InstanceConfig;
import annis.service.objects.FrequencyTable;
import com.vaadin.data.Property;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
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
    frequency("Frequency bars"), scatterplot("Scatter Plot");
    
    public final String shortName;
    
    VisType(String shortName)
    {
      this.shortName = shortName;
    }    
  }

  public static final int MAX_ITEMS = 25;

  private FrequencyWhiteboard whiteboard;
  
  private final HorizontalLayout toolLayout;
  
  private final ComboBox typeSelection;
  private final OptionGroup options;
  private FrequencyTable lastTable;

  public FrequencyChart(FrequencyResultPanel freqPanel)
  {
    setSizeFull();
    
    typeSelection = new ComboBox();
    typeSelection.addItem(VisType.frequency);
    typeSelection.setItemCaption(VisType.frequency, VisType.frequency.shortName);
    typeSelection.addItem(VisType.scatterplot);
    typeSelection.setItemCaption(VisType.scatterplot, VisType.scatterplot.shortName);
    typeSelection.setNullSelectionAllowed(false);
    typeSelection.setValue(VisType.frequency);
    typeSelection.setNewItemsAllowed(false);
    typeSelection.setSizeUndefined();
    
    
    options = new OptionGroup();
    options.setSizeUndefined();
    options.addStyleName("horizontal-optiongroup");
    options.addItem(FrequencyWhiteboard.Scale.LINEAR);
    options.addItem(FrequencyWhiteboard.Scale.LOG10);
    options.setItemCaption(FrequencyWhiteboard.Scale.LINEAR, "linear scale");
    options.setItemCaption(FrequencyWhiteboard.Scale.LOG10, "log<sub>10</sub> scale");
    options.setSizeUndefined();
    
    options.setHtmlContentAllowed(true);
    options.setImmediate(true);
    options.setValue(FrequencyWhiteboard.Scale.LINEAR);
   
    options.addValueChangeListener(new Property.ValueChangeListener()
    {
      @Override
      public void valueChange(Property.ValueChangeEvent event)
      {
        // redraw graph with right scale
        if (lastTable != null)
        {
          setFrequencyData(lastTable);
        }
      }
    });
    
    toolLayout = new HorizontalLayout(typeSelection, options);
    toolLayout.setWidth("100%");
    toolLayout.setHeight("-1px");
    toolLayout.setComponentAlignment(typeSelection, Alignment.MIDDLE_LEFT);
    toolLayout.setComponentAlignment(options, Alignment.MIDDLE_LEFT);
    
    addComponent(toolLayout);
    InnerPanel panel = new InnerPanel(freqPanel);
    addComponent(panel);

    setExpandRatio(panel, 1.0f);

  }

  public void setFrequencyData(FrequencyTable table)
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
    whiteboard.setFrequencyData(clippedTable, (FrequencyWhiteboard.Scale) options.
      getValue(), font, fontSize);
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
