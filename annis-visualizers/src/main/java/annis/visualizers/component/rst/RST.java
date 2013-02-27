/*
 * Copyright 2012 Corpuslinguistic working group Humboldt University Berlin.
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
package annis.visualizers.component.rst;

import annis.libgui.VisualizationToggle;
import annis.libgui.visualizers.AbstractVisualizer;
import annis.libgui.visualizers.VisualizerInput;
import net.xeoh.plugins.base.annotations.PluginImplementation;

/**
 * Imitates the RST-diagrams from the RST-Tool (http://www.wagsoft.com/RSTTool/)
 * for a single match.
 *
 * Note, that the rst-xml-format models edges beetwen nucleus and satellite the
 * other way around, than in the common visualization of rst-graphs. So the
 * dominance relation between nodes must be defined in AQL like that:
 *
 * node & node & "Therefore" & #1 >rst[relname="nonvolitional-result"] #2 & #2
 *
 * The AQL-Query above searches for a satellite which contains the token
 * "Therefore" and dominates a node with the rst edge type "nonvolitional-
 * result". Important is, that the first node dominates the second one, also the
 * visualization shows it the other way around.
 *
 * @author Benjamin Weißenfels <b.pixeldrama@gmail.com>
 */
@PluginImplementation
public class RST extends AbstractVisualizer<RSTPanel> {

  @Override
  public String getShortName() {
    return "rst";
  }

  @Override
  public RSTPanel createComponent(VisualizerInput visInput,
          VisualizationToggle visToggle) {
    return new RSTPanel(visInput);
  }
}
