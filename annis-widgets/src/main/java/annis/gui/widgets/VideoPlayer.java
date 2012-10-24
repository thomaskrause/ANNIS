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
package annis.gui.widgets;

import annis.gui.widgets.gwt.client.VVideoPlayer;
import com.vaadin.ui.ClientWidget;

/**
 * Audio player that implements the functions needed by ANNIS.
 * @author Thomas Krause <thomas.krause@alumni.hu-berlin.de>
 */
@ClientWidget(VVideoPlayer.class)
public class VideoPlayer extends MediaPlayerBase
{
  public VideoPlayer(String resourceURL, String mimeType)
  {
    super(resourceURL, mimeType);
  }
}
