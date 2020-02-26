package sample.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

class SampleSwingPanel extends JPanel implements ActionListener {
  private static final String ADD_COMMAND = "add";
  private static final String REMOVE_COMMAND = "remove";
  private static final String CLEAR_COMMAND = "clear";
  private final JTextArea textArea;

  SampleSwingPanel() {
    super(new BorderLayout());

    // Create the components.
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

    // Lay everything out.
    JPanel panel = new JPanel(new GridLayout(1, 3));
    panel.add(addButton);
    panel.add(removeButton);
    panel.add(clearButton);
    panel.add(textArea);

    add(panel, BorderLayout.SOUTH);
  }

  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    if (actionEvent.getActionCommand().equals(ADD_COMMAND)) {
      doAdd();
    }
    if (actionEvent.getActionCommand().equals(REMOVE_COMMAND)) {
      doRemove();
    }
    if (actionEvent.getActionCommand().equals(CLEAR_COMMAND)) {
      doClear();
    }
  }

  private void doClear() {
    textArea.setText(null);
  }

  private void doRemove() {
    textArea.append("Removing...\n");
  }

  private void doAdd() {
    textArea.append("Adding...\n");
  }
}
