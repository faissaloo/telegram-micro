"use strict";

const fs = require("fs");
const net = require("net");

const readyPath = process.argv[2];
const resultPath = process.argv[3];
if (!readyPath || !resultPath) {
  process.stderr.write("usage: gcf-loopback-server.js READY RESULT\n");
  process.exit(64);
}

let accepted = 0;
let closed = 0;
let sawPing = false;
let sawDone = false;
let finished = false;

const server = net.createServer((socket) => {
  accepted += 1;
  let received = "";
  let pongSent = false;

  socket.setEncoding("ascii");
  socket.on("data", (chunk) => {
    received += chunk;
    if (!pongSent && received.indexOf("PING") >= 0) {
      sawPing = true;
      pongSent = true;
      socket.write("PONG", "ascii");
    }
    if (received.indexOf("DONE") >= 0) {
      sawDone = true;
    }
  });
  socket.on("end", () => socket.end());
  socket.on("error", (error) => {
    process.stderr.write("loopback socket error: " + error.message + "\n");
  });
  socket.on("close", () => {
    closed += 1;
    if (accepted >= 4 && closed >= 4 && sawPing && sawDone) {
      server.close();
    }
  });
});

server.on("error", (error) => {
  process.stderr.write("loopback server error: " + error.message + "\n");
  process.exit(1);
});

server.on("close", () => {
  if (finished) {
    return;
  }
  finished = true;
  fs.writeFileSync(resultPath, "GCF LOOPBACK SERVER PASSED\n", {mode: 0o600});
});

server.listen(0, "127.0.0.1", () => {
  fs.writeFileSync(readyPath, String(server.address().port) + "\n", {
    mode: 0o600,
  });
});

const timeout = setTimeout(() => {
  if (finished) {
    return;
  }
  process.stderr.write(
    "loopback timeout: accepted=" + accepted + " closed=" + closed +
    " ping=" + sawPing + " done=" + sawDone + "\n"
  );
  process.exit(1);
}, 20000);
timeout.unref();
