/**
 * ProcFile.java
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
 * ProcFile
 */
public class ProcFile {

    /**
     * Debug option
     */
    private final static boolean DEBUG = true;

    /**
     * This is a set path terminators found in entries that that have both
     * a lat/lon and waypoint identifier specified.
     *
     * See: http://www.peter2000.co.uk/aviation/misc/rnavmanual1_8.pdf
     */
    private final static HashSet<String> PATH_TERMINATORS = new HashSet<String>();
    static {
        PATH_TERMINATORS.addAll(
            Arrays.asList(
                new String[] {
                   "AF",  // Arc to fix
                // "CA",  // Course to altitude
                // "CD",  // Course to a DME distance
                   "CF",  // Course to fix
                // "CI",  // Course to intercept
                // "CR",  // Course to radial termination
                   "DF",  // Direct to fix
                   "FA",  // Fix to altitude
                   "FC",  // Track from fix to distance leg
                   "FD",  // Track from fix to DME distance
                   "FM",  // From a fix to a manual termination
                   "HA",  // Racetrack pattern/course reversals at fix term inating at altitude
                   "HF",  // Racetrack pattern/course reversals at fix terminating at fix
                   "HM",  // Racetrack pattern/course reversals at fix with manual termination
                   "IF",  // Initial fix
                   "PI",  // Procedure turn
                   "RF",  // Constant radius arc
                   "TF",  // Track to fix
                // "VA",  // Heading to an altitude
                // "VD",  // Heading to a DME distance
                // "VI",  // Heading to an intercept
                // "VM",  // Heading to a manual termination
                // "VR",  // Heading to a radial termination
                   "RW",  // Special runway entry just used here (and does not have a lat/log)
                }
            )
        );
    }

    /**
     * sections
     */
    private final ArrayList<Section> sections = new ArrayList<Section>();

    /**
     * paths
     */
    private final HashMap<String,HashSet<String>> paths = new HashMap<String,HashSet<String>>();

    /**
     * coords
     */
    private final HashMap<String,float[]> coords = new HashMap<String,float[]>();

    /**
     * distances
     */
    private double[] distances;

    /**
     * navIds
     */
    private String[] navIds;

    /**
     * load
     */
     public static ProcFile load(String icao) {
         String data = readIcao(icao);
         return (data == null) ? null : new ProcFile(data);
     }

    /**
     * ProcFile
     */
    private ProcFile(String data) {
        createSections(data);
        processSections();
    }

    /**
     * toString
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (DEBUG) {
            sb.append("--sections--\n");
            for (Section sect : sections) {
                sect.append(sb);
            }
        }
        sb.append("--paths--\n");
        String[] keys = paths.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (String key : keys) {
            sb.append(key).append('\n');
            for (String path : paths.get(key)) {
                sb.append("    ");
                sb.append(path);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * sectionStart
     */
    private static boolean sectionStart(String line) {
        return line.matches("^(SID|STAR|APPTR|FINAL),.*");
    }

    /**
     * readIcao
     */
    private static String readIcao(String icao) {
        if (icao != null && icao.length() > 0) {
            return readFile(new File(XHSIPreferences.defaultDataDirectory(), "GNS430/navdata/Proc"), icao + ".txt");
        } else {
            return null;
        }
    }

    /**
     * readFile
     */
    private static String readFile(File dir, String name) {
        File file = new File(dir, name);
        if (file.exists()) {
            try {
                return new Scanner(file).useDelimiter("\\Z").next().replaceAll("\r", ""); // File -> String
            } catch (Exception ex) {
            }
        }
        return null;
    }

    /**
     * createSections
     */
    private void createSections(String data) {
        TreeMap<Double,String> dists = new TreeMap<Double,String>();
        String[] lines = data.split("\n");
        int i = 0;
        while (i < lines.length && !sectionStart(lines[i])) {
            i++; // Skip to first secton start
        }
        while (i < lines.length) {
            String header = lines[i++];
            Section sect = new Section(header);
            int runwayPos = -1;
            if (sect.isFinal()) {
                try {
                    runwayPos = Integer.parseInt(sect.header[4]); // Entry that should be the runway
                } catch (Exception ex) {
                }
            }
            int ec = 0;
            while (i < lines.length && !sectionStart(lines[i])) {
                 String[] entry = sect.addEntry(lines[i++]);
                 try {
                     if (entry != null && PATH_TERMINATORS.contains(entry[0])) {
                         String name = entry[1];
                         String lat  = entry[2];
                         String lon  = entry[3];
                         float[] latlon = new float[] {Float.parseFloat(lat), Float.parseFloat(lon)};
                         coords.put(name, latlon);
                         double d = latlon[0]*latlon[0] + latlon[1]*latlon[1]; // Pythagoras w/o the sqrt
                         dists.put(d, name);
                     }
                 } catch (Exception ex) {
                     System.out.println("Error parsing " + lines[i-1] + " ex=" + ex);
                 }
                 ec++;
                 /*
                  * If this is a FINAL and the entry indexed by runwayPos does not specify the
                  * runway then insert a special "RW" entry after this to show this is where
                  * the to-runway leg is
                  */
                 if (runwayPos == ec && !entry[1].startsWith("RW")) {
                     sect.addEntry("RW,RW"+sect.header[2]);
                     sect.hasRunway = true;
                 }
            }
            distances = new double[dists.size()];
            navIds    = new String[dists.size()];
            int n = 0;
            for (Map.Entry<Double,String> ent : dists.entrySet()) {
                distances[n] = ent.getKey();
                navIds[n]    = ent.getValue();
                n++;
            }
            sections.add(sect);
        }
    }

    /**
     * getLatLon
     */
    public float[] getLatLon(String id) {
        return coords.get(id.toUpperCase());
    }

    /**
     * findClosestNavId (This feature is currently unused)
     */
    public String findClosestNavId(float[] latlon) {
        if (distances.length == 0) {
            return null;
        } else {
            double d = latlon[0]*latlon[0] + latlon[1]*latlon[1]; // Pythagoras w/o the sqrt
            int i = closest(distances, d);
            return navIds[i];
        }
    }

    /**
     * closest
     *
     * See http://codereview.stackexchange.com/questions/47328/find-the-value-nearest-to-k-in-the-sorted-array
     */
    public static int closest(double[] a, double x) {
        int i = Arrays.binarySearch(a, x);
        if (i >= 0) {
            return i;
        } else {
            i = 0 - i - 1;
            if (i == 0) {
                return 0;
            } else if (i >= a.length) {
                return a.length - 1;
            } else {
                return Math.abs(x - a[i-1]) < Math.abs(x - a[i]) ? i-1 : i;
            }
        }
    }

    /**
     * processSections
     */
    private void processSections() {
        /*
         * Sort the sections (not needed)
         */
        if (DEBUG) {
            Collections.sort(sections, new Comparator<Section>() {
                public int compare(Section a, Section b) {
                    return a.sortKey().compareTo(b.sortKey());
                }
            });
        }

        /*
         * Make a first pass through all the sections to build a map
         * of all the FINAL sections  This is indexed by the identifier
         * that links the APPTR sections to the FINAL sections.
         */
        HashMap<String,Section> finals = new HashMap<String,Section>();
        for (Section sect : sections) {
            if (sect.isFinal()) { // Only FINALs
                finals.put(sect.header[1], sect);
            }
        }

        /*
         * The pathSet is a temporary collection of all the all possible paths, through
         * the graph, so if a path is found like (a -> b -> c -> d) it will also include
         * (b -> c -> d) and (c -> d). Later these are indexed by the first term
         * so all the possible next waypoints can be found in a context free manner.
         */
        HashSet<String> pathSet = new HashSet<String>();

        /*
         * Make a second pass through all the sections looking for all the APPTR
         * records. Each of these have the entries in their corrisponding FINAL
         * section merged on the end. The FINAL records are also added on their
         * own to the set of possible paths as they can be used without an APPTR.
         */
        for (Section sect : sections) {
            if (sect.isApptr() || sect.isFinal()) { // Both APPTRs and FINALs
                ArrayList<String> list = new ArrayList<String>();
                String last = "";
                if (sect.isApptr()) {
                    for (String[] entry : sect.entries) {
                        /*
                         * Only use records that have waypoint identifiers, and filter out any
                         * identifiers that are are the same as the last one
                         */
                        if (PATH_TERMINATORS.contains(entry[0]) && !last.equals(entry[1])) {
                            last = entry[1]; // The Waypoint name
                            list.add(last);
                        }
                    }
                    String id = sect.header[1]; // This is the identidier that ties the APPTR to the FINAL
                    sect = finals.get(id);      // Setup to append the FINAL entries
                }
                /*
                 * Now append the entries in the related FINAL section
                 */
                if (sect != null) {
                    boolean hasRunway = sect.hasRunway;
                    String[] lastEntry = null;
                    boolean lastIsHM = false;
                    for (String[] entry : sect.entries) {
                        /*
                         * Here the same fitering is done as before, but additionally a runway
                         * identifier is inserted in all FINAL sections where none is present.
                         *
                         * (This is a legacy feature that shoukd not be needed any longer but has
                         * been retained while the new solution to this problem in createSections()
                         * is evaluaded).
                         */
                        if (!PATH_TERMINATORS.contains(entry[0])) {
                            if (!hasRunway) {
                                System.out.println("WARNING Final w/o runway in " + sect.header[1] + " *********************************************");
                                last = "RW"+sect.header[2];
                                list.add(last); // Add runway specifier in place of the first non-lat/log entry
                                hasRunway = true;
                            }
                        } else if (!last.equals(entry[1])) {
                            last = entry[1]; // The Waypoint name
                            list.add(last);
                        }
                        lastIsHM = entry[0].equals("HM");
                    }
                    /*
                     * If the FINAL ends with a HM entry then add the last entry again to reflect that this is a hold
                     */
                    if (lastIsHM) {
                        list.add(last);
                    }
                }
                /*
                 * 'list' has all the waypoints from the start of the APPTR section to the end of
                 * its FINAL. All the possible paths from any point in this chain and are now added
                 * to the path list. If the path starts before the runway identifier it is part of
                 * the approach and it's first term is unchanged. If it occurs at or after the
                 * runway identifier it is part of a missed approach and the first term gets a '-'
                 * appended. This difference can be used to narrow the possible set of paths, if it
                 * is known that the a/c has passed the runway and the button is .pressed to remove
                 * the 'susp' condition.
                 */
                String[] alist = list.toArray(new String[0]);
                String suffix = "";
                for (int i = 1 ; i < alist.length ; i++) {
                    StringBuilder sb = new StringBuilder();
                    String key = alist[i-1];
                    if (key.startsWith("RW")) {
                        suffix = "-";
                    }
                    sb.append(key).append(suffix);
                    boolean missed = suffix.length() > 0;
                    for (int j = i ; j < alist.length ; j++) {
                        String str = alist[j];
                        sb.append(' ').append(missed ? str.toLowerCase() : str); // l/c = part of missied approach
                        missed |= str.startsWith("RW");

                    }
                    pathSet.add(sb.toString());
                }
            }
        }
        /*
         * Finally a map is created of all the paths. This is indexed by the first term and
         * contains a set of all possible paths from that waypoint forward.
         */
        for (String path : pathSet) {
            int pos = path.indexOf(' ');
            String key = path.substring(0, pos);
            String val = path.substring(pos + 1);
            HashSet<String> hs = paths.get(key);
            if (hs == null) {
                hs = new HashSet<String>();
                paths.put(key, hs);
            }
            hs.add(val.trim());
        }
    }

    /**
     * getApproachPaths
     */
    public HashSet<String> getApproachPaths(String start) {
        return paths.get(start);
    }

    /**
     * getMissedPaths
     */
    public HashSet<String> getMissedPaths(String start) {
        return paths.get(start + "-");
    }

    /**
     * Section
     */
    private static class Section {

        /**
         * header
         */
        String[] header;

        /**
         * entries
         */
        ArrayList<String[]> entries = new ArrayList<String[]>();

        /**
         * entries
         */
        boolean hasRunway = false;

        /**
         * Section
         */
        Section(String line) {
            header = line.split(",");
        }

        /**
         * isApptr
         */
        boolean isApptr() {
            return header[0].charAt(2) == 'P'; // apPtr
        }

        /**
         * isFinal
         */
        boolean isFinal() {
            return header[0].charAt(2) == 'N'; // fiNal
        }

        /**
         * isStar
         */
        boolean isStar() {
            return header[0].charAt(2) == 'A'; // stAr
        }

        /**
         * isSid
         */
        boolean isSid() {
            return header[0].charAt(2) == 'D'; // siD
        }

        /**
         * addEntry
         */
        String[] addEntry(String line) {
            if (line.length() > 0) {
                String[] parts = line.split(",");
                entries.add(parts);
                hasRunway |= parts[1].startsWith("RW");
                return parts;
            } else {
                return null;
            }
        }

        /**
         * sortKey
         *
         * Order = SID, STAR, APPTR, FINAL
         */
        public String sortKey() {
            StringBuilder sb = new StringBuilder();
            if (header[0].charAt(0) == 'S') {
               sb.append('1');
            } else {
               sb.append('2'); // Make APPTR, FINAL file after STAR and SID
            }
            return append(sb, header).toString();
        }

        /**
         * toString
         */
        public String toString() {
            return append(new StringBuilder()).toString();
        }

        /**
         * append
         */
        private StringBuilder append(StringBuilder sb) {
            append(sb, header);
            sb.append('\n');
            for (String[] entry : entries) {
                 sb.append("    ");
                 append(sb, entry);
                 sb.append('\n');
            }
            return sb;
        }

        /**
         * append
         */
        private StringBuilder append(StringBuilder sb, String[] parts) {
            int n = 0;
            for (String str : parts) {
                if (n++ > 0) {
                    sb.append(",");
                }
                sb.append(str);
            }
            return sb;
        }
    }
}
