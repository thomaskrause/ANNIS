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

import annis.service.internal.AnnisServiceRunner;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class BrowserController implements Initializable
{

  private final static Logger log = LoggerFactory.getLogger(
    BrowserController.class);

  @FXML
  private WebView webview;

  @FXML
  private Label status;

  @FXML
  private Button debug;

  private boolean wasStarted = false;

  private AnnisServiceRunner runner;

  private final ExecutorService exec = Executors.newCachedThreadPool();

  /**
   * Initializes the controller class.
   *
   * @param url
   * @param rb
   */
  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    exec.submit(() ->
    {
      try
      {
        startService();
        startJetty();

        Platform.runLater(() ->
        {
          WebEngine web = webview.getEngine();
          web.load("http://localhost:8080/annis-gui/");
          
          status.textProperty().setValue("Loaded test");
        });
      }
      catch (Exception ex)
      {
        log.error("Could not start something", ex);
      }
    });

  }

  public void doReset()
  {
    webview.getEngine().load("http://localhost:8080/annis-gui/");
  }

  public void doDebug()
  {
    webview.getEngine().executeScript(
      "if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
  }

  private void startService() throws Exception
  {

    // starts RMI service at bean creation
    runner = new AnnisServiceRunner();
    runner.setUseAuthentification(false);
    runner.start(true);

  }

  private void startJetty() throws Exception
  {
    // disable jetty logging
    org.eclipse.jetty.util.log.Log.setLog(new JettyNoLogger());

    Server jetty = new Server(8080);
    // add context for our bundled webapp
    WebAppContext context = new WebAppContext("./webapp/", "/annis-gui");
    context.setInitParameter("managerClassName",
      "annis.security.TestSecurityManager");
    String webxmlOverrride = System.getProperty("annis.home")
      + "/conf/override-web.xml";//ClassLoader.getSystemResource("webxmloverride.xml").toString();
    List<String> listWebXMLOverride = new LinkedList<>();
    listWebXMLOverride.add(webxmlOverrride);
    context.setOverrideDescriptors(listWebXMLOverride);

    // Exclude some jersey classes explicitly from the web application classpath.
    // If they still exists some automatic dependency resolution of Jersey will
    // fail.
    // Whenever we add new dependencies on jersey classes for the service but 
    // not for the GUI and "Missing dependency" errors occur, add the classes
    // to the server class list
    context.addServerClass("com.sun.jersey.json.");
    context.addServerClass("com.sun.jersey.server.");

    jetty.setHandler(context);

    // start
    jetty.start();
  }

}
