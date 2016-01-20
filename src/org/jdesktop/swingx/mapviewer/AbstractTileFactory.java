// Disassembled from Jar file

package org.jdesktop.swingx.mapviewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jdesktop.swingx.mapviewer.util.GeoUtil;
import org.jdesktop.swingx.util.PaintUtils;

import java.io.*;

public abstract class AbstractTileFactory extends TileFactory {

    /**
     * SYNC
     */
    private final static Object SYNC = new Object();

    /**
     * LOG
     */
    private final static Logger LOG = Logger.getLogger(AbstractTileFactory.class.getName());

    /**
     * cacheDir
     */
    private static File cacheDir;
    static {
        String str = System.getProperty("tile.cache");
        if (str != null) {
            File f = new File(str);
            if (!f.exists()) {
                System.err.println("Cache directory '" + str + "' does not exist");
            } else {
                cacheDir = f;
            }
        }
    }

    /**
     * USING_DISK_CACHE
     */
    private final static boolean USING_DISK_CACHE = cacheDir != null;

    /**
     * MAX_MAP_ENTRIES
     */
    private final static int MAX_MAP_ENTRIES = USING_DISK_CACHE ? 250 : 2500;

    /**
     * tileMap
     */
    private final static Map<String,Tile> tileMap = new LinkedHashMap<String,Tile>(MAX_MAP_ENTRIES, 0.75f, true) { // access order
        protected boolean removeEldestEntry(Map.Entry<String,Tile> eldest) {
            return size() >  MAX_MAP_ENTRIES && !eldest.getValue().isLoading();
        }
    };

    /**
     * tileQueue
     */
    private final static BlockingQueue<Tile> tileQueue = new PriorityBlockingQueue(5, new Comparator() {
        public int compare(Object t1, Object t2) {
            int o1 = ((Tile)t1).getPriortyOrder();
            int o2 = ((Tile)t2).getPriortyOrder();
            return (o1 < o2) ? -1 : 1; // Can never be equal
        }
        public boolean equals(Object obj) {
            return obj == this;
        }
    });

    /**
     * THREADPOOLSIZE
     */
    private final static int THREADPOOLSIZE = 10;

    /**
     * service
     */
    private final static ExecutorService service = Executors.newFixedThreadPool(THREADPOOLSIZE,
        new ThreadFactory() {
            private int count = 0;
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "tile-pool-" + this.count++);
                t.setPriority(1);
                t.setDaemon(true);
                return t;
            }
        }
    );

    /**
     * AbstractTileFactory
     */
    public AbstractTileFactory(TileFactoryInfo info) {
        super(info);
    }

    /**
     * getTile
     */
    public Tile getTile(int x, int y, int zoom) {
        return getTile(x, y, zoom, false);
    }

    /**
     * prefetchTile
     */
    public void prefetchTile(int x, int y, int zoom) {
        if (USING_DISK_CACHE) {
            getTile(x, y, zoom, true);
        }
    }

    /**
     * getTile
     */
    private Tile getTile(int tpx, int tpy, int zoom, boolean prefetch) {
        int numTilesWide = (int)getMapSize(zoom).getWidth();
        int tileX = tpx;
        if (tileX < 0) {
            tileX = numTilesWide - Math.abs(tileX) % numTilesWide;
        }
        tileX %= numTilesWide;
        int tileY = tpy;
        String url = getInfo().getTileUrl(tileX, tileY, zoom);
        if (prefetch && tileInCacheDir(url)) {
            return null;
        } else {
            synchronized (SYNC) {
                Tile tile = tileMap.get(url);
                if (tile != null) {
                    if (!prefetch && !tile.isLoaded() && tile.getPriority() == Tile.Priority.Low) {
                        promote(tile);
                    }
                } else {
                    boolean valid = GeoUtil.isValidTile(tileX, tileY, zoom, getInfo());
                    if (!valid) {
                        tile = new Tile(tileX, tileY, zoom);
                    } else {
                        Tile.Priority pri = prefetch ? Tile.Priority.Low : Tile.Priority.High;
                        tile = new Tile(tileX, tileY, zoom, url, pri, this);
                    }
                    tileMap.put(url, tile);
                    if (valid) {
                        startLoading(tile);
                    }
                }
                return tile;
            }
        }
    }

    /**
     * promote
     */
    private void promote(Tile tile) {
        synchronized (SYNC) {
            if (tileQueue.contains(tile)) {
                try {
                    tileQueue.remove(tile);
                    tile.setPriority(Tile.Priority.High);
                    tileQueue.put(tile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * startLoading
     */
    protected void startLoading(Tile tile) {
        synchronized (SYNC) {
            if (tile.isLoading()) {
                System.out.println("already loading. bailing");
                return;
            } else {
                tile.setLoading(true);
                try {
                    tileQueue.put(tile);
                    service.submit(new TileRunner());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public TileCache getTileCache() {
        return null;
    }

    public void setTileCache(TileCache cache) {
    }

    public void setThreadPoolSize(int size) {
    }

    /**
     * tileToFile
     */
    private static File tileToFile(Tile tile) {
        return tileToFile(tile.getURL());
    }

    /**
     * tileToFile
     */
    private static File tileToFile(String url) {
        String cacheName = url.substring("http://".length()).replace('/', '~').replaceAll("\\?.*$", "");
        return new File(cacheDir, cacheName);
    }

    /**
     * tileInCacheDir
     */
    private boolean tileInCacheDir(String url) {
        Boolean res = fileExistance.get(url);
        if (res == null) {
            res = tileToFile(url).exists();
            if (fileExistance.size() > 256) {
                fileExistance = new HashMap<String,Boolean>(256);
              //System.out.println("fileExistance flushed");
            }
            if (res) {
                fileExistance.put(url, res);
            }
        }
        return res;
    }
    private HashMap<String,Boolean> fileExistance = new HashMap<String,Boolean>(256);

    /**
     * TileRunner
     */
    private class TileRunner implements Runnable {

        /**
         * run
         */
        public void run() {
            Tile tile;
            synchronized (SYNC) {
                tile = tileQueue.remove();
            }
            BufferedImage image = null;
            try {
                image = loadImage(tile);
            } finally {
                setImage(tile, image);
            }
        }

        /**
         * loadImage
         */
        private BufferedImage loadImage(Tile tile) {
            String url = tile.getURL();
            if (url != null) {
                for (int trys = 4 ; trys >= 0 ; --trys) {
                    try {
                        return readIntoCache(tile);
                    } catch (Throwable ex) {
                        tile.setError(ex);
                        if (ex.toString().contains("timed out") && trys > 0) {
                            AbstractTileFactory.LOG.log(Level.INFO, "Timeout loading: " + url);
                        } else {
                            AbstractTileFactory.LOG.log(Level.SEVERE, "Failed to load a tile at url: " + url, ex);
                        }
                    }
                }
            }
            return null;
        }

        /**
         * setImage
         */
        private void setImage(final Tile tile, final BufferedImage img) {
            try {
                SwingUtilities.invokeAndWait(
                    new Runnable() {
                        public void run() {
                            setImage0(tile, img);
                        }
                    }
                );
            } catch (Exception ex) {
                setImage0(tile, img);
            }
        }

        /**
         * setImage0
         */
        private void setImage0(Tile tile, BufferedImage img) {
            synchronized (SYNC) {
                tile.setLoading(false);
                if (img != null) {
                    tile.image = new SoftReference(img);
                    tile.setLoaded(true);
                } else if (USING_DISK_CACHE && tile.getPriority() == Tile.Priority.Low) {
                    // The tile entry is not really needed -- See note below.
                    tileMap.remove(tile.getURL());
                    //System.out.println("-- Checked: "+tile.getURL());
                }
            }
        }

        /**
         * readIntoCache
         */
        private BufferedImage readIntoCache(Tile tile) throws Exception {
            byte[] data = readIntoCache0(tile);
            return (data == null) ? null : PaintUtils.loadCompatibleImage(new ByteArrayInputStream(data));
        }

        /**
         * readIntoCache0
         *
         * If the system is caching images on the disk and this tile request is
         * speculative then this fuction does not need to return the data, it just
         * must make sure it exists on disk. If it is needed in the future then
         * no HTTP request will be needed and it may be in the file system cache.
         */
        private byte[] readIntoCache0(Tile tile) throws Exception {
         // Thread.sleep(1000); // Slow to make it easier to see load order
            String url = tile.getURL();
            if (USING_DISK_CACHE) {
                File file = tileToFile(tile);
                long lth = file.length();
                if (lth > 0) {
                    if (tile.getPriority() == Tile.Priority.Low) {
                        return null;
                    } else {
                        FileInputStream fis = new FileInputStream(file);
                        byte[] data = new byte[(int)lth];
                        fis.read(data);
                        fis.close();
                      //System.out.println("++ Read: "+tileToFile(url));
                        return data;
                    }
                }
            }
          //AbstractTileFactory.LOG.log(Level.INFO,"Fetching: "+url);
            System.out.println("*** Fetching: "+url);
            InputStream is = new URL(url).openStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            while (true) {
                int n = is.read(buf);
                if (n == -1) {
                    break;
                }
                bout.write(buf, 0, n);
            }
            byte[] data = bout.toByteArray();
            is.close();
            if (USING_DISK_CACHE) {
                FileOutputStream fos = new FileOutputStream(tileToFile(tile));
                fos.write(data);
                if (tile.getPriority() == Tile.Priority.Low) {
                    data = null;
                }
            }
            return data;
        }
    }
}