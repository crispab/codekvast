package sample.webstart;

import javax.swing.*;

public class SampleSwingApp extends JFrame {
    public static void main(String[] args) {
        SampleSwingApp app = new SampleSwingApp();
        app.createGUI();
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