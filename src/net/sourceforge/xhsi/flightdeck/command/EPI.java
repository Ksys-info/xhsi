/**
 * EPI.java
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

import java.util.HashSet;
import org.cutre.soft.ExtPlaneInterface;
import org.cutre.soft.epi.data.DataRef;

/**
 * EPI
 */
public class EPI {

    private final static ExtPlaneInterface epi = new ExtPlaneInterface("192.168.1.13", 51000);
    private final static HashSet<String>  subs = new HashSet<String>();

    /**
     * restart
     */
    public synchronized static boolean restart() {
        return epi.restart();
    }

    /**
     * subscribe
     */
    public synchronized static boolean subscribe(String name) {
        boolean res = restart();
        if (res && !subs.contains(name)) {
            subs.add(name);
            epi.includeDataRef(name);
        }
        return res;
    }

    /**
     * getInt
     */
    public static int getInt(String name, int dflt) {
        if (subscribe(name)) {
            try {
                DataRef dr = epi.getDataRef(name);
                if (dr != null) {
                    return Integer.parseInt(dr.getValue()[0]);
                }
            } catch (Exception ex) {
            }
        }
        return dflt;
    }

    /**
     * getFloat
     */
    public static float getFloat(String name, float dflt) {
        if (subscribe(name)) {
            try {
                DataRef dr = epi.getDataRef(name);
                if (dr != null) {
                    return Float.parseFloat(dr.getValue()[0]);
                }
            } catch (Exception ex) {
            }
        }
        return dflt;
    }

    /**
     * setInt
     */
    public static void setInt(String name, int value) {
        if (subscribe(name)) {
            epi.setDataRefValue(name, Integer.toString(value));
        }
    }

    /**
     * setFloat
     */
    public static void setFloat(String name, float value) {
        if (subscribe(name)) {
            epi.setDataRefValue(name, Float.toString(value));
        }
    }

    /**
     * sendCommand
     */
    public static void sendCommand(String cmd) {
        if (restart()) {
            epi.sendCommand(cmd);
        }
    }
}
