package mtproto.send.upload;

import media.MediaReference;
import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.Serializer;
import mtproto.send.SendEncrypted;

public final class SendUploadGetFile extends SendEncrypted {
  public static final int FLAG_PRECISE = 1 << 0;
  public static final int FLAG_CDN_SUPPORTED = 1 << 1;
  public static final int DEFAULT_LIMIT = 8192;

  public SendUploadGetFile(MediaReference reference, long offset, int limit) {
    int flags = 0;

    if (reference == null || !reference.usable()) {
      throw new IllegalArgumentException("Media reference is required.");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must not be negative.");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("Limit must be positive.");
    }

    message_data
      .append_int(CombinatorIds.upload_get_file)
      .append_int(flags)
      .append_raw_bytes(serializeLocation(reference))
      .append_long(offset)
      .append_int(limit);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }

  public static byte[] serializeLocation(MediaReference reference) {
    Serializer serializer = new Serializer();
    byte[] fileReference;

    if (reference == null || !reference.usable()) {
      throw new IllegalArgumentException("Media reference is incomplete.");
    }

    fileReference = new byte[reference.fileReferenceLength];
    System.arraycopy(reference.fileReference, 0, fileReference, 0, reference.fileReferenceLength);

    if (reference.kind == MediaReference.KIND_DOCUMENT) {
      return serializer
        .append_int(CombinatorIds.input_document_file_location)
        .append_long(reference.id)
        .append_long(reference.accessHash)
        .append_byte_string(fileReference)
        .append_string("")
        .end();
    }
    if (reference.kind == MediaReference.KIND_PHOTO) {
      return serializer
        .append_int(CombinatorIds.input_photo_file_location)
        .append_long(reference.id)
        .append_long(reference.accessHash)
        .append_byte_string(fileReference)
        .append_string(reference.thumbSize != null ? reference.thumbSize : "")
        .end();
    }

    throw new IllegalArgumentException("Unsupported media reference kind.");
  }
}
