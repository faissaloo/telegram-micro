# Telegram Micro Server
**This folder contains a server application that can support telegram-micro in the cryptographic calculations at startup.**

Telegram-Micro will communicate in **unencrypted** HTTP with this server. 
This is no problem for the first step, but maybe for the second step of this process: https://core.telegram.org/mtproto/auth_key#presenting-proof-of-work-server-authentication

## Requirements

* node.js
* UNIXoid system with `bc` and `factor` als command line utils (should be standard on Linux systems)

## Startup
`node index.js`
The server will listen on all interfaces on port 5221.

## Configuration in telegram-micro

The URL must be changed in telegram-micro everywhere where `new ServerConnection(url)` is used. That is in two locations:

* src/app/MTProto/PrimeDecomposer.java
* src/app/MTProto/send/SendSetClientDHParams.java

