/*
 * Copyright 2015 SFB 632.
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
package de.hu_berlin.german.korpling.annis.kickstarter;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class MainFX extends Application
{
  
  private final static Logger log = LoggerFactory.getLogger(MainFX.class);

  @Override
  public void start(Stage primaryStage)
  {

    Parent root;
    try
    {
      root = FXMLLoader.load(getClass().getResource("Browser.fxml"));
      Scene scene = new Scene(root, 300, 250);

      primaryStage.setTitle("ANNIS Kickstarter");
      primaryStage.setScene(scene);
      primaryStage.show();
    }
    catch (IOException ex)
    {
      // TODO: show old kickstarter as fallback or at least a good visually displayed error message
      log.error("Could not start application: is JavaFX correctly installed?", ex);
    }

  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    launch(args);
  }

}
