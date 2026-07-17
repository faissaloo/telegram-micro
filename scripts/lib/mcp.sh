#!/usr/bin/env bash

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  printf 'This file is a shell library and must be sourced.\n' >&2
  exit 64
fi

mcp_require_tools() {
  local command_name

  for command_name in node curl; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
      printf '%s is required for MCP automation.\n' "${command_name}" >&2
      return 127
    fi
  done
}

mcp_validate_port() {
  local candidate="$1"

  if [[ ! "${candidate}" =~ ^[0-9]+$ ]] ||
     (( candidate < 1024 || candidate > 65535 )); then
    printf 'MCP port must be an integer between 1024 and 65535.\n' >&2
    return 1
  fi
}

mcp_pick_port() {
  local configured="${SQUIRRELJME_MCP_PORT:-}"

  if [[ -n "${configured}" ]]; then
    mcp_validate_port "${configured}" || return 1
    printf '%s\n' "${configured}"
    return
  fi

  mcp_require_tools || return
  node -e '
    const net = require("net");
    const server = net.createServer();
    server.on("error", () => process.exit(1));
    server.listen(0, "127.0.0.1", () => {
      process.stdout.write(String(server.address().port));
      server.close();
    });
  '
}

mcp_call() {
  local mcp_port="$1"
  local payload="$2"

  printf '%s' "${payload}" | \
    curl --fail --silent --show-error \
      --request POST "http://127.0.0.1:${mcp_port}/mcp" \
      --header 'Content-Type: application/json' \
      --data-binary @-
}

mcp_wait_until_ready() {
  local mcp_port="$1"
  local client_name="$2"
  local attempts="${3:-120}"
  local attempt

  for ((attempt = 1; attempt <= attempts; ++attempt)); do
    if mcp_call "${mcp_port}" \
      "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"${client_name}\",\"version\":\"1.0\"}}}" \
      >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for MCP on port %s.\n' "${mcp_port}" >&2
  return 1
}

mcp_capture_id_from_snapshot() {
  local snapshot_file="$1"

  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const content = response.result && response.result.content;
    if (!Array.isArray(content) || !content[0] || !content[0].text)
      process.exit(2);
    const snapshot = JSON.parse(content[0].text);
    const controls = snapshot.controls || [];
    const byId = new Map(controls.map((control) => [control.id, control]));
    const window = controls.find((control) =>
      control.kind === "window" && control.visible && Array.isArray(control.children));
    if (!window)
      process.exit(3);
    let captureId = window.id || "object:window";
    for (const childId of window.children) {
      const child = byId.get(childId);
      if (child && Array.isArray(child.actions) &&
          child.actions.includes("screenshot") &&
          !["menu-bar", "menu", "menu-item"].includes(child.kind)) {
        captureId = child.id;
        break;
      }
    }
    process.stdout.write(captureId);
  ' "${snapshot_file}"
}

mcp_write_screenshot() {
  local response_file="$1"
  local output_file="$2"

  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const content = response.result && response.result.content;
    const image = Array.isArray(content) &&
      content.find((item) => item.type === "image" && item.data);
    if (!image)
      process.exit(4);
    const decoded = Buffer.from(image.data, "base64");
    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
    if (decoded.length < signature.length ||
        !decoded.subarray(0, signature.length).equals(signature))
      process.exit(5);
    fs.writeFileSync(process.argv[2], decoded, {mode: 0o600});
  ' "${response_file}" "${output_file}"
}

mcp_assert_png_has_content() {
  local png_file="$1"

  node -e '
    const fs = require("fs");
    const zlib = require("zlib");
    const png = fs.readFileSync(process.argv[1]);
    let offset = 8;
    let width = 0;
    let height = 0;
    let bitDepth = 0;
    let colorType = 0;
    let interlace = 0;
    const idat = [];

    while (offset + 12 <= png.length) {
      const length = png.readUInt32BE(offset);
      const type = png.toString("ascii", offset + 4, offset + 8);
      const start = offset + 8;
      const end = start + length;
      if (end + 4 > png.length)
        process.exit(6);
      if (type === "IHDR") {
        width = png.readUInt32BE(start);
        height = png.readUInt32BE(start + 4);
        bitDepth = png[start + 8];
        colorType = png[start + 9];
        interlace = png[start + 12];
      } else if (type === "IDAT") {
        idat.push(png.subarray(start, end));
      }
      offset = end + 4;
      if (type === "IEND")
        break;
    }

    const channels = colorType === 6 ? 4 : colorType === 2 ? 3 :
      colorType === 0 ? 1 : 0;
    if (width < 120 || height < 160 || bitDepth !== 8 ||
        interlace !== 0 || channels === 0 || idat.length === 0)
      process.exit(7);

    const raw = zlib.inflateSync(Buffer.concat(idat));
    const stride = width * channels;
    if (raw.length !== height * (stride + 1))
      process.exit(8);

    const histogram = new Uint32Array(256);
    let previous = Buffer.alloc(stride);
    let rawOffset = 0;
    let minimum = 255;
    let maximum = 0;

    function paeth(left, up, upperLeft) {
      const prediction = left + up - upperLeft;
      const leftDistance = Math.abs(prediction - left);
      const upDistance = Math.abs(prediction - up);
      const upperLeftDistance = Math.abs(prediction - upperLeft);
      return leftDistance <= upDistance && leftDistance <= upperLeftDistance ?
        left : upDistance <= upperLeftDistance ? up : upperLeft;
    }

    for (let y = 0; y < height; ++y) {
      const filter = raw[rawOffset++];
      const current = Buffer.alloc(stride);
      if (filter > 4)
        process.exit(9);
      for (let x = 0; x < stride; ++x) {
        const encoded = raw[rawOffset++];
        const left = x >= channels ? current[x - channels] : 0;
        const up = previous[x];
        const upperLeft = x >= channels ? previous[x - channels] : 0;
        let predictor = 0;
        if (filter === 1)
          predictor = left;
        else if (filter === 2)
          predictor = up;
        else if (filter === 3)
          predictor = Math.floor((left + up) / 2);
        else if (filter === 4)
          predictor = paeth(left, up, upperLeft);
        current[x] = (encoded + predictor) & 255;
      }
      for (let x = 0; x < stride; x += channels) {
        const red = current[x];
        const green = channels === 1 ? red : current[x + 1];
        const blue = channels === 1 ? red : current[x + 2];
        const luma = Math.floor((299 * red + 587 * green + 114 * blue) / 1000);
        ++histogram[luma];
        minimum = Math.min(minimum, luma);
        maximum = Math.max(maximum, luma);
      }
      previous = current;
    }

    let dominant = 0;
    for (const count of histogram)
      dominant = Math.max(dominant, count);
    const pixelCount = width * height;
    if (maximum - minimum < 24 || pixelCount - dominant < 100)
      process.exit(10);
  ' "${png_file}"
}
