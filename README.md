# APRSdroid TelemetrySender

This app uses on-screen buttons to launch the APRSdroid service and to
send APRS telemetry packets via `SEND_PACKET`. It also sends telemetry
data obtained from the local Android sensors on a configureable interval.

At the moment the following sensors are supported:
 * Linear Acceleration
 * Ambient temperature
 * Relative humidity
 * Pressure
 * Gravity
 * Gyroscope
 * Magnetic field

Please see [APRSdroid](http://aprsdroid.org/) for the master application.

This code is (C) [Georg Lukas](http://op-co.de/).

Optimizations by [Andreas Pachler, OE8APR](http://socialhams.net/oe8apr)
