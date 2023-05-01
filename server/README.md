# Telegram Micro Server
**This folder contains a server application that can support telegram-micro with any heavy calculations that are safe to send unencrypted**

Telegram-Micro will communicate in **unencrypted** HTTP with this server. It is only used for calculating the diffie-hellman parameters, which is fine because these are already sent unencrypted to Telegram's server's anyway. See: https://security.stackexchange.com/a/94397/180197

## Requirements

* `bash`, `nc`, `awk`, `bc` and `factor` als command line utils (should be standard on Linux systems)

## Startup
`bash server.sh`
The server will listen on all interfaces on port 3000.

## Configuration in telegram-micro

The URL must be set in the application description (.jad file). If no property "authHelpUrl" is found, the values will be calculated on the device (takes much more time).
