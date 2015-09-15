package sample.webstart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SampleSwingPanel extends JPanel implements ActionListener {
    private int newNodeSuffix = 1;
    private static String ADD_COMMAND = "add";
    private static String REMOVE_COMMAND = "remove";
    private static String CLEAR_COMMAND = "clear";
    private final JTextArea textArea;

    public SampleSwingPanel() {
        super(new BorderLayout());

        //Create the components.
        JButton addButton = new JButton("Add");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);

        JButton removeButton = new JButton("Remove");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);

        JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand(CLEAR_COMMAND);
        clearButton.addActionListener(this);

        textArea = new JTextArea();
        textArea.setRows(5);

        //Lay everything out.
        JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.add(addButton);
        panel.add(removeButton);
        panel.add(clearButton);
        panel.add(textArea);

        add(panel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        textArea.append(actionEvent.getActionCommand() + "\n");
    }
}
