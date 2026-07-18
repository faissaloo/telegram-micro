package ui;

public interface AppShellActions {
  void openDialog(int index);
  void openDiscovery();
  void searchDiscovery(String query);
  void openMessageSearch();
  void searchCurrentHistory(String query);
  void openSelectedDiscoveryResult();
  void openComposeForSelectedDiscoveryResult();
  void openPeerForSelectedDiscoveryResult();
  void openCompose();
  void replyToSelectedMessage();
  void forwardSelectedMessage();
  void openSelectedMessageMediaExternally();
  void planSelectedMessageMedia();
  void clearMediaQueue();
  void deleteSelectedMessage();
  void openPeerForSelectedDialog();
  void openPeerDialog();
  void openComposeForSelectedPeer();
  void refreshSelectedPeerInfo();
  void joinSelectedPeer();
  void leaveSelectedPeer();
  void startSecretChatWithSelectedPeer();
  void acceptSelectedPeerSecretChat();
  void discardSelectedPeerSecretChat();
  void refreshDialogs();
  void refreshCurrentHistory();
  void sendCurrentCompose(String text);
  void reconnectSession();
}
