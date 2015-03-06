/**
 * PropertiesDialog.java
 *
 * Properties editor dialogue
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.sourceforge.xhsi;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * A very simple property file editor
 */
public class PropertiesDialog extends JDialog implements ActionListener {

    /**
     * panel
     */
    private final PropertiesPanel panel;

    /**
     * PropertiesDialog
     */
    public PropertiesDialog(JFrame owner_frame, PropertiesPanel.Entry[] entries, Properties props, String title, PropertiesPanel.Done doneFtn) {
        super(owner_frame, title);
        panel = new PropertiesPanel(entries, props, doneFtn);
        setResizable(false);
        Container content_pane = getContentPane();
        content_pane.setLayout(new BorderLayout());
        content_pane.add(panel, BorderLayout.CENTER);
        content_pane.add(createOkCancel(), BorderLayout.SOUTH);
        pack();
    }

    /**
     * createOkCancel
     */
    private JPanel createOkCancel() {
        FlowLayout layout     = new FlowLayout();
        JPanel buttons_panel  = new JPanel(layout);
        JButton ok_button     = new JButton("OK");
        JButton cancel_button = new JButton("Cancel");
        ok_button.setActionCommand("OK");
        cancel_button.setActionCommand("Cancel");
        ok_button.addActionListener(this);
        cancel_button.addActionListener(this);
        buttons_panel.add(ok_button);
        buttons_panel.add(cancel_button);
        return buttons_panel;
    }


    /**
     * setVisible
     */
    public void setVisible(boolean value) {
        if (value) {
            panel.updateEntries();
        }
        super.setVisible(value);
    }

    /**
     * actionPerformed
     */
    public void actionPerformed(ActionEvent event) {
        setVisible(false);
        if (event.getActionCommand().equals("OK")) {
            panel.apply();
        }
    }
}


