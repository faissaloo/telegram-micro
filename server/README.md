# Telegram Micro Server
**This folder contains a server application that can support telegram-micro in the cryptographic calculations at startup.**

Telegram-Micro will communicate in **unencrypted** HTTP with this server. 
This is no problem for the first step, but maybe for the second step of this process: https://core.telegram.org/mtproto/auth_key#presenting-proof-of-work-server-authentication

## Requirements

* `bash`, `nc`, `awk`, `bc` and `factor` als command line utils (should be standard on Linux systems)

## Startup
`bash server.sh`
The server will listen on all interfaces on port 3000.

## Configuration in telegram-micro

The URL must be set in the application description (.jad file). If no property "authHelpUrl" is found, the values will be calculated on the device (takes much more time).

