/**
 * Analysis.java
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
import java.awt.Graphics2D;
import java.util.ArrayList;

import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.model.Aircraft;
import net.sourceforge.xhsi.model.Avionics;
import net.sourceforge.xhsi.model.ModelFactory;
import net.sourceforge.xhsi.model.NavigationRadio;


/**
 * Analysis
 */
public class Analysis extends SuperAnalysis {

    public final static int FLAP_ZERO = 0;
    public final static int FLAP_TOFF = 1;
    public final static int FLAP_MAX  = Integer.MAX_VALUE;

    int     ap_mode        = avionics.autopilot_mode();
    boolean ap_cmd         = ap_mode > 1;
    int     ap_alt         = Math.round(avionics.autopilot_altitude());
    int     ap_vvi         = Math.round(avionics.autopilot_vv());
    int     ap_hdg         = Math.round(avionics.heading_bug());
    int     h_src          = avionics.hsi_source();
    int     mp_mode        = avionics.map_mode();
    int     mp_smode       = avionics.map_submode();
    boolean nav            = avionics.ap_vorloc_on() || avionics.ap_lnav_on();
    boolean hsel           = avionics.ap_hdg_sel_on();
    boolean hsel_error     = hsel && p.warn_hsel && (mp_smode == Avionics.EFIS_MAP_APP || mp_smode == Avionics.EFIS_MAP_VOR);
    boolean wlv            = !nav && !hsel;
    boolean flying         = !aircraft.on_ground();
    float   flaps          = aircraft.get_flap_position();
    boolean ap_vs_on       = avionics.ap_vs_on();
    boolean ap_ptch_on     = avionics.ap_pitch_on();
    boolean ap_flc_on      = avionics.ap_flch_on();
    int     flap_pos       = FLAP_ZERO;

    int     enumber        = aircraft.num_engines();
    int     atarget        = Math.round(avionics.autopilot_altitude());
    int     etype          = avionics.get_engine_type();
    String  etypeStr       = "UKN";

    int     ra_bug         = p.alt_dh ? p.alt_dh_bug : aircraft.ra_bug();
    int     dh             = ra_bug;
    int     ra             = Math.round(aircraft.agl_m() * 3.28084f);
    int     alt            = Math.round(aircraft.altitude_ind());
    int     vvi            = Math.round(aircraft.vvi());
    int     hdg            = Math.round(aircraft.heading());
    int     ias            = Math.round(aircraft.airspeed_ind());
    int     gs             = Math.round(aircraft.ground_speed());
    boolean climbing       = vvi >  50;
    boolean decending      = vvi < -50;

    float   aoa            = aircraft.aoa();
    boolean low_fuel       = aircraft.low_fuel();
    boolean retractable    = aircraft.has_retractable_gear();
    boolean gear_not_up    = retractable && !aircraft.gear_is_up();
    boolean gear_is_down   = !retractable || aircraft.gear_is_down();
    boolean gear_unsafe    = aircraft.gear_unsafe();
    boolean gear_warning   = aircraft.gear_warning() || (ra < 500 && decending && !gear_is_down);
    boolean pbreak         = aircraft.get_parking_brake() > 0.01f && p.warn_bgf;
    boolean bad_climb      = false; // (for now)
    boolean vvi_warn       = (p.warn_bad_vs && p.warn_no_ap && ap_cmd) && (ap_vs_on || ap_flc_on || ap_ptch_on) &&
                             ((climbing && (alt > atarget || ap_vs_on)) || (decending && alt < atarget));


    String  fuel           = "";
    String  gear           = "";
    String  flcmd          = "";
    String  ff_Str         = "";
    String  ff_pcStr       = "";
    String  ff_rnge        = "";
    String  ff_endu        = "";
    String  wvec           = "";
    String  ra_str         = "";
    boolean ra_flash       = false;
    Color   ra_box         = null;
    Color   ra_color       = Color.WHITE;
    Color   dh_color       = Color.GREEN;
    Color   a_vs_box       = null;
    Color   c_vs_box       = null;
    Color   a_flc_box      = null;
    Color   a_alt_box      = null;
    Color   aoa_color      = null;

    long    time_zulu      = (long)(aircraft.sim_time_zulu()     * 1000);
    long    time_local     = (long)(aircraft.sim_time_local()    * 1000);
    long    time_flight    = (long)(aircraft.total_flight_time() * 1000);
    long    currentMillis  = System.currentTimeMillis();

    float   target_dist    = 0;
    float   target_ete     = 0;
    boolean makingProgress = true;

    float   n1_pwr         = 0;
    float   epr_pwr        = 0;
    float   trq_pwr        = 0;
    float   map_pwr        = 0;
    float   rpm_pwr        = 0;
    float   ff_pwr         = 0;
    float   ff_total       = 0;
    float   ff_perMN       = 0;
    int     ff_pcent       = 50;

    String  pwr_low        = null;
    String  pwr_high       = null;
    int     pwr_pcent      = -1;
    String  pwr_pcStr      = "?";

    boolean warn_gs        = false;
    boolean error_aoa      = false;
    boolean warn_aoa       = false;
    boolean info_aoa       = false;
    String  aoa_label      = (aoa < 0) ? "aoa" : "AOA";
    String  aoa_text       = "The angle of attact is "+aoa;
    String  xpdr = String.format("%04d", avionics.transponder_code());
    private boolean xplaneRunning = true;


    /**
     * Values that are used by several button types are calculated here to avoid
     * calling the X-Plane API more than necessary.
     */
    public Analysis(FlightState fs, CommanderProperties p, XCommBuffers xcb, Aircraft aircraft, Avionics avionics) {

        super(fs, p, aircraft, avionics);
        this.xcb = xcb;

        // ---------- Calculate AOA ----------

        if (aircraft.on_ground() && aircraft.ground_speed() < aircraft.get_Vso()*2/3) {
            aoa = 0.0f; // fixed at 0.0 when on the ground and ground speed is less than 2/3*Vso
        } else {
            aoa = (0.0f + ((int)(aoa * 10))) / 10 ; //  round
        }

        // ---------- Calculate engine type ----------

        switch (etype) {
            case XHSISettings.ENGINE_TYPE_N1:  etypeStr = "N1";  break;
            case XHSISettings.ENGINE_TYPE_EPR: etypeStr = "EPR"; break;
            case XHSISettings.ENGINE_TYPE_TRQ: etypeStr = "TRQ"; break;
            case XHSISettings.ENGINE_TYPE_MAP: etypeStr = "MAP"; break;
        }

        // ---------- Calculate fuel ----------

        float totalFuel = 0;
        int n_tanks = aircraft.num_tanks();
        for (int i = 0 ; i < n_tanks ; i++) {
            totalFuel += aircraft.get_fuel(i);
        }
        totalFuel *= aircraft.fuel_multiplier();
        fuel = "" + Math.round(totalFuel);

        // ---------- Calculate ff, ffp, endu, and rnge ----------

        if (enumber > 0) {
            float fmult = 3600 * aircraft.fuel_multiplier(); // gal/h etc.
            float ffmax = enumber * aircraft.get_max_FF() * fmult;
            for (int i = 0 ; i < enumber ; i++) {
                n1_pwr   += aircraft.get_N1(i);
                epr_pwr  += aircraft.get_EPR(i);
                trq_pwr  += aircraft.get_TRQ_LbFt(i);
                map_pwr  += aircraft.get_MPR(i);
                rpm_pwr  += aircraft.get_prop_RPM(i);
                ff_total += aircraft.get_FF(i) * fmult;
            }

            n1_pwr   = ((float)Math.round(n1_pwr   * 10 / enumber)) / 10;
            epr_pwr  = ((float)Math.round(epr_pwr  * 10 / enumber)) / 10;
            trq_pwr  = ((float)Math.round(trq_pwr  * 10 / enumber)) / 10;
            map_pwr  = ((float)Math.round(map_pwr  * 10 / enumber)) / 10;
            rpm_pwr  = ((float)Math.round(rpm_pwr  * 10 / enumber)) / 10;
            ff_pwr   = ((float)Math.round(ff_total * 10 / enumber)) / 10;
            ff_total = ((float)Math.round(ff_total * 10          )) / 10;
            ff_pcent = Math.round(ff_total * 100 / ffmax);

            ff_Str   = "" + ff_total;
            ff_pcStr = "" + ff_pcent + "%";
            ff_perMN = ff_total / gs;

            float hoursToRun = totalFuel / ff_total;
            ff_endu  = "" + hhmm(hoursToRun);
            ff_rnge  = "" + Math.round(gs * hoursToRun);

        // ---------- Calculate percentage power ----------

            String tag = p.powerMode;
            if (tag != null) {
                float lo = 0;
                float hi = Element.s2float(p.props.getProperty("powerMax"+tag), -1);
                if (hi < 0 && tag.charAt(0) == 'F') { // Use default FF
                    hi = ffmax / enumber;
                }
                if (lo >= 0 && hi > lo) {
                    float pwr = getPower(tag);
                    float range = hi - lo;
                    float ratio = (pwr - lo) / range;
                    pwr_pcent = Math.round(ratio * 100);
                    pwr_pcStr = Integer.toString(pwr_pcent) + "%";
                    if (p.warn_pwr) {
                        if(p.low_pwr >= 0 && pwr_pcent < p.low_pwr) {
                            pwr_low = "The engine power is low ("+pwr_pcent+"%)";
                        }
                        if(p.high_pwr >= 0 && pwr_pcent > p.high_pwr) {
                            pwr_high = "The engine power is high ("+pwr_pcent+"%)";
                        }
                    }
                }
            }
        }

        // ---------- Calculate flap_pos ----------

        int f_pos = Math.round(aircraft.get_flap_position() * 100);
        if (f_pos > 0) {
            int f_det = aircraft.get_flap_detents();
            int f_hnd = Math.round(aircraft.get_flap_handle() * 100);
            int d_num = Math.round(f_hnd * f_det / 100);
            flap_pos = (d_num > 1) ? ((d_num == f_det) ? FLAP_MAX : d_num) : FLAP_TOFF;
        }

        bad_climb = (gear_not_up || flap_pos > Analysis.FLAP_TOFF) && (ra > 500) && (vvi > p.vspeed * 1.5);


        // ---------- Calculate wvec ----------

        int wdir = Math.round(aircraft.get_environment().wind_direction() + aircraft.magnetic_variation()) % 360;
        int wspd = Math.round(aircraft.get_environment().wind_speed());
        wvec = (wspd > 0) ? "" + wdir + "\u00B0" +"/"+ wspd : "Calm";

        // ---------- Calculate levels of aoa ----------

        if (p.warn_aoa) {
            error_aoa = (aoa <= p.aoa_r_n || aoa >= p.aoa_r_p);
            if (error_aoa) {
                aoa_color = Color.RED;
            } else {
                warn_aoa = (aoa <= p.aoa_y_n || aoa >= p.aoa_y_p);
                if (warn_aoa) {
                    aoa_color = Color.YELLOW;
                } else {
                    info_aoa = (aoa <= p.aoa_g_n || aoa >= p.aoa_g_p);
                    if (info_aoa) {
                        aoa_color = Color.GREEN;
                    }
                }
            }
        }

        // ---------- Calculate box for vs, alt, and flc ----------

        if (ap_cmd) {
            if (warn_aoa || error_aoa) {
                if (ap_vs_on) {
                    a_vs_box  = aoa_color;
                }
                if (ap_flc_on) {
                    a_flc_box  = aoa_color;
                }
            }
            if (aoa_color != Color.RED && p.warn_alt) {
                if (avionics.ap_alt_hold_arm() && !ap_vs_on && !ap_flc_on) { // ALT arm with no V/S or FLC
                    a_vs_box  = Color.YELLOW;
                    a_alt_box = Color.YELLOW;
                    a_flc_box = Color.YELLOW;
                }
                if (ap_vs_on && vvi == 0) {     // V/S mode with VVI set to zero
                    a_vs_box  = Color.YELLOW;
                    c_vs_box  = Color.YELLOW;
                }
                if (ap_vs_on && alt == ap_alt) { // V/S mode but reached Alt
                    a_vs_box  = Color.YELLOW;
                    a_alt_box = Color.YELLOW;
                }
            }
        }
        if (vvi_warn && a_vs_box != Color.RED) {
            a_vs_box  = Color.YELLOW;
        }

        // ---------- Calculate ra and dh ----------

        if (ra < 2500) { // only works below this hight
            ra_str = ""+ra;
            if (p.warn_alt) {
                if (ra_bug > 0 && fs.highest >= 1000 && ra < fs.highest + 250) {
                    if (ra <= 20) {
                        ra_box   = Color.RED;
                        dh_color = Color.RED;
                        dh = ra;
                    } else if (ra < ra_bug) {
                        ra_box = Color.RED;
                        dh = (ra + 5) / 10 * 10; // 10 foot lumps
                        dh_color = (ra > 100) ? Color.YELLOW : Color.CYAN;
                    } else if (ra < ra_bug + 250) {
                        ra_box  = Color.RED;
                        ra_flash = true;
                    } else if (ra < ra_bug + 500) {
                        ra_box = Color.YELLOW;
                    } else if (ra < ra_bug + 1000) {
                        ra_box = Color.GREEN;
                    }
                } else {
                    ra_color = Color.WHITE;
                }
            }
        }

        // ---------- Calculate if a GS button needs a the yellow flashing box ----------

        boolean gs_active = false;
        float gs_value = 0.0f;
        if (h_src == Avionics.HSI_SOURCE_GPS) {
            gs_active = avionics.gps_gs_active();
            gs_value  = avionics.gps_vdef_dot();
        } else if (h_src == Avionics.HSI_SOURCE_NAV1) {
            gs_active = avionics.nav1_gs_active();
            gs_value  = avionics.nav1_vdef_dot();
        } else if (h_src == Avionics.HSI_SOURCE_NAV2) {
            gs_active = avionics.nav2_gs_active();
            gs_value  = avionics.nav2_vdef_dot();
        }
        warn_gs = gs_active && Math.abs(gs_value) < 2.49f && !avionics.ap_gs_on() && !avionics.ap_gs_arm();

        // ---------- Calculate if progress towards target is being made ----------

        if (ap_cmd && !hsel && flying) {
            float dist = getTargetValue('D');
            if (dist != 0) {
                if (dist < fs.lastDistance) {
                    fs.makingProgress = true;
                }
                if (dist > fs.lastDistance) {
                    fs.makingProgress = false;
                }
                fs.lastDistance = dist;
                target_ete = getTargetValue('E');
            }

            target_dist    = dist;
            makingProgress = fs.makingProgress;
        }

        // ---------- Calculate highest and autopilotWasOn ----------

        if (!flying) {
            fs.highest = 0;
            fs.autopilotWasOn = false;
        } else {
            fs.highest = Math.max(ra, fs.highest);
            fs.autopilotWasOn |= ap_cmd;
        }

        // ---------- Calculate the textual data for xCOMM ----------

        if (xcb != null) {
            writeXCommData();
        }
    }

    /**
     * getPower
     */
    float getPower(String tag) {
        return getPower(tag, false);
    }

    /**
     * getPower
     */
    float getPower(String tag, boolean setEtype) {
        float total = 0;
        int etype = -1;
        switch (tag.charAt(0)) {
            case 'N': total = n1_pwr;  etype = XHSISettings.ENGINE_TYPE_N1;  break; // N1
            case 'E': total = epr_pwr; etype = XHSISettings.ENGINE_TYPE_EPR; break; // EPR
            case 'T': total = trq_pwr; etype = XHSISettings.ENGINE_TYPE_TRQ; break; // TRQ
            case 'M': total = map_pwr; etype = XHSISettings.ENGINE_TYPE_MAP; break; // MAP
            case 'R': total = rpm_pwr;                                       break; // RPM
            case 'F': total = ff_pwr;                                        break; // FF
        }
        if (setEtype && etype >= 0) {
            avionics.set_engine_type(etype);
        }
        return total;
    }

    /**
     * getTargetValue
     */
    private float getTargetValue(char code) {
        int source;
        switch (avionics.map_submode()) {
            case Avionics.EFIS_MAP_MAP:
            case Avionics.EFIS_MAP_NAV:
            case Avionics.EFIS_MAP_PLN: {
                source = Avionics.HSI_SOURCE_GPS;
                break;
            }
            default: {
                source = avionics.hsi_source();
                break;
            }
        }
        NavigationRadio radio = null;
        switch (source) {
            case Avionics.HSI_SOURCE_GPS:  radio = avionics.get_gps_radio();  break;
            case Avionics.HSI_SOURCE_NAV1: radio = avionics.get_nav_radio(1); break;
            case Avionics.HSI_SOURCE_NAV2: radio = avionics.get_nav_radio(2); break;
        }
        return (radio == null) ? 0 : (code == 'D') ? radio.get_distance() : radio.get_ete();
    }

    /**
     * hhmm
     */
    String hhmm(float hours) {
        String hh = Integer.toString(Math.round(hours));
        String mm = Integer.toString(Math.round((hours - (int)hours) * 60));
        if (mm.length() == 1) {
            mm = "0" + mm;
        }
        return hh+":"+mm;
    }

    /**
     * prt
     */
    public static void prt(Object obj) {
        System.out.println(obj);
    }

    /**
     * invalidate
     */
    public void invalidate() {
        xplaneRunning = false;
        writeXCommData();
    }

    /**
     * isValid
     */
    public boolean isValid() {
        return xplaneRunning;
    }

    // ================================== Support for xCOMM ==================================

    /**
     * xcb
     */
    XCommBuffers xcb;

    /**
     * writeXCommData
     */
    private void writeXCommData() {
        Composer c = new Composer();
        c.currentColor = Color.RED;
        if (!isValid()) {
            c.write("XXXX No Data from X-Plane XXXX", "XHSI not receiving");
        } else if (!aircraft.battery()) {
            c.write("MASTER OFF", "The electrical master switch is off");
        } else {
            displayErrors(c);
            c.currentColor = Color.YELLOW;
            displayWarnings(c);
            c.currentColor = Color.GREEN;
            displayInfo(c);
        }
        xcb.update(c.messages);
    }

    // ------------------------------------------- Errors -------------------------------------------

    /**
     * displayErrors
     */
    private void displayErrors(Composer c) {
        c.write("STALL",     "The aircraft is stalling",          aircraft.stall_warning());
        c.write("TERRAIN",   "The aircraft may hit the ground ",  aircraft.terrain_warning());
        c.write("GEAR",      "The landing gear is NOT down",      gear_warning && p.warn_bgf);
        c.write("BREAK",     "The breaks are on",                 pbreak);
        c.write("AUTOPILOT", "The autopilot is disconnected",     p.warn_no_ap && ap_mode == 0);
        c.write("FLAPS",     "The flaps are not set for takeoff", flap_pos > FLAP_TOFF && !flying && p.warn_bgf);
        c.write("BAD-CLIMB", "Climb with gear or flaps extended", bad_climb);
        c.write("PITOT",     "The pitot heater is off",           p.warn_pitot && aircraft.pitot_heat());
        c.write(aoa_label,    aoa_text,                           error_aoa && p.warn_aoa);
    }

    // ------------------------------------------- Warnings -------------------------------------------

    /**
     * displayWarnings
     */
    private void displayWarnings(Composer c) {
        c.write("ICING",     "Ice has been detected on the aircraft", aircraft.icing());
        c.write("FUEL",      "The aircraft is low on fuel",           low_fuel);

        for (int i = 0 ; i < aircraft.num_engines() ; i++) {
            String problem = "";
            if (aircraft.fuel_press_alert(i)) {
                problem += "Low fuel pressure ";
            }
            if (aircraft.oil_temp_alert(i)) {
                problem += "High oil temp ";
            }
            if (aircraft.oil_press_alert(i)) {
                problem += "Low oil pressure ";
            }
            c.write("ENGINE"+(i+1), problem, problem.length()  > 0);
        }

        c.write(aoa_label, aoa_text, warn_aoa && p.warn_aoa);

        if (vvi_warn && !bad_climb) {
            if (ap_vs_on && vvi > 0) {
                c.write("V/S CLIMB", "Autopilot ascent in V/S mode");
            } else if (vvi > 0) {
                c.write("V/S NO TARGET", "Autopilot ascent with no upper target altitude");
            } else {
                c.write("V/S NO TARGET", "Autopilot descent with no lower target altitude");
            }
        }

        c.write("AUTOPILOT", "The autopilot is disconnected",        p.warn_no_ap && ap_mode == 1);
        c.write("FLAPS",     "The flaps are fully extended",         flap_pos == FLAP_MAX && flying && p.warn_bgf);
        c.write("GEAR",      "The landing gear is unsafe",           gear_unsafe && !gear_warning && p.warn_bgf);
        c.write("WLV",       "Autopilot in wing level mode",         p.warn_hsel && wlv);
        c.write("HDG",       "Autopilot in heading mode",            hsel_error);
        c.write("DIRECTION", "The distance to target is increasing", p.warn_dist && !makingProgress);
        c.write("GS-OFF",    "Glide slope is not selected",          warn_gs);
    }

    // ------------------------------------------- Info -------------------------------------------

    /**
     * displayInfo
     */
    private void displayInfo(Composer c) {
        c.write(aoa_label,     aoa_text,                   info_aoa && p.warn_aoa);
        c.write("CLIMBING",   "VVI is "+vvi ,              p.warn_vs && vvi >  p.vspeed);
        c.write("DESCENDING", "VVI is "+vvi ,              p.warn_vs && vvi < -p.vspeed);
        c.write("GEAR",       "The landing gear is down",  gear_not_up && !gear_unsafe && !gear_warning && p.warn_bgf);
        c.write("FLAPS",      "The flaps are deployed",    flap_pos > FLAP_ZERO && flap_pos < FLAP_MAX && flying && p.warn_bgf);
        c.write("power",       pwr_low,                    pwr_low  != null);
        c.write("POWER",       pwr_high,                   pwr_high != null);
    }

    // ------------------------------------------- Composer -------------------------------------------

    /**
     * Composer
     */
    static class Composer {

        /**
         * messages
         */
        ArrayList<Display.XCommEntry> messages = new ArrayList<Display.XCommEntry>();

        /**
         * currentColor
         */
        Color currentColor = Color.RED;

        /**
         * write
         */
        private void write(String label, String explaination) {
            messages.add(new Display.XCommEntry(" "+label+" ", explaination, currentColor));
        }

        /**
         * write
         */
        private void write(String txt, String explaination, boolean condition) {
            if (condition) {
                write(txt, explaination);
            }
        }

        /**
         * write
         */
        private void write(String txt, Color color) {
            currentColor = color;
            write(txt, "Sorry, no explaination");
        }
    }
}


// ================================== SuperAnalysis ==================================

/**
 * SuperAnalysis
 */
class SuperAnalysis {

    final FlightState fs;
    final Aircraft aircraft;
    final Avionics avionics;
    final CommanderProperties p;

    SuperAnalysis(FlightState fs, CommanderProperties p, Aircraft aircraft, Avionics avionics) {
        this.fs = fs;
        this.p  = p;
        this.aircraft = aircraft;
        this.avionics = avionics;
    }
}
