package sample.webstart;

import se.crisp.codekvast.collector.CodekvastCollector;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.io.FileSystemInvocationDataDumper;
import se.crisp.codekvast.shared.util.ConfigUtils;

import javax.swing.*;
import java.io.File;

public class SampleSwingApp extends JFrame {
    public static void main(String[] args) {
        initializeCodekvast();

        SampleSwingApp app = new SampleSwingApp();
        app.createGUI();
    }

    private static void initializeCodekvast() {
        CollectorConfig config = CollectorConfig
                .builder()
                .appName(ConfigUtils.expandVariables(null, "$APP_NAME"))
                .appVersion("from static aspect")
                .collectorResolutionSeconds(5)
                .methodVisibility("public")
                .codeBase("$APP_HOME/lib")
                .packagePrefixes("sample")
                .verbose(true)
                .dataPath(new File("/tmp/codekvast"))
                .tags("")
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