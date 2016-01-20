/**
 * Display.java
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
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
import javax.swing.Timer;


/**
 * A display for the Commander windows.
 *
 * (It will shortly be clear just how little I know about Swing!)
 */
public class Display implements MouseListener {

    private final static int  INITIAL_MIN_WIDTH  = 40;
    private final static int  INITIAL_MIN_HEIGHT = 20;
    private final static int  INITIAL_FONTSIZE   = 12;
    public  final static Font INITIAL_FONT = new Font("Arial", Font.BOLD, INITIAL_FONTSIZE);

    protected Dimension preferredSize = new Dimension(INITIAL_MIN_WIDTH, INITIAL_MIN_HEIGHT);
    protected double fontScale   = 1;
    protected double firstHeight = 0;

    /**
     * The background color
     */
    final Color bgcolor;

    /**
     * The number of columns
     */
    protected int cols = 4;

    /**
     * List of property entries to be edited
     */
    private final Entry[] entries;

    /**
     * cmd
     */
    private Commander cmd;

    /**
     * initialized
     */
    boolean initialized = false;

    /**
     * repeatTimer
     */
    private Timer repeatTimer = new Timer(0, null);

    /**
     * Display
     */
    public Display(Commander cmd, Entry[] entries, String title, int cols, Color bgcolor) {
        this.cmd     = cmd;
        this.entries = entries;
        this.cols    = cols;
        this.bgcolor = bgcolor;
    }

    /**
     * prt
     */
    private static void prt(Object str) {
        System.out.println(str);
    }

    /**
     * getGBC
     */
    private GridBagConstraints newGBC(int x, int y, int gwidth, double weightx) {
        return new GridBagConstraints(
                                       x,                            // gridx
                                       y,                            // gridy
                                       gwidth,                       // width
                                       1,                            // height
                                       weightx,
                                       1.0,
                                       GridBagConstraints.CENTER,    // anchor
                                       GridBagConstraints.BOTH,      // fill
                                       new Insets(4, 4, 4, 4),       // insets
                                       0,                            // ipadx
                                       0                             // ipady
                                     );
    }

    /**
     * initialize
     */
    boolean initialize() {
        JPanel panel = cmd.cc;
        if (!initialized) {
            initialized = true;
            panel.setBackground(bgcolor);
            int y = 0;
            int x = 0;
            for (Entry entry : entries) {
                JComponent comp = entry.initEntry(this, x);
                int gridwidth   = entry.slots();
                GridBagConstraints cons = newGBC(x, y, gridwidth, entry.getWeightx());
                if (comp != null) {
                    double w = entry.preferredWidth(preferredSize.getWidth());
                    double h = preferredSize.getHeight();
                    comp.setPreferredSize(new Dimension((int)(w * gridwidth), (int) h));
                    comp.addMouseListener(this);
                    panel.add(comp, cons);
                }
                x += gridwidth;
                if (x == cols) {
                    x = 0;
                    y++;
                }
            }
        }
        return initialized;
    }

    /**
     * updateEntries
     */
    public void updateEntries() {
        for (Entry entry : entries) {
            entry.updateEntry(this);
        }
    }

    /**
     * mousePressed
     */
    public void mousePressed(final MouseEvent event) {
        final boolean right = event.getButton() == 3 | event.isControlDown() | event.isMetaDown() | event.isAltDown();
        for (final Entry entry : entries) {
            final boolean res = entry.mouseEvent(event, right);
            if (res) {
                entry.click(event, right, false);
/* TODO
                if (entry.canRepeat()) {
//System.err.println("+Repeat+");
                    repeatTimer = new Timer(10,  // execute every 100 msec
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
//System.err.println("--rpt--");
                                entry.click(event, right, true);
                            }
                        }
                    );
                    repeatTimer.setInitialDelay(500); // start repeating only after 1/2 second
                    repeatTimer.start();
                }
                return;
*/
            }
        }
    }

    public void mouseReleased(MouseEvent event) {
//System.err.println("-Repeat-");
        repeatTimer.stop();
        for (final Entry entry : entries) {
            entry.clickReleased(event);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    // ------------------------------------- Entry -------------------------------------

    /**
     * Entry class
     */
    static abstract class Entry extends Element {

        /**
         * The component returned by initEntry()
         */
        JComponent mainComponent;

        /**
         * The initial font size in the component
         */
        float savedFontSize;

        /**
         * The last fontScale used
         */
        double savedFontScale = 1;

        /**
         * conf
         */
        protected CmdConfigurator conf;

        /**
         * disp
         */
        protected Display disp;

        /**
         * getWeightx
         */
        double getWeightx() {
            return 0.25; // Smaller value
        }

        /**
         * preferredWidth
         */
        double preferredWidth(double dflt) {
            return dflt;
        }

        /**
         * This is the number of button slots the component will take in the frame
         */
        int slots() {
            return 1;
        }

        /**
         * getComponentFontSize
         */
        float getComponentFontSize(JComponent comp) {
            if (comp != null) {
                Font font = comp.getFont();
                if (font == null) {
                    font = getFont();
                    comp.setFont(font);
                }
                if (font != null) {
                    return font.getSize2D();
                }
            }
            return 0;
        }

        /**
         * initEntry
         */
        JComponent initEntry(Display disp, int x) {
            this.disp = disp;
            this.conf = disp.cmd.conf;
            mainComponent = init(disp, x);
            if (savedFontSize == 0) {
                savedFontSize = getComponentFontSize(mainComponent);
            }
            return mainComponent;
        }

        /**
         * scaleFont
         */
        boolean scaleFont(JComponent comp) {
            return scaleFont(comp, 1);
        }

        /**
         * scaleFont
         */
        boolean scaleFont(JComponent comp, float extra) {
            Font font;
            if (savedFontScale != disp.fontScale && (font = comp.getFont()) != null) {
                comp.setFont(font.deriveFont(font.getStyle(), (float)(savedFontSize * disp.fontScale)));
                return true;
            } else {
                return false;
            }
        }

        /**
         * getFont
         */
        Font getFont() {
            Font f = INITIAL_FONT;
            return (getFontAdjust() ==  1) ? f : f.deriveFont(INITIAL_FONTSIZE * getFontAdjust());
        }

        /**
         * updateEntry
         */
        void updateEntry(Display disp) {
            if (savedFontSize > 0) {
                if (scaleFont(mainComponent)) {
                    setMinimumSize();
                }
            }
            update(disp);
            savedFontScale = disp.fontScale;
        }

        /**
         * modifyText
         */
        String modifyText(String str) {
            return (str == null) ? null : str.replace('-', '\u2013');
        }

        /**
         * init
         */
        JComponent init(Display disp, int x) {
            return init(disp);
        }

        /**
         * init
         */
        abstract JComponent init(Display disp);

        /**
         * update
         */
        void update(Display disp) {
        }

        /**
         * setMinimumSize
         */
        public void setMinimumSize() {
            String text = getMininumText();
            if (text.length() > 0 && mainComponent != null) {
                Font f = mainComponent.getFont();
                int width = mainComponent.getFontMetrics(f).stringWidth(text);
              //System.out.println("f="+f+" width=" + width);
                mainComponent.setMinimumSize(new Dimension(width ,0));
            }
        }


        /**
         * getMininumText
         */
        String getMininumText() {
            return "";
        }

        /**
         * mouseEvent
         */
        boolean mouseEvent(MouseEvent event, boolean right) {
            return false;
        }

        /**
         * canRepeat
         */
        boolean canRepeat() {
            return false;
        }

        /**
         * click
         */
        void click(MouseEvent event, boolean right, boolean repeat) {
            click(event, right);
        }

        /**
         * click
         */
        void click(boolean right, boolean repeat) {
            click(right);
        }

        /**
         * click
         */
        void click(MouseEvent event, boolean right) {
            click(right);
        }

        /**
         * click
         */
        void click(boolean right) {
            click();
        }

        /**
         * click
         */
        void click() {
        }

        /**
         * clickReleased
         */
        void clickReleased(MouseEvent event) {
        }
    }

    // ------------------------------------- Label -------------------------------------

    /**
     * Label class
     */
    static class Label extends Entry {

        JButton jlab;

        /**
         * init
         */
        JComponent init(Display disp) {
            //jlab = new JLabel(text, JLabel.TRAILING); // <- don't know why this does not work
            jlab = newButton(getText());
            jlab.setForeground(getForeground());
            jlab.setBackground(getBackground());
            if (getJustify() == RIGHT) {
                jlab.setHorizontalAlignment(JButton.TRAILING);
            }
            return jlab;
        }

        /**
         * update
         */
        void update(Display disp) {
            jlab.repaint(); // I don't know why this is needed
        }

        /**
         * newButton
         */
        JButton newButton(String str) {
            JButton button = new JButton(escape(str));
            button.setFont(getFont());
            button.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            button.setBorderPainted(true);
            button.setFocusPainted(false);
            return button;
        }

        /**
         * update
         */
        void update(Analysis a) {
        }
    }

    // ---------------------------------------- Gap ----------------------------------------

    /**
     * Gap class
     */
    static final class Gap extends Label {

        /**
         * update
         */
        //void update(Analysis a) {
        //}

        /**
         * newButton
         */
        JButton newButton(String str) {
            if (str != null) {
                str = "          ".substring(0, s2int(str, 0));
            }
            JButton button = super.newButton(str);
            button.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            return button;
        }

        /**
         * getWeightx
         */
        double getWeightx() {
            return 0.0; // Smallest value
        }
    }

    // ------------------------------------- TextField -------------------------------------

    /**
     * TextField class
     */
    static abstract class TextField extends Entry {

        JTextField jcomp;
        int width = 4;

        /**
         * init
         */
        JComponent init(Display disp) {
            String text = getText(); // getValue();
            jcomp = new JTextField(width);
            jcomp.setBorder(BorderFactory.createEmptyBorder());
            jcomp.setFont(getFont());
            if (text.startsWith("_") && text.endsWith("_")) {
                text = text.substring(1, text.length() - 1);
                set(text);
                jcomp.setHorizontalAlignment(JTextField.CENTER);
            }
            jcomp.setText(text);
            return jcomp;
        }

        /**
         * update
         */
        void update(Display disp) {
            scaleFont(jcomp);
            jcomp.setText(" " + getText());
        }

        /**
         * getWeightx
         */
        double getWeightx() {
            return 0.75; // Larger value
        }
    }

    // ------------------------------------- NumberField -------------------------------------

    /**
     * NumberField class
     */
    static abstract class NumberField extends TextField {

        JButton up;
        JButton dn;

        /**
         * init
         */
        JComponent init(Display disp) {
            super.init(disp);
            up = newButton("  \u02c4  ");
            dn = newButton("  \u02c5  ");

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.weighty = 1.0;
            c.ipadx   = 0;
            c.ipady   = 0;
            c.insets  = new Insets(0, 0, 0, 0);
            c.gridy   = 0;

            c.gridx   = 0;
            c.weightx = 0.0;
            c.anchor  = GridBagConstraints.EAST;
            c.fill    = GridBagConstraints.VERTICAL;
            panel.add(up, c);

            c.gridx   = 1;
            c.weightx = 1.0;
            c.anchor  = GridBagConstraints.CENTER;
            c.fill    = GridBagConstraints.BOTH;
            panel.add(jcomp, c);

            c.gridx   = 2;
            c.weightx = 0.0;
            c.anchor  = GridBagConstraints.WEST;
            c.fill    = GridBagConstraints.VERTICAL;
            panel.add(dn, c);

            up.addMouseListener(disp);
            dn.addMouseListener(disp);
            jcomp.addMouseListener(disp);

            return panel;
        }

        /**
         * mouseEvent
         */
        boolean mouseEvent(MouseEvent event, boolean right) {
            String res = null;
            if (event.getSource() == up) {
                return true;
            } else if (event.getSource() == dn) {
                return true;
            } else if (event.getSource() == jcomp) {
                return true;
            }
            return false;
        }

        /**
         * click
         */
        void click(MouseEvent event, boolean right, boolean repeat) {
            if (event.getSource() == jcomp) {
                click(right, repeat);
            } else {
                click((event.getSource() == up) ? 1 : -1, right, repeat);
            }
        }

        /**
         * click
         */
        void click(int value, boolean right, boolean repeat) {
            click(value * (right ? 10 : repeat ? 5 : 1));
        }

        /**
         * click
         */
        void click(int value) {
        }

        /**
         * newButton
         */
        JButton newButton(String text) {
            JButton b = new JButton(escape(text));
            b.setFont(getFont());
            b.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBackground(Color.BLACK);
            b.setForeground(Color.WHITE);
            return b;
        }

        /**
         * update
         */
        final void update(Display disp) {
            scaleFont(up);
            scaleFont(dn);
            super.update(disp);
            up.repaint();  // I don't know why this
            dn.repaint();  //      is necessary
        }

        /**
         * The is is the number of button slots is component will take in the pane
         */
        int slots() {
            return 2;
        }

        /**
         * canRepeat
         */
        boolean canRepeat() {
            return true;
        }
    }

    // ------------------------------------- AbsButton -------------------------------------

    /**
     * AbsButton class
     */
    static abstract class AbsButton extends Entry {

        XButton xbtn;

        /**
         * newButton
         */
        XButton newButton() {
            return new XButton(this);
        }

        /**
         * init
         */
        JComponent init(Display disp) {
            xbtn = newButton();
            xbtn.setText(getText());
            return xbtn;
        }

        /**
         * colorBorder
         */
        void colorBorder(Color color) {
            colorBorder(color, 2);
        }

        /**
         * colorBorder(
         */
        void colorBorder(Color color, int width) {
            xbtn.colorBorder(color, width);
        }

        /**
         * mouseEvent
         */
        boolean mouseEvent(MouseEvent event, boolean right) {
            return event.getSource() == xbtn;
        }

    }

    // ------------------------------------- IluminatedButton -------------------------------------

    /**
     * IluminatedButton class. These buttons are black and have illuminated text
     * that is either green (for true) white (for false), or yellow (for armed).
     */
    static abstract class IluminatedButton extends AbsButton implements ComponentListener {

        /**
         * init
         */
        JComponent init(Display disp) {
            if (disp.firstHeight == 0) {
                disp.firstHeight = disp.preferredSize.getHeight();
            }
            return super.init(disp);
        }

        /**
         * newButton
         */
        XButton newButton() {
            XButton b = super.newButton();
            b.addComponentListener(this);
            return b;
        }

        /**
         * setBorder
         */
        void setBorder() {
            Color box = getBorderColor();
            int rate = getBlinkRate();
            if (getBlinking() && rate > 0 && (System.currentTimeMillis() / rate) % 2 == 0) {
                box = null;
            }
            colorBorder(box, getBorderWidth());
            xbtn.setForeground(getForeground());
            xbtn.setBackground(getBackground());
        }

        /**
         * update
         */
        final void update(Display disp) {
            setBorder();
            String bcolor = getButtonColor();
            if (getBlank()) {
                xbtn.setForeground(getBackground());
            } else if (bcolor == ARMED) {
                xbtn.setForeground(getArmedColor());
            } else if (bcolor == TRUE) {
                xbtn.setForeground(getTrueColor());
            } else if (bcolor == FALSE) {
                xbtn.setForeground(getFalseColor());
            }
            xbtn.setText(getText());
        }

        /**
         * getMininumText
         */
        String getMininumText() {
            return "XXXX";
        }

        /**
         * componentResized
         */
        public void componentResized(ComponentEvent e) {
            xbtn.getSize(disp.preferredSize);
            disp.fontScale = disp.preferredSize.getHeight() / disp.firstHeight;
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }
    }


    // ------------------------------------- XCommEntry -------------------------------------

    final static class XCommEntry extends IluminatedButton {

        String desc;

        /**
         * XCommEntry
         */
        XCommEntry(String text, String desc, Color color) {
            setText(text);
            setForeground(color);
            this.desc = desc;
        }

        /**
         * update
         */
        void update(Analysis a) {
        }

        /**
         * fontResized
         */
        public void fontResized(float scale) {
            Font font = xbtn.getFont();
            xbtn.setFont(font.deriveFont(font.getStyle(), (float)(savedFontSize * scale)));
            super.componentResized(null);
        }

        /**
         * equals
         */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof XCommEntry)) {
                return false;
            }
            XCommEntry xc = (XCommEntry)obj;
            return xc.getText().equals(getText()) && xc.desc.equals(desc) && xc.getForeground().equals(getForeground());
        }

        /**
         * equals
         */
        public int hashCode() {
            return getText().hashCode() ^ desc.hashCode() ^ getForeground().hashCode();
        }
    }


    // ------------------------------------- XButton -------------------------------------

    private static class XButton extends JButton {

        Entry entry;

        /**
         * XButton
         */
        XButton(Entry entry) {
            this.entry = entry;
            setFont(entry.getFont());
            setBorder(BorderFactory.createLineBorder(entry.getBackground(), 2, false));
            setBorderPainted(true);
            setFocusPainted(false);
            setForeground(entry.getForeground());
            setBackground(entry.getBackground());
        }

        /**
         * colorBorder(
         */
        void colorBorder(Color color, int width) {
            if (!entry.isXplaneRunning()) {
                color = Color.RED;
                width = 1;
            } else if (color == null) {
                color = entry.getBackground();
                width = 2;
            }
            setBorder(BorderFactory.createLineBorder(color, width, false));
        }
    }
}






