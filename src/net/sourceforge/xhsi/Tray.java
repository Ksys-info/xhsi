/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This a modified version of the code taken from:
 *
 *    https://docs.oracle.com/javase/tutorial/uiswing/examples/misc/TrayIconDemoProject/src/misc/TrayIconDemo.java
 *
 * See:
 *
 *    https://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html
 */
package net.sourceforge.xhsi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Tray {

    /**
     * tray
     */
    private static SystemTray tray = SystemTray.getSystemTray();

    /**
     * trayIcon
     */
    private static TrayIcon trayIcon;

    /**
     * init
     */
    public static void init(final XHSI xhsi, Image image) {
        trayIcon = new TrayIcon(image);
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE); // Turn off metal's use of bold fonts
        SwingUtilities.invokeLater(new Runnable() {      // Schedule a job for the event-dispatching thread adding TrayIcon.
            public void run() {
                createAndShowGUI(xhsi);
            }
        });
    }

    /**
     * createAndShowGUI
     */
    private static void createAndShowGUI(final XHSI xhsi) {
        if (SystemTray.isSupported()) {
            PopupMenu popup       = new PopupMenu();
            MenuItem  exitItem    = new MenuItem("Exit");
            MenuItem  restartItem = new MenuItem("Restart");
            final MenuItem  aboutItem   = new MenuItem("About");
            MenuItem  flipItem    = new MenuItem("Switch Always On Top");
            flipItem.setFont(new Font("Dialog", Font.BOLD, 12));

            popup.add(flipItem);
            popup.addSeparator();
            if (XHSI.RESTART_CODE != 0) {
                popup.add(restartItem);
            }
            popup.add(exitItem);
            popup.addSeparator();
            popup.add(aboutItem);

            trayIcon.setPopupMenu(popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("XHSI");

            try {
                tray.add(trayIcon);
            } catch (AWTException ex) {
                System.err.println("TrayIcon could not be added.");
                return;
            }

            trayIcon.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    xhsi.flipAlwaysOnTop();
                }
            });

            flipItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    xhsi.flipAlwaysOnTop();
                }
            });

            restartItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    xhsi.restart();
                }
            });

            aboutItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    xhsi.showActionDialog();
                }
            });

            exitItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    tray.remove(trayIcon);
                    xhsi.shutdown_threads();
                }
            });
        }
    }
}
