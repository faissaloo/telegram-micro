package mtproto.recieve.upload;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import support.ArrayPlus;
import support.Decode;

public final class RecieveUploadFile {
  public static final int KIND_NONE = 0;
  public static final int KIND_FILE = 1;
  public static final int KIND_CDN_REDIRECT = 2;

  public final int kind;
  public final int mtime;
  public final byte[] bytes;

  private RecieveUploadFile(int kind, int mtime, byte[] bytes) {
    this.kind = kind;
    this.mtime = mtime;
    this.bytes = bytes != null ? bytes : new byte[0];
  }

  public static RecieveUploadFile from_rpc_response(RPCResponse response) {
    byte[] body;
    int offset = 0;
    int storageFileType;
    int mtime;
    int byteLength;
    int paddedLength;
    byte[] data;

    if (response == null || response.body == null) {
      throw new IllegalArgumentException("Upload response is required.");
    }
    if (response.type == CombinatorIds.upload_file_cdn_redirect) {
      return new RecieveUploadFile(KIND_CDN_REDIRECT, 0, null);
    }
    if (response.type != CombinatorIds.upload_file) {
      throw new IllegalArgumentException("Unexpected upload.File constructor.");
    }

    body = response.body;
    if (body.length < 8) {
      throw new IllegalArgumentException("upload.File payload is truncated.");
    }

    storageFileType = Decode.Little.int_decode(body, offset);
    offset += 4;
    if (storageFileType == 0) {
      throw new IllegalArgumentException("upload.File storage type is missing.");
    }

    mtime = Decode.Little.int_decode(body, offset);
    offset += 4;
    if (offset >= body.length) {
      throw new IllegalArgumentException("upload.File bytes payload is missing.");
    }

    byteLength = body[offset] & 0xFF;
    offset += 1;
    if (byteLength >= 254) {
      if (offset + 3 > body.length) {
        throw new IllegalArgumentException("upload.File extended bytes length is truncated.");
      }
      byteLength = Decode.Little.int24_decode(body, offset);
      offset += 3;
    }
    if (byteLength < 0 || offset + byteLength > body.length) {
      throw new IllegalArgumentException("upload.File bytes payload is truncated.");
    }

    data = ArrayPlus.subarray(body, offset, byteLength);
    paddedLength = byteLength;
    while ((paddedLength & 3) != 0) {
      paddedLength += 1;
    }
    offset += paddedLength;
    if (offset > body.length) {
      throw new IllegalArgumentException("upload.File padding exceeds payload length.");
    }
    return new RecieveUploadFile(KIND_FILE, mtime, data);
  }
}
