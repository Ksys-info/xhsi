# xhsi

A version of XHSI with control panel(s)


You must first install the X-Plane plugins for XHSI:

    http://sourceforge.net/projects/xhsi

and ExtPlane:

    http://dankrusi.com/downloads/ExtPlane-Plugin-v0.1.zip



Start X-Plane first, and then run:

    java -jar xhsi.jar

The commander configuration can be found in

    XHSI > Preferences > Commander

First try the 4-Column and 5-Column internal autopilot panels, and then
take a look at CMD.config option (and file)



The modified ND display can produce a street map or a satellite image.
The map data is read dynamically using HTTP connections, so for this
to work you must be connected to the internet and and not have a firewall
blocking the program etc. To use it right click on the ND panel and twiddle
with the mouse wheel. If the Java property "tile.cache" is set then the map
data will be saved to disk e.g:

    java -Dtile.cache=c:\tile\cache -jar xhsi.jar

There is a demo video at:

    https://youtu.be/W7Rb7cuiglk