## Synopsis

The [Heavy-Duty OBD] In-Use Monitor Performance Ratio (IUMPR) Data Collection Tool Process automates the collection and analysis of vehicle test data required by 13 CCR 1971.1 (l)(2.3.3). The process was created through collaboration between HD engine manufacturers and ARB staff. It is memorialized by [draft] SAE J3162™ to provide a conventional industry standard framework for this realization of the process that is provided by the released software in this repository.

To satisfy the regulation given in (l)(2.3.3), vehicles are driven to verify that the subject monitors operate in use, as evidenced by SAE J1939-73 defined data that counts the number of times all aspects of an OBD monitor have run (as displayed by DM20) and that flags a monitor has run (as displayed by DM5). Common software for data collection provides for consistent and uniform data collection among the heavy-duty engine manufacturers producing a standardized report format for review by California Air Resources Board (ARB) staff.


## Background for the HD OBD IUMPR Tool

California's Heavy-Duty On-Board Diagnostic (HD-OBD) regulations require vehicle and engine manufacturers to implement on-board monitoring strategies capable of detecting emission-related malfunctions as vehicles are driven on the road.  When a malfunction is detected, the vehicle's Malfunction Indicator Light (MIL) is illuminated, and fault information is stored in the vehicle's on-board computer to aid technicians in vehicle repair.  The purpose of HD-OBD systems is to alert vehicle operators to the presence of malfunctions as they occur so that the time between their occurrence and repair is shortened, minimizing the excess emissions caused by the malfunction.

To be effective in accomplishing this goal, system and component monitoring strategies that comprise the HD OBD system must function with adequate frequency while the vehicle is driven.  To track the in-use performance, HD OBD systems store information in the on-board computer to:

1. Determine the operational frequency of several monitoring strategies relative to specific drive cycle conditions.  This is accomplished by incrementing a counter when a monitoring strategy subject to the requirements operates on the road, and also tracking when the vehicle's driving cycle meets specified milestones (e.g., 600 seconds of engine operation with 300 cumulative seconds of operation at a vehicle speed greater than 25 miles per hour).  The first counter serves as the numerator and the second as the denominator to calculate the In-Use Monitor Performance Ratio for the monitor.  Overall, the collected ratio data is subject to minimum frequencies that depend on the model year of the engine.

2. Record whether or not specific monitoring strategies have been able to complete an evaluation of component/system performance since the last time the vehicle OBD system's memory was cleared.  These data are known as "readiness indicators" and play a critical role in vehicle inspection and maintenance programs.

The HD-OBD regulations require manufacturers to test production vehicles every model year to verify that the software used to track the numerators and denominators works properly during on-road driving (Title 13 California Code of Regulations, Section 1971.1(l)(2.3.3)).  Data collection involves operating the vehicle(s) on the road such that the necessary operating conditions for applicable monitoring strategies are encountered while tracking the status of the stored numerators, denominators, and readiness indicators to verify incrementing or setting as expected.

## Installation
In order to use the IUMPR Data Collection Tool, a Windows 7 (or greater) computer is required with TMC RP1210 Adapters installed.

Download the IUMPR-installer.exe file from releases (https://github.com/Equipment-and-Tool-Institute/iumpr/releases) and execute.  The default installation location works the best, however alternative locations may be used.

Additional information about using the application can be found in the Help file.

## Contribute
This project was developed in Java using Eclipse.  All tools necessary for development, other than an installed JDK, are included in the repository.

Ant targets that should be run when doing development are:
* findBugs - which performs a static analysis of the code
* run.tests - which executes all unit and systems tests

Additionally, it's recommended that the "docs" target also be executed in order to verify that there are no errors with the JavaDocs.

The code base includes a simulated engine for testing without a connected module.  In order to use the simulated engine, the application must be run in debug mode and the Loop Back Adapter must be used.

To build a new release, verify that the environment variable JRE_HOME points to a 32 bit JRE that can be included in the build like this:

```
C:\>echo %JRE_HOME%
C:\Program Files (x86)\Java\jre1.8.0_121
```

Then execute the "dist" target.  It will generate the jar, followed by an exe and finally the installer.

## Author
This software was developed for the [Equipment & Tool Institute](http://etools.org) by [Solid Design, Inc.](http://soliddesign.net)

[Note: SAE J3162™ is a trademark of SAE International, Inc.]

## License

MIT License

Copyright (c) 2017 Equipment & Tool Institute

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
