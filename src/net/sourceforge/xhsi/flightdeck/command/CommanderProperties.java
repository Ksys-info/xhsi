/**
 * CommanderProperties.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import net.sourceforge.xhsi.PropertiesPanel;

/**
 * CommanderProperties
 */
public class CommanderProperties extends CommanderPropertiesSuper {

    /**
     * create (read from disk)
     */
    public static CommanderProperties create(String path) {
        Properties props = new Properties();
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                props.load(in);
                in.close();
            } catch (Exception ex) {
                System.out.println("Error reading file: " + file + " ex=" + ex);
            }
        }
        return new CommanderProperties(props);
    }

    /**
     * create (write to disk)
     */
    public static CommanderProperties create(String path, Properties props) {
        File file = new File(path);
        try {
            FileOutputStream out = new FileOutputStream(file);
            props.store(out, "Config for Commander");
            out.close();
        } catch (Exception ex) {
            System.out.println("Error writing file: " + file + " ex=" + ex);
        }
        return new CommanderProperties(props);
    }

    /**
     * Constructor
     */
    private CommanderProperties(Properties props) {
        super(props);
    }

    /**
     * getProperties
     */
    public Properties getProperties() {
        return props;
    }

    /**
     * getString
     */
    private String getString(String key, String dflt) {
        String value = props.getProperty(key);
        return (value != null) ? value : dflt;
    }

    /**
     * getBoolean
     */
    private boolean getBoolean(String key) {
        return "true".equals(props.get(key));
    }

    /**
     * getFloat
     */
    private float getFloat(String key) {
        return getFloat(key, -1);
    }

    /**
     * getFloat
     */
    private float getFloat(String key, float dflt) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (Exception ex) {
            }
        }
        return dflt;
    }

    /**
     * getInteger
     */
    private int getInteger(String key) {
        return (int)getFloat(key);
    }

    /**
     * getEntryTable
     */
    public static PropertiesPanel.Entry[] getEntryTable() {
        return new PropertiesPanel.Entry[] {
            //                         -key-      -default-     -label-
            new PropertiesPanel.Entry("def5",       false,   "Use 5-column internal configuration"),
            new PropertiesPanel.Entry("file",       false,   "Load configuration from \"CMD.config\" file (requires restart)"),
            new PropertiesPanel.Entry("alt_dh",     false,   "Use following decision height setting"),
            new PropertiesPanel.Entry("alt_dh_bug", "500",   "The decision height value"),
            new PropertiesPanel.Entry("warn_no_ap", false,   "Warn when autopilot is disengaged"),
            new PropertiesPanel.Entry("warn_bad_vs",false,   "Warn when autopilot has bad V/S mode"),
            new PropertiesPanel.Entry("warn_pwr",   false,   "Warn of high/low engine power settings"),
            new PropertiesPanel.Entry("warn_bgf",   false,   "Warn of various settings for breaks, gear, and flaps"),
            new PropertiesPanel.Entry("warn_alt",   false,   "Warn of possible V/S errors"),
            new PropertiesPanel.Entry("low_pwr",     "35",   "Low engine power level (percentage)"),
            new PropertiesPanel.Entry("high_pwr",    "85",   "High engine power level (percentage)"),
            new PropertiesPanel.Entry("warn_pitot", false,   "Warn if pitot heater is off"),
            new PropertiesPanel.Entry("warn_aoa",   false,   "Enable AOA warnings"),
            new PropertiesPanel.Entry("aoa_g_p",     "2.0",  "Green positive AOA"),
            new PropertiesPanel.Entry("aoa_y_p",     "4.0",  "Yellow positive AOA"),
            new PropertiesPanel.Entry("aoa_r_p",     "6.0",  "Red positive AOA"),
            new PropertiesPanel.Entry("aoa_g_n",    "-1.0",  "Green negitive AOA"),
            new PropertiesPanel.Entry("aoa_y_n",    "-2.0",  "Yellow negitive AOA"),
            new PropertiesPanel.Entry("aoa_r_n",    "-3.0",  "Red negitive AOA"),
            new PropertiesPanel.Entry("warn_dist",  false,   "Warn if distance to target is increasing"),
            new PropertiesPanel.Entry("warn_hsel",  false,   "Warn autopilot is in heading (HDG) or wing level (WLV) mode"),
            new PropertiesPanel.Entry("warn_vs",    false,   "Warn when climbing and descending"),
            new PropertiesPanel.Entry("vspeed",      "100",  "Vertical speed"),
            new PropertiesPanel.Entry("xCOMMfont",   "2.0",  "Font adjustment for xCOMM"),
        };
    }

    /*
     * Convenient instance variables
     */

    boolean file        = getBoolean("file");
    boolean def5        = getBoolean("def5");
    boolean alt_dh      = getBoolean("alt_dh");
    int     alt_dh_bug  = getInteger("alt_dh_bug");
    boolean warn_no_ap  = getBoolean("warn_no_ap");
    boolean warn_bad_vs = getBoolean("warn_bad_vs");
    boolean warn_pwr    = getBoolean("warn_pwr");
    int     low_pwr     = getInteger("low_pwr");
    int     high_pwr    = getInteger("high_pwr");
    boolean warn_pitot  = getBoolean("warn_pitot");
    boolean warn_aoa    = getBoolean("warn_aoa");
    boolean warn_bgf    = getBoolean("warn_bgf");
    boolean warn_alt    = getBoolean("warn_alt");
    float   aoa_g_p     = getFloat("aoa_g_p");
    float   aoa_y_p     = getFloat("aoa_y_p");
    float   aoa_r_p     = getFloat("aoa_r_p");
    float   aoa_g_n     = getFloat("aoa_g_n");
    float   aoa_y_n     = getFloat("aoa_y_n");
    float   aoa_r_n     = getFloat("aoa_r_n");
    boolean warn_dist   = getBoolean("warn_dist");
    boolean warn_hsel   = getBoolean("warn_hsel");
    boolean warn_vs     = getBoolean("warn_vs");
    float   vspeed      = getFloat("vspeed");
    float   xCOMMfont   = getFloat("xCOMMfont");
    String  powerMode   = getString("powerMode", "FF"); // This is an important default so the internal configs will work
}


/**
 * Sneeky superclass to initilize "props" before the instance variables in
 * CommanderProperties are initialized. This means a new dialogue entry can
 * be created by adding only two lines to this file rather than three :-)
 */
class CommanderPropertiesSuper {

    /**
     * The properties object
     */
    final Properties props;

    /**
     * CommanderPropertiesSuper
     */
    CommanderPropertiesSuper(Properties props) {
        this.props = props;
        for (PropertiesPanel.Entry entry : CommanderProperties.getEntryTable()) {
            String value = props.getProperty(entry.key);
            if (value == null) {
                value = entry.init.toString();
                props.setProperty(entry.key, value); // Load the default
            }
        }
    }
}