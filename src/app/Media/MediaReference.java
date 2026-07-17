package media;

import reader.DialogListModel;

public final class MediaReference {
  public static final int KIND_NONE = 0;
  public static final int KIND_DOCUMENT = 1;
  public static final int KIND_PHOTO = 2;

  public static final int FILE_REFERENCE_MAX = 96;
  public static final int THUMB_SIZE_MAX = 8;
  public static final int MIME_TYPE_MAX = 24;

  public int kind = KIND_NONE;
  public long id = 0;
  public long accessHash = 0;
  public int dcId = 0;
  public int expectedSize = 0;
  public String thumbSize = "";
  public String mimeType = "";
  public final byte[] fileReference = new byte[FILE_REFERENCE_MAX];
  public int fileReferenceLength = 0;

  public void clear() {
    kind = KIND_NONE;
    id = 0;
    accessHash = 0;
    dcId = 0;
    expectedSize = 0;
    thumbSize = "";
    mimeType = "";
    fileReferenceLength = 0;
  }

  public void copyFrom(MediaReference other) {
    clear();
    if (other == null) {
      return;
    }
    kind = other.kind;
    id = other.id;
    accessHash = other.accessHash;
    dcId = other.dcId;
    expectedSize = other.expectedSize;
    thumbSize = DialogListModel.limit(other.thumbSize, THUMB_SIZE_MAX);
    mimeType = DialogListModel.limit(other.mimeType, MIME_TYPE_MAX);
    if (other.fileReferenceLength > 0) {
      fileReferenceLength = other.fileReferenceLength <= FILE_REFERENCE_MAX
        ? other.fileReferenceLength
        : FILE_REFERENCE_MAX;
      System.arraycopy(other.fileReference, 0, fileReference, 0, fileReferenceLength);
    }
  }

  public boolean usable() {
    return (kind == KIND_DOCUMENT || kind == KIND_PHOTO) &&
      id != 0 &&
      accessHash != 0 &&
      dcId > 0 &&
      fileReferenceLength > 0;
  }

  public boolean setDocument(
    long id,
    long accessHash,
    int dcId,
    byte[] fileReference,
    int fileReferenceLength,
    int expectedSize,
    String mimeType
  ) {
    clear();
    kind = KIND_DOCUMENT;
    this.id = id;
    this.accessHash = accessHash;
    this.dcId = dcId;
    this.expectedSize = expectedSize > 0 ? expectedSize : 0;
    this.mimeType = DialogListModel.limit(mimeType != null ? mimeType : "", MIME_TYPE_MAX);
    return setFileReference(fileReference, fileReferenceLength);
  }

  public boolean setPhoto(
    long id,
    long accessHash,
    int dcId,
    byte[] fileReference,
    int fileReferenceLength,
    String thumbSize
  ) {
    clear();
    kind = KIND_PHOTO;
    this.id = id;
    this.accessHash = accessHash;
    this.dcId = dcId;
    this.thumbSize = DialogListModel.limit(thumbSize != null ? thumbSize : "", THUMB_SIZE_MAX);
    return setFileReference(fileReference, fileReferenceLength);
  }

  private boolean setFileReference(byte[] fileReference, int fileReferenceLength) {
    if (fileReference == null || fileReferenceLength <= 0 || fileReferenceLength > FILE_REFERENCE_MAX) {
      clear();
      return false;
    }
    this.fileReferenceLength = fileReferenceLength;
    System.arraycopy(fileReference, 0, this.fileReference, 0, fileReferenceLength);
    return true;
  }
}
