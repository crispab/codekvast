package sample.swing;

import se.crisp.codekvast.agent.collector.CodekvastCollector;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.io.FileSystemInvocationDataDumper;
import se.crisp.codekvast.agent.lib.util.ConfigUtils;

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
        CodekvastCollector.initialize(config, new FileSystemInvocationDataDumper(config, System.err));
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
