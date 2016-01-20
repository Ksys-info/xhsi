/**
 * Commander.java
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

import java.awt.Point;
import java.awt.Frame;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.util.Date;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.model.Airport;
import net.sourceforge.xhsi.model.Avionics;
import net.sourceforge.xhsi.model.Aircraft;
import net.sourceforge.xhsi.model.ComRadio;
import net.sourceforge.xhsi.model.FMS;
import net.sourceforge.xhsi.model.FMSEntry;
import net.sourceforge.xhsi.model.Localizer;
import net.sourceforge.xhsi.model.ModelFactory;
import net.sourceforge.xhsi.model.NavigationObjectRepository;
import net.sourceforge.xhsi.model.Runway;
import net.sourceforge.xhsi.model.NavigationRadio;
import net.sourceforge.xhsi.flightdeck.mfd.DestinationAirport;

import net.sourceforge.xhsi.flightdeck.GraphicsConfig;


/**
 * Commander
 */
public class Commander extends CmdSubcomponent {

    /**
     * The CmdComponent for this window
     */
    final CmdComponent cc;

    /**
     * The configurator for all Commander windows
     */
    final CmdConfigurator conf;

    /**
     * The version number of the latest configuration
     */
    private int confVersion;

    /**
     * Instrument name
     */
    private final String instName;

    /**
     * The display for this commander
     */
    private Display disp;

    /**
     * The list of window components used in the window for this commander
     */
    private Element[] entryFastList;

    /**
     * The preferred window size
     */
    private Dimension preferredSize;

    /**
     * The constructor
     */
    public Commander(CmdComponent cc, int windowNumber) {
        super(cc.conf.getModelFactory(), cc.cmd_gc);
        this.cc = cc;
        this.conf = cc.conf;
        CmdConfigurator.Config config = conf.getCmdEntryTable(windowNumber);
        instName = config.name;
        preferredSize = config.preferredSize;
        reload(false);
    }

    /**
     * getPreferredSize
     */

    public Dimension getPreferredSize() {
        return preferredSize;
    }

    /**
     * reload
     */
    void reload(boolean validate) {
        cc.removeAll();
        CmdConfigurator.Config con = conf.getCmdEntryTable(instName);
        if (con != null) {
            Display.Entry[] et = con.entries;
            disp = new Display(this, et, "Commander", con.columns, con.bgcolor);
            if (validate) {
                cc.revalidate();
                cc.repaint();
            }
            entryFastList = et;
            confVersion = conf.version;
        } else {
            disp = null;
            cc.frame.setVisible(false);
        }
    }

    /**
     * paint
     */
    public void paint(Graphics2D g2) {
        if (conf.version != confVersion) {
            reload(true);
        }
        if (disp != null) {
            if (!disp.initialized) {
                cc.frame.setBackground(disp.bgcolor);
                g2.setBackground(disp.bgcolor);
                g2.clearRect(0, 0, cmd_gc.frame_size.width, cmd_gc.frame_size.height);
                disp.initialize();
            }
            try {
                Analysis a = conf.getAnalysis();
                for (Element elmt : entryFastList) {
                    elmt.updateElement(a);
                }
                disp.updateEntries();
            } catch(Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Exception " + ex);
            }
        }
    }
}
