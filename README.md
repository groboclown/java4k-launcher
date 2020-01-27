# The java-gaming.org Java4k game competition launcher.


This tool allows anyone with Java installed to play any of the games from the
Java4k contests, regardless of whether Java is enabled in the browser or not.

[The original thread about the development work.](http://www.java-gaming.org/topics/java4k-launcher-v1-1/31391/view.html)


This project was originally released on a Mercurial project back in 2014, on the Pikacode site.  However, that site has gone dark.  In an effort to preserve the work done there, I've uploaded it here.

The last release, v1.1, was built on Dec 12, 2013.



## Attributions


### Libraries

* Substance Look and Feel library
  * From the [Insubstantial project](https://github.com/Insubstantial)
    * License: [BSD-like](https://github.com/Insubstantial/insubstantial/blob/master/substance/www/license.html)
    * This version has some modifications:
      * It no longer references AWTUtilities, so it's not restricted to just Oracle JRE distributions.  This might cause issues with rounded corners on the dialogs and frames, though.
      * It no longer throws or prints errors if UI components are created outside the event dispatch thread.  Though it's useful, it prevents many WebStart games - some WebStart games do not correctly follow this behavior, and we shouldn't prevent them for running because of it.
* For all other libraries, see the [lib](lib) directory.  Most of those are used for building.

### Images

* reload.png
    * Source: [http://openiconlibrary.sourceforge.net/gallery2/?./Icons/actions/view-refresh-5.png]()
    * Author: N/A
    * License: None
* cached-1.png
    * Source: [http://openiconlibrary.sourceforge.net/gallery2/?./Icons/status/user-extended-away.png]()
    * Author: N/A
    * License: None
* not-available.png and current.png
    * Source: [oxygen icons](http://www.oxygen-icons.org/).
    * License: Dual: CC-BY-SA 3.0 or LGPL
* cached-2.png
    * Source: [Tango harm-on-icons](http://gnome-look.org/content/show.php/Tango+mine?content=76316)
    * License: CC-BY 3.0

