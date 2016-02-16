/**
 * Buttons.java
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

import java.awt.Color;
import java.util.Date;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.sourceforge.xhsi.XHSI;
import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.XHSIInstrument;
import net.sourceforge.xhsi.model.Aircraft;
import net.sourceforge.xhsi.model.Avionics;
import net.sourceforge.xhsi.flightdeck.nd.JXMap;
import net.sourceforge.xhsi.flightdeck.mfd.DestinationAirport;
import net.sourceforge.xhsi.model.xplane.XPlaneSimDataRepository;
import org.c7.io.Message;

/**
 * Buttons
 */
public class Buttons {

    // --------------------------------- Radio stuff ---------------------------------

    private static class Radio {
        int mode, div, get_curr, get_stby, set_curr, set_stby;
        Radio(int mode, int div, int get_curr, int get_stby, int set_curr, int set_stby) {
            this.mode     = mode;
            this.div      = div;
            this.get_curr = get_curr;
            this.get_stby = get_stby;
            this.set_curr = set_curr;
            this.set_stby = set_stby;
        }
    }

    private static HashMap<String, Radio> radioMap = new HashMap<String, Radio>();

    static {
        radioMap.put("NAV1", new Radio(
            'N',
            100,
            Avionics.RADIO_NAV1,
            Avionics.RADIO_NAV1_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_NAV1_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_NAV1_STDBY_FREQ_HZ
        ));
        radioMap.put("NAV2", new Radio(
            'N',
            100,
            Avionics.RADIO_NAV2,
            Avionics.RADIO_NAV2_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_NAV2_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_NAV2_STDBY_FREQ_HZ
        ));
        radioMap.put("ADF1", new Radio(
            'A',
            1,
            Avionics.RADIO_ADF1,
            Avionics.RADIO_ADF1_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_ADF1_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_ADF1_STDBY_FREQ_HZ
        ));
        radioMap.put("ADF2", new Radio(
            'A',
            1,
            Avionics.RADIO_ADF2,
            Avionics.RADIO_ADF2_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_ADF2_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_ADF2_STDBY_FREQ_HZ
        ));
        radioMap.put("COM1", new Radio(
            'C',
            100,
            Avionics.RADIO_COM1,
            Avionics.RADIO_COM1_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_COM1_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_COM1_STDBY_FREQ_HZ
        ));
        radioMap.put("COM2", new Radio(
            'C',
            100,
            Avionics.RADIO_COM2,
            Avionics.RADIO_COM2_STDBY,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_COM2_FREQ_HZ,
            XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_COM2_STDBY_FREQ_HZ
        ));
    }

    // --------------------------- getInstrumentFrame --------------------------

        static JFrame getInstrumentFrame(XHSI xhsi, String name) {
            for (XHSIInstrument inst : xhsi.getInstruments()) {
                if (name.equals(inst.du.get_name())) {
                    return inst.frame;
                }
            }
            return null;
        }

    // --------------------------------- Button --------------------------------

    static abstract class Button extends Display.IluminatedButton {
        Color trim_color(float trim) {
            float abs = Math.abs(trim);
            if (abs > 6) return Color.RED;
            if (abs > 3) return Color.YELLOW;
            return Color.GRAY.brighter();
        }
    }

    // --------------------------------- ApSlave --------------------------------

    static abstract class ApSlave extends Button {
        final void update(Analysis a) {
            switch (a.ap_mode) {
                case 0: {
                    noForeground();
                    break;
                }
                /*
                case 1: {
                    if (a.p.warn_no_ap) {
                        box(Color.YELLOW, false);
                        setBorderWidth(1);
                    }
                    update2(a);
                    break;
                }
                */
                default: {
                    update2(a);
                    break;
                }
            }
        }
        abstract void update2(Analysis a);
    }

    // --------------------------------- Number --------------------------------

    static abstract class Number extends Display.NumberField {
        int addRounded(float value, int plus) {
            int round = Math.abs(plus);
            int val = (int)(value + plus);
            return val / round * round;
        }
    }

    // --------------------------------- xAOT --------------------------------

    static class xAOT extends Button {
        xAOT() {
            setButtonColors(Color.GREEN, Color.GRAY);
        }
        void update(Analysis a) {
            set(xhsi.isAlwaysOnTop());
        }
        void click(boolean right) {
            if (right) {
                EPI.sendCommand("cmd once sim/GPS/g430n1_popup");
            } else {
                xhsi.setAlwaysOnTop(!xhsi.isAlwaysOnTop());
            }
        }
    }

    // --------------------------------- x430 --------------------------------

    static class x430 extends Button {
        boolean g430visable = false;
        x430() {
            //setButtonColors(Color.RED.darker(), Color.GRAY);
        }
        void update(Analysis a) {
            set(g430visable);
        }
        void click(boolean right) {
            g430visable = !g430visable;
            if (!right) {
                EPI.sendCommand("cmd once sim/GPS/g430n1_popup");
            }
        }
    }


    // --------------------------------- xJXM --------------------------------

    static class xJXM extends Button {
        void update(Analysis a) {
        }
        void click() {
            JXMap.nextMode();
        }
    }

    // --------------------------------- xVIEW --------------------------------

    static class xVIEW extends Button {

        static String selected;
        String key;

        xVIEW() {
            setButtonColors(Color.RED, Color.GRAY);
        }

        void update(Analysis a) {
            if (key == null) {
                String[] parts = getText().split("\\:", 2);
                if (parts.length == 2) {
                    setText(parts[0]);
                    key = parts[1];
                } else {
                    key = "???";
                    setText(key);
                }
            }
           set(selected == key);
        }

        void click() {
            selected = key;
            EPI.sendCommand("cmd once " + key);
        }
    }

    // --------------------------------- xMFD --------------------------------
/*

    static class xMFD extends Button {

        int[] list = null;
        int next = -1;

        void init() {
            setButtonColors(Color.ORANGE, Color.WHITE);
            String s = getText();
            if (s != null) {
                int pos = s.indexOf('[');
                if (pos >= 0) {
                    setText((pos > 0) ? s.substring(0, pos) : "MFD");
                    s = s.substring(pos+1, s.length() - 1);
                    String[] slist = s.split(",");
                    int count = 0;
                    for (String str : slist) {
                        if (getMode(str) >= 0) {
                            count++;
                        }
                    }
                    list = new int[count];
                    int i = 0;
                    for (String str : slist) {
                        int val = getMode(str);
                        if (val >= 0) {
                            list[i++] = val;
                        }
                    }
                }
            }
        }

        void update(Analysis a) {
            set(DestinationAirport.isForced());
        }

        void click(boolean right) {
            if (!right) {
                settings.setMFD(list[++next % list.length]);
            } else {
                String s = (String)JOptionPane.showInputDialog("Specify IACO code ");
                if ((s != null) && (s.length() > 0)) {
                    DestinationAirport.forceDestination(s);
                } else {
                    DestinationAirport.forceDestination(null);
                }
            }
        }

        int getMode(String str) {
            String s = str.toLowerCase();
            if (s.startsWith("arpt" )) return Avionics.MFD_MODE_ARPT;       // 0
            if (s.startsWith("fpln" )) return Avionics.MFD_MODE_FPLN;       // 1
            if (s.startsWith("rtu"  )) return Avionics.MFD_MODE_RTU;        // 2
            if (s.startsWith("eicas")) return Avionics.MFD_MODE_EICAS;      // 3
            if (s.startsWith("eng"  )) return Avionics.MFD_MODE_ENGINE;     // also 3
            if (s.startsWith("bleed")) return Avionics.MFD_MODE_BLEED;      // 4
            if (s.startsWith("cab"  )) return Avionics.MFD_MODE_CAB_PRESS;  // 5
            if (s.startsWith("press")) return Avionics.MFD_MODE_CAB_PRESS;  // also 5
            if (s.startsWith("elec" )) return Avionics.MFD_MODE_ELEC;       // 6
            if (s.startsWith("hyd"  )) return Avionics.MFD_MODE_HYDR;       // 7
            if (s.startsWith("fuel" )) return Avionics.MFD_MODE_FUEL;       // 8
            if (s.startsWith("apu"  )) return Avionics.MFD_MODE_APU;        // 9
            if (s.startsWith("cond" )) return Avionics.MFD_MODE_COND;       // 10
            if (s.startsWith("air"  )) return Avionics.MFD_MODE_COND;       // also 10
            if (s.startsWith("door" )) return Avionics.MFD_MODE_DOOR_OXY;   // 11
            if (s.startsWith("wheel")) return Avionics.MFD_MODE_WHEELS;     // 12
            if (s.startsWith("fctl" )) return Avionics.MFD_MODE_FCTL;       // 13
            if (s.startsWith("sys"  )) return Avionics.MFD_MODE_SYS;        // 14
            if (s.startsWith("cruis")) return Avionics.MFD_MODE_SYS;        // also 14
            if (s.startsWith("stat" )) return Avionics.MFD_MODE_STATUS;     // 15
            return -1;
        }
    }
*/

    // --------------------------------- xVIS --------------------------------

/*
    static class xVIS extends Button {

        HashMap<String,JFrame> frames = new HashMap<String,JFrame>();
        String[] list = null;
        int entry = 0;
        boolean on = false;

        void init() {
            setButtonColors(Color.WHITE, Color.GRAY);
            String s = getText();
            if (s != null) {
                int pos = s.indexOf('[');
                if (pos >= 0) {
                    setText((pos > 0) ? s.substring(0, pos) : "VIS");
                    s = s.substring(pos+1, s.length() - 1);
                    list = s.split(",");
                    for (XHSIInstrument inst : xhsi.getInstruments()) {
                        String iname = inst.du.get_name();
                        for (String str : list) {
                            for (String wname : str.split("\\+")) {
                                if (wname.equals(iname)) {
                                    frames.put(iname, inst.frame);
                                }
                            }
                        }
                    }
                }
            }
        }

        void update(Analysis a) {
            if (list != null) {
                for (int i = 0 ; i < list.length ; i++) {
                    boolean show = (i == entry && on);
                    for (String winname : list[i].split("\\+")) {
                        JFrame jf = frames.get(winname);
                        if (jf != null) {
                            if (jf.isVisible() != show) { // This test seems to be needed
                                jf.setVisible(show);
                            }
                        }
                    }
                }
                set(on);
            }
        }

        void click(boolean right) {
            if (right) {
                on = false;
            } else {
                if (!on) {
                    on = true;
                } else {
                    entry = ++entry % list.length;
                }
            }
        }
    }

*/

    // --------------------------------- xVIS --------------------------------

    static class xVIS extends Button {

        final static int OFF = -1;
        final static int ON  = 1;
        static HashMap<String,Integer> wstate = new HashMap<String,Integer>();

        String[] list = null;
        int entry = 0;
        int lastEntry = 0;

        void init() {
            setButtonColors(Color.ORANGE, Color.WHITE);
            String s = getText();
            if (s != null) {
                int pos = s.indexOf('[');
                if (pos >= 0) {
                    setText((pos > 0) ? s.substring(0, pos) : "VIP");
                    s = s.substring(pos+1, s.length() - 1);
                    list = s.split(",");
                    //int i = 0;
                    //for (String st : list) {
                    //    System.out.println("["+i+"]="+st);
                    //    i++;
                    //}
                }
            }
        }

        void update(Analysis a) {
            if (list != null) {
                if (lastEntry  != entry) {
                    boolean wasMfs = isMfd(list[lastEntry]);
                    boolean nowMfs = isMfd(list[entry]);
                    if (!nowMfs || !wasMfs) {
                        setVisible(list[lastEntry], false);
                    }
                    setVisible(list[entry], true);
                    lastEntry = entry;
                }
                set(list[entry].equals("arpt") && DestinationAirport.isForced());
            }
        }

        void setVisible(String sx, boolean value) {
            for (String s : sx.split("\\+")) {
                setVisible0(s, value);
            }
        }

        void setVisible0(String str, boolean value) {
            int mfd = mfdMode(str);
            String winname = (mfd == -1) ? str : "MFD";
            Integer wsvalue = (value) ? ((mfd == -1) ? ON : mfd) : OFF;
            if (!wsvalue.equals(wstate.get(winname))) {
                wstate.put(winname, wsvalue);
                setVisible(winname, mfd, value);
            }
        }

        void setVisible(String winname, int mfd, boolean value) {
            for (XHSIInstrument inst : xhsi.getInstruments()) {
                if (winname.equals(inst.du.get_name())) {
                    JFrame jf = inst.frame;
                    if (jf.isVisible() != value) { // This test seems to be needed
                        jf.setVisible(value);
                    }
                    if (value && mfd >= 0) {
                        settings.setMFD(mfd);
                    }
                    return;
                }
            }
            // If not found here it must be an external program like xhid or xpd
            if (XHSI.SENDMESSAGES) {
                try {
                    Message.writeTo("xhsi", winname, (value) ? "show" : "hide");
                } catch (Exception ex) {
                    System.out.println("Message send exception: "+ex);
                }
            }
        }

        void click(boolean right) {
            if (right) {
                if (list[entry].equals("arpt")) {
                    String s = (String)JOptionPane.showInputDialog("Specify IACO code ");
                    if ((s != null) && (s.length() > 0)) {
                        DestinationAirport.forceDestination(s);
                    } else {
                        DestinationAirport.forceDestination(null);
                    }
                }
            } else {
                entry = ++entry % list.length;
            }
        }

        boolean isMfd(String str) {
            return mfdMode(str) >= 0;
        }

        int mfdMode(String str) {
            String s = str.toLowerCase();
            if (s.startsWith("arpt" )) return Avionics.MFD_MODE_ARPT;       // 0
            if (s.startsWith("fpln" )) return Avionics.MFD_MODE_FPLN;       // 1
            if (s.startsWith("rtu"  )) return Avionics.MFD_MODE_RTU;        // 2
            if (s.startsWith("lower")) return Avionics.MFD_MODE_EICAS;      // 3
            if (s.startsWith("eng"  )) return Avionics.MFD_MODE_ENGINE;     // also 3
            if (s.startsWith("bleed")) return Avionics.MFD_MODE_BLEED;      // 4
            if (s.startsWith("cab"  )) return Avionics.MFD_MODE_CAB_PRESS;  // 5
            if (s.startsWith("press")) return Avionics.MFD_MODE_CAB_PRESS;  // also 5
            if (s.startsWith("elec" )) return Avionics.MFD_MODE_ELEC;       // 6
            if (s.startsWith("hyd"  )) return Avionics.MFD_MODE_HYDR;       // 7
            if (s.startsWith("fuel" )) return Avionics.MFD_MODE_FUEL;       // 8
            if (s.startsWith("apu"  )) return Avionics.MFD_MODE_APU;        // 9
            if (s.startsWith("cond" )) return Avionics.MFD_MODE_COND;       // 10
            if (s.startsWith("air"  )) return Avionics.MFD_MODE_COND;       // also 10
            if (s.startsWith("door" )) return Avionics.MFD_MODE_DOOR_OXY;   // 11
            if (s.startsWith("wheel")) return Avionics.MFD_MODE_WHEELS;     // 12
            if (s.startsWith("fctl" )) return Avionics.MFD_MODE_FCTL;       // 13
            if (s.startsWith("sys"  )) return Avionics.MFD_MODE_SYS;        // 14
            if (s.startsWith("cruis")) return Avionics.MFD_MODE_SYS;        // also 14
            if (s.startsWith("stat" )) return Avionics.MFD_MODE_STATUS;     // 15
            return -1;
        }
    }


    // --------------------------------- xTIME --------------------------------

    static class xTIME extends Button {

        boolean zulu = false;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        xTIME() {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        void update(Analysis a) {
            long time = zulu ? a.time_zulu : a.time_local;
            set(sdf.format(new Date(time)) + (zulu ? "z" : ""));
        }

        void click() {
            zulu = !zulu;
        }
    }

    // --------------------------------- xSTPW --------------------------------

    static class xSTPW extends Button {

        long now;
        long time = 0;
        boolean running = false;
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

        void update(Analysis a) {
            now = a.time_flight;
            color(a);
            set(sdf.format(new Date(running ? now - time : time)));
        }

        void click(boolean right) {
            if (right) {
                time = running ? time : 0;
            } else {
                running = !running;
                time = now - time;
            }
        }

        void color(Analysis a) {
            Color color = running ? Color.ORANGE : (time == 0) ? Color.GRAY : Color.GREEN;
            if (getForeground() != color) {
                color(color);
            }
        }
    }

    // --------------------------------- xPPWR --------------------------------

    static class xPPWR extends Button {

        void update(Analysis a) {
            color((a.pwr_high != null || a.pwr_low != null) ? Color.GREEN : Color.WHITE);
            set(a.pwr_pcStr);
        }

        void click(boolean right) {
        }
    }

    // --------------------------------- xPSET --------------------------------

    static class xPSET extends xPPWR {

        String tag;
        Analysis a;

        void update(Analysis a) {
            if (tag == null) {
                tag = getText();
            }
            setBackground(a.p.powerMode.equals(tag) ? Color.RED : Color.BLACK);
            set(a.getPower(tag));
            this.a = a;
        }

        void click(boolean right) {
            conf.setProperty("powerMode", tag);
            float pwr = a.getPower(tag, true);
            if (right) {
                conf.setProperty("powerMax"+tag, ""+pwr);
                if ("TRQ".equals(tag))  {
                    avionics.set_max_trq_override(pwr);
                }
            }
        }
    }

    // --------------------------------- xPROP --------------------------------

    static class xPROP extends Number { // Modifies a floating point property value

        String key;
        float mult = 1;

        void update(Analysis a) {
            if (key == null) {
                String[] s = getText().split("-", 2); // "<name>[-<multiplier>]"
                key = s[0];
                if (s.length > 1) {
                    try {
                        mult = Float.parseFloat(s[1]);
                    } catch (Exception  ex) {
                    }
                }
            }
            String s = conf.getProperty(key);
            set((s == null) ? "-1" : s);
        }

        void click(int value) {
            conf.incProperty(key, value * mult);
        }
    }

    // --------------------------------- rCURR --------------------------------

    static class rCURR extends Button {

        String key;
        float freq;
        Radio radio;
        boolean adf;

        void update(Analysis a) {
            if (key == null) {
                key = getText();
                if (key != null) {
                    radio = radioMap.get(key);
                    if (radio != null) {
                        adf = radio.mode == 'A';
                    }
                }
            }
            if (radio != null) {
                freq = avionics.get_radio_freq(radio.get_curr) ;
                if (freq > 0) {
                    set(String.format(adf ? "%.0f" : "%.2f", freq / radio.div));
                }
            }
        }

        void click() {
            if (radio != null && freq > 0) {
                float sdby = avionics.get_radio_freq(radio.get_stby);
                avionics.sendDataPoint(radio.set_stby, (int)freq);
                avionics.sendDataPoint(radio.set_curr, (int)sdby);
            }
        }
    }

    // --------------------------------- rSDBY --------------------------------

    static class rSDBY extends Number {

        String key;
        float freq;
        Radio radio;
        boolean adf;
        boolean com;

        void update(Analysis a) {
            if (key == null) {
                key = getText();
                if (key != null) {
                    radio = radioMap.get(key);
                    if (radio != null) {
                        adf = radio.mode == 'A';
                        com = radio.mode == 'C';
                    }
                }
            }
            if (radio != null) {
                freq = avionics.get_radio_freq(radio.get_stby) / radio.div;
                if (freq > 0) {
                    if (adf) {
                        set(String.format("%.0f", freq));
                    } else {
                        String s = String.format("%.2f", freq);
                        set(s);
                        if (s.length() > 5 && (s.charAt(5) == '2' || s.charAt(5) == '7')) {
                            freq += 0.005;
                        }
                    }
                }
            }
        }

        void click(int val, boolean right, boolean repeat) {
            if (radio != null && freq > 0) {
                float value = (float) val;
                if (adf) {
                    if (repeat) {  // mainly useful for ADF
                        value *= 10;
                    } else if (!right) {
                        value *= 100;
                    }
                } else {
                    if (right) {
                        value *= (com ? 2.5f : 5f);
                    } else {
                        value *= 100;
                    }
                }

                float newval = s2float(getText(), -1) * radio.div + value;
                if (newval > 0) {
                    avionics.sendDataPoint(radio.set_stby, (int)newval);
                }
            }
        }
    }

    // --------------------------------- rXPDR --------------------------------

    static class rXPDR extends Button {

        private final static String mode[] = { "OFF", "STBY", "ON", "TA", "TA/RA" };

        String key;
        int oper = -1;
        String xpdr;

        void update(Analysis a) {
            if (key == null) {
                key = getText();
                oper = (int)s2float(key, -1);
                if (oper < 0) {
                    oper = (int)key.charAt(0);
                }
            }
            xpdr = a.xpdr;
            switch (oper) {
                case 0: case 1: case 2: case 3: {
                    set(""+xpdr.charAt(oper));
                    break;
                }
                case 'I': {
                    set(key);
                    break;
                }
                default: {
                    set(mode[this.avionics.transponder_mode()]);
                    break;
                }
            }
        }

        void click(boolean right) {
            int val = right ? -1 : 1;
            switch (oper) {
                case 0: case 1: case 2: case 3: {
                    byte[] b = xpdr.getBytes();
                    int ch = b[oper] - '0';
                    ch += val;
                    b[oper] = (byte)('0' + (ch & 0x7));
                    float newval = s2float(new String(b), 7777.0f);
                    avionics.sendDataPoint(XPlaneSimDataRepository.SIM_COCKPIT_RADIOS_TRANSPONDER_CODE, (int)newval);
                    break;
                }
                case 'I': {
                    EPI.sendCommand("cmd once sim/transponder/transponder_ident");
                    break;
                }
                default: {
                    avionics.set_xpdr_mode(Math.abs(avionics.transponder_mode() + val) % mode.length);
                    break;
                }
            }
        }
    }

    // --------------------------------- gDTO --------------------------------

    static class gDTO extends Button {
        void update(Analysis a) {
        }
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_direct");
        }
    }

    // --------------------------------- gMENU --------------------------------

    static class gMENU extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_menu");
        }
    }

    // --------------------------------- gCLR --------------------------------

    static class gCLR extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_clr");
        }
    }

    // --------------------------------- gENT --------------------------------

    static class gENT extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_ent");
        }
    }

    // --------------------------------- gCDI --------------------------------

    static class gCDI extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_cdi");
        }
    }

    // --------------------------------- gOBS --------------------------------

    static class gOBS extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_obs");
        }
    }

    // --------------------------------- gMSG --------------------------------

    static class gMSG extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_msg");
        }
    }

    // --------------------------------- gFPL --------------------------------

    static class gFPL extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_fpl");
        }
    }

    // --------------------------------- gVNAV --------------------------------

    static class gVNAV extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_vnav");
        }
    }

    // --------------------------------- gPROC --------------------------------

    static class gPROC extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_proc");
        }
    }

    // --------------------------------- gVFF --------------------------------

    static class gVFF extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_nav_ff");
        }
    }

    // --------------------------------- gCFF --------------------------------

    static class gCFF extends gDTO {
        void click() {
            EPI.sendCommand("cmd once sim/GPS/g430n1_com_ff");
        }
    }

    // --------------------------------- gZOOM --------------------------------

    static class gZOOM extends Number {
        void update(Analysis a) {
        }
        void click(int value) {
            if (value > 0) {
                EPI.sendCommand("cmd once sim/GPS/g430n1_zoom_in");
            } else {
                EPI.sendCommand("cmd once sim/GPS/g430n1_zoom_out");
            }
        }
    }

    // --------------------------------- gFREQ --------------------------------

    static class gFREQ extends gZOOM {
        void click(int val, boolean right, boolean repeat) {            // Up / Down buttons
            if (right) {
                if (val > 0) {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_coarse_down");
                } else {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_coarse_up");
                }
            } else {
                if (val > 0) {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_fine_down");
                } else {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_fine_up");
                }
            }
        }
        void click() {                                                  // Center label
            EPI.sendCommand("cmd once sim/GPS/g430n1_nav_com_tog");
        }
    }

    // --------------------------------- gCHPG --------------------------------

    static class gCHPG extends gZOOM {
        void click(int val, boolean right, boolean repeat) {            // Up / Down buttons
            if (right) {
                if (val > 0) {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_chapter_dn");
                } else {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_chapter_up");
                }
            } else {
                if (val > 0) {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_page_dn");
                } else {
                    EPI.sendCommand("cmd once sim/GPS/g430n1_page_up");
                }
            }
        }
        void click() {                                                  // Center label
            EPI.sendCommand("cmd once sim/GPS/g430n1_cursor");
        }

    }

    // --------------------------------- hGPS --------------------------------

    static class hGPS extends Button {

        private final static Color LBLUE  = new Color(0x50FFFF);
        private final static Color DBLUE  = Color.BLUE.darker();
        private final static Color LGRAY  = new Color(0xCCCCCC);
        private final static Color DGRAY  = Color.GRAY.darker().darker();
        private final static Color LGREEN = Color.GREEN;
        private final static Color DGREEN = Color.GREEN.darker().darker();

        void update(Analysis a) {
            set(a.h_src == Avionics.HSI_SOURCE_GPS);
            setColorsFor(Avionics.EFIS_RADIO_NAV);
        }

        void click() {
            settings.setSource(Avionics.HSI_SOURCE_GPS);
        }

        void setColorsFor(int value) {
            switch (value) {
                case Avionics.EFIS_RADIO_ADF: setButtonColors(LBLUE,  DBLUE,  null);  break;
                case Avionics.EFIS_RADIO_NAV: setButtonColors(LGREEN, DGREEN, null);  break;
                default:                      setButtonColors(LGRAY,  DGRAY,  null);  break;
            }
        }
    }

    // --------------------------------- hNAV1 --------------------------------

    static class hNAV1 extends hGPS {
        void update(Analysis a) {
            set(a.h_src == Avionics.HSI_SOURCE_NAV1);
            setColorsFor(avionics.efis_radio1());
        }
        void click(boolean right) {
            if (right) {
                avionics.set_radio1(nextOption(avionics.efis_radio1()));
            } else {
                settings.setSource(Avionics.HSI_SOURCE_NAV1);
            }
        }
        int nextOption(int value) {
            return (value + 1) % 3;
        }
    }

    // --------------------------------- hNAV2 --------------------------------

    static class hNAV2 extends hNAV1 {
        void update(Analysis a) {
            set(a.h_src == Avionics.HSI_SOURCE_NAV2);
            setColorsFor(avionics.efis_radio2());
        }
        void click(boolean right) {
            if (right) {
                avionics.set_radio2(nextOption(avionics.efis_radio2()));
            } else {
                settings.setSource(Avionics.HSI_SOURCE_NAV2);
            }
        }
    }

    // --------------------------------- aAP --------------------------------

    static boolean ap_show_mode_1 = true;

    static class aAP extends Button {
        int ap_mode;
        void update(Analysis a) {
            ap_mode = a.ap_mode;
            set(ap_mode == 2, ap_mode == 1 && ap_show_mode_1);
            if (a.p.warn_no_ap && ap_mode < 2) {
                Color c = a.fs.autopilotWasOn ? Color.RED : Color.YELLOW;
                box(c, a.fs.autopilotWasOn);
            }
        }
        void click(boolean right) {
            if ((right && ap_mode == 1) || (ap_show_mode_1 && ap_mode == 0)) {
                EPI.sendCommand("cmd once sim/autopilot/fdir_toggle");
            } else {
                EPI.sendCommand("cmd once sim/autopilot/servos_toggle");
            }
        }
    }

    // --------------------------------- aFD --------------------------------

    static class aFD extends Button {
        int ap_mode;
        void update(Analysis a) {
            ap_mode = a.ap_mode;
            set(ap_mode == 1);
            ap_show_mode_1 = false;

        }
        void click() {
            if (ap_mode < 2) {
                EPI.sendCommand("cmd once sim/autopilot/fdir_toggle");
            }
        }
    }

    // --------------------------------- aVNAV --------------------------------

    static class aVNAV extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_vnav_on(), avionics.ap_vnav_arm());
        }
        void click() {
            EPI.setInt("sim/operation/override/override_autopilot", 1);
            EPI.setInt("sim/cockpit/autopilot/autopilot_state", 4096);   // Toggle vnav
            EPI.setInt("sim/operation/override/override_autopilot", 0);
        }
    }

    // --------------------------------- aTHR --------------------------------

    static class aTHR extends ApSlave {
        void update2(Analysis a) {
            int state = avionics.autothrottle_on() ? 2 : avionics.autothrottle_enabled() ? 1 : 0;
            set(state == 2, state == 1);
            if (state < 2) {
                box(Color.YELLOW, a.fs.autopilotWasOn && !avionics.ap_flch_on());
            }
        }
        void click() {
            avionics.send_ap_key_press(3);
        }
    }

    // --------------------------------- aFLC --------------------------------

    static class aFLC extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_flch_on());
            box(a.a_flc_box);
        }
        void click() {
            avionics.send_ap_key_press(4);
        }
    }

    // --------------------------------- aHDG --------------------------------

    static class aHDG extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_hdg_sel_on());
            if (a.hsel_error) {
                box(Color.YELLOW, false);
            }
        }
        void click() {
            avionics.send_ap_key_press(5); // sim/autopilot/heading
        }
    }

    // --------------------------------- aVS --------------------------------

    static class aVS extends ApSlave {
        void update2(Analysis a) {
            box(a.a_vs_box, a.warn_aoa || a.error_aoa || (a.vvi_warn && a.ap_vs_on));
            set(avionics.ap_vs_on(), avionics.ap_vs_arm());
        }
        void click() {
            avionics.send_ap_key_press(6);
        }
    }

    // --------------------------------- aNAV --------------------------------

    static class aNAV extends ApSlave {
        void update2(Analysis a) {
            set(a.nav, avionics.ap_vorloc_arm() || avionics.ap_lnav_arm());
        }
        void click() {
            avionics.send_ap_key_press(7);
        }
    }

    // --------------------------------- aGS --------------------------------

    static class aGS extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_gs_on(), avionics.ap_gs_arm());
            if (a.warn_gs) {
                box(Color.YELLOW, true);
            }
        }
        void click() {
            avionics.send_ap_key_press(9);
        }
    }

    // --------------------------------- aBC --------------------------------

    static class aBC extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_bc_on(), avionics.ap_bc_arm());
        }
        void click() {
            avionics.send_ap_key_press(10);
        }
    }

    // --------------------------------- aALT --------------------------------

    static class aALT extends ApSlave {
        void update2(Analysis a) {
            set(avionics.ap_alt_hold_on(), avionics.ap_alt_hold_arm());
            box(a.a_alt_box);
        }
        void click() {
            avionics.send_ap_key_press(11);
        }
    }

    // --------------------------------- aWLV --------------------------------

    static class aWLV extends ApSlave {
        void update2(Analysis a) {
            set(a.wlv);
            if (a.p.warn_hsel && a.wlv) {
                box(Color.YELLOW, false);
            }
        }
        void click() {
            if (avionics.ap_hdg_sel_on()) {
                avionics.send_ap_key_press(5);                                  // sim/autopilot/heading
            } else if (avionics.ap_vorloc_on() || avionics.ap_lnav_on()) {
                avionics.send_ap_key_press(7);                                  // sim/autopilot/nav
            }
        }
    }

    // --------------------------------- nHDG --------------------------------

    static class nHDG extends Number {
        void update(Analysis a) {
            set(a.ap_hdg);
        }
        void click(int value) {
            avionics.send_ap_heading(addRounded(avionics.heading_bug(), value));
        }
    }

    // --------------------------------- nTHR --------------------------------

    static class nTHR extends Number {
        void update(Analysis a) {
            float speed = avionics.autopilot_speed();
            set(getMachMode() ? String.format("%.3f", speed) : Integer.toString((int)speed));
        }

        void click(int value, boolean right, boolean repeat) {              // Up & down buttons
            float speed = avionics.autopilot_speed();
            if (getMachMode()) {
                speed += (right ? 0.01f : 0.001f) * value;
            } else {
                speed = addRounded(speed, (right ? 50 : 5) * value);
            }
            avionics.send_ap_airspeed(speed);
        }

        void click(boolean right) {                                         // Number display
            if (right) {
                avionics.send_ap_key_press(1); // toggle mach mode
            }
        }

        boolean getMachMode() {
            return avionics.autopilot_speed_is_mach();
        }
    }

    // --------------------------------- nVS --------------------------------

    static class nVS extends Number {
        void update(Analysis a) {
            set(a.ap_vvi);
        }
        void click(int value) {
            avionics.send_ap_vv(addRounded(avionics.autopilot_vv(), value * 100));
        }
    }

    // --------------------------------- nALT --------------------------------

    static class nALT extends Number {
        void update(Analysis a) {
            set(a.ap_alt);
        }
        void click(int value) {
            avionics.send_ap_altitude(addRounded(avionics.autopilot_altitude(), value * 100));
        }
    }

    // --------------------------------- nZOOM --------------------------------

    static class nZOOM extends Number {
        void update(Analysis a) {
            set(settings.getZoomRange(avionics.map_range_index(), avionics.map_zoomin()));
        }

        void click(boolean right) {                                         // Number display
            if (right) {
                JXMap.setMode(JXMap.OFF);
            } else {
                int mode = JXMap.getMode();
                JXMap.setMode(mode == JXMap.STREET ? JXMap.SATELLITE : JXMap.STREET);
            }
        }


        void click(int value, boolean right, boolean repeat) {              // Up & down buttons
            if (value > 0) {
                settings.mapZoomOut();
            } else {
                settings.mapZoomIn();
            }
        }
    }

    // --------------------------------- cALT --------------------------------

    static class cALT extends Button {
        void update(Analysis a) {
            set(a.alt);
            if (aircraft.terrain_warning() && a.p.warn_alt) {
                box(Color.RED);
            }
        }
        void click() {
            avionics.send_ap_altitude((int)aircraft.altitude_ind());
        }
    }

    // --------------------------------- cVS --------------------------------

    static class cVS extends Button {
        void update(Analysis a) {
            box(a.c_vs_box, a.warn_aoa || a.error_aoa);
            set(a.vvi);
        }
        void click() {
            avionics.send_ap_vv((int)aircraft.vvi());
        }
    }

    // --------------------------------- cHDG --------------------------------

    static class cHDG extends Button {
        void update(Analysis a) {
            set(a.hdg);
            if (a.p.warn_dist && !a.makingProgress) {
                box(Color.YELLOW, false);
            }
        }
        void click() {
            avionics.send_ap_heading((int)aircraft.heading());
        }
    }

    // --------------------------------- cIAS --------------------------------

    static class cIAS extends Button {
        void update(Analysis a) {
            set(a.ias);
            box(Color.WHITE, false);
            setBorderWidth(1);
            if (aircraft.stall_warning() && a.p.warn_alt) {
                box(Color.RED);
            }
        }
        void click() {
            avionics.send_ap_airspeed((int)aircraft.airspeed_ind());
        }
    }

    // --------------------------------- cBARO --------------------------------

    static class cBARO extends Number {

        void update(Analysis a) {
            int val = getBaro();
            set(get2992Mode() ? String.format("%.2f", ((float)val) / 100) : Integer.toString(val));
        }

        void click(int value, boolean right, boolean repeat) {              // Up & down buttons
            setBaro(getBaro() + (value * (right ? 10 : repeat ? 5 : 1)));   // Right adds 10, repeat adds 5, left adds 1
        }

        void click(boolean right) {                                         // Number display
            if (right) {
                toggleMode();                                               // Right toggles mode
            } else {
                setBaro(get2992Mode() ? 2992 : 1013);                       // Left sets to standard
            }
        }

        int getBaro() {
            return Math.round(aircraft.altimeter_in_hg() * 101300 / (get2992Mode() ? 1013 : 2992)); // To make 29.92 into 2992 or 1013
        }

        void setBaro(int ival) {
            float val = ((float)(ival * (get2992Mode() ? 1013 : 2992))) / 101300;
            avionics.sendDataPoint(XPlaneSimDataRepository.SIM_COCKPIT2_GAUGES_ACTUATORS_BAROMETER_SETTING_IN_HG_PILOT, val);
        }

        boolean get2992Mode() {
            return "2992".equals(conf.getProperty("baroMode"));
        }

        void toggleMode() {
            conf.setProperty("baroMode", get2992Mode() ? "1013" : "2992");
        }
    }

    // --------------------------------- cOBS1 --------------------------------

    static class cOBS1 extends Number {

        void update(Analysis a) {
            set(getObs());
        }

        void click(int value) {
            avionics.set_nav1_obs(getObs() + value);
        }

        int getObs() {
            return Math.round(avionics.nav1_obs());
        }
    }

    // --------------------------------- cOBS2 --------------------------------

    static class cOBS2 extends cOBS1 {

        void click(int value) {
            avionics.set_nav2_obs(getObs() + value);
        }

        int getObs() {
            return Math.round(avionics.nav2_obs());
        }
    }

    // --------------------------------- cBRK --------------------------------

    static class cBRK extends Button {
        void update(Analysis a) {
            //float brk = EPI.getFloat("sim/flightmodel/controls/parkbrake", 0);
            //float brk = EPI.getFloat("sim/cockpit2/controls/parking_brake_ratio", 0);

            float brk = a.pbreak ? 1 : 0;
            set(brk == 1);
            if (brk > 0) {
                box(Color.RED, false);
            }

        }
        void click() {
            EPI.sendCommand("cmd once sim/flight_controls/brakes_toggle_max");
            System.out.println("Break applied at location: " + aircraft.lat() + " - " + aircraft.lon());
        }
    }


    // --------------------------------- cPH --------------------------------

    static class cPH extends Button {
        boolean ph;
        void update(Analysis a) {
            //ph = EPI.getInt("sim/cockpit2/ice/ice_pitot_heat_on_pilot", 0) == 1;
            ph =  aircraft.pitot_heat();
            set(!ph);
            if (ph) {
                box(Color.RED, false);
            }

        }
        void click() {
            EPI.sendCommand("cmd once sim/ice/pitot_heat0_" + ((ph) ? "on" : "off"));
        }
    }

    // --------------------------------- cYD --------------------------------

    static class cYD extends Button {
        void update(Analysis a) {
            boolean yd = EPI.getInt("sim/cockpit2/switches/yaw_damper_on", 0) == 1;
            set(yd);
        }
        void click() {
            EPI.sendCommand("cmd once sim/systems/yaw_damper_toggle");
        }
    }

    // --------------------------------- cFDN --------------------------------

    static class cFDN extends Button {
        void update(Analysis a) {
        }
        void click() {
            EPI.sendCommand("cmd once sim/flight_controls/flaps_down");
        }
    }

    // --------------------------------- cFUP --------------------------------

    static class cFUP extends Button {
        void update(Analysis a) {
        }
        void click() {
            EPI.sendCommand("cmd once sim/flight_controls/flaps_up");
        }
    }


    // --------------------------------- cGDN --------------------------------

    static class cGDN extends Button {
        void update(Analysis a) {
        }
        void click() {
            EPI.sendCommand("cmd once sim/flight_controls/landing_gear_down");
        }
    }

    // --------------------------------- cGUP --------------------------------

    static class cGUP extends Button {
        void update(Analysis a) {
        }
        void click() {
            EPI.sendCommand("cmd once sim/flight_controls/landing_gear_up");
        }
    }

    // --------------------------------- mAPP --------------------------------

    static class mAPP extends Button {
        void update(Analysis a) {
            set(a.mp_smode == Avionics.EFIS_MAP_APP);
        }
        void click() {
            settings.setMode(Avionics.EFIS_MAP_APP);
        }
    }

    // --------------------------------- mVOR --------------------------------

    static class mVOR extends Button {
        void update(Analysis a) {
            set(a.mp_smode == Avionics.EFIS_MAP_VOR);
        }
        void click() {
            settings.setMode(Avionics.EFIS_MAP_VOR);
        }
    }

    // --------------------------------- mMAP --------------------------------

    static class mMAP extends Button {
        void update(Analysis a) {
            set(a.mp_smode == Avionics.EFIS_MAP_MAP);
        }
        void click() {
            settings.setMode(Avionics.EFIS_MAP_MAP);
        }
    }

    // --------------------------------- mNAV --------------------------------

    static class mNAV extends Button {
        void update(Analysis a) {
            set(a.mp_smode == Avionics.EFIS_MAP_NAV);
        }
        void click() {
            settings.setMode(Avionics.EFIS_MAP_NAV);
        }
    }

    // --------------------------------- mPLN --------------------------------

    static class mPLN extends Button {
        void update(Analysis a) {
            set(a.mp_smode == Avionics.EFIS_MAP_PLN);
        }
        void click() {
            settings.setMode(Avionics.EFIS_MAP_PLN);
        }
    }

    // --------------------------------- mCTR --------------------------------

    static class mCTR extends Button {
        int mode;
        void update(Analysis a) {
            mode = a.mp_mode;
            set(mode == Avionics.EFIS_MAP_CENTERED);
        }
        void click() {
            avionics.set_mode(mode == Avionics.EFIS_MAP_CENTERED ? Avionics.EFIS_MAP_EXPANDED : Avionics.EFIS_MAP_CENTERED);
        }
    }


    // --------------------------------- sARPT --------------------------------

    static class sARPT extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_arpt());
        }
        void click() {
            settings.toggleSymbol("ARPT");
        }
    }

    // --------------------------------- sWPT --------------------------------

    static class sWPT extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_wpt());
        }
        void click() {
            settings.toggleSymbol("WPT");
        }
    }

    // --------------------------------- sVOR --------------------------------

    static class sVOR extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_vor());
        }
        void click() {
            settings.toggleSymbol("VOR");
        }
    }

    // --------------------------------- sNDB --------------------------------

    static class sNDB extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_ndb());
        }
        void click() {
            settings.toggleSymbol("NDB");
        }
    }

    // --------------------------------- sTFC --------------------------------

    static class sTFC extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_tfc());
        }
        void click() {
            settings.toggleSymbol("TFC");
        }
    }

    // --------------------------------- sPOS --------------------------------

    static class sPOS extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_pos());
        }
        void click() {
            settings.toggleSymbol("POS");
        }
    }

    // --------------------------------- sDATA --------------------------------

    static class sDATA extends Button {
        void update(Analysis a) {
            set(avionics.efis_shows_data());
        }
        void click() {
            settings.toggleSymbol("DATA");
        }
    }

    // --------------------------------- cGS --------------------------------

    static class cGS extends Button {
        void update(Analysis a) {
            set(a.gs);
        }
    }

    // --------------------------------- cAOA --------------------------------

    static class cAOA extends Button {
        void update(Analysis a) {
            box(a.aoa_color, a.warn_aoa || a.error_aoa);
            set(a.aoa);
        }
    }

    // --------------------------------- cGEAR --------------------------------

//System.out.println(" a.bad_climb="+a.bad_climb+" gear_warning="+a.gear_warning+" a.gear_unsafe="+a.gear_unsafe+" a.gear_not_up="+a.gear_not_up);

    static class cGEAR extends Button {
        void update(Analysis a) {
            if (a.gear_not_up || a.gear_unsafe || a.gear_warning || (a.bad_climb && a.gear_not_up)) {
                Color c = Color.RED;
                if (a.p.warn_bgf && (a.bad_climb || a.gear_warning)) {
                    box(Color.RED, true /*a.gear_warning*/ );
                }
                if (a.gear_unsafe) {
                    c = Color.YELLOW;
                } else if (a.gear_is_down) {
                    c = Color.GREEN;
                }
                color(c);
                set("GEAR");
            } else {
                set("");
            }
        }
    }

    // --------------------------------- cFLAP --------------------------------

    static class cFLAP extends Button {
        void update(Analysis a) {
            int f_pos = Math.round(aircraft.get_flap_position() * 100);
            if (f_pos > 0) {
                int f_det = aircraft.get_flap_detents();
                int f_hnd = Math.round(aircraft.get_flap_handle() * 100);
                int d_num = Math.round(f_hnd * f_det / 100);
                if (a.flap_pos > Analysis.FLAP_TOFF) {
                    Color bcolor = (a.flap_pos == Analysis.FLAP_MAX || a.bad_climb) ? Color.RED : Color.YELLOW;
                    box(bcolor, a.bad_climb);
                }
                color(f_hnd != f_pos ? Color.YELLOW : Color.GREEN);
                set("F " + d_num + "/" + f_det);
            } else {
                set("");
            }
        }
    }

    // --------------------------------- cFF --------------------------------

    static class cFF extends Button {
        void update(Analysis a) {
            color(a.low_fuel ? Color.YELLOW : Color.WHITE);
            set(a.ff_Str);
        }
    }

    // --------------------------------- cFFP --------------------------------

    static class cFFP extends Button {
        void update(Analysis a) {
            color(a.p.powerMode.charAt(0) == 'F' && (a.pwr_high != null || a.pwr_low != null) ? Color.GREEN : Color.WHITE);
            set(a.ff_pcStr);
        }
    }

    // --------------------------------- cFPMN --------------------------------

    static class cFPMN extends Button {
        void update(Analysis a) {
            String str = ""+a.ff_perMN;
            set(str);
        }
    }

    // --------------------------------- cFUEL --------------------------------

    static class cFUEL extends Button {
        void update(Analysis a) {
            color(a.low_fuel ? Color.YELLOW : Color.WHITE);
            set(a.fuel);
        }
    }

    // --------------------------------- cRNGE --------------------------------

    static class cRNGE extends Button {
        void update(Analysis a) {
            color(a.low_fuel ? Color.YELLOW : Color.WHITE);
            set(a.ff_rnge);
        }
    }

    // --------------------------------- cENDU --------------------------------

    static class cENDU extends Button {
        void update(Analysis a) {
            color(a.low_fuel ? Color.YELLOW : Color.WHITE);
            set(a.ff_endu);
        }
    }

    // --------------------------------- cRA --------------------------------

    static class cRA extends Button {
        void update(Analysis a) {
            color(a.ra_color);
            set(a.ra_str);
            box(a.ra_box, a.ra_flash);
        }
    }

    // --------------------------------- cDH --------------------------------

    static class cDH extends Button {
        void update(Analysis a) {
            color(a.dh_color);
            set(a.dh);
        }
    }

    // --------------------------------- cPTCH --------------------------------

    static class cPTCH extends Button {
        void update(Analysis a) {
            float value = aircraft.get_pitch_trim() * 10;
            color(trim_color(value));
            set(String.format("%.1f", value) + "p");
        }
    }

    // --------------------------------- cROLL --------------------------------

    static class cROLL extends Button {
        void update(Analysis a) {
            float value = aircraft.get_roll_trim() * 10;
            color(trim_color(value));
            set(String.format("%.1f", value) + "r");
        }
    }

    // --------------------------------- cYAW --------------------------------

    static class cYAW extends Button {
        void update(Analysis a) {
            float value = aircraft.get_yaw_trim() * 10;
            color(trim_color(value));
            set(String.format("%.1f", value) + "y");
        }
    }

    // --------------------------------- cMARK --------------------------------

    static class cMARK extends Button {
        void update(Analysis a) {
            String mark = null;
            Color color = null;
            if (avionics.outer_marker()) {
                color = new Color(0x0080FF);
                mark = "OM";
            } else if (avionics.middle_marker()) {
                color = Color.ORANGE;
                mark = "MM";
            } else if (avionics.inner_marker()) {
                color = Color.WHITE;
                mark = "IM";
            }
            if (mark != null) {
                set(mark);
                color(color);
                box(color, false);
            } else {
                set("");
            }
        }
    }

    // --------------------------------- cTAT --------------------------------

    static class cTAT extends Button {
        void update(Analysis a) {
            set(Math.round(aircraft.tat()));
        }
    }

    // --------------------------------- cWVEC --------------------------------

    static class cWVEC extends Button {
        void update(Analysis a) {
            set(a.wvec);
        }
    }

    // --------------------------------- cDIST --------------------------------

    static class cDIST extends Button {
        void update(Analysis a) {
            set(a.target_dist);
        }
    }

    // --------------------------------- cETE --------------------------------

    static class cETE extends Button {
        void update(Analysis a) {
            int hours = (int)a.target_ete;
            int mins  = (int)((a.target_ete - hours) * 60);
            String str = "" + hours + (":" + mins+ "0").substring(0, 3);
            set(str);
        }
    }
}
