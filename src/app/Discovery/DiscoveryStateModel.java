package discovery;

import reader.DialogListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;

public final class DiscoveryStateModel {
  public static final int QUERY_MAX = 32;
  public static final int RESULT_CAPACITY = 12;
  public static final int TITLE_MAX = 40;
  public static final int SUBTITLE_MAX = 64;

  public static final class Result {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public int dialogIndex = -1;
    public String title = "";
    public String subtitle = "";
    public String username = "";

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      dialogIndex = -1;
      title = "";
      subtitle = "";
      username = "";
    }

    public boolean hasDialog() {
      return dialogIndex >= 0;
    }
  }

  public final Result[] results;
  public String query = "";
  public int count = 0;
  public int selectedIndex = 0;
  public String summary = "LOCAL 0";
  public String detail = "Search cached dialogs and usernames.";

  public DiscoveryStateModel() {
    results = new Result[RESULT_CAPACITY];
    for (int i = 0; i < results.length; ++i) {
      results[i] = new Result();
    }
  }

  public void clear() {
    query = "";
    clearResults();
    summary = "LOCAL 0";
    detail = "Search cached dialogs and usernames.";
  }

  public boolean replaceQuery(String value) {
    String next = value != null ? value.trim() : "";

    if (next.length() > QUERY_MAX) {
      return false;
    }
    query = next;
    return true;
  }

  public void search(ReaderStateModel reader) {
    String normalizedQuery;

    clearResults();
    if (reader == null) {
      summary = "LOCAL 0";
      detail = "Reader state is unavailable.";
      return;
    }

    normalizedQuery = normalizeQuery(query);
    appendDialogMatches(reader, normalizedQuery);
    appendPeerMatches(reader, normalizedQuery);

    summary = "LOCAL " + Integer.toString(count);
    if (normalizedQuery.length() == 0) {
      detail = count > 0
        ? "Showing cached dialogs and peer identities."
        : "No cached dialogs or peer identities yet.";
    } else {
      detail = count > 0
        ? "Showing cached matches for " + query + "."
        : "No cached matches for " + query + ".";
    }
  }

  public boolean selectIndex(int index) {
    if (index < 0 || index >= count) {
      return false;
    }
    selectedIndex = index;
    return true;
  }

  public Result selected() {
    if (count == 0 || selectedIndex < 0 || selectedIndex >= count) {
      return null;
    }
    return results[selectedIndex];
  }

  private void clearResults() {
    for (int i = 0; i < count; ++i) {
      results[i].clear();
    }
    count = 0;
    selectedIndex = 0;
  }

  private void appendDialogMatches(ReaderStateModel reader, String normalizedQuery) {
    for (int i = 0; i < reader.dialogs.count && count < results.length; ++i) {
      DialogListModel.Item dialog = reader.dialogs.items[i];
      PeerDirectory.Item peer = reader.peers.find(dialog.peerKind, dialog.peerId);

      if (!matchesDialog(dialog, peer, normalizedQuery)) {
        continue;
      }
      appendResult(
        dialog.peerKind,
        dialog.peerId,
        i,
        dialog.title,
        dialogSubtitle(dialog, peer),
        peer != null ? peer.username : ""
      );
    }
  }

  private void appendPeerMatches(ReaderStateModel reader, String normalizedQuery) {
    for (int i = 0; i < reader.peers.count && count < results.length; ++i) {
      PeerDirectory.Item peer = reader.peers.items[i];

      if (containsResult(peer.peerKind, peer.peerId) ||
          !matchesPeer(peer, normalizedQuery)) {
        continue;
      }
      appendResult(
        peer.peerKind,
        peer.peerId,
        reader.dialogIndexForPeer(peer.peerKind, peer.peerId),
        peer.title.length() > 0 ? peer.title : reader.peers.titleOrFallback(peer.peerKind, peer.peerId),
        peerSubtitle(peer),
        peer.username
      );
    }
  }

  private boolean matchesDialog(DialogListModel.Item dialog, PeerDirectory.Item peer, String normalizedQuery) {
    if (dialog == null) {
      return false;
    }
    if (normalizedQuery.length() == 0) {
      return true;
    }
    return containsNormalized(dialog.title, normalizedQuery) ||
      containsNormalized(dialog.preview, normalizedQuery) ||
      containsNormalized(peer != null ? peer.username : "", normalizedQuery) ||
      containsNormalized(Long.toString(dialog.peerId), normalizedQuery);
  }

  private boolean matchesPeer(PeerDirectory.Item peer, String normalizedQuery) {
    if (peer == null) {
      return false;
    }
    if (normalizedQuery.length() == 0) {
      return false;
    }
    return containsNormalized(peer.title, normalizedQuery) ||
      containsNormalized(peer.username, normalizedQuery) ||
      containsNormalized(Long.toString(peer.peerId), normalizedQuery);
  }

  private void appendResult(
    int peerKind,
    long peerId,
    int dialogIndex,
    String title,
    String subtitle,
    String username
  ) {
    Result result;

    if (count >= results.length) {
      return;
    }

    result = results[count];
    result.peerKind = peerKind;
    result.peerId = peerId;
    result.dialogIndex = dialogIndex;
    result.title = limit(title, TITLE_MAX);
    result.subtitle = limit(subtitle, SUBTITLE_MAX);
    result.username = limit(username, PeerDirectory.USERNAME_MAX);
    count += 1;
  }

  private boolean containsResult(int peerKind, long peerId) {
    for (int i = 0; i < count; ++i) {
      if (results[i].peerKind == peerKind && results[i].peerId == peerId) {
        return true;
      }
    }
    return false;
  }

  private String dialogSubtitle(DialogListModel.Item dialog, PeerDirectory.Item peer) {
    StringBuffer out = new StringBuffer();
    String username = peer != null ? peer.username : "";

    out.append(peerKindName(dialog.peerKind));
    if (username != null && username.length() > 0) {
      out.append(" @");
      out.append(username);
    }
    if (dialog.unreadCount > 0) {
      out.append(" ");
      out.append(Integer.toString(dialog.unreadCount));
      out.append(" unread");
    }
    if (dialog.preview != null && dialog.preview.length() > 0) {
      if (out.length() > 0) {
        out.append(" | ");
      }
      out.append(dialog.preview);
    }
    return out.toString();
  }

  private String peerSubtitle(PeerDirectory.Item peer) {
    StringBuffer out = new StringBuffer();

    out.append(peerKindName(peer.peerKind));
    if (peer.username != null && peer.username.length() > 0) {
      out.append(" @");
      out.append(peer.username);
    }
    if (peer.hasAccessHash) {
      if (out.length() > 0) {
        out.append(" | ");
      }
      out.append(peer.minAccessHash ? "min hash" : "full hash");
    }
    return out.toString();
  }

  private static String normalizeQuery(String value) {
    String normalized = lower(value != null ? value.trim() : "");

    if (normalized.length() > 0 && normalized.charAt(0) == '@') {
      return normalized.substring(1);
    }
    return normalized;
  }

  private static boolean containsNormalized(String value, String normalizedQuery) {
    if (normalizedQuery == null || normalizedQuery.length() == 0) {
      return true;
    }
    return lower(value).indexOf(normalizedQuery) >= 0;
  }

  private static String lower(String value) {
    return value != null ? value.toLowerCase() : "";
  }

  private static String limit(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  public static String peerKindName(int peerKind) {
    switch (peerKind) {
      case DialogListModel.PEER_USER:
        return "user";
      case DialogListModel.PEER_CHAT:
        return "group";
      case DialogListModel.PEER_CHANNEL:
        return "channel";
      default:
        return "peer";
    }
  }
}
