/**
 * Element.java
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

import net.sourceforge.xhsi.XHSI;
import net.sourceforge.xhsi.XHSISettings;
import net.sourceforge.xhsi.model.Aircraft;
import net.sourceforge.xhsi.model.Avionics;

/**
 * Element
 */
public abstract class Element {

    public final static String TRUE  = "$$TRUE$$";
    public final static String FALSE = "$$FALSE$$";
    public final static String ARMED = "$$ARMED$$";

    public final static int LEFT   = 1;
    public final static int CENTER = 2;
    public final static int RIGHT  = 3;

    private String  text;
    private boolean blank         = false;
    private float   fontAdjust    = 1;
    private int     justify       = CENTER;
    private String  buttonColor   = null;
    private Color   foreground    = Color.WHITE;
    private Color   background    = Color.BLACK;
    private Color   trueColor     = Color.GREEN;
    private Color   falseColor    = Color.WHITE;
    private Color   armedColor    = Color.ORANGE;
    private Color   borderColor   = null;
    private int     borderWidth   = 2;
    private int     blinkRate     = 1000;
    private boolean blinking      = true;
    private boolean xplaneRunning = true;

    protected String  getText()         { return text;          }
    protected boolean getBlank()        { return blank;         }
    protected float   getFontAdjust()   { return fontAdjust;    }
    protected int     getJustify()      { return justify;       }
    protected String  getButtonColor()  { return buttonColor;   }
    protected Color   getForeground()   { return foreground;    }
    protected Color   getBackground()   { return background;    }
    protected Color   getTrueColor()    { return trueColor;     }
    protected Color   getFalseColor()   { return falseColor;    }
    protected Color   getArmedColor()   { return armedColor;    }
    protected Color   getBorderColor()  { return borderColor;   }
    protected int     getBorderWidth()  { return borderWidth;   }
    protected int     getBlinkRate()    { return blinkRate;     }
    protected boolean getBlinking()     { return blinking;      }
    protected boolean isXplaneRunning() { return xplaneRunning; }

    /**
     * setText
     */
    final void setText(String str) {
        if (str == null) {
            text = "null";
        } else {
            text = escape(str);
        }
    }

    /**
     * setBackground
     */
    final void setBackground(Color color) {
        background = color;
    }

    /**
     * setForeground
     */
    final void setForeground(Color color) {
        foreground = color;
    }

    /**
     * setTextColor
     */
    final void setButtonColor(String str) {
        buttonColor = str;
    }

    /**
     * setButtonColors
     */
    final void setButtonColors(Color tc, Color fc, Color ac) {
        if (tc != null) trueColor  = tc;
        if (fc != null) falseColor = fc;
        if (ac != null) armedColor = ac;
    }

    /**
     * setButtonColors
     */
    final void setButtonColors(Color tc, Color fc) {
        setButtonColors(tc, fc, null);
    }

    /**
     * setBorderColor
     */
    final void setBorderColor(Color color) {
        borderColor = color;
    }

    /**
     * setJustifyRight
     */
    final void setJustifyRight() {
        justify = RIGHT;
    }

    /**
     * setJustifyLeft
     */
    final void setJustifyLeft() {
        justify = LEFT;
    }

    /**
     * setFontAdjust
     */
    final void setFontAdjust(float value) {
        fontAdjust = value;
    }

    /**
     * setBlinkRate
     */
    final void setBlinkRate(int rate) {
        blinkRate = rate;
    }

    /**
     * setBorderWidth
     */
    final void setBorderWidth(int width) {
        borderWidth = width;
    }

    /**
     * color
     */
    final void color(Color color) {
        if (color != null) {
            foreground = color;
        }
    }

    /**
     * noForeground
     */
    final void noForeground() {
        blank = true;
    }

    /**
     * box
     */
    final void box(Color color, boolean blinking) {
        if (color != null) {
            setBorderColor(color);
            this.blinking = blinking;
            if (color.equals(Color.RED)) {
                setBlinkRate(500);
            }
        }
    }

    /**
     * box
     */
    final void box(Color color) {
        box(color, true);
    }

    /**
     * set
     */
    final void set(String str) {
        setText(str);
    }

    /**
     * set
     */
    final void set(int n) {
        setText(Integer.toString(n));
    }

    /**
     * set
     */
    final void set(float f) {
        setText(String.format("%.1f", f));
    }

    /**
     * set
     */
    final void set(boolean isOn, boolean isArmed) {
        if (isOn) {
            setButtonColor(TRUE);
        } else if (isArmed) {
            setButtonColor(ARMED);
        } else {
            setButtonColor(FALSE);
        }
    }

    /**
     * set
     */
    final void set(boolean isOn) {
        set(isOn, false);
    }

    /**
     * updateElement
     */
    final void updateElement(Analysis a) {
        blank         = false;
        blinking      = true;
        blinkRate     = 1000;
        borderWidth   = 2;
        borderColor   = null;
        xplaneRunning = a.isValid();
        update(a);
    }

    /**
     * update
     */
    abstract void update(Analysis a);


    /**
     * escape
     */
    public static  String escape(String str) {
        if (str != null) {
            str = str.replace("\\s", " ");
            str = str.replace("\\^", "\u02c4"); //  Up arrow
            str = str.replace("\\v", "\u02c5"); //  Down arrow
            str = str.replace('-', '\u2013');
        }
        return str;
    }

    /**
     * s2int
     */
    public static int s2int(String str, int dflt) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
        }
        return dflt;
    }

    /**
     * s2float
     */
    public static float s2float(String str, float dflt) {
        try {
            return Float.parseFloat(str);
        } catch (Exception ex) {
        }
        return dflt;
    }

    final XHSISettings settings = XHSISettings.get_instance();
    protected XHSI xhsi;
    protected Aircraft aircraft;
    protected Avionics avionics;

    /**
     * setContext (These values should really be passed via a constructor,
     * but not having to implement all these methods in the subclasses
     * of IlluminatedButton make this hack desirable.)
     */
    final void setContext(XHSI xhsi, Aircraft aircraft, Avionics avionics) {
        this.xhsi     = xhsi;
        this.aircraft = aircraft;
        this.avionics = avionics;
    }

    /**
     * init
     */
    void init() {
    }

    /**
     * prt
     */
    static void prt(Object str) {
        System.out.println(str);
    }
}
