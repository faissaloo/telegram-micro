TelegramLite  [![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
==
TelegramLite is an attempt to build a Telegram implementation for J2ME, there's still alot of work to be done but I think I've gotten most of the difficult stuff out of the way now (in terms of protocol and such) it's just a matter of implementing all the MTProto objects and creating a UI.  

Building
--
This project can be built using the Sun Java Wireless Toolkit, which you can install on Ubuntu by following this [StackOverflow answer](https://stackoverflow.com/a/60260530/5269447). Once installed you should copy this repository to your j2mewtk folder (default is `~/j2mewtk`) under `2.5.2/apps/` and you should see it when you go to 'open project'. Then you can click `build` then `run`.  

Troubleshooting WTK:
--
 - If the emulator comes up with a white screen restart your computer, I think there's some weird conflict with Docker, so launching docker at any point during a session means you'll need to restart your computer.
 - Additional emulator instances may randomly appear when running, just get used to ignoring or closing them, it's a bug with ktoolbar.
