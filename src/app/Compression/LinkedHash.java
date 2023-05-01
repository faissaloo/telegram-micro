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

class LinkedHash {

  private static final int HASH_PRIME = 31;

  private short[] entries;
  private int[] keys;
  private int[] markers;
  private int size;

  LinkedHash(int size) {
    if (size > 0x8FFF) {
      throw new RuntimeException("invalid table size");
    }
    this.entries = new short[(int) (size * 1.333)];
    this.keys = new int[size];
    this.markers = new int[size];
    reset();
  }

  void reset() {
    for (int i = 0; i < this.entries.length; i++) {
      this.entries[i] = -1;
    }
    this.size = 0;
  }

  int size() {
    return this.size;
  }

  private int calcHash(int key) {
    int hash = HASH_PRIME + (key & 0xFF);
    hash = hash * HASH_PRIME + ((key >>> 8) & 0xFF);
    hash = hash * HASH_PRIME + ((key >>> 16) & 0xFF);
    hash = hash * HASH_PRIME + ((key >>> 24) & 0xFF);
    return Math.abs(hash) % this.entries.length;
  }

  private int find(int key) {
    int hash = calcHash(key);
    int q = 1;
    int idx;
    while ((idx = this.entries[hash]) >= 0) {
      if (this.keys[idx] == key) {
        return hash;
      }
      hash = Math.abs(hash + q * q) % this.entries.length;
      q++;
    }
    // Not present.
    return hash;
  }

  //  int newKey(int oldKey, byte lastByte) {
  //    int newKey = ((0x0000FFFF & oldKey) << 8) | (0xFF & lastByte);
  //    return newKey;
  //  }

  int put(int oldKey, byte lastByte, int marker) {
    int key = ((0x0000FFFF & oldKey) << 8) | (0xFF & lastByte);
    if (marker >= 0) {
      int hash = find(key);
      if (this.entries[hash] < 0) {
        if (this.size == this.markers.length) {
          // Table is full. Need to remove an item.
          return key;
        }
        this.entries[hash] = (short) this.size;
        this.keys[this.size] = key;
        this.markers[this.size] = marker;
        this.size++;
      }
    }
    return key;
  }

  int get(byte[] buffer, int start, int length) {
    if (length > 2) {
      int key = 0;
      key |= (0xFF & buffer[start]) << 16;//24;
      key |= (0xFF & buffer[start + 1]) << 8;//16;
      key |= (0xFF & buffer[start + 2]);// << 8;
      //key |= (0xFF & buffer[start + 3]);
      int hash = find(key);
      int idx = this.entries[hash];
      if (idx >= 0) {
        return this.markers[idx];
      }
    }
    return -1;
  }
}
