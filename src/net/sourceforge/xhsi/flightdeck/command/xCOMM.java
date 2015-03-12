/**
 * xCOMM.java
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

package net.sourceforge.xhsi.flightdeck.command;

import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.BorderFactory;

/**
 * xCOMM
 */
public class xCOMM extends Display.Entry implements MouseListener, ComponentListener {

    /**
     * NODESC
     */
    private final static String NODESC = "foo";

    /**
     * nullMsg
     */
    private Display.XCommEntry nullMsg = new Display.XCommEntry("", NODESC, Color.RED);

    /**
     * panel
     */
    private JPanel panel = new JPanel(new GridBagLayout());

    /**
     * slots
     */
    private int slots;

    /**
     * initialPanelHeight
     */
    int initialPanelHeight = 0;

    /**
     * cons
     */
    private GridBagConstraints cons = new GridBagConstraints(
        1,                           // gridx
        1,                           // gridy
        1,                           // width
        1,                           // height
        1.0,                         // weightx
        1.0,                         // weighty
        GridBagConstraints.EAST,     // anchor
        GridBagConstraints.NONE,     // fill
        new Insets(0, 0, 0, 0),      // insets
        0,                           // ipadx
        0                            // ipady
    );

    /**
     * buttons
     */
    private ArrayList<Display.XCommEntry> messages = new ArrayList<Display.XCommEntry>();

    /**
     * holdTime
     */
    private long holdTime = 0;

    /**
     * init
     */
    JComponent init(Display disp) {
        throw new Error();
    }

    /**
     * init
     */
    JComponent init(Display disp, int x) {
        slots = disp.cols - x; // take all the remaining slots in the row
        nullMsg.initEntry(disp, -1);
        cons.weightx = 1;
        cons.anchor = GridBagConstraints.WEST;
        cons.gridx = 999;
        panel.add(nullMsg.xbtn, cons);
        cons.anchor = GridBagConstraints.EAST;
        cons.weightx = 0;
        panel.setBackground(Color.BLACK);
        panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        panel.addComponentListener(this);
        return panel;
    }

    /**
     * slots
     */
    int slots() {
        return slots;
    }

    /**
     * getWeightx
     */
    double getWeightx() {
        return 1.0; // Large value
    }

    /**
     * update
     */
    void update(Analysis a) {
        setMessages(a.xcb.messages, a.p.xCOMMfont);
    }

    /**
     * setMessages
     */
    void setMessages(ArrayList<Display.XCommEntry> newMessages, float fontAdjust) {
        if (messages != newMessages && System.currentTimeMillis() - holdTime > 5000L) {
            for (Display.XCommEntry m : messages) {
                ((JButton)m.xbtn).removeMouseListener(this);
                panel.remove(m.xbtn);
            }
            messages = newMessages;
            cons.gridx = 0;
            cons.weightx = 0;
            for (Display.XCommEntry m : messages) {
                m.setFontAdjust(fontAdjust);
                m.initEntry(disp, -1);
                ((JButton)m.xbtn).addMouseListener(this);
                panel.add(m.xbtn, cons);
                cons.gridx++;
            }
            panel.validate();
            panel.repaint();
        }
    }

    /**
     * mouseReleased
     */
    public void mouseReleased(MouseEvent event) {
        for (Display.XCommEntry m : messages) {
            if (event.getSource() == m.xbtn && m.desc != NODESC) {
                ArrayList<Display.XCommEntry> newMessgaes = new ArrayList<Display.XCommEntry>();
                newMessgaes.add(new Display.XCommEntry(m.desc, NODESC, m.getForeground()));
                setMessages(newMessgaes, m.getFontAdjust());
                holdTime = System.currentTimeMillis();
                return;
            }
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * componentResized
     */
    public void componentResized(ComponentEvent e) {
        int ph = panel.getHeight();
        if (initialPanelHeight == 0) {
            initialPanelHeight = ph;
        } else {
            for (Display.XCommEntry m : messages) {
                m.fontResized(((float)ph) / initialPanelHeight);
            }
        }
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }
}
