/**
 * Approach.java
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
package net.sourceforge.xhsi.flightdeck.nd;

import java.io.*;
import java.util.*;
import net.sourceforge.xhsi.*;
import net.sourceforge.xhsi.model.*;

/**
 * Approach
 */
public class Approach {

    /**
     * DEBUG
     */
    public final static boolean DEBUG = System.getProperty("approach.debug") != null;

    /**
     * Reference to the parient MovingMap
     */
    private final MovingMap mm;

    /**
     * state
     */
    private ApproachState state;

    /**
     * Approach
     */
    public Approach(MovingMap mm) {
        this.mm = mm;
        state = new ApproachState(mm, "");
    }

    /**
     * init
     */
    float[] init() {
        StringBuilder sb  = new StringBuilder();
        int count = mm.fms.get_nb_of_entries();
        for (int i = 0 ; i < count ; i++) {
            FMSEntry entry = mm.fms.get_entry(i);
            if (entry != null) {
                sb.append(entry.name);
                sb.append(entry.active ? '*' : '&');
            }
        }
        String str = sb.toString();
        String pstr = "";
        if (DEBUG) {
            pstr = "++++++++++++++++++++++++++++++++++++++++++++++++++++++++ count=" + count + " fingerPrint=" + str;
        }
        if (!state.fingerPrint.equals(str)) {
            state = new ApproachState(mm, str);
            pstr += " *** NEW STATE ***";
        }
        if (DEBUG) {
            out.println(pstr);
        }
        return state.legs.get(state.currentLeg - 1).getLocation(false);
    }

    /**
     * kill
     */
    void kill() {
        if (state.fingerPrint.length() > 0) {
            if (DEBUG) {
                out.println("**KILL**");
            }
            state = new ApproachState(mm, "");
        }
    }

    /**
     * getPath
     */
    ArrayList<ApproachState.Leg> getPath(String fmsId, String navId) {
        if (fmsId.equals(navId) && state.legs.size() == 1) {
            return null;
        } else {
            return state.getPath(navId);
        }
    }

    /**
     * getCurrentLeg
     */
    int getCurrentLeg() {
        return state.currentLeg;
    }

    /**
     * ApproachState
     */
    static class ApproachState {

        /**
         * Reference to the parient MovingMap
         */
        private final MovingMap mm;

        /**
         * aircraft
         */
        private final Aircraft aircraft;

        /**
         * avionics
         */
        private final Avionics avionics;

        /**
         * fingerPrint
         */
        final String fingerPrint;

        /**
         * path
         */
        private final ProcFile file;

        /**
         * This is a list of approach legs (and may also include missed approach legs).
         * The first leg is always the aircraft position either when the constructor
         * was called. If this list is only one element long then this means that this
         * class did not receive enough information to produce a set of approach
         * waypoints. This can occur if an approach is being flown to an airport that
         * has no GNS 430 "proc" file. or if no approach is being flown. This latter
         * condition can only be established by the fact the FMS entry name is not the
         * same as the GPS nav target name. Anyway, in In all cases when legs.size() == 1,
         * this class just acts as a perpositary for the last known aircraft position poior
         * to some desernable change in the FMS flight plan.
         */
        private final ArrayList<Leg> legs = new ArrayList<Leg>();

        /**
         * airportId
         */
        private final String airportId;

        /**
         * airportLoc
         */
        private final float[] airportLoc;

        /**
         * lastNavId
         */
        private String lastNavId;

        /**
         * currentLeg
         */
        private int currentLeg;

        /**
         * ApproachState
         */
        private ApproachState(MovingMap mm, String fingerPrint) {
            this.mm          = mm;
            this.fingerPrint = fingerPrint;
            this.aircraft    = mm.aircraft;
            this.avionics    = mm.avionics;
            FMSEntry dst     = getLastFmsWaypoint();
            if (dst != null) {
                airportId  = dst.name;
                airportLoc = new float[] {dst.lat, dst.lon};
                file = ProcFile.load(airportId);
                if (DEBUG) {
                    out.println("airportId=" + airportId + ((file == null) ? "** NOT FOUND**" : ("\n" + file)));
                }
            } else {
                airportId  = null;
                airportLoc = null;
                file       = null;
            }
            clearLegs(null);
        }

        /**
         * ApproachState
         */
        ApproachState(ApproachState state) {
            this(state.mm, state.fingerPrint);
        }

        /**
         * getLastFmsWaypoint
         */
        private FMSEntry getLastFmsWaypoint() {
            FMSEntry wp = mm.avionics.get_fms().get_last_waypoint();
            return (wp != null && wp.type == FMSEntry.ARPT) ? wp : null;
        }


        // ------------------------------------------------------------------------
        //                              Leg management
        // ------------------------------------------------------------------------

        /**
         * Leg
         */
        class Leg {
            final String  navId;
            private float[] location;
            final boolean isCertain;
            final boolean calculate;
            final boolean missed;

            /**
             * Leg
             */
            Leg(String navId, float[] location, boolean certain, boolean calculate) {
                this.navId     = navId.toUpperCase();
                this.location  = location;
                this.isCertain = certain;
                this.calculate = calculate;
                this.missed    = navId.length() > 0 && Character.isLowerCase(navId.charAt(0));
            }

            /**
             * getLocation
             */
            float[] getLocation(boolean legIsActive) {
                if (calculate && legIsActive) {
                    location = getCurrentNavPosition(); // Later calls get more accurate so keep calling...
                }
                return location;
            }

            /**
             * toString
             */
            public String toString() {
                String locStr = (location == null) ? "" : " location=[" + location[0] + "," + location[1] + "]";
                return "{navId=" + navId + locStr + " isCertain=" + isCertain + "}";
            }
        }

        /**
         * getLeg
         */
        private ArrayList<ApproachState.Leg> getPath(String navId) {
            boolean setup = false;
            if (legs.size() > 1) {
                boolean navIdEqualsAirport = navId.equals(airportId);
                for (int i = currentLeg ; i < legs.size() ; i++) {
                    String id = legs.get(i).navId;
                    if (id.equals(navId) || (navIdEqualsAirport && id.startsWith("RW"))) {
                        currentLeg = i;
                        return legs;
                    }
                }
                /*
                 * If the navId cannot be found in the ProcFile data then it must be a
                 * special waypoint inserted by the GPS simulatlor so just insert it
                 * in the list. Otherwise it is time to reevaluate the path that
                 * is being used.
                 */
                float[] posn = file.getLatLon(navId);
                if (posn == null) {
                    legs.add(++currentLeg, new Leg(navId, getCurrentNavPosition(), true, true));
                    if (DEBUG) {
                        out.println("+++ appended new leg currentLeg=" + currentLeg);
                    }
                } else {
                    /*
                     * It is impossible to know if a new point appears because of a direct-to or an
                     * activate-leg operation. Here the assumptioon is made that it is the former if the
                     * current leg was uncertain, othersize it is assumed to be a direct-to. The
                     * difference is only in where the red line will origionate.
                     */
                    Leg old = legs.get(currentLeg);
                    setupLegs(navId, false, (old.isCertain) ? null : old);
                }
            } else if (!navId.equals(lastNavId)) {
                setupLegs(navId, false, null);
            }
            return (legs.size() <= 1) ? null : legs;
        }

        /**
         * clearLegs
         */
        private void clearLegs(Leg oldLeg) {
            legs.clear();
            Leg start = (oldLeg != null) ? oldLeg : new Leg("", new float[] {mm.center_lat, mm.center_lon}, true, false);
            legs.add(start);
            currentLeg = 1;
        }

        /**
         * setupLegs
         */
        private void setupLegs(String navId, boolean missedApproach, Leg oldLeg) {
            if (DEBUG) {
                out.println("+++ setupLegs navId = " + navId + " missedApproach=" + missedApproach + " oldLeg=" + oldLeg);
            }
            clearLegs(oldLeg);
            lastNavId = navId;
            if (file != null && navId != null) {
                HashSet<String> navSet = findPaths(navId, missedApproach);
                if (navSet != null) {
                   /*
                    * If oldLeg is non-null see if a smaller set can be found by staring from
                    * there and removing all the entries that do not start with navId. (A better
                    * approach would be to carry on like this beyond just the first two waypoints
                    * but just this works out pretty well.)
                    */
                    if (navSet.size() > 1 && oldLeg != null) {
                        HashSet<String> oldSet = findPaths(oldLeg.navId, missedApproach);
                        if (oldSet != null) {
                            HashSet<String> possibles = new HashSet<String>();
                            String pref = navId + " ";
                            for (String str : oldSet) {
                                if (str.startsWith(pref)) {
                                    possibles.add(str.substring(pref.length()));
                                }
                            }
                            if (possibles.size() != 0 && possibles.size() < navSet.size()) {
                                if (DEBUG) {
                                    out.println("+++ setupLegs possibles reduced from " + navSet.size() + " to " + possibles.size());
                                }
                                navSet = possibles;
                            }
                        }
                    }
                    addPathLegs(navId, navSet);
                }
            }
        }

        /**
         * findPaths
         */
        HashSet<String> findPaths(String navId, boolean missedApproach) {
             HashSet<String> pathSet = missedApproach ? null : file.getApproachPaths(navId);
             return (pathSet != null) ? pathSet : file.getMissedPaths(navId);
        }

        /**
         * addPathLegs
         */
        private void addPathLegs(String navId, HashSet<String> pathSet) {
            /*
             * Always add the current navId because this is never in doubt.
             */
            addLeg(navId);

            /*
             * Get the set of possible paths from this point forward. If there is only
             * one (a common case) then just add the legs for this path. Otherwise try
             * to deal with the more complex multi-path scenario.
             */
            String[] paths = pathSet.toArray(new String[0]);
            if (paths.length == 1) {
                if (DEBUG) {
                    out.println("+++ addPathLegs single navId = " + navId + " path =" + paths[0]);
                }
                for (String id : paths[0].split("\\s+")) {
                    addLeg(id);
                }
            } else {
                addMultiPathLegs(paths);
            }
        }

        /**
         * addMultiPathLegs
         */
        private void addMultiPathLegs(String[] paths) {
            if (DEBUG) {
                for (String path : paths) {
                    out.println("path="+path);
                }
            }

            /*
             * First find out if all the paths lead to the same runway.
             */
            String[][] pathParts = new String[paths.length][];
            String[] runways = new String[pathParts.length];
            String runway = "";
            int minj = Integer.MAX_VALUE;
            for (int i = 0 ; i < paths.length ; i++) {
                pathParts[i] = paths[i].split("\\s+");
                minj = Math.min(minj, pathParts[i].length);
                for (int j = 0 ; j < pathParts[i].length ; j++) {
                    if (pathParts[i][j].startsWith("RW")) {
                        runway = pathParts[i][j];
                        runways[i] = runway;
                    }
                }
            }

            /*
             * If they do not then note this so an uncertain line to the airport.will be written.
             * When the next waypoint is reached, it should be found in the ProcFile and
             * this will trigger a re-evaluation of the approach which will eventually lead
             * to a single unambigious runway (and evevtually a single unambigious path).
             */
            for (String rw : runways) {
                if (!runway.equals(rw)) {
                    if (DEBUG) {
                        out.println("different runways  runway="+runway+" rw="+rw);
                    }
                    runway = null;
                    break;
                }
            }

            if (DEBUG) {
                out.println(" runway="+runway+" minj="+minj);
            }

            /*
             * Add all the common legs (if any) before the runway
             */
            boolean runwayLegAdded = false;
            jloop: for (int j = 0 ; j < minj; j++) {
                String str = pathParts[0][j];
                for (int i = 1 ; i < pathParts.length ; i++) {
                    if (!str.equals(pathParts[i][j])) {
                        break jloop;
                    }
                }
                addLeg(str);
                if (str.startsWith("RW")) {
                    runwayLegAdded = true;
                    break jloop;
                }
            }

            /*
             * If the runway was not reached then either add an uncertain leg to the
             * airport (because the runways are different) or add an uncertain leg to it.
             */
            if (!runwayLegAdded) {
                if (runway == null) {
                    legs.add(new Leg(airportId, airportLoc, false, false));
                } else {
                    addLeg(runway, false);
                }
            }

            /*
             * Now look to see if there is a common ending to the paths. This appears to be
             * fairly common. as it seems that quite often one path will differ from another
             * by just having one extra waypoint directly after the runway (this also seems
             * to occur frequently before the runway as well). In these isolated subset cases
             * a single uncertain leg can act as a "catch all," If not, all the missied approach
             * waypoints are omitted (here again, a simpler path will after the missed approach).
             */
            ArrayList<String> ends = new ArrayList<String>();
            boolean certain = false;
            kloop: for (int k = -1 ;; --k) {
                String str = pathParts[0][pathParts[0].length + k];
                for (int i = 1 ; i < pathParts.length ; i++) {
                    String str2 = pathParts[i][pathParts[i].length + k];
                    if (!str.equals(str2)) {
                        break kloop;
                    } else if (str.startsWith("RW")) {
                        certain = true;
                        break kloop;
                    } else {
                        ends.add(0, str);
                    }
                }
            }
            for (String end : ends) {
                addLeg(end, certain);
                certain = true;
            }
        }

        /**
         * addLeg
         */
        private void addLeg(String navId) {
            addLeg(navId, true);
        }

        /**
         * addLeg
         */
        private void addLeg(String navId, boolean certain) {
            /*
             * Occasionally a path entry is synthesized (in ProcFile.java) for runways when
             * they are missing (at least explicitly) from a FINAL section. The location is
             * often known becasue the runway may be explicitly discribed in another FINAL
             * section. However, this is not always case and so the fallback used
             * here is to use the airport location instead (which is known), but also to
             * mark the leg as one that must have the location calculated when it becomes
             * active. This works because a lat/log can be calculated from the a/c position
             * and the direction and distance to the GPS nav target (when the nav id is equal
             * to the FMS entry name). This will always be the near end of the target runway.
             * (It can be a little inaccurate to start with, but after a short time a nice
             * average position can be calculated.)
             */
            float[] posn = file.getLatLon(navId);
            legs.add(new Leg(navId, (posn == null) ? airportLoc : posn, certain, posn == null));
        }


        // ------------------------------------------------------------------------
        //                          Lat / Lon reconstruction
        // ------------------------------------------------------------------------

        /**
         * getCurrentNavPosition
         */
        float[] getCurrentNavPosition() {
            NavigationObject nobj = mm.nor.get_nav_object(avionics.gps_nav_id());  // (Note: This facility is currently disabled)
            if (nobj != null) {
                return new float[] {nobj.lat, nobj.lon};
            } else {
                return getAveragedNavPositionFromGpsData();
            }
        }


       /*
        * The data derived from getCurrentNavPositionFromGpsData() is not always accurate. This is especially
        * true just after a new waypoint is selected by the X-Plane Garmin GPS where lat/lon errors as large
        * as 0.25 degrees can be seen. This only appears to be for the first few (or maybe just one) call
        * but it seems prudent to skip though a few. This is what happens below, after which an average value
        * is calculated.
        */
        private final static int CALC_SKIP = 10;
        private final static int CALC_COUNT = 10;
        String navCalcLast;
        int navCalcCount;
        float[] navCalcFirst;
        float[] navCalcTotals;
        float[] navCalcAverage;

        /**
         * getAveragedNavPositionFromGpsData
         */
        float[] getAveragedNavPositionFromGpsData() {
            String navId = avionics.gps_nav_id();
            if (!navId.equals(navCalcLast)) {
                navCalcLast = navId;
                navCalcCount = 0;
                navCalcTotals = null;
                navCalcAverage = null;
            }
            if(navCalcAverage != null) {
                return navCalcAverage;
            } else {
                float[] res = getCurrentNavPositionFromGpsData();
                navCalcCount++;
                if (navCalcFirst == null) {
                    navCalcFirst = res;
                }
                if (navCalcCount == CALC_SKIP) {
                    navCalcTotals = new float[2];
                }
                if (navCalcTotals != null) {
                    navCalcTotals[0] += res[0];
                    navCalcTotals[1] += res[1];
                }
                if (navCalcCount == CALC_SKIP+CALC_COUNT-1) {
                    navCalcAverage = new float[] {navCalcTotals[0] / CALC_COUNT, navCalcTotals[1] / CALC_COUNT};
                    navCalcTotals = null;
                  //System.out.println("navCalc first=" + navCalcFirst[0] + ","+navCalcFirst[1] + " last=" + res[0] + "," + res[1] + " average=" + navCalcAverage[0] + "," + navCalcAverage[1]);
                    return navCalcAverage;
                } else {
                    return navCalcFirst; // Use first until average is calculated then switch to that
                }
            }
        }

        /**
         * getCurrentNavPositionFromGpsData
         */
        float[] getCurrentNavPositionFromGpsData() {
            double dir = Math.toRadians(aircraft.heading() - aircraft.magnetic_variation() + avionics.get_gps_radio().get_rel_bearing());
            double dis = CoordinateSystem.nm_to_radians(avionics.get_gps_radio().get_distance());
            MovingMap.Geo p0 = MovingMap.Geo.makeGeoDegrees(mm.center_lat, mm.center_lon);
            MovingMap.Geo p1 = p0.offset(dis, dir);
          //System.out.print("getCurrentNavPosition for "+navId+" = " + p1.getLatitude() + ", " + p1.getLongitude());
          //System.out.print(" dist="+avionics.get_gps_radio().get_distance()+" course= " + avionics.get_gps_radio().get_course());
          //System.out.println(" center_lat= " + center_lat + " center_lon=" + center_lon);
            return new float[] {(float)p1.getLatitude(), (float)p1.getLongitude()};
        }
    }


    // ------------------------------------------------------------------------
    //                           Debugging Support
    // ------------------------------------------------------------------------


    /**
     * lastString
     */
    private String lastString;

    /**
     * done
     */
    void done() {
        String res = out.toString();
        if (!res.equals(lastString)) {
            if (res.length() > 0) {
                System.out.println(res);
                System.out.println("--------------------------------------------------------");
            }
            lastString = res;
        }
    }

    /**
     * Prt
     */
    static class Prt {
        StringBuilder sb = new StringBuilder();

        Prt print(Object obj) {
            return print(obj, false);
        }

        Prt println(Object obj) {
            return print(obj, true);
        }

        Prt println() {
            return print("", true);
        }

        private Prt print(Object obj, boolean nl) {
            if (obj instanceof float[]) {
                float[] fx = (float[]) obj;
                if (fx.length == 0) {
                   sb.append("[]");
                } else {
                   sb.append('[');
                   int i = 0;
                   for (; i < fx.length - 1; i++) {
                        sb.append(fx[i]);
                        sb.append(',');
                   }
                   sb.append(fx[i]);
                   sb.append(']');
                }
            } else {
                sb.append(obj);
            }
            if (nl) {
                sb.append('\n');
            }
            if (sb.length() > (1024*1024)) { // Don't run the VM out of memory if prog is stuck in a loop
                System.out.print(toString());
            }

            return this;
        }

        public String toString() {
            if (sb.length() == 0) {
                return "";
            } else {
                String res = sb.toString();
                sb = new StringBuilder();
                return res;
            }
        }
    }

    /**
     * out
     */
    static Prt out = new Prt();
}

