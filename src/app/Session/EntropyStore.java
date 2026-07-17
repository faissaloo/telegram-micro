package session;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

/** Persists the seed ratchet separately from Telegram authorization state. */
public final class EntropyStore {
  public static final String STORE_NAME = "TelegramMicroEntropy";
  private static final int FIRST_RECORD_ID = 1;

  private final String storeName;

  public EntropyStore() {
    this(STORE_NAME);
  }

  public EntropyStore(String storeName) {
    if (storeName == null || storeName.length() == 0) {
      throw new IllegalArgumentException("Store name is required.");
    }
    this.storeName = storeName;
  }

  public boolean load(byte[] out) {
    RecordStore store = null;
    if (out == null || out.length < EntropySeedCodec.SEED_BYTES) {
      return false;
    }

    try {
      store = RecordStore.openRecordStore(storeName, false);
      int nextRecordId = store.getNextRecordID();
      for (int id = FIRST_RECORD_ID; id < nextRecordId; ++id) {
        try {
          if (EntropySeedCodec.decode(out, store.getRecord(id))) {
            return true;
          }
        } catch (InvalidRecordIDException ignored) {
        }
      }
      return false;
    } catch (RecordStoreNotFoundException missing) {
      return false;
    } catch (RecordStoreException failure) {
      System.out.println("ENTROPY STORE LOAD FAILED: " + failure.toString());
      return false;
    } finally {
      closeQuietly(store);
    }
  }

  public boolean save(byte[] seed) {
    RecordStore store = null;
    byte[] encoded = EntropySeedCodec.encode(seed);
    if (encoded == null) {
      return false;
    }

    try {
      store = RecordStore.openRecordStore(storeName, true);
      int recordId = firstRecordId(store);
      if (recordId > 0) {
        store.setRecord(recordId, encoded, 0, encoded.length);
      } else {
        store.addRecord(encoded, 0, encoded.length);
      }
      return true;
    } catch (RecordStoreException failure) {
      System.out.println("ENTROPY STORE SAVE FAILED: " + failure.toString());
      return false;
    } finally {
      clear(encoded);
      closeQuietly(store);
    }
  }

  private int firstRecordId(RecordStore store) throws RecordStoreException {
    int nextRecordId = store.getNextRecordID();
    for (int id = FIRST_RECORD_ID; id < nextRecordId; ++id) {
      try {
        store.getRecord(id);
        return id;
      } catch (InvalidRecordIDException ignored) {
      }
    }
    return 0;
  }

  private static void closeQuietly(RecordStore store) {
    if (store == null) {
      return;
    }
    try {
      store.closeRecordStore();
    } catch (RecordStoreException ignored) {
    }
  }

  private static void clear(byte[] value) {
    if (value == null) {
      return;
    }
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
