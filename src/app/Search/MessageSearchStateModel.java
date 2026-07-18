package search;

import reader.DialogListModel;
import reader.MessageListModel;

/** One explicit, bounded server-search page for the selected dialog. */
public final class MessageSearchStateModel {
  public static final int QUERY_MAX = 48;
  public static final int RESULT_CAPACITY = 6;
  public static final int RESULT_WORKING_BYTES = 512;
  public static final int STATE_WORKING_BYTES = 256;

  public final MessageListModel results = new MessageListModel(RESULT_CAPACITY);
  public int peerKind = DialogListModel.PEER_UNKNOWN;
  public long peerId = 0;
  public String peerTitle = "";
  public String query = "";
  public int totalCount = 0;
  public boolean loading = false;
  public String summary = "SEARCH 0";
  public String detail = "Enter text to search this chat on Telegram.";

  public void clear() {
    peerKind = DialogListModel.PEER_UNKNOWN;
    peerId = 0;
    peerTitle = "";
    query = "";
    results.clear();
    totalCount = 0;
    loading = false;
    summary = "SEARCH 0";
    detail = "Enter text to search this chat on Telegram.";
  }

  public boolean bindPeer(int peerKind, long peerId, String peerTitle) {
    if (!isPeerKind(peerKind) || peerId == 0) {
      return false;
    }
    if (this.peerKind != peerKind || this.peerId != peerId) {
      query = "";
      results.clear();
      totalCount = 0;
      loading = false;
    }
    this.peerKind = peerKind;
    this.peerId = peerId;
    this.peerTitle = DialogListModel.limit(peerTitle, DialogListModel.TITLE_MAX);
    summary = "SEARCH " + Integer.toString(results.count);
    detail = query.length() > 0
      ? "Search this chat again or edit the query."
      : "Enter text to search this chat on Telegram.";
    return true;
  }

  public boolean begin(String value) {
    if (!isPeerKind(peerKind) || peerId == 0 || !replaceQuery(value)) {
      return false;
    }
    results.clear();
    totalCount = 0;
    loading = true;
    summary = "SEARCH...";
    detail = "Searching Telegram for messages in " + peerTitle + "...";
    return true;
  }

  public boolean replaceQuery(String value) {
    String next = value != null ? value.trim() : "";

    if (next.length() == 0 || next.length() > QUERY_MAX) {
      return false;
    }
    if (!next.equals(query)) {
      results.clear();
      totalCount = 0;
    }
    query = next;
    return true;
  }

  public boolean appendResult(
    int messageId,
    String sender,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    int dateSeconds
  ) {
    if (!loading || messageId <= 0) {
      return false;
    }
    return results.append(
      messageId,
      sender,
      text,
      outgoing,
      false,
      hasMedia,
      mediaLabel,
      mediaInfo,
      null,
      dateSeconds
    );
  }

  public void finish(int serverTotalCount) {
    totalCount = serverTotalCount < results.count ? results.count : serverTotalCount;
    loading = false;
    results.selectedIndex = 0;
    summary = "SEARCH " + Integer.toString(results.count) + "/" + Integer.toString(totalCount);
    if (results.count == 0) {
      detail = "No messages matched " + query + " in " + peerTitle + ".";
    } else if (totalCount > results.count) {
      detail = "Showing the first " + Integer.toString(results.count) +
        " of " + Integer.toString(totalCount) + " server matches.";
    } else {
      detail = "Showing " + Integer.toString(results.count) + " server matches.";
    }
  }

  public void fail(String failureDetail) {
    loading = false;
    summary = "SEARCH FAIL";
    detail = failureDetail != null && failureDetail.length() > 0
      ? failureDetail
      : "Message search failed; edit the query and retry.";
  }

  public boolean selectIndex(int index) {
    if (index < 0 || index >= results.count) {
      return false;
    }
    results.selectedIndex = index;
    return true;
  }

  public MessageListModel.Item selected() {
    if (results.count == 0 || results.selectedIndex < 0 ||
        results.selectedIndex >= results.count) {
      return null;
    }
    return results.items[results.selectedIndex];
  }

  private static boolean isPeerKind(int peerKind) {
    return peerKind == DialogListModel.PEER_USER ||
      peerKind == DialogListModel.PEER_CHAT ||
      peerKind == DialogListModel.PEER_CHANNEL;
  }
}
