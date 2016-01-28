/**
 * CmdComponent.java
 *
 * Copyright (C) 2007  Georg Gruetter (gruetter@gmail.com)
 * Copyright (C) 2009  Marc Rogiers (marrog.123@gmail.com)
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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;


import net.sourceforge.xhsi.XHSI;
import net.sourceforge.xhsi.XHSIStatus;
import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.XHSIPreferences;
import net.sourceforge.xhsi.PreferencesObserver;

import net.sourceforge.xhsi.model.Aircraft;
import net.sourceforge.xhsi.model.Avionics;
import net.sourceforge.xhsi.model.Observer;
import net.sourceforge.xhsi.model.ModelFactory;



public class CmdComponent extends JPanel implements Observer, PreferencesObserver, ActionListener {

    private static final long serialVersionUID = 1L;
    public static boolean COLLECT_PROFILING_INFORMATION = false;
    public static long NB_OF_PAINTS_BETWEEN_PROFILING_INFO_OUTPUT = 100;
    private static Logger logger = Logger.getLogger("net.sourceforge.xhsi");

    private static int lastWinNumber = 0;
    private static int lowWinNumber = 0;

    // subcomponents --------------------------------------------------------
    ArrayList subcomponents = new ArrayList();
    long[] subcomponent_paint_times = new long[15];
    long total_paint_times = 0;
    long nb_of_paints = 0;
    Graphics2D g2;
    CmdGraphicsConfig cmd_gc;
    ModelFactory model_factory;
    Commander commander;
    Aircraft aircraft;
    Avionics avionics;
    JFrame frame;
    final CmdConfigurator conf = CmdConfigurator.getInstance();
    private XCommBuffers xcb = new XCommBuffers();
    long lastData = 0;
    int winNumber;

    public CmdComponent(int du, JFrame frame, int winNumber) {
        super(new GridBagLayout());
        this.cmd_gc = new CmdGraphicsConfig(this, du);
        this.model_factory = conf.getModelFactory();
        this.aircraft = this.model_factory.get_aircraft_instance();
        this.avionics = this.aircraft.get_avionics();
        this.frame = frame;
        this.cmd_gc.reconfig = true;
        this.winNumber = winNumber;
        update0();
        addComponentListener(cmd_gc);
        subcomponents.add(commander = new Commander(this, winNumber));
        Timer t = new Timer(250, this);
        t.start();
        t.setInitialDelay(2000);
        setVisible(true);
        repaint();
    }

    /**
     * getPreferredSize
     */
    public Dimension getPreferredSize() {
        return commander.getPreferredSize();
    }

    /**
     * actionPerformed
     */
    public void actionPerformed(ActionEvent e) {
      //prt("*AP* " + (System.currentTimeMillis() - lastData) + " w="+winNumber+" vis="+frame.isVisible());
        long now = System.currentTimeMillis();
        if (frame.isVisible()) {
            Analysis a = conf.getAnalysis();
            if ((now - lastData) > 1000 /*&& a != null*/) {
                a.invalidate();
            }
        }
        repaint();
        if (winNumber == lowWinNumber && (now / 250) % 20 == 0) { // Every 5 seconds
            conf.saveAllWindowPositions();
        }
    }

    /**
     * update0
     */
    private void update0() {
        if (winNumber <= lastWinNumber) {
            lowWinNumber = winNumber;
            conf.setAnalysis(new Analysis(conf.getFlightState(), conf.getCommanderProperties(), xcb, aircraft, avionics));
        }
        lastWinNumber = winNumber;
    }

    /**
     * update
     */
    public void update() {
        lastData = System.currentTimeMillis();
        update0();
        if (frame.isVisible()) {
            repaint();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g2 = (Graphics2D)g;
        g2.setRenderingHints(cmd_gc.rendering_hints);
        g2.setStroke(new BasicStroke(2.0f));
        if (XHSIPreferences.get_instance().get_relief_border()) {
            g2.setBackground(cmd_gc.backpanel_color);
        } else {
            g2.setBackground(Color.BLACK);
        }

        // send Graphics object to cmd_gc to recompute positions, if necessary because the panel has been resized or a mode setting has been changed
        cmd_gc.update_config( g2 );


        // rotate the display
        XHSIPreferences.Orientation orientation = XHSIPreferences.get_instance().get_panel_orientation( this.cmd_gc.display_unit );
        if ( orientation == XHSIPreferences.Orientation.LEFT ) {
            g2.rotate(-Math.PI/2.0, cmd_gc.frame_size.width/2, cmd_gc.frame_size.width/2);
        } else if ( orientation == XHSIPreferences.Orientation.RIGHT ) {
            g2.rotate(Math.PI/2.0, cmd_gc.frame_size.height/2, cmd_gc.frame_size.height/2);
        } else if ( orientation == XHSIPreferences.Orientation.DOWN ) {
            g2.rotate(Math.PI, cmd_gc.frame_size.width/2, cmd_gc.frame_size.height/2);
        }


        //g2.clearRect(0, 0, cmd_gc.frame_size.width, cmd_gc.frame_size.height);

        long time = 0;
        long paint_time = 0;

        for (int i=0; i<this.subcomponents.size(); i++) {
            if (CmdComponent.COLLECT_PROFILING_INFORMATION) {
                time = System.currentTimeMillis();
            }

            // paint each of the subcomponents
            ((CmdSubcomponent) this.subcomponents.get(i)).paint(g2);

            if (CmdComponent.COLLECT_PROFILING_INFORMATION) {
                paint_time = System.currentTimeMillis() - time;
                this.subcomponent_paint_times[i] += paint_time;
                this.total_paint_times += paint_time;
            }
        }

        cmd_gc.reconfigured = false;

        nb_of_paints += 1;

        if (CmdComponent.COLLECT_PROFILING_INFORMATION) {
            if (this.nb_of_paints % CmdComponent.NB_OF_PAINTS_BETWEEN_PROFILING_INFO_OUTPUT == 0) {
                logger.info("Paint profiling info");
                logger.info("=[ Paint profile info begin ]=================================");
                for (int i=0;i<this.subcomponents.size();i++) {
                    logger.info(this.subcomponents.get(i).toString() + ": " +
                            ((1.0f*this.subcomponent_paint_times[i])/(this.nb_of_paints*1.0f)) + "ms " +
                            "(" + ((this.subcomponent_paint_times[i] * 100) / this.total_paint_times) + "%)");
                }
                logger.info("Total                    " + (this.total_paint_times/this.nb_of_paints) + "ms \n");
                logger.info("=[ Paint profile info end ]===================================");
            }
        }
    }

    public void componentResized() {
    }

    public void preference_changed(String key) {
        logger.finest("Preference changed");
        this.cmd_gc.reconfig = true;
        repaint();
    }

    public void forceReconfig() {
        componentResized();
        this.cmd_gc.reconfig = true;
        repaint();

    }


    /**
     * prt
     */
    public static void prt(Object str) {
        System.out.println(str);
    }
}
