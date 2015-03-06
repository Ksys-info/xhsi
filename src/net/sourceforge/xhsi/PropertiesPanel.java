/**
 * PropertiesPanel.java
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
public class PropertiesPanel extends JPanel {

    /**
     * Class with "done()" method to be called when "OK" is clicked
     */
    private final Done doneFtn;

    /**
     * List of property entries to be edited
     */
    private final Entry[] entries;

    /**
     * The properties object
     */
    private final Properties props;

    /**
     * PropertiesPanel
     */
    public PropertiesPanel(Entry[] entries, Properties props, Done doneFtn) {
        this.entries = entries;
        this.props = props;
        this.doneFtn = doneFtn;

        setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        int dialog_line = 0;
        cons.ipadx = 10;
        cons.ipady = 0;
        cons.insets = new Insets(5, 10, 0, 0);
        for (Entry entry : entries) {
            entry.label = new JLabel(entry.desc, JLabel.TRAILING);
            String value = props.getProperty(entry.key);
            if (value == null) {
                value = entry.init.toString();
                props.setProperty(entry.key, value);    // If the user hits cancel we want the default value
            }
            if (entry.init instanceof Boolean) {
                ButtonGroup group = entry.group;
                if (group == null) {
                    JCheckBox cb = new JCheckBox();
                    cb.setSelected(value.equals("true"));
                    entry.jcomp = cb;
                } else {
                    JRadioButtonMenuItem jb = new JRadioButtonMenuItem();
                    jb.setSelected(value.equals("true"));
                    entry.group.add(jb);
                    entry.jcomp = jb;
                }
            } else {
                if (value.startsWith("---")) {
                    entry.jcomp = new JLabel(value); // seporator line
                } else {
                    entry.jcomp = new JTextField(entry.width);
                    ((JTextField)entry.jcomp).setText(value);
                }
            }
            cons.gridx = 0;
            cons.gridy = dialog_line;
            cons.anchor = GridBagConstraints.EAST;
            add(entry.label, cons);
            cons.gridx = 2;
            cons.gridy = dialog_line;
            cons.anchor = GridBagConstraints.WEST;
            add(entry.jcomp, cons);
            dialog_line++;
        }
    }


    /**
     * updateEntries
     */
    public void updateEntries() {
        for (Entry entry : entries) {
            String value = props.getProperty(entry.key);
            JComponent jcomp = entry.jcomp;
            if (jcomp instanceof JCheckBox) {
                ((JCheckBox)jcomp).setSelected("true".equals(value));
            } else if (jcomp instanceof JRadioButtonMenuItem) {
                boolean val = "true".equals(value);
                ((JRadioButtonMenuItem)jcomp).setSelected(val);
            } else if (jcomp instanceof JButton) {
                boolean val = "true".equals(value);
                ((JButton)jcomp).setSelected(val);
            } else if (jcomp instanceof JTextField) {
                ((JTextField)jcomp).setText(value);
            }
        }
    }

    /**
     * apply
     */
    public void apply() {
        //Properties newprops = new Properties();
        //for (Object key : props.keySet()) {
        //    newprops.put(key, props.get(key));
        //}
        Properties newprops = props;
        for (Entry entry : entries) {
            if (entry.init instanceof Boolean) {
                if (entry.jcomp instanceof JCheckBox) {
                    newprops.put(entry.key, ((JCheckBox)entry.jcomp).isSelected() ? "true" : "false");
                } else {
                    newprops.put(entry.key, ((JRadioButtonMenuItem)entry.jcomp).isSelected() ? "true" : "false");
               }
            } else if (entry.jcomp instanceof JTextField) {
                newprops.put(entry.key, ((JTextField)entry.jcomp).getText());
            }
        }
        doneFtn.done(newprops);
    }


    /**
     * Entry class
     */
    public static class Entry {

        public String key;
        public String desc;
        public int width;
        public Object init;
        JLabel label;
        JComponent jcomp;
        ButtonGroup group = null;

        /**
         * Entry
         */
        public Entry(String key, boolean init, String desc, ButtonGroup group) {
            this(key, new Boolean(init), desc, 0);
            this.group = group;
        }

        /**
         * Entry
         */
        public Entry(String key, boolean init, String desc) {
             this(key, new Boolean(init), desc, 0);
        }

        /**
         * Entry
         */
        public Entry(String key, String init, String desc) {
             this(key, init, desc, 4);
        }

        /**
         * Entry
         */
        public Entry(String key, Object init, String desc, int width) {
            this.key = key;
            this.init = init;
            this.desc = desc;
            this.width = width;
        }
    }

    /**
     * Done interface
     */
    public static interface Done {
        public void done(Properties props);
    }
}


