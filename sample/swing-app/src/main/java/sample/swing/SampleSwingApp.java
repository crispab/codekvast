package sample.swing;

import io.codekvast.javaagent.CodekvastAgent;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import javax.swing.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class SampleSwingApp extends JFrame {
  public static void main(String[] args) {
    initializeCodekvast();

    SampleSwingApp app = new SampleSwingApp();
    app.createGUI();
  }

  private static void initializeCodekvast() {
    AgentConfig config =
        AgentConfigFactory.createTemplateConfig().toBuilder()
            .appName("swing-app")
            .appVersion("from static aspect")
            .codeBase("build/install/swing-app/lib")
            .packages("sample")
            .methodVisibility("private")
            .build();
    CodekvastAgent.initialize(config);
  }

  private void createGUI() {
    // Create and set up the content pane.
    SampleSwingPanel newContentPane = new SampleSwingPanel();
    newContentPane.setOpaque(true);
    setContentPane(newContentPane);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    pack();
    setVisible(true);
  }
}
