/**
 * JXMap.java
 *
 * This file is taken in part from XHSI, and a project called XPDisplay at:
 *
 *    http://www.duncanjauncey.com/06-xpdisplay/
 *
 * As such is it covered by two copyright notices:
 *
 *    Copyright (c) Duncan Jauncey 2013.   Free for non-commercial use.
 *
 * and
 *
 *    Copyright (C) 2009-2011  Marc Rogiers (marrog.123@gmail.com)
 *
 *    This program is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU General Public License
 *    as published by the Free Software Foundation; either version 2
 *    of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package net.sourceforge.xhsi.flightdeck.nd;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import net.sourceforge.xhsi.*;
import net.sourceforge.xhsi.model.*;
import org.jdesktop.swingx.mapviewer.*;

/**
 * JXMap produces a moving map underlay for the ND panel. It can produce
 * a street map or a satellite image. The map data is read dynamically
 * using a HTTP connection, so for this to work you must be connected to
 * the internet and and not have a firewall blocking the program etc.
 * To use it right click on the ND panel and twiddle with the mouse wheel.
 * If the Java property "tile.cache" is set then the map data will
 * be saved e.g. "java -Dtile.cache=c:\tile\cache -jar xhsi.jar"
 *
 * Demo: https://youtu.be/W7Rb7cuiglk
 */
public class JXMap {

    /**
     * This will load in parallel, the map data not being viewed so that
     * one can switch from the street map to the satellite images (and
     * vise versa) without a long delay because the data is sought from
     * the internet. This feature only works when a disk cache has been
     * configured (See: AbstractTileFactory). If this is done, the basic
     * performace cost is fairly low as the data is loaded into the disk
     * cache with a low priority operation, and it is not loaded into
     * primary storage unless it is subsequently needed.
     */
    private final static boolean PARALLEL_LOADING = true;

    /**
     * Debug option
     */
    private final static boolean DEBUG = false;

    /**
     * Debug option
     */
    private final static boolean SHOW_TILE_BOUNDRIES = DEBUG;


    // ------------------------------------------------------------------------
    //                           The static interface
    // ------------------------------------------------------------------------

    /**
     * No map displayed
     */
    public final static int OFF = 0;

    /**
     * Display street map
     */
    public final static int STREET = 1;

    /**
     * Display satellite images
     */
    public final static int SATELLITE = 2;

    /**
     * Current display mode
     */
    private static int displayMode = OFF;

    /**
     * getMode
     */
    public static int getMode() {
        return displayMode;
    }

    /**
     * setMode
     */
    public static void setMode(int mode) {
        displayMode = mode;
    }

    /**
     * nextMode
     */
    public static void nextMode() {
        displayMode = ++displayMode % 3;
    }


    // ------------------------------------------------------------------------
    //                            The main class
    // ------------------------------------------------------------------------

    private final static TileFactory street = OpenStreetMapTileProvider.getDefaultTileFactory();
    private final static TileFactory satellite = MicrosoftMapTileProvider.getDefaultTileFactory();
    private final XHSISettings settings = XHSISettings.get_instance();
    private final JXMapViewer map = new JXMapViewer();
    private final NDGraphicsConfig nd_gc;
    private final Aircraft aircraft;
    private final FMS fms;
    private int currentMode = OFF;
    private int last_degrees_in_frame_x1000;

    /**
     * JXMap
     */
    public JXMap(ModelFactory model_factory, NDGraphicsConfig nd_gc) {
        this.nd_gc = nd_gc;
        this.aircraft = model_factory.get_aircraft_instance();
        this.fms = aircraft.get_avionics().get_fms();
        removeMouseListeners(map);
    }

    /**
     * Removes the mouse listeners that JXMapViewer unhelpfully provides.
     */
    private void removeMouseListeners(JPanel p) {
        for (MouseListener l : p.getMouseListeners()) {
            p.removeMouseListener(l);
        }
        for (MouseMotionListener l : p.getMouseMotionListeners()) {
            p.removeMouseMotionListener(l);
        }
        for (MouseWheelListener l : p.getMouseWheelListeners()) {
            p.removeMouseWheelListener(l);
        }
    }

    /**
     * paint2D (Called from NDComponent)
     */
    public void paint2D(Graphics2D g2) {
        if (displayMode != OFF) {
            if (currentMode != displayMode) {
                currentMode = displayMode;
                switch (currentMode) {
                    case STREET:    map.setTileFactory(street);    break;
                    case SATELLITE: map.setTileFactory(satellite); break;
                }
            }
            new Calculate().drawMap(g2);
        }
    }

    /**
     * Calculate
     */
    private class Calculate {

        final int   max_frame_pixels = Math.round((float)Math.sqrt(Math.pow(nd_gc.frame_size.width, 2) + Math.pow(nd_gc.frame_size.height, 2))); // Pythagoras
        final int   radius           = nd_gc.rose_radius;
        final int   mult             = nd_gc.map_zoomin ? 100 : 1;
        final int   map_center_x     = nd_gc.map_center_x;
        final int   map_center_y     = nd_gc.map_center_y;
        final float map_up           = (nd_gc.hdg_up) ? aircraft.heading() - aircraft.magnetic_variation() // Heading up
                                     : (nd_gc.trk_up) ? aircraft.track()   - aircraft.magnetic_variation() // Track up
                                     : 0.0f;                                                               // North up

      //final float delta_lat;
        final float delta_lon;
        final float center_lat;
        final float center_lon;
        final float pixels_per_deg_lon;
        final float degrees_per_pixel;

        /**
         * Calculate
         */
        Calculate() {
            if (nd_gc.mode_plan && fms.is_active() && !XHSIPreferences.get_instance().get_plan_aircraft_center()) {
                FMSEntry entry = (FMSEntry) fms.get_displayed_waypoint();
                if (entry == null) {
                    entry = (FMSEntry) fms.get_active_waypoint();
                }
                center_lat = entry.lat;
                center_lon = entry.lon;
            } else {
                center_lat = aircraft.lat();
                center_lon = aircraft.lon();
            }
          //delta_lat = nd_gc.max_range * CoordinateSystem.deg_lat_per_nm();
            delta_lon = nd_gc.max_range * CoordinateSystem.deg_lon_per_nm(center_lat);
            pixels_per_deg_lon = radius / delta_lon * mult;
            degrees_per_pixel  = 1.0f / pixels_per_deg_lon;
        }

        /**
         * drawMap
         */
        private void drawMap(Graphics2D g2) {
            TileFactory tf = map.getTileFactory();
            TileFactoryInfo info = tf.getInfo();
            int zoom = calculateZoom(info);
            double map_stretch = pixels_per_deg_lon / info.getLongitudeDegreeWidthInPixels(zoom);

            AffineTransform tx = new AffineTransform();
            tx.translate(map_center_x, map_center_y);    // Translate center to origin
            tx.scale(map_stretch, map_stretch);          // Account for difference in map scale
            tx.rotate(Math.toRadians(-1.0f * map_up));   // Rotate around origin
          //tx.translate(-map_center_x, -map_center_y);  // Translate origin back to center (Or rather don't)

            if (DEBUG) {
                System.out.println("CB = "+(""+g2.getClipBounds()).substring(18));
                g2.setFont(nd_gc.font_xxxl);
            }

            Point2D centerPixel = tf.geoToPixel(new GeoPosition(center_lat, center_lon), zoom);
            int radius = (max_frame_pixels+1)/2;
            Shape clipBounds = inverseTranslateBounds(g2.getClipBounds(), tx);

            AffineTransform orig = g2.getTransform();
            g2.transform(tx);
            map.paint2D(g2, zoom, centerPixel, radius, clipBounds);
            g2.setTransform(orig);
        }

        /**
         * inverseTranslateBounds
         *
         * The default clipping of rotated windows is very simple and after
         * a small angle of rotation the program can end up pulling a lot
         * image files that are never displayed. This is a more effective
         * system that will create an inverse transformation of the graphics
         * clip bounds.
         */
        private Shape inverseTranslateBounds(Rectangle rect, AffineTransform tx) {
            try {
                return tx.createInverse().createTransformedShape(rect);
            } catch (Exception ex) {
                System.out.println(ex);
                return null;
            }
        }

        /**
         * calculateZoom
         *
         * This currently favors greater detail. Change the break in the
         * for loop if you want a cruder diplay (and hence less I/O).
         */
        private int calculateZoom(TileFactoryInfo info) {
            double degrees_in_frame = max_frame_pixels * degrees_per_pixel;
            int degrees_in_frame_x1000 = (int)(degrees_in_frame * 1000); // Quantize a little so this does not trigger on every frame
            if (degrees_in_frame_x1000 != last_degrees_in_frame_x1000) {
                last_degrees_in_frame_x1000 = degrees_in_frame_x1000;
                double map_stretch = 0;
                int min = info.getMinimumZoomLevel();
                int max = info.getMaximumZoomLevel();
                int zoom = max;
                for (; zoom >= min ; --zoom) {
                    map_stretch = pixels_per_deg_lon / info.getLongitudeDegreeWidthInPixels(zoom);
                    if (map_stretch < 1) {
                        break;
                    }
                }
                if (DEBUG) {
                    System.out.println("zoom1: " + zoom + " map_stretch=" + map_stretch);
                }
                map.setZoom(zoom);
                return zoom;
            } else {
                return map.getZoom();
            }
        }
    }


    // ------------------------------------------------------------------------
    //                              JXMapViewer
    // ------------------------------------------------------------------------

    /**
     * JXMapViewer
     *
     * This is a specialised subclass of the regular class that will do better
     * clipping, However, not all the normal functionality has been retained.
     */
    private static class JXMapViewer extends org.jdesktop.swingx.JXMapViewer {

        /**
         * paint2D
         */
        public void paint2D(Graphics2D g2, int zoom, Point2D centerPixel, int radius, Shape clipBounds) {
            int tileSize     = getTileFactory().getTileSize(zoom);
            int tileRadius   = (2*radius) / tileSize + 2;
            int centerPixelX = (int)centerPixel.getX();
            int centerPixelY = (int)centerPixel.getY();
            int centerTileX  = centerPixelX / tileSize;
            int centerTileY  = centerPixelY / tileSize;
            int total = (2*tileRadius) * (2*tileRadius);
            int[] list = new int[total];
            int p = 0;
            for (int x = -tileRadius ; x < tileRadius ; x++) {
                for (int y = -tileRadius ; y < tileRadius ; y++) {
                    if(checkTile(g2, centerTileX + x, centerTileY + y, centerPixelX, centerPixelY, tileSize, clipBounds)) {
                        int k = x*x + y*y;                      // This is the sort kay
                        int v = ((x & 0xFF) << 8) | (y & 0xFF); // This is the data
                        list[p++] = (k << 16) | v;
                    }
                }
            }
            Arrays.sort(list);     // Sort so the tiles nearest the center of the ND will be loaded first
            int start = total - p; // Skip over the zero entries that were sorted to the head
            String str = "";
            p = 0;
            for (int i = start ; i < total ; i++) {
                int val = list[i];
                int x = ((val >> 8) & 0xFF) << 24 >> 24;
                int y = ((val >> 0) & 0xFF) << 24 >> 24;
                drawTile(g2, centerTileX + x, centerTileY + y, centerPixelX, centerPixelY, tileSize, zoom);

                if (DEBUG) {
                    String id = " #" + p + " [" + x + "," + y + "]";
                    drawCross(g2, centerTileX + x, centerTileY + y, centerPixelX, centerPixelY, tileSize, id, p);
                    str += id;
                    p++;
                }
            }
            if (DEBUG) {
                System.out.println("tileRadius = " + tileRadius + " " + p + "/" + total + " =" + str);
            }
        }

        /**
         * checkTile
         */
        private boolean checkTile(Graphics g2, int tileX, int tileY, int centerPixelX, int centerPixelY, int tileSize, Shape clipBounds) {
            int ox = tileX * tileSize - centerPixelX;
            int oy = tileY * tileSize - centerPixelY;
            return (clipBounds == null) ? true : clipBounds.intersects(new Rectangle(ox, oy, tileSize, tileSize));
        }

        /**
         * drawTile
         */
        private void drawTile(Graphics g2, int tileX, int tileY, int centerPixelX, int centerPixelY, int tileSize, int zoom) {
            int ox = tileX * tileSize - centerPixelX;
            int oy = tileY * tileSize - centerPixelY;
            TileFactory tf = getTileFactory();
            Tile tile = tf.getTile(tileX, tileY, zoom);
            if (PARALLEL_LOADING) {
                 TileFactory other = (tf == street) ? satellite : street;
                 ((org.jdesktop.swingx.mapviewer.AbstractTileFactory)other).prefetchTile(tileX, tileY, zoom);
            }
            if (tile.isLoaded()) {
                g2.drawImage(tile.getImage(), ox, oy, null);
                if (SHOW_TILE_BOUNDRIES) {
                    g2.setColor(Color.RED);
                    g2.drawRect(ox, oy, tileSize, tileSize);
                }
            } else {
                Image image = getLoadingImage();
                int imageX = (tileSize - image.getWidth(null))  / 2;
                int imageY = (tileSize - image.getHeight(null)) / 2;
                g2.setColor(Color.GRAY);
                g2.fillRect(ox, oy, tileSize, tileSize);
                g2.drawImage(image, ox + imageX, oy + imageY, null);
            }
        }

        /**
         * drawCross
         */
        private void drawCross(Graphics g2, int itpx, int itpy, int vpbx, int vpby, int tileSize, String str, int p) {
            int ox = itpx * tileSize - vpbx;
            int oy = itpy * tileSize - vpby;
            int hsize = tileSize / 2;
            int middleX = ox+hsize;
            int middleY = oy+hsize;
            g2.setColor((p == 0) ? Color.YELLOW : Color.GREEN);
            g2.drawLine(middleX-10, middleY,    middleX+10, middleY);
            g2.drawLine(middleX,    middleY-10, middleX, middleY+10);
            g2.setColor((p == 0) ? Color.RED : Color.BLUE);
            g2.drawRect(ox+2, oy+2, tileSize-4, tileSize-4);
            g2.drawString(str, ox+64, oy+64);
            str = "{"+(middleX)+","+(middleY)+","+tileSize+","+tileSize+"}";
            g2.drawString(str, ox+64, oy+128);

        }
    }


    // ------------------------------------------------------------------------
    //                          OpenStreetMapTileProvider
    // ------------------------------------------------------------------------

    /**
     * OpenStreetMapTileProvider
     */
    private static class OpenStreetMapTileProvider {

        private static final int minZoom = 1;
        private static final int maxZoom = 15;
        private static final int totZoom = 17;
        private static final int tileSize = 256;
        private static final boolean xr2l = true;
        private static final boolean yt2b = true;
        private static final String baseURL = "http://tile.openstreetmap.org/";
        private static final TileFactoryInfo OPEN_STREET_MAPS_TILE_INFO =
            new OpenStreetTileFactoryInfo(minZoom, maxZoom, totZoom, tileSize, xr2l, yt2b, baseURL);

        /**
         * getDefaultTileFactory
         */
        public static TileFactory getDefaultTileFactory() {
            return (new DefaultTileFactory(OPEN_STREET_MAPS_TILE_INFO));
        }

        /**
         * OpenStreetTileFactoryInfo
         */
        private static class OpenStreetTileFactoryInfo extends TileFactoryInfo {
            public OpenStreetTileFactoryInfo(int minZoom, int maxZoom, int mapZoom, int tileSize, boolean xr2l, boolean ytb2, String baseURL) {
                super(minZoom, maxZoom, mapZoom, tileSize, xr2l, ytb2, baseURL, null, null, null);
            }
            public String getTileUrl(int x, int y, int zoom) {
                return baseURL + "/" + (totZoom - zoom) + "/" + x + "/" + y + ".png";  // http://c.tile.openstreetmap.org/7/65/41.png
            }
        }
    }

    // ------------------------------------------------------------------------
    //                        MicrosoftMapTileProvider
    // ------------------------------------------------------------------------

    /**
     * MicrosoftMapTileProvider
     */
    public static class MicrosoftMapTileProvider {

        private static final String VERSION = "174";
        private static final int minZoom = 1;
        private static final int maxZoom = 16;
        private static final int totZoom = 17;
        private static final int tileSize = 256;
        private static final boolean xr2l = true;
        private static final boolean yt2b = true;
        private static final String baseURL = "http://a2.ortho.tiles.virtualearth.net/tiles/a";
        private static final MicrosoftTileFactoryInfo MICROSOFT_MAPS_TILE_INFO =
            new MicrosoftTileFactoryInfo(minZoom, maxZoom, totZoom, tileSize, xr2l, yt2b, baseURL);

        /**
         * getDefaultTileFactory
         */
        public static TileFactory getDefaultTileFactory() {
            return (new DefaultTileFactory(MICROSOFT_MAPS_TILE_INFO));
        }

        /**
         * MicrosoftTileFactoryInfo
         */
        private static class MicrosoftTileFactoryInfo extends TileFactoryInfo {
            private MicrosoftTileFactoryInfo(int minZoom, int maxZoom, int mapZoom, int tileSize, boolean xr2l, boolean ytb2, String baseURL) {
                super(minZoom, maxZoom, mapZoom, tileSize, xr2l, ytb2, baseURL, null, null, null);
            }
            public String getTileUrl(int x, int y, int zoom) {
                return baseURL + xyzoom2quadrants(x, y, zoom) + ".jpeg?g=" + VERSION;
            }
            public String xyzoom2quadrants(int x, int y, int zoom) {
                StringBuffer quad = new StringBuffer();
                int level = 1 << (maxZoom - zoom);
                while (level > 0) {
                    int ix = 0;
                    if (x >= level) {
                        ix++;
                        x -= level;
                    }
                    if (y >= level) {
                        ix += 2;
                        y -= level;
                    }
                    quad.append(ix);
                    // now descend into that square
                    level /= 2;
                }
                return new String(quad);
            }
        }
    }
}

