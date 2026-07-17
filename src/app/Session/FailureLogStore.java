package session;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

public final class FailureLogStore {
  public static final String STORE_NAME = "TelegramMicroFailures";
  public static final int RECORD_ID = 1;

  private final String storeName;

  public FailureLogStore() {
    this(STORE_NAME);
  }

  public FailureLogStore(String storeName) {
    if (storeName == null || storeName.length() == 0) {
      throw new IllegalArgumentException("Store name is required.");
    }
    this.storeName = storeName;
  }

  public boolean load(FailureLog out) {
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
          data = store.getRecord(id);
        } catch (InvalidRecordIDException ignored) {
          continue;
        }

        if (data != null && FailureLogCodec.decode(out, data)) {
          return true;
        }
      }

      clear();
      out.clear();
      return false;
    } catch (RecordStoreNotFoundException missing) {
      out.clear();
      return false;
    } catch (RecordStoreException failure) {
      System.out.println("FAILURE LOG LOAD FAILED: " + describe(failure));
      out.clear();
      return false;
    } finally {
      closeQuietly(store);
    }
  }

  public boolean save(FailureLog log) {
    RecordStore store = null;
    byte[] data;

    if (log == null) {
      return false;
    }

    data = FailureLogCodec.encode(log);
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
      System.out.println("FAILURE LOG SAVE FAILED: " + describe(failure));
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
      System.out.println("FAILURE LOG CLEAR FAILED: " + describe(failure));
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
