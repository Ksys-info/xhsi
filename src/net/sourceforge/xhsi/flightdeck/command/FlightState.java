/**
 * FlightState.java
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

/**
 * FlightState
 */
public class FlightState  {

    /**
     * The highest point since takeoff
     */
    int highest;

    /**
     * True if the autopilot was engaged after takeoff
     */
    boolean autopilotWasOn;

    /**
     * The last recorded distance to target
     */
    float lastDistance = 0;

    /**
     * Turue if distance to target is reducing
     */
    boolean makingProgress = true;
}
