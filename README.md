Example FRC program to demo advanced logging
============================================

Project uses <https://github.com/dominikWin/badlog> for logging in java and <https://github.com/dominikWin/badlogvis> to
generate the html.

To run this program you will need a pigeon from cross the road electronics and one TalonSRX to run unmodified.
There is a powershell script to automate pulling files from the robot and process them. So you need Windows with Powershell 7 or newer,
and it uses ssh to access robo rio.

* To get started download html generator from <https://github.com/dominikWin/badlogvis/releases/latest>
and put in a local folder.
* Update code to use your can ids or comment out code will rotate one motor based on gyro so elevate
* Deploy code to your robot
* Enable teleop
* When running robot pressing button 1 will reboot pigeon to simulate fault
* When you are done testing, disable the robot.
* Update the powershell script with your config see script for comments
* Run script it will copy files, delete from rio, process, and then open latest file in your default browser.
