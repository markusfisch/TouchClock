Touch Clock for Android
=======================

Editable clock view for Android. Gives users a straight forward,
traditional way of setting a time (and optionally a duration).

Minnimum SDK version 4.

![Screenshot](http://markusfisch.github.io/TouchClock/screenshot.png)

See TouchClockSample for a sample implementation.

Features
--------

TouchClockView has some public members for customization:

* Show/hide duration by setting _useDuration_ to true/false
* Set colors for
	* face marks in _markColor_
	* hour hand in _hourColor_
	* minute hand in _minuteColor_
	* duration hand in _stopColor_

You can also subclass TouchClockView and overwrite _getClockFace()_ to
draw your own custom clock face.

License
-------

This code is licensed under the [MIT license][1].

[1]: http://opensource.org/licenses/mit-license.php
