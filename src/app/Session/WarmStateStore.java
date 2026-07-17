package session;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

public final class WarmStateStore {
  public static final String STORE_NAME = "TelegramMicroWarmState";
  public static final int RECORD_ID = 1;

  private final String storeName;

  public WarmStateStore() {
    this(STORE_NAME);
  }

  public WarmStateStore(String storeName) {
    if (storeName == null || storeName.length() == 0) {
      throw new IllegalArgumentException("Store name is required.");
    }
    this.storeName = storeName;
  }

  public boolean load(WarmStateSnapshot out) {
    RecordStore store = null;

    if (out == null) {
      return false;
    }

    try {
      store = RecordStore.openRecordStore(storeName, false);
      int nextRecordId = store.getNextRecordID();

      for (int id = RECORD_ID; id < nextRecordId; ++id) {
        byte[] data;

        try {
          int recordSize = store.getRecordSize(id);
          if (recordSize <= 0 || recordSize > WarmStateSnapshotCodec.MAX_ENCODED_BYTES) {
            continue;
          }
          data = store.getRecord(id);
        } catch (InvalidRecordIDException ignored) {
          continue;
        }

        if (data != null && WarmStateSnapshotCodec.decode(out, data)) {
          return true;
        }
      }

      clear();
      return false;
    } catch (RecordStoreNotFoundException missing) {
      return false;
    } catch (RecordStoreException failure) {
      System.out.println("WARM STATE LOAD FAILED: " + describe(failure));
      return false;
    } finally {
      closeQuietly(store);
    }
  }

  public boolean save(WarmStateSnapshot snapshot) {
    RecordStore store = null;
    byte[] data;

    if (snapshot == null || !snapshot.hasUsefulState()) {
      return false;
    }

    data = WarmStateSnapshotCodec.encode(snapshot);
    if (data == null) {
      return false;
    }

    try {
      store = RecordStore.openRecordStore(storeName, true);
      int recordId = firstRecordId(store);

      if (recordId > 0) {
        store.setRecord(recordId, data, 0, data.length);
      } else {
        store.addRecord(data, 0, data.length);
      }
      return true;
    } catch (RecordStoreException failure) {
      System.out.println("WARM STATE SAVE FAILED: " + describe(failure));
      return false;
    } finally {
      closeQuietly(store);
    }
  }

  public boolean clear() {
    try {
      RecordStore.deleteRecordStore(storeName);
      return true;
    } catch (RecordStoreNotFoundException missing) {
      return true;
    } catch (RecordStoreException failure) {
      System.out.println("WARM STATE CLEAR FAILED: " + describe(failure));
      return false;
    }
  }

  private int firstRecordId(RecordStore store) throws RecordStoreException {
    int nextRecordId = store.getNextRecordID();

    for (int id = RECORD_ID; id < nextRecordId; ++id) {
      try {
        store.getRecord(id);
        return id;
      } catch (InvalidRecordIDException ignored) {
      }
    }
    return 0;
  }

  private String describe(Throwable throwable) {
    StringBuffer description = new StringBuffer();
    Throwable current = throwable;

    while (current != null) {
      if (description.length() > 0) {
        description.append(" <- ");
      }
      description.append(current.getClass().getName());
      if (current.getMessage() != null && current.getMessage().length() > 0) {
        description.append(": ");
        description.append(current.getMessage());
      }
      current = current.getCause();
    }
    return description.toString();
  }

  private void closeQuietly(RecordStore store) {
    if (store == null) {
      return;
    }

    try {
      store.closeRecordStore();
    } catch (RecordStoreException ignored) {
    }
  }
}
