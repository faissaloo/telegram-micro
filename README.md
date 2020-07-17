TelegramMicro  [![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
==
TelegramMicro is an attempt to build a Telegram implementation for J2ME, there's still alot of work to be done but I think I've gotten most of the difficult stuff out of the way now (in terms of protocol and such) it's just a matter of implementing all the MTProto objects and creating a UI.  

Useful links
--
 - [CLDC 1.1 reference](https://docs.oracle.com/javame/config/cldc/ref-impl/cldc1.1/jsr139/)  
 - [MIDP 2.0 reference](https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/)

Building
--
This project can be built using the Sun Java Wireless Toolkit, which you can install on Ubuntu by following this [StackOverflow answer](https://stackoverflow.com/a/60260530/5269447). Once installed you should copy this repository to your j2mewtk folder (default is `~/j2mewtk`) under `2.5.2/apps/` and you should see it when you go to 'open project'. Then you can click `build` then `run`.  

Troubleshooting WTK
--
 - If the emulator comes up with a white screen restart your computer, I think there's some weird conflict with Docker, so launching docker at any point during a session means you'll need to restart your computer.
 - Additional emulator instances may randomly appear when running, just get used to ignoring or closing them, it's a bug with ktoolbar.

Sideprojects
--
If you want to help out but don't want to work on this part of the project, there are a few things that would be useful:
 - A J2ME runtime that can run headlessly so we can have a real CI pipeline  
 - A modern libre replacement for the J2ME WTK that doesn't require as much hassle to get running
 - Some kind of preprocessor or compiler that would give us [compile time function execution](https://en.wikipedia.org/wiki/Compile_time_function_execution) so that we can optimise things like public key storage while keeping the code readable
 - A highly optimised server that solves [`n=pq`](https://en.wikipedia.org/wiki/Pollard%27s_rho_algorithm) for 64-bit integers given `n` where `p < q` and `p` and `q` are prime. This task would lend itself well to C or even Rust. This would be useful in improving performance at the proof of work stage of the authentication process: https://core.telegram.org/mtproto/auth_key#proof-of-work
