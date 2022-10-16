// Gzip implementation for J2ME
// Copyright 2011 Igor Gatis  All rights reserved.
// http://code.google.com/p/compress-j2me/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//     * Redistributions of source code must retain the above copyright notice,
//       this list of conditions and the following disclaimer.
//
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//
//     * Neither the name of Google Inc. nor the names of its contributors may
//       be used to endorse or promote products derived from this software
//       without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ZStream {

  static final int GZIP_MAGIC_NUMBER = 0x8B1F;
  //private static final byte FTEXT = 0x01;
  static final byte FHCRC = 0x02;
  static final byte FEXTRA = 0x04;
  static final byte FNAME = 0x08;
  static final byte FCOMMENT = 0x10;
  //private static final byte FRESERVED = (byte) 0xE0;
  static final byte CM_DEFLATE = 8;

  //---------------------------------------------------------------------------
  // Circular buffer feature.
  //---------------------------------------------------------------------------

  private int size;
  private boolean hasCircularBuffer;
  private byte[] circularBuffer;
  private int bufferMask;
  private int bufferOffset;
  private int bufferSize;

  private ZStream(boolean keepCrc, int bitsCircularBuffer) {
    this.keepCrc = keepCrc;
    if (bitsCircularBuffer > 0) {
      this.hasCircularBuffer = true;
      this.bufferMask = (1 << bitsCircularBuffer) - 1;
      this.circularBuffer = new byte[1 << bitsCircularBuffer];
    }
  }

  public int getSize() {
    return size;
  }

  //---------------------------------------------------------------------------
  // CRC feature.
  //---------------------------------------------------------------------------

  static final int[] CRC_TABLE;
  static {
    CRC_TABLE = new int[256];
    for (int n = 0; n < 256; n++) {
      int c = n;
      for (int k = 0; k < 8; k++) {
        if ((c & 0x01) != 0) {
          c = 0xEDB88320 ^ (c >>> 1);
        } else {
          c = c >>> 1;
        }
      }
      CRC_TABLE[n] = c;
    }
  }

  private boolean keepCrc;
  private int rawCrc = 0 ^ 0xFFFFFFFF;

  public void setKeepCrc(boolean keepCrc) {
    this.keepCrc = keepCrc;
  }

  void resetCrc() {
    this.rawCrc = 0 ^ 0xFFFFFFFF;
    this.keepCrc = true;
  }

  int getCrc() {
    return (int) (this.rawCrc ^ 0xFFFFFFFF);
  }

  private void updateCrc(byte ch) {
    int c = this.rawCrc;
    c = CRC_TABLE[(c & 0xFF) ^ (0xFF & ch)] ^ (c >>> 8);
    this.rawCrc = c;
  }

  //---------------------------------------------------------------------------
  // Output stream
  //---------------------------------------------------------------------------

  private OutputStream out;

  public ZStream(OutputStream output, boolean keepCrc, int bitsCircularBuffer) {
    this(keepCrc, bitsCircularBuffer);
    this.out = output;
  }

  private void writeInternal(int ch) throws IOException {
    this.out.write(ch);
    this.size++;
    if (this.keepCrc) {
      updateCrc((byte) ch);
    }
    if (this.hasCircularBuffer) {
      this.circularBuffer[this.bufferOffset] = (byte) ch;
      this.bufferOffset = (this.bufferOffset + 1) & this.bufferMask;
      if (this.bufferSize < this.circularBuffer.length) {
        this.bufferSize++;
      }
    }
  }

  void write(int ch) throws IOException {
    if (this.unaligned) {
      throw new RuntimeException("Unaligned byte");
    }
    writeInternal(ch);
  }

  void writeLittleEndian(int n, int len) throws IOException {
    for (int i = 0; i < len; i++) {
      write((n >>> (8 * i)) & 0xFF);
    }
  }

  void copyFromEnd(int distance, int length) throws IOException {
    if (!this.hasCircularBuffer) {
      throw new RuntimeException("buffer unavailable");
    }
    if (distance > this.bufferSize) {
      throw new RuntimeException("invalid distance");
    }
    int start = this.bufferOffset - distance;
    start = (start + this.circularBuffer.length) & this.bufferMask;
    for (int i = 0; i < length; i++) {
      int idx = (start + i) & this.bufferMask;
      write(this.circularBuffer[idx]);
    }
  }

  void flush() throws IOException {
    this.out.flush();
  }

  //---------------------------------------------------------------------------
  // Output stream
  //---------------------------------------------------------------------------
  private InputStream in;

  public ZStream(InputStream input, boolean keepCrc, int bitsCircularBuffer) {
    this(keepCrc, bitsCircularBuffer);
    this.in = input;
  }

  public ZStream(InputStream input) {
    this(input, false, 0);
  }

  public int byteAtDistance(int distance) {
    if (!this.hasCircularBuffer) {
      throw new RuntimeException("buffer unavailable");
    }
    if (distance > this.bufferSize) {
      return -1;
      //throw new RuntimeException("invalid distance");
    }
    int idx = this.bufferOffset - distance;
    idx = (idx + this.circularBuffer.length) & this.bufferMask;
    return 0xFF & this.circularBuffer[idx];
  }

  private int readInternal() throws IOException {
    int ch = this.in.read();
    if (ch >= 0) {
      this.size++;
      if (this.keepCrc) {
        updateCrc((byte) ch);
      }
      if (this.hasCircularBuffer) {
        this.circularBuffer[this.bufferOffset] = (byte) ch;
        this.bufferOffset = (this.bufferOffset + 1) & this.bufferMask;
        if (this.bufferSize < this.circularBuffer.length) {
          this.bufferSize++;
        }
      }
    }
    return ch;
  }

  int read() throws IOException {
    if (this.unaligned) {
      throw new RuntimeException("Unaligned byte");
    }
    return readInternal();
  }

  int read(byte[] buffer, int start, int length) throws IOException {
    for (int i = 0; i < length; i++) {
      int ch = read();
      if (ch < 0) {
        return i;
      }
      buffer[start + i] = (byte) ch;
    }
    return length;
  }

  String readZeroTerminatedString() throws IOException {
    StringBuffer buffer = new StringBuffer(128);
    int ch;
    while ((ch = read()) > 0) {
      buffer.append((char) ch);
    }
    return buffer.toString();
  }

  private static void checkNoEOF(int ch) throws IOException {
    if (ch < 0) {
      throw new IOException("Unexpected EOF.");
    }
  }

  void skipBytes(int n) throws IOException {
    while (n-- > 0) {
      int ch = read();
      checkNoEOF(ch);
    }
  }

  int readLittleEndian(int n) throws IOException {
    int v = 0;
    for (int i = 0; i < n; i++) {
      int ch = read();
      checkNoEOF(ch);
      v |= ch << (i << 3);
    }
    return v;
  }

  //---------------------------------------------------------------------------
  // Bit stream feature.
  //---------------------------------------------------------------------------

  private boolean unaligned;
  private int bitBuffer;
  private int bitOffset;

  void alignInputBytes() throws IOException {
    readBits(this.bitOffset);
    this.unaligned = false;
  }

  int readBits(int numBits) throws IOException {
    this.unaligned = true;
    while (this.bitOffset < numBits) {
      int tmp = readInternal();
      if (tmp < 0) {
        checkNoEOF(tmp);
        //return -1;
      }
      this.bitBuffer |= tmp << this.bitOffset;
      this.bitOffset += 8;
    }
    int mask = (1 << numBits) - 1;
    int code = this.bitBuffer & mask;
    this.bitBuffer >>>= numBits;
    this.bitOffset -= numBits;
    //System.out.println(numBits + ":" + bin(code, numBits));
    return code;
  }

  //  static String bin(int bits, int length) {
  //    StringBuffer buffer = new StringBuffer();
  //    while (length-- > 0) {
  //      buffer.append((bits & (1 << length)) != 0 ? '1' : '0');
  //    }
  //    return buffer.toString();
  //  }

  void writeBits(int code, int numBits) throws IOException {
    this.unaligned = true;
    //System.out.println(numBits + ":" + bin(code, numBits));
    int mask = (1 << numBits) - 1;
    this.bitBuffer |= (mask & code) << this.bitOffset;
    this.bitOffset += numBits;

    while (this.bitOffset > 8) {
      writeInternal((byte) this.bitBuffer);
      this.size++;
      this.bitBuffer >>>= 8;
      this.bitOffset -= 8;
    }
  }

  void alignOutputBytes() throws IOException {
    while (this.bitOffset > 0) {
      writeInternal(0xFF & this.bitBuffer);
      this.size++;
      this.bitBuffer >>>= 8;
      this.bitOffset -= 8;
    }
    this.unaligned = false;
    flush();
  }
}
