// Disassembled from Jar file

package org.jdesktop.swingx.mapviewer;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jdesktop.beans.AbstractBean;

public class Tile extends AbstractBean {
  private static final Logger LOG = Logger.getLogger(Tile.class.getName());
  private static int nextNumber = 0;
  private int order = nextNumber++;
  private Priority priority = Priority.High;
  private boolean isLoading = false;
  private Throwable error;
  private String url;
  private boolean loaded = false;
  private int zoom;
  private int x;
  private int y;
  SoftReference<BufferedImage> image = new SoftReference(null);
  private PropertyChangeListener uniqueListener = null;
  private TileFactory dtf;

  public Tile(int x, int y, int zoom) {
    loaded = false;
    this.zoom = zoom;
    this.x = x;
    this.y = y;
  }

  Tile(int x, int y, int zoom, String url, Priority priority, TileFactory dtf) {
    this.url = url;
    loaded = false;
    this.zoom = zoom;
    this.x = x;
    this.y = y;
    this.priority = priority;
    this.dtf = dtf;
  }

  public synchronized boolean isLoaded() {
    return loaded;
  }

  public synchronized int getPriortyOrder() {
    return (order & 0x3FFFFFFF) | (priority == Priority.Low ? 0x40000000 : 0);
  }

  synchronized void setLoaded(boolean loaded) {
    boolean old = isLoaded();
    this.loaded = loaded;
    firePropertyChange("loaded", Boolean.valueOf(old), Boolean.valueOf(isLoaded()));
  }

  public Throwable getUnrecoverableError() {
    return error;
  }

  public Throwable getLoadingError() {
    return error;
  }

  public BufferedImage getImage() {
    BufferedImage img = (BufferedImage)image.get();
    if (img == null) {
      setLoaded(false);
      dtf.startLoading(this);
    }
    return img;
  }

  public int getZoom() {
    return zoom;
  }

  public void addUniquePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    if ((uniqueListener != null) && (uniqueListener != listener)) {
      removePropertyChangeListener(propertyName, uniqueListener);
    }
    if (uniqueListener != listener) {
      uniqueListener = listener;
      addPropertyChangeListener(propertyName, uniqueListener);
    }
  }

  void firePropertyChangeOnEDT(final String propertyName, final Object oldValue, final Object newValue) {
    if (!EventQueue.isDispatchThread())
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          firePropertyChange(propertyName, oldValue, newValue);
        }
      });
  }

  private static void p(String string) {
    System.out.println(string);
  }

  public Throwable getError() {
    return error;
  }

  public void setError(Throwable error) {
    this.error = error;
  }

  public boolean isLoading() {
    return isLoading;
  }

  public void setLoading(boolean isLoading) {
    this.isLoading = isLoading;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public String getURL() {
    return url;
  }

  public int getX() { return x;
  }

  public int getY() { return y;
  }

  static {
    LOG.setLevel(Level.OFF);
  }

  public static enum Priority {
    High, Low;
  }
}