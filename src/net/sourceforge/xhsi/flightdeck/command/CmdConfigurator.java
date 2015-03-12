/**
 * CmdConfigurator.java
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

import net.sourceforge.xhsi.XHSI;
import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.XHSIPreferences;
import net.sourceforge.xhsi.XHSIInstrument;
import net.sourceforge.xhsi.PropertiesPanel;
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

/**
 * CmdConfigurator
 */
public class CmdConfigurator {

    /**
     * MAX_WINS
     */
    public final static int MAX_WINS = 8;

    /**
     * conf
     */
    private static CmdConfigurator conf;

    /**
     * xhsi
     */
    final XHSI xhsi;

    /**
     * ready
     */
    private boolean ready;

    /**
     * mf
     */
    private ModelFactory mf;

    /**
     * aircraft
     */
    private Aircraft aircraft;

    /**
     * avionics
     */
    private Avionics avionics;

    /**
     * fs
     */
    private FlightState fs = new FlightState();

    /**
     * The variable that holds the current property state
     */
    CommanderProperties cp = CommanderProperties.create("CMD.properties"); // Read

    /**
     * fileMode
     */
    private boolean fileMode;

    /**
     * tables
     */
    private Config[] tables;

    /**
     * version
     */
    int version;

    /**
     * nalysis
     */
    private Analysis analysis;

    /**
     * The number of button columns
     */
    private int columns = 4;

    /**
     * The font size adjuctment for labels
     */
    private float labelAdjust = 0.9f;

    /**
     * The background color
     */
    private static Color backgroundColor = new Color(0x505050);

    /**
     * The font color for labels
     */
    private Color labelColor = new Color(0x00D0D0);

    /**
     * The constructor
     */
    public CmdConfigurator(XHSI xhsi) {
        this.xhsi = xhsi;
        if (conf != null) {
            throw new Error("Only one instance allowed");
        }
        conf = this;
        reload();
        setMissingSizes();

    }

    /**
     * setMissingSizes
     */
    private void setMissingSizes() {
        int highest = findHighestDU();
        XHSIPreferences xp = XHSIPreferences.get_instance();
        int id = XHSIInstrument.CMD_ID;
        for (Config con : tables) {
             if (id > highest) {
                 xp.set_preference("du."+id+".width",  ""+((int)con.preferredSize.getWidth()));
                 xp.set_preference("du."+id+".height", ""+((int)con.preferredSize.getHeight()));
             }
             id++;
        }
    }

    /**
     * findHighestDU
     */
    private int findHighestDU() {
        Properties props = new Properties();
        File file = new File("XHSI.properties");
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                props.load(in);
                in.close();
                for (int i = 0 ; i < 99 ; i++) {
                    if (props.getProperty("du."+i+".width") == null) {
                        return i - 1;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error reading file: " + file + " ex=" + ex);
            }
        }
        return 0;
    }

    /**
     * getInstance
     */
    public static CmdConfigurator getInstance() {
        return conf;
    }

    /**
     * getWindowCount
     */
    public static int getWindowCount() {
        return conf.tables.length;
    }

    /**
     * setModelFactory
     */
    public void setModelFactory(ModelFactory mf) {
        this.mf = mf;
        this.aircraft = mf.get_aircraft_instance();
        this.avionics = aircraft.get_avionics();
        reload(); // Must reload because the initial load has null aircraft/avionics entries
    }

    /**
     * getModelFactory
     */
    public ModelFactory getModelFactory() {
        return mf;
    }

    /**
     * reload
     */
    void reload() {
        cp = CommanderProperties.create("CMD.properties"); // Read
        fileMode = cp.file;
        tables = getAPEntryTables();
        version++;
        initAll();
    }

    /**
     * ready
     */
    public void ready() {
        this.ready = true;
        initAll();
    }

    /**
     * initAll
     */
    private void initAll() {
        if (ready) {
            for (Config cfg : tables) {
                for (Display.Entry entry : cfg.entries) {
                    entry.init();
                }
            }
        }
    }

    /**
     * getCmdEntryTable
     */
    public Config getCmdEntryTable(int n) {
        return tables[n];
    }


    /**
     * getCmdEntryTable
     */
    public Config getCmdEntryTable(String str) {
        for (Config con : tables) {
            if (con.name.equals(str)) {
                return con;
            }
        }
        return null;
    }


    /**
     * getName
     */
    public String getName(int n) {
        return tables[n].name;
    }

    /**
     * getEntryTable
     */
    public PropertiesPanel.Entry[] getEntryTable() {
        return cp.getEntryTable();
    }

    /**
     * getCommanderProperties
     */
    public CommanderProperties getCommanderProperties() {
        return cp;
    }

    /**
     * setProperties
     */
    public void setProperties(Properties props, PropertiesPanel panel) {
        boolean nfm = "true".equals(props.getProperty("file"));
        if (nfm != fileMode) {
            if (JOptionPane.showConfirmDialog(null, "Exit XHSI?") == 0) {
                CommanderProperties.create("CMD.properties", props); // Write
                System.exit(0);
            } else {
                props.setProperty("file", fileMode ? "true" : "false");
                panel.updateEntries();
            }
        }
        cp = CommanderProperties.create("CMD.properties", props); // Write
        reload();
    }

    /**
     * getProperties
     */
    public Properties getProperties() {
        return cp.getProperties();
    }

    /**
     * getProperty
     */
    public String getProperty(String key) {
        return cp.props.getProperty(key);
    }

    /**
     * setProperty
     */
    public void setProperty(String key, String value) {
        cp.props.setProperty(key, value);
        cp = CommanderProperties.create("CMD.properties", cp.props); // Write
    }

    /**
     * incProperty
     */
    public void incProperty(String key, float value) {
        String s = getProperty(key);
        try {
            float f = Float.parseFloat(s) + value;
            if (f >= 100) {
                f = (float) Math.round(f);
            } else {
                f = ((float) Math.round(f*10)) / 10;
            }
            setProperty(key, Float.toString(f));
        } catch (Exception ex) {
        }
    }

    /**
     * setAnalysis
     */
    void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    /**
     * getAnalysis
     */
    Analysis getAnalysis() {
        return analysis;
    }

    /**
     * getFlightState
     */
    FlightState getFlightState() {
        return fs;
    }

    /**
     * prt
     */
    private static void prt(Object str) {
        System.out.println(str);
    }

    /**
     * parseColor
     */
    private Color parseColor(String str) {
        if (!str.startsWith("#")) {
            throw new RuntimeException("Color does not start with '#'" + str);
        }
        try {
            int rgb = Integer.parseInt(str.substring(1), 16);
            return new Color(rgb);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse color " + str);
        }
    }

    /**
     * parseInteger
     */
    private int parseInteger(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse int " + str);
        }
    }

    /**
     * parseFloat
     */
    private float parseFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse float " + str);
        }
    }

    /**
     * newElement
     */
    private Display.Entry newElement(String key) {
        final String bname = Buttons.class.getName();
        Display.Entry entry = newElementPrim(bname.substring(0, bname.length() - 7) + key);
        if (entry != null) {
            return entry;
        }
        entry = newElementPrim(bname+"$"+key);
        if (entry != null) {
            return entry;
        }
        throw new RuntimeException("Cannot find class for: "+key);
    }

    /**
     * newElement
     */
    private Display.Entry newElementPrim(String name) {
        try {
            Class<?> klass = Class.forName(name);
            return (Display.Entry) klass.newInstance();
        } catch (ClassNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Exception creating entry: "+ex);
        }
    }

    /**
     * add
     */
    private Element add(ArrayList<Display.Entry> list, Display.Entry entry) {
        entry.setContext(xhsi, aircraft, avionics);
        list.add(entry);
        return entry;
    }

    /**
     * gap
     */
    private void gap(ArrayList<Display.Entry> list) {
        Element e = add(list, new Display.Gap());
        e.setBackground(backgroundColor);
    }

    /**
     * lab
     */
    private void lab(ArrayList<Display.Entry> list, String text) {
        Element e = add(list, new Display.Label());
        e.setText(text);
        e.setBackground(backgroundColor);
        e.setForeground(labelColor);
        e.setJustifyRight();
        e.setFontAdjust(labelAdjust);
    }

    /**
     * num
     */
    private void num(ArrayList<Display.Entry> list, String[] parms) {
        Color bgcolor = Color.WHITE;
        String name = parms[0];
        for (int i = 1 ; i < parms.length ; i++) {
            String prm = parms[i];
            if (prm.startsWith("#")) {
                bgcolor = parseColor(prm);
            } else {
                name = prm;
            }
        }

        Element e = add(list, newElement(parms[0]));
        e.setText(name);
        e.setBackground(bgcolor);
        e.setForeground(Color.BLACK);
        e.setJustifyLeft();
    }

    /**
     * btn
     */
    private void btn(ArrayList<Display.Entry> list, String[] parms, Color bgcolor) {
        String label = parms[0].substring(1);
        for (int i = 1 ; i < parms.length ; i++) {
            String prm = parms[i];
            if (prm.startsWith("#")) {
                bgcolor = parseColor(prm);
            } else {
                label = prm;
            }
        }
        Element e = add(list, newElement(parms[0]));
        e.setText(label);
        e.setBackground(bgcolor);
    }

    /**
     * getEntryTables
     */
    private Config[] getAPEntryTables() {
        File file = new File("CMD.config");
        if (file.exists() && cp.file) {
            System.out.println("Loading from CMD.config file");
            try {
                String content = new Scanner(file).useDelimiter("\\Z").next(); // ha!
                return makeEntryTables(content);
            } catch (Exception ex) {
                System.out.println("Problem reading autopilot config");
                ex.printStackTrace(System.out);
            }
        }
        System.out.println("Using internal autopilot config");
        return makeEntryTables(cp.def5 ? defaultConfig5 : defaultConfig4);
    }

    /**
     * defaultConfig4
     */
    private String defaultConfig4 =
         "instrument(Autopilot)                                         \n"
        +"background(#283848)                                           \n"
        +"columns(4)                                                    \n"
        +"width(270)                                                    \n"
        +"height(368)                                                   \n"
        +"btn(hNAV1)        btn(hNAV2)  btn(hGPS)    btn(aAP)           \n"
        +"num(nALT)                     btn(aALT)    btn(aVNAV #808080) \n"
        +"num(nHDG)                     btn(aHDG)    btn(aNAV LOC)      \n"
        +"num(nVS )                     btn(aVS)     btn(aWLV)          \n"
        +"num(nTHR)                     btn(aTHR)    btn(aGS)           \n"
        +"txt(cGEAR)        txt(cFLAP)  btn(aBC)     btn(aFLC)          \n"
        +"btn(mAPP)         btn(mVOR)   btn(mNAV)    btn(mARC)          \n"
        +"btn(mPLN PLAN)    btn(sPOS)   btn(sDATA)   gap()              \n"
        +"btn(sARPT)        btn(sWPT)   btn(sVOR)    btn(sNDB)          \n"
        +"btn(sTFC)         lab(RNGE:)  num(nZOOM)                      \n";

    /**
     * defaultConfig5
     */
    private String defaultConfig5 =
         "instrument(Autopilot)                                                      \n"
        +"background(#283848)                                                        \n"
        +"columns(5)                                                                 \n"
        +"width(270)                                                                 \n"
        +"height(368)                                                                \n"
        +"gap()       btn(hNAV1)  btn(hNAV2)         btn(hGPS)    btn(aAP)           \n"
        +"num(nALT)               txt(cALT #B00000)  btn(aALT)    btn(aVNAV #808080) \n"
        +"num(nHDG)               txt(cHDG #B00000)  btn(aHDG)    btn(aNAV LOC)      \n"
        +"num(nVS )               txt(cVS  #B00000)  btn(aVS)     btn(aWLV)          \n"
        +"num(nTHR)               txt(cIAS #B00000)  btn(aTHR)    btn(aGS)           \n"
        +"gap()       lab(AOA:)   txt(cAOA)          txt(cFLAP)   btn(aBC)           \n"
        +"gap()       lab(FF:)    txt(cFF)           txt(cGEAR)   btn(aFLC)          \n"
        +"gap()       lab(Trim:)  txt(cPTCH)         txt(cYAW)    txt(cROLL)         \n"
        +"btn(mAPP)   btn(mVOR)   btn(mNAV)          btn(mARC)    btn(mPLN PLAN)     \n"
        +"btn(sPOS)   btn(sDATA)  lab(RNGE:)         num(nZOOM)                      \n"
        +"btn(sARPT)  btn(sWPT)   btn(sVOR)          btn(sNDB)    btn(sTFC)          \n";


    /**
     * makeEntryTables
     */
    private Config[] makeEntryTables(String config) {
        int width  = CmdGraphicsConfig.INITIAL_PANEL_SIZE + 2*CmdGraphicsConfig.INITIAL_BORDER_SIZE;
        int height = width;
        int instruments = -1;
        ArrayList<Config> res =  new ArrayList<Config>();
        ArrayList<Display.Entry> list = null;
        String name = "xxx";
        String[] lines = config.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0 && !line.startsWith("#")) {
                String[] items = line.split("\\)");
                for (String item : items) {
                    item = item.trim();
                    int pos = item.indexOf('(');
                    if (pos <= 0) {
                        throw new RuntimeException("Cannot find '(' in item" + item);
                    }
                    String op = item.substring(0, pos);
                    String parms = item.substring(pos+1).trim();
                    if (op.equals("gap")) {
                        gap(list);
                    } else {
                        if (parms.length() == 0) {
                            throw new RuntimeException("Cannot parse item" + item);
                        }
                        if (op.equals("lab")) {
                            lab(list, parms);
                        } else if (op.equals("btn")) {
                            btn(list, parms.split("\\s+"), Color.BLACK);
                        } else if (op.equals("txt")) {
                            btn(list, parms.split("\\s+"), backgroundColor);
                        } else if (op.equals("num")) {
                            num(list, parms.split("\\s+"));
                        } else if (op.equals("background")) {
                            backgroundColor = parseColor(parms);
                        } else if (op.equals("columns")) {
                            columns = parseInteger(parms);
                        } else if (op.equals("width")) {
                            width = parseInteger(parms);
                        } else if (op.equals("height")) {
                            height = parseInteger(parms);
                        } else if (op.equalsIgnoreCase("labelColor")) {
                            labelColor = parseColor(parms);
                        } else if (op.equalsIgnoreCase("labelAdjust")) {
                            labelAdjust = parseFloat(parms);
                        } else if (op.equalsIgnoreCase("instrument")) {
                            if (instruments++ >= 0) {
                                if (instruments > MAX_WINS) {
                                    throw new RuntimeException("Too many instruments " + instruments);
                                }
                                res.add(new Config(name, columns, backgroundColor, width, height, list.toArray(new Display.Entry[0])));
                            }
                            list =  new ArrayList<Display.Entry>();
                            name = parms;
                        } else {
                            throw new RuntimeException("Cannot parse item: " + item);
                        }
                    }
                }
            }
        }
        res.add(new Config(name, columns, backgroundColor, width, height, list.toArray(new Display.Entry[0])));
        return res.toArray(new Config[0]);
    }

    /**
     * Config
     */
    static class Config {
        final String name;
        final int columns;
        final Color bgcolor;
        final Dimension preferredSize;
        final Display.Entry[] entries;
        Config(String name, int columns, Color bgcolor, int width, int height, Display.Entry[] entries) {
            this.name = name;
            this.columns = columns;
            this.bgcolor = bgcolor;
            this.preferredSize = new Dimension(width, height);
            this.entries = entries;
        }
    }
}
