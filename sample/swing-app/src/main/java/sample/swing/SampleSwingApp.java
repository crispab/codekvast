package sample.swing;

import io.codekvast.javaagent.CodekvastJavaAgent;
import io.codekvast.javaagent.config.CollectorConfig;
import io.codekvast.javaagent.config.CollectorConfigFactory;
import io.codekvast.javaagent.util.ConfigUtils;

import javax.swing.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class SampleSwingApp extends JFrame {
    public static void main(String[] args) {
        initializeCodekvast();

        SampleSwingApp app = new SampleSwingApp();
        app.createGUI();
    }

    private static void initializeCodekvast() {
        CollectorConfig config = CollectorConfigFactory
                .createTemplateConfig()
                .toBuilder()
                .appName(ConfigUtils.expandVariables(null, "$APP_NAME"))
                .appVersion("from static aspect")
                .codeBase("$APP_HOME/lib")
                .packages("sample")
                .build();
        CodekvastJavaAgent.initialize(config);
    }

    private void createGUI() {
        //Create and set up the content pane.
        SampleSwingPanel newContentPane = new SampleSwingPanel();
        newContentPane.setOpaque(true);
        setContentPane(newContentPane);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }
}
