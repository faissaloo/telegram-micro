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

class Huffman {

  //private static final int RIGHT_CHILD_OFFSET = 0;
  private static final int LEFT_CHILD_OFFSET = 16;
  private static final int CHILD_CONTENT_MASK = 0xFFFF;
  private static final int MAX_CHILD_INDEX = 1 << 15;

  public static final int MAX_POINTER_INDEX = CHILD_CONTENT_MASK
      - MAX_CHILD_INDEX;

  public static int nodeLabel(int node) {
    if (node >= MAX_CHILD_INDEX) {
      return node - MAX_CHILD_INDEX;
    }
    return -1;
  }

  public static int leftChild(int content) {
    return (content >>> LEFT_CHILD_OFFSET) & CHILD_CONTENT_MASK;
  }

  public static int rightChild(int content) {
    return content & CHILD_CONTENT_MASK;
  }

  static int appendChild(int[] tree, int nodeCount, int path, int pathLength,
      int pointer) {
    int idx = 0;
    while (pathLength-- > 0) {
      int child_offset = 0;
      if ((path & (1 << pathLength)) == 0) {
        child_offset = LEFT_CHILD_OFFSET;
      }
      int child_content = (tree[idx] >>> child_offset) & CHILD_CONTENT_MASK;
      if (child_content == 0) {
        // Add new child.
        if (nodeCount >= MAX_CHILD_INDEX) {
          throw new RuntimeException("Too many nodes: " + nodeCount);
        }
        if (pathLength > 0) {
          tree[idx] |= nodeCount << child_offset;
          idx = nodeCount;
          nodeCount++;
        } else {
          tree[idx] |= (pointer + MAX_CHILD_INDEX) << child_offset;
        }
      } else if (child_content < MAX_CHILD_INDEX) {
        // Child exists.
        idx = child_content;
      } else {
        throw new RuntimeException("Invalid tree");
      }
    }
    return nodeCount;
  }

  public static int[] buildCodeTree(int maxBits, char[] node_len) {
    if (node_len.length > MAX_POINTER_INDEX) {
      throw new RuntimeException("Too many leaves: " + node_len.length);
    }
    // Step 1: Count the number of codes for each code length.
    char[] bl_count = new char[maxBits + 1];
    for (int n = 0; n < node_len.length; n++) {
      int len = node_len[n];
      if (len > 0) {
        bl_count[len]++;
      }
    }
    // Step 2: Find the numerical value of the smallest code for each code length.
    char[] next_code = new char[bl_count.length + 1];
    char code = 0;
    for (int bits = 1; bits <= bl_count.length; bits++) {
      code = (char) ((code + bl_count[bits - 1]) << 1);
      next_code[bits] = code;
    }
    // Step 3: Assign numerical values to all codes.
    int maxChildCount = Math.min((1 << (maxBits + 1)) - 1, maxBits
        * node_len.length + 1);
    int[] tree = new int[maxChildCount];
    int nodeCount = 1; // Root already exists.
    for (int n = 0; n < node_len.length; n++) {
      int len = node_len[n];
      if (len > 0) {
        int path = next_code[len];
        if (path > ((1 << len) - 1)) {
          throw new RuntimeException("Invalid symbol");
        }
        next_code[len]++;
        nodeCount = appendChild(tree, nodeCount, path, len, n);
      }
    }
    // Shrinks tree if occupations is below 80%.
    if ((nodeCount / (double) tree.length) < 0.8) {
      int[] shorter_tree = new int[nodeCount];
      System.arraycopy(tree, 0, shorter_tree, 0, nodeCount);
      tree = shorter_tree;
    }
    return tree;
  }

  static int decodeSymbol(ZStream in, int[] tree) throws IOException {
    //StringBuffer path = new StringBuffer();
    int node = 0;
    do {
      int child_offset = 0;
      if (in.readBits(1) == 0) {
        child_offset = LEFT_CHILD_OFFSET;
        //  path.append('0');
        //} else {
        //  path.append('1');
      }
      int child_content = (tree[node] >>> child_offset) & CHILD_CONTENT_MASK;
      int value = nodeLabel(child_content);
      if (value >= 0) {
        //System.out.println(path.length() + ":" + path.reverse());
        return value;
      }
      node = child_content;
    } while (node > 0);
    //TreeNode.printCodes(tree);
    //throw new IOException("code not found: " + path.reverse());
    throw new IOException("code not found");
  }

  //---------------------------------------------------------------------------
  // Deflate specific.
  //---------------------------------------------------------------------------

  static final int END_OF_BLOCK_CODE = 256;

  // 0x01FF=length, 0xE000=extra bits.
  private static final int _LITLEN_CHAR_BITS_EXTRA_OFFSET = 9;
  private static final int _LITLEN_CHAR_BITS_RANGE_START_MASK = 0x1FF;
  // Could use chars instead, but we want to reuse bsearch().
  private static final int[] LITLEN_CHAR_BITS = new int[] { //
      0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0x20B, 0x20D, 0x20F, 0x211,
      0x413, 0x417, 0x41B, 0x41F, 0x623, 0x62B, 0x633, 0x63B, 0x843, 0x853,
      0x863, 0x873, 0xA83, 0xAA3, 0xAC3, 0xAE3, 0x102 };

  static int decodeLength(int litLenCode, ZStream in) throws IOException {
    litLenCode -= Huffman.END_OF_BLOCK_CODE + 1;
    int codedLength = LITLEN_CHAR_BITS[litLenCode];
    int length = codedLength & _LITLEN_CHAR_BITS_RANGE_START_MASK;
    int extraBits = codedLength >>> _LITLEN_CHAR_BITS_EXTRA_OFFSET;
    if (extraBits > 0) {
      int extra = in.readBits(extraBits);
      //System.out.println(extraBits + ":" + ZStream.bin(extra, extraBits));
      length += extra;
    }
    return length;
  }

  // 0xFFFF0000=extra bits, 0x0000FFFF=distance.
  private static final int _DIST_CHAR_BITS_EXTRA_OFFSET = 16;
  private static final int _DIST_CHAR_BITS_VALUE_MASK = 0xFFFF;
  private static final int[] DIST_CHAR_BITS = new int[] { //
      0x00001, 0x00002, 0x00003, 0x00004, 0x10005, 0x10007, 0x20009, 0x2000D,
      0x30011, 0x30019, 0x40021, 0x40031, 0x50041, 0x50061, 0x60081, 0x600C1,
      0x70101, 0x70181, 0x80201, 0x80301, 0x90401, 0x90601, 0xA0801, 0xA0C01,
      0xB1001, 0xB1801, 0xC2001, 0xC3001, 0xD4001, 0xD6001, };

  static int decodeDistance(int distCode, ZStream in) throws IOException {
    int codedDistance = DIST_CHAR_BITS[distCode];
    int distance = codedDistance & _DIST_CHAR_BITS_VALUE_MASK;
    int extraBits = codedDistance >>> _DIST_CHAR_BITS_EXTRA_OFFSET;
    if (extraBits > 0) {
      int extra = in.readBits(extraBits);
      //System.out.println(extraBits + ":" + ZStream.bin(extra, extraBits));
      distance += extra;
    }
    return distance;
  }

  static final int[] CANONICAL_LITLENS_TREE;
  static {
    char[] node_len = new char[288];
    int i = 0;
    while (i < 144) {
      node_len[i++] = 8;
    }
    while (i < 256) {
      node_len[i++] = 9;
    }
    while (i < 280) {
      node_len[i++] = 7;
    }
    while (i < node_len.length) {
      node_len[i++] = 8;
    }
    CANONICAL_LITLENS_TREE = Huffman.buildCodeTree(9, node_len);
  }

  static final int[] CANONICAL_DISTANCES_TREE;
  static {
    char[] node_len = new char[30];
    for (int i = 0; i < node_len.length; i++) {
      node_len[i] = 5;
    }
    CANONICAL_DISTANCES_TREE = Huffman.buildCodeTree(5, node_len);
  }

  static final byte[] HC_PERM = {
      16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };

  static char[] readLengths(ZStream in, int[] hcTree, int size)
      throws IOException {
    char[] lengths = new char[size];
    for (int i = 0; i < lengths.length;) {
      int code = Huffman.decodeSymbol(in, hcTree);
      int toCopy = 0;
      int repeat = 0;
      switch (code) {
      case 16:
        toCopy = lengths[i - 1];
        repeat = 3 + in.readBits(2);
        break;
      case 17:
        repeat = 3 + in.readBits(3);
        break;
      case 18:
        repeat = 11 + in.readBits(3);
        break;
      default:
        lengths[i++] = (char) code;
        continue;
      }
      for (; repeat > 0 && i < lengths.length; repeat--) {
        lengths[i++] = (char) toCopy;
      }
    }
    return lengths;
  }

  private static final int _LITERAL_TO_CANONICAL_HUFFMAN_BITS_MASK = 0x01FF;
  private static final int _LITERAL_TO_CANONICAL_HUFFMAN_NUMBITS_OFFSET = 9;

  private static int reverse(int path, int numBits) {
    int reservedPath = 0;
    for (int i = 0; i < numBits; i++) {
      reservedPath <<= 1;
      if ((path & (1 << i)) != 0) {
        reservedPath |= 1;
      }
    }
    return reservedPath;
  }

  private static char pathAndNumBitsToChar(int path, int numBits) {
    int reservedPath = reverse(path, numBits);
    return (char) (reservedPath | (numBits << _LITERAL_TO_CANONICAL_HUFFMAN_NUMBITS_OFFSET));
  }

  private static final char[] _LITERAL_TO_CANONICAL_HUFFMAN;
  static {
    _LITERAL_TO_CANONICAL_HUFFMAN = new char[287];
    int i;
    for (i = 0; i < 144; i++) {
      _LITERAL_TO_CANONICAL_HUFFMAN[i] = pathAndNumBitsToChar(0x30 + i, 8);
    }
    for (; i < 256; i++) {
      _LITERAL_TO_CANONICAL_HUFFMAN[i] = pathAndNumBitsToChar(
          0x190 + (i - 144), 9);
    }
    for (; i < 280; i++) {
      _LITERAL_TO_CANONICAL_HUFFMAN[i] = pathAndNumBitsToChar(0x00 + (i - 256),
          7);
    }
    for (; i < 287; i++) {
      _LITERAL_TO_CANONICAL_HUFFMAN[i] = pathAndNumBitsToChar(0xC0 + (i - 280),
          8);
    }
  }

  static void encodeLiteral(int ch, ZStream out) throws IOException {
    //System.out.println("lit="+ch);
    int litLenCode = _LITERAL_TO_CANONICAL_HUFFMAN[ch];
    int code = litLenCode & _LITERAL_TO_CANONICAL_HUFFMAN_BITS_MASK;
    int codeNumBits = litLenCode >>> _LITERAL_TO_CANONICAL_HUFFMAN_NUMBITS_OFFSET;
    out.writeBits(code, codeNumBits);
  }

  private static int bsearch(int key, int[] values, int mask) {
    int p = 0, q = values.length;
    while (p < q) {
      int m = (p + q + 1) / 2;
      int value = values[m] & mask;
      if (key == value) {
        return m;
      }
      if (key < value) {
        q = m - 1;
      } else {
        p = m;
      }
    }
    return p;
  }

  static void encodeLength(int length, ZStream out) throws IOException {
    //System.out.println("len="+length);
    int len_extra_idx = bsearch(length, LITLEN_CHAR_BITS,
        _LITLEN_CHAR_BITS_RANGE_START_MASK);
    int idx = len_extra_idx + (END_OF_BLOCK_CODE + 1);
    int codeStruct = _LITERAL_TO_CANONICAL_HUFFMAN[idx];
    int code = codeStruct & _LITERAL_TO_CANONICAL_HUFFMAN_BITS_MASK;
    int codeNBits = codeStruct >>> _LITERAL_TO_CANONICAL_HUFFMAN_NUMBITS_OFFSET;
    out.writeBits(code, codeNBits);

    int extraStruct = LITLEN_CHAR_BITS[len_extra_idx];
    int extraNBits = extraStruct >>> _LITLEN_CHAR_BITS_EXTRA_OFFSET;
    if (extraNBits > 0) {
      length -= extraStruct & _LITLEN_CHAR_BITS_RANGE_START_MASK;
      out.writeBits(length, extraNBits);
    }
  }

  private static final byte[] _DISTANCE_TO_CANONICAL_HUFFMAN;
  static {
    _DISTANCE_TO_CANONICAL_HUFFMAN = new byte[30];
    for (int i = 0; i < _DISTANCE_TO_CANONICAL_HUFFMAN.length; i++) {
      _DISTANCE_TO_CANONICAL_HUFFMAN[i] = (byte) reverse(i, 5);
    }
  }

  static void encodeDistance(int distance, ZStream out) throws IOException {
    //System.out.println("dist="+distance);
    int idx = bsearch(distance, DIST_CHAR_BITS, _DIST_CHAR_BITS_VALUE_MASK);
    out.writeBits(_DISTANCE_TO_CANONICAL_HUFFMAN[idx], 5);
    int extraStruct = DIST_CHAR_BITS[idx];
    int extraNBits = extraStruct >>> _DIST_CHAR_BITS_EXTRA_OFFSET;
    if (extraNBits > 0) {
      distance -= extraStruct & _DIST_CHAR_BITS_VALUE_MASK;
      out.writeBits(distance, extraNBits);
    }
  }
}
