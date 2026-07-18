package ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import compose.ComposeStateModel;
import discovery.DiscoveryStateModel;
import peer.PeerActionStateModel;
import reader.DialogListModel;
import reader.MessageListModel;
import search.MessageSearchStateModel;
import support.WorkingSet;

public class AppShellScreen implements CommandListener {
  private MIDlet midlet;
  private Display display;
  private AppShellModel model;
  private AppShellActions actions;

  private DialogsCanvas dialogsCanvas;
  private HistoryCanvas historyCanvas;
  private Form statusForm;
  private Form composeForm;
  private Form mediaForm;
  private List discoveryList;
  private TextBox discoveryQueryBox;
  private MessageSearchCanvas messageSearchCanvas;
  private TextBox messageSearchQueryBox;
  private Form messageSearchDetailForm;
  private Form peerForm;
  private TextField composeField;
  private final char[] messageSearchTimeBuffer = new char[MessageTime.LABEL_LENGTH];

  private final Command refreshCommand = new Command("Refresh", Command.SCREEN, 1);
  private final Command newCommand = new Command("New", Command.SCREEN, 2);
  private final Command replyCommand = new Command("Reply", Command.SCREEN, 2);
  private final Command forwardCommand = new Command("Forward", Command.SCREEN, 3);
  private final Command mediaCommand = new Command("Media", Command.SCREEN, 4);
  private final Command openMediaCommand = new Command("Open", Command.OK, 1);
  private final Command planMediaCommand = new Command("Save", Command.SCREEN, 2);
  private final Command clearMediaCommand = new Command("Clear", Command.SCREEN, 3);
  private final Command deleteCommand = new Command("Delete", Command.SCREEN, 5);
  private final Command peerCommand = new Command("Peer", Command.SCREEN, 6);
  private final Command statusCommand = new Command("Status", Command.SCREEN, 7);
  private final Command composeCommand = new Command("Compose", Command.SCREEN, 8);
  private final Command reconnectCommand = new Command("Reconnect", Command.SCREEN, 9);
  private final Command searchCommand = new Command("Search", Command.OK, 1);
  private final Command editCommand = new Command("Edit", Command.SCREEN, 2);
  private final Command openCommand = new Command("Open", Command.OK, 1);
  private final Command sendCommand = new Command("Send", Command.OK, 1);
  private final Command joinCommand = new Command("Join", Command.SCREEN, 2);
  private final Command leaveCommand = new Command("Leave", Command.SCREEN, 3);
  private final Command secretCommand = new Command("Secret", Command.SCREEN, 4);
  private final Command acceptSecretCommand = new Command("Accept", Command.SCREEN, 4);
  private final Command discardSecretCommand = new Command("Discard", Command.SCREEN, 5);
  private final Command resultsCommand = new Command("Results", Command.BACK, 2);
  private final Command searchBackCommand = new Command("Back", Command.BACK, 1);
  private final Command dialogsCommand = new Command("Dialogs", Command.BACK, 2);
  private final Command historyCommand = new Command("History", Command.SCREEN, 3);
  private final Command exitCommand = new Command("Exit", Command.EXIT, 8);

  public AppShellScreen(MIDlet midlet, Display display, AppShellModel model, AppShellActions actions) {
    this.midlet = midlet;
    this.display = display;
    this.model = model;
    this.actions = actions;
  }

  public void show() {
    showCurrent();
  }

  public void showCurrent() {
    if (model.router.currentScreen == AppShellModel.SCREEN_COMPOSE) {
      showCompose();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_HISTORY) {
      showHistory();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_STATUS) {
      showStatus();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_DISCOVERY) {
      showDiscovery();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_PEER) {
      showPeer();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_MEDIA) {
      showMedia();
    } else if (model.router.currentScreen == AppShellModel.SCREEN_MESSAGE_SEARCH) {
      showMessageSearchResults();
    } else {
      showDialogs();
    }
  }

  public void showDialogs() {
    model.navigateDialogs();
    rebuildDialogs();
    display.setCurrent(dialogsCanvas);
  }

  public void showHistory() {
    model.navigateHistory();
    rebuildHistory();
    display.setCurrent(historyCanvas);
  }

  public void showStatus() {
    model.navigateStatus();
    rebuildStatus();
    display.setCurrent(statusForm);
  }

  public void showCompose() {
    model.navigateCompose();
    rebuildCompose();
    display.setCurrent(composeForm);
  }

  public void showDiscovery() {
    model.navigateDiscovery();
    rebuildDiscovery();
    display.setCurrent(discoveryList);
  }

  public void showDiscoveryQuery() {
    rebuildDiscoveryQuery();
    display.setCurrent(discoveryQueryBox);
  }

  public void showMessageSearchResults() {
    model.navigateMessageSearch();
    rebuildMessageSearchResults();
    display.setCurrent(messageSearchCanvas);
  }

  public void showMessageSearchQuery() {
    model.navigateMessageSearch();
    rebuildMessageSearchQuery();
    display.setCurrent(messageSearchQueryBox);
  }

  public void showMessageSearchDetail() {
    model.navigateMessageSearch();
    rebuildMessageSearchDetail();
    display.setCurrent(messageSearchDetailForm);
  }

  public boolean isShowingMessageSearchResults() {
    return messageSearchCanvas != null && display.getCurrent() == messageSearchCanvas;
  }

  public void showPeer() {
    model.navigatePeer();
    rebuildPeer();
    display.setCurrent(peerForm);
  }

  public void syncVisibleState() {
    if (display.getCurrent() == composeForm && composeField != null) {
      model.updateComposeDraft(composeField.getString());
    }
  }

  private void rebuildDialogs() {
    if (dialogsCanvas == null) {
      dialogsCanvas = new DialogsCanvas(model, actions);
      dialogsCanvas.addCommand(refreshCommand);
      dialogsCanvas.addCommand(newCommand);
      dialogsCanvas.addCommand(statusCommand);
      dialogsCanvas.addCommand(reconnectCommand);
      dialogsCanvas.addCommand(exitCommand);
      dialogsCanvas.setCommandListener(this);
    }
    dialogsCanvas.refresh();
  }

  private void rebuildHistory() {
    if (historyCanvas == null) {
      historyCanvas = new HistoryCanvas(model);
      historyCanvas.addCommand(refreshCommand);
      historyCanvas.addCommand(searchCommand);
      historyCanvas.addCommand(replyCommand);
      historyCanvas.addCommand(forwardCommand);
      historyCanvas.addCommand(peerCommand);
      historyCanvas.addCommand(composeCommand);
      historyCanvas.addCommand(reconnectCommand);
      historyCanvas.addCommand(dialogsCommand);
      historyCanvas.addCommand(statusCommand);
      historyCanvas.addCommand(exitCommand);
      historyCanvas.setCommandListener(this);
    }
    historyCanvas.removeCommand(deleteCommand);
    historyCanvas.removeCommand(mediaCommand);
    if (model.canDeleteSelectedMessage()) {
      historyCanvas.addCommand(deleteCommand);
    }
    if (model.canViewSelectedMessageMedia()) {
      historyCanvas.addCommand(mediaCommand);
    }
    if (model.reader.history.count > 0 && model.reader.history.selectedIndex >= model.reader.history.count) {
      model.selectLatestMessage();
    }
    historyCanvas.refresh();
  }

  private void rebuildStatus() {
    if (statusForm == null) {
      statusForm = new Form("Status");
      statusForm.addCommand(refreshCommand);
      statusForm.addCommand(newCommand);
      statusForm.addCommand(reconnectCommand);
      statusForm.addCommand(dialogsCommand);
      statusForm.addCommand(historyCommand);
      statusForm.addCommand(exitCommand);
      statusForm.setCommandListener(this);
    } else {
      statusForm.deleteAll();
    }

    statusForm.append("Auth: " + model.authSummary);
    statusForm.append("\nBoot: " + model.bootSummary);
    statusForm.append("\nSession: " + model.sessionSummary);
    statusForm.append("\nResume: " + model.resumeSummary);
    statusForm.append("\nSync: " + model.syncSummary);
    statusForm.append("\nSync detail: " + model.syncDetail);
    statusForm.append("\nReader: " + model.readerSummary);
    statusForm.append("\nOutbound: " + model.outboundSummary);
    statusForm.append("\nOutbound detail: " + model.outboundDetail);
    statusForm.append("\nFailure: " + model.failureSummary);
    statusForm.append("\nFailure detail: " + model.failureDetail);
    statusForm.append("\nMedia: " + model.mediaSummary);
    statusForm.append("\nMedia detail: " + model.mediaDetail);
    statusForm.append("\nDevice: " + model.deviceSummary);
    statusForm.append("\nDetail: " + model.activeDetail);

    if (model.lastPlan != null) {
      WorkingSet.Plan plan = model.lastPlan;
      statusForm.append("\n\nBudget: " + Integer.toString(plan.availableBytes / 1024) + " KiB");
      statusForm.append("\nRequired: " + Integer.toString(plan.requiredBytes / 1024) + " KiB");
      statusForm.append("\nOptional: " + Integer.toString(plan.optionalBytes / 1024) + " KiB");
      statusForm.append("\nHeadroom: " + Integer.toString(plan.headroomBytes / 1024) + " KiB");
      statusForm.append("\nIdle prefetch: " + (plan.allowIdlePrefetch ? "yes" : "no"));
    }
  }

  private void rebuildCompose() {
    ComposeStateModel compose = model.compose;
    Form next = new Form("Compose");
    boolean forwardOnly = compose.hasForwardSource();

    next.append("Peer: " + compose.peerTitle);
    next.append("\nCompose: " + compose.statusName());
    if (compose.contextMode != ComposeStateModel.CONTEXT_NONE) {
      next.append("\nContext: " + compose.contextName() + " " + compose.contextLabel);
      if (compose.contextPreview != null && compose.contextPreview.length() > 0) {
        next.append("\nPreview: " + compose.contextPreview);
      }
    }
    if (compose.lastErrorMessage != null && compose.lastErrorMessage.length() > 0) {
      next.append("\nError: " + compose.lastErrorMessage);
    }
    if (compose.lastMessageId > 0) {
      next.append("\nLast id: " + Integer.toString(compose.lastMessageId));
    }
    if (forwardOnly) {
      next.append("\nMode: direct forward");
      composeField = null;
    } else {
      next.append("\nLimit: " + Integer.toString(ComposeStateModel.TEXT_MAX) + " chars");
      composeField = new TextField("Message", compose.text, ComposeStateModel.TEXT_MAX, TextField.ANY);
      next.append(composeField);
    }

    if (compose.canSend()) {
      next.addCommand(sendCommand);
    }
    next.addCommand(newCommand);
    next.addCommand(reconnectCommand);
    next.addCommand(historyCommand);
    next.addCommand(dialogsCommand);
    next.addCommand(statusCommand);
    next.addCommand(exitCommand);
    next.setCommandListener(this);
    composeForm = next;
  }

  private void rebuildDiscovery() {
    DiscoveryStateModel discovery = model.discovery;

    if (discoveryList == null) {
      discoveryList = new List("New Chat", List.IMPLICIT);
      discoveryList.addCommand(editCommand);
      discoveryList.addCommand(refreshCommand);
      discoveryList.addCommand(peerCommand);
      discoveryList.addCommand(composeCommand);
      discoveryList.addCommand(dialogsCommand);
      discoveryList.addCommand(statusCommand);
      discoveryList.addCommand(exitCommand);
      discoveryList.setSelectCommand(openCommand);
      discoveryList.setCommandListener(this);
    } else {
      discoveryList.deleteAll();
    }

    if (discovery.count == 0) {
      discoveryList.append(
        discovery.query.length() > 0
          ? "No cached matches for " + discovery.query
          : "No cached peers yet",
        null
      );
    } else {
      for (int i = 0; i < discovery.count; ++i) {
        DiscoveryStateModel.Result result = discovery.results[i];
        discoveryList.append(discoveryLabel(result), null);
      }
      if (discovery.selectedIndex >= 0 && discovery.selectedIndex < discovery.count) {
        discoveryList.setSelectedIndex(discovery.selectedIndex, true);
      }
    }
    discoveryList.setTitle(
      discovery.query.length() > 0
        ? "New: " + discovery.query
        : "New Chat"
    );
  }

  private void rebuildDiscoveryQuery() {
    if (discoveryQueryBox == null) {
      discoveryQueryBox = new TextBox("Find", model.discovery.query, DiscoveryStateModel.QUERY_MAX, TextField.ANY);
      discoveryQueryBox.addCommand(searchCommand);
      discoveryQueryBox.addCommand(resultsCommand);
      discoveryQueryBox.addCommand(exitCommand);
      discoveryQueryBox.setCommandListener(this);
    } else {
      discoveryQueryBox.setString(model.discovery.query);
    }
  }

  private void rebuildMessageSearchResults() {
    if (messageSearchCanvas == null) {
      messageSearchCanvas = new MessageSearchCanvas(model, this);
      messageSearchCanvas.addCommand(openCommand);
      messageSearchCanvas.addCommand(editCommand);
      messageSearchCanvas.addCommand(refreshCommand);
      messageSearchCanvas.addCommand(searchBackCommand);
      messageSearchCanvas.addCommand(statusCommand);
      messageSearchCanvas.addCommand(exitCommand);
      messageSearchCanvas.setCommandListener(this);
    }
    messageSearchCanvas.removeCommand(openCommand);
    if (model.messageSearch.results.count > 0) {
      messageSearchCanvas.addCommand(openCommand);
    }
    messageSearchCanvas.refresh();
  }

  private void rebuildMessageSearchQuery() {
    MessageSearchStateModel search = model.messageSearch;

    if (messageSearchQueryBox == null) {
      messageSearchQueryBox = new TextBox(
        "Search messages",
        search.query,
        MessageSearchStateModel.QUERY_MAX,
        TextField.ANY
      );
      messageSearchQueryBox.addCommand(searchCommand);
      messageSearchQueryBox.addCommand(resultsCommand);
      messageSearchQueryBox.addCommand(exitCommand);
      messageSearchQueryBox.setCommandListener(this);
    } else {
      messageSearchQueryBox.setString(search.query);
    }
    messageSearchQueryBox.setTitle(DialogListModel.limit(
      "Search " + search.peerTitle + " (1-" +
        MessageSearchStateModel.QUERY_MAX + ")",
      32
    ));
  }

  private void rebuildMessageSearchDetail() {
    MessageListModel.Item result = model.selectedMessageSearchResult();
    Form next = new Form("Search result");

    if (result == null) {
      next.append("No search result is selected.");
    } else {
      next.append("Chat: " + model.messageSearch.peerTitle);
      next.append("\nQuery: " + model.messageSearch.query);
      next.append("\nResult: " + Integer.toString(model.messageSearch.results.selectedIndex + 1) +
        " of " + Integer.toString(model.messageSearch.results.count));
      next.append("\nSender: " + result.sender);
      next.append("\nTime: " + messageSearchTime(result));
      next.append("\nMessage id: " + Integer.toString(result.messageId));
      if (result.text != null && result.text.length() > 0) {
        next.append("\nText: " + result.text);
      }
      if (result.hasMedia) {
        next.append("\nMedia: " + result.mediaSummary());
      }
    }
    next.addCommand(resultsCommand);
    next.addCommand(historyCommand);
    next.addCommand(exitCommand);
    next.setCommandListener(this);
    messageSearchDetailForm = next;
  }

  private String messageSearchTime(MessageListModel.Item result) {
    int length = result != null
      ? MessageTime.formatUtc(result.dateSeconds, messageSearchTimeBuffer)
      : 0;

    return length > 0
      ? new String(messageSearchTimeBuffer, 0, length)
      : "unknown";
  }

  private void rebuildPeer() {
    PeerActionStateModel peer = model.peer;
    Form next = new Form("Peer");

    next.append("Title: " + peer.title);
    next.append("\nKind: " + peer.kindName());
    next.append("\nPeer id: " + Long.toString(peer.peerId));
    if (peer.username != null && peer.username.length() > 0) {
      next.append("\nUsername: @" + peer.username);
    }
    next.append("\nRoute: " + peer.statusName());
    next.append("\nSummary: " + peer.summary);
    next.append("\nDetail: " + peer.detail);
    next.append("\nAbout: " + peer.about);
    next.append("\nDialog: " + (peer.hasDialog ? "cached" : "not cached"));
    next.append("\nAccess hash: " +
      (peer.hasAccessHash ? (peer.minAccessHash ? "min" : "full") : "none"));
    if (peer.participantCountKnown) {
      next.append("\nMembers: " + Integer.toString(peer.participantCount));
    }
    next.append("\nSecret: " + peer.secretSummary);

    if (peer.hasDialog) {
      next.addCommand(openCommand);
    }
    next.addCommand(refreshCommand);
    if (peer.canCompose) {
      next.addCommand(composeCommand);
    }
    if (peer.canJoin) {
      next.addCommand(joinCommand);
    }
    if (peer.canLeave) {
      next.addCommand(leaveCommand);
    }
    if (peer.canSecretChat) {
      next.addCommand(secretCommand);
    }
    if (peer.canAcceptSecretChat) {
      next.addCommand(acceptSecretCommand);
    }
    if (peer.canDiscardSecretChat) {
      next.addCommand(discardSecretCommand);
    }
    next.addCommand(newCommand);
    next.addCommand(dialogsCommand);
    next.addCommand(statusCommand);
    next.addCommand(exitCommand);
    next.setCommandListener(this);
    peerForm = next;
  }

  private void rebuildMedia() {
    reader.MessageListModel.Item message = model.selectedMessage();
    Form next = new Form("Media");
    String policy = model.selectedMessageMediaPolicy();

    if (message == null || !message.hasMedia) {
      next.append("No media is selected.");
    } else {
      next.append("Sender: " + message.sender);
      if (message.messageId > 0) {
        next.append("\nMessage id: " + Integer.toString(message.messageId));
      }
      next.append("\nType: " +
        (message.mediaLabel != null && message.mediaLabel.length() > 0 ? message.mediaLabel : "MEDIA"));
      if (message.mediaInfo != null && message.mediaInfo.length() > 0) {
        next.append("\nDetail: " + message.mediaInfo);
      }
      next.append("\nPolicy: " + policy);
      next.append("\nAction: " + model.selectedMessageMediaPolicyDetail());
      next.append("\nQueued: " + (model.isSelectedMessageMediaPlanned() ? "yes" : "no"));
      next.append("\nState: " + model.selectedMessageMediaQueueState());
      if (model.selectedMessageMediaQueueDetail().length() > 0) {
        next.append("\nState detail: " + model.selectedMessageMediaQueueDetail());
      }
      next.append("\nQueue: " + model.mediaSummary);
      if (model.mediaDetail != null && model.mediaDetail.length() > 0) {
        next.append("\nQueue detail: " + model.mediaDetail);
      }
      if (message.text != null && message.text.length() > 0) {
        next.append("\nText: " + message.text);
      }
      next.append("\nMessage: " + (message.pending ? "pending" : "cached"));
    }

    if (model.canOpenSelectedMessageMediaExternally()) {
      next.addCommand(openMediaCommand);
    }
    if (model.canPlanSelectedMessageMedia()) {
      next.addCommand(planMediaCommand);
    }
    if (model.canClearMediaQueue()) {
      next.addCommand(clearMediaCommand);
    }
    next.addCommand(historyCommand);
    next.addCommand(dialogsCommand);
    next.addCommand(statusCommand);
    next.addCommand(exitCommand);
    next.setCommandListener(this);
    mediaForm = next;
  }

  private void showMedia() {
    model.navigateMedia();
    rebuildMedia();
    display.setCurrent(mediaForm);
  }

  private String discoveryLabel(DiscoveryStateModel.Result result) {
    StringBuffer label = new StringBuffer();

    label.append(result.title);
    if (result.subtitle != null && result.subtitle.length() > 0) {
      label.append(" | ");
      label.append(result.subtitle);
    }
    return label.toString();
  }

  private void syncDiscoverySelection() {
    int selectedIndex;

    if (discoveryList == null || model.discovery.count <= 0) {
      return;
    }
    selectedIndex = discoveryList.getSelectedIndex();
    if (selectedIndex >= 0) {
      model.selectDiscoveryIndex(selectedIndex);
    }
  }

  public void commandAction(Command command, Displayable displayable) {
    if (displayable == composeForm) {
      syncVisibleState();
    }

    if (command == exitCommand) {
      midlet.notifyDestroyed();
      return;
    }

    if (displayable == discoveryQueryBox) {
      if (command == searchCommand) {
        if (actions != null) {
          actions.searchDiscovery(discoveryQueryBox.getString());
        }
        showDiscovery();
        return;
      }
      if (command == resultsCommand) {
        showDiscovery();
        return;
      }
    }

    if (displayable == messageSearchQueryBox) {
      if (command == searchCommand) {
        if (actions != null) {
          actions.searchCurrentHistory(messageSearchQueryBox.getString());
        } else if (model.beginMessageSearch(messageSearchQueryBox.getString())) {
          showMessageSearchResults();
        }
        return;
      }
      if (command == resultsCommand) {
        showMessageSearchResults();
        return;
      }
    }

    if (displayable == messageSearchDetailForm && command == resultsCommand) {
      showMessageSearchResults();
      return;
    }

    if (command == newCommand) {
      if (actions != null) {
        actions.openDiscovery();
      } else {
        model.openDiscovery();
      }
      showDiscovery();
      return;
    }

    if (displayable == discoveryList) {
      syncDiscoverySelection();
      if (command == openCommand || command == List.SELECT_COMMAND) {
        if (actions != null) {
          actions.openSelectedDiscoveryResult();
        }
        return;
      }
      if (command == editCommand) {
        showDiscoveryQuery();
        return;
      }
      if (command == refreshCommand) {
        if (actions != null) {
          actions.searchDiscovery(model.discovery.query);
        } else {
          model.searchDiscovery(model.discovery.query);
        }
        showDiscovery();
        return;
      }
      if (command == composeCommand) {
        if (actions != null) {
          actions.openComposeForSelectedDiscoveryResult();
        }
        return;
      }
      if (command == peerCommand) {
        if (actions != null) {
          actions.openPeerForSelectedDiscoveryResult();
        }
        return;
      }
    }

    if (displayable == messageSearchCanvas) {
      if (command == openCommand) {
        if (model.messageSearch.results.count > 0) {
          showMessageSearchDetail();
        }
        return;
      }
      if (command == editCommand) {
        showMessageSearchQuery();
        return;
      }
      if (command == refreshCommand) {
        if (actions != null) {
          actions.searchCurrentHistory(model.messageSearch.query);
        } else if (model.beginMessageSearch(model.messageSearch.query)) {
          showMessageSearchResults();
        }
        return;
      }
    }

    if (displayable == peerForm) {
      if (command == refreshCommand) {
        if (actions != null) {
          actions.refreshSelectedPeerInfo();
        }
        return;
      }
      if (command == openCommand) {
        if (actions != null) {
          actions.openPeerDialog();
        }
        return;
      }
      if (command == composeCommand) {
        if (actions != null) {
          actions.openComposeForSelectedPeer();
        }
        return;
      }
      if (command == joinCommand) {
        if (actions != null) {
          actions.joinSelectedPeer();
        }
        return;
      }
      if (command == leaveCommand) {
        if (actions != null) {
          actions.leaveSelectedPeer();
        }
        return;
      }
      if (command == secretCommand) {
        if (actions != null) {
          actions.startSecretChatWithSelectedPeer();
        }
        return;
      }
      if (command == acceptSecretCommand) {
        if (actions != null) {
          actions.acceptSelectedPeerSecretChat();
        }
        return;
      }
      if (command == discardSecretCommand) {
        if (actions != null) {
          actions.discardSelectedPeerSecretChat();
        }
        return;
      }
    }

    if (displayable == mediaForm) {
      if (command == openMediaCommand) {
        if (actions != null) {
          actions.openSelectedMessageMediaExternally();
        }
        return;
      }
      if (command == planMediaCommand) {
        if (actions != null) {
          actions.planSelectedMessageMedia();
        }
        return;
      }
      if (command == clearMediaCommand) {
        if (actions != null) {
          actions.clearMediaQueue();
        }
        return;
      }
    }

    if (command == refreshCommand) {
      if (actions != null) {
        if (displayable == historyCanvas) {
          actions.refreshCurrentHistory();
        } else {
          actions.refreshDialogs();
        }
      }
      return;
    }
    if (displayable == historyCanvas && command == searchCommand) {
      if (actions != null) {
        actions.openMessageSearch();
      } else if (model.openMessageSearch()) {
        showMessageSearchQuery();
      }
      return;
    }
    if (command == replyCommand) {
      if (actions != null) {
        actions.replyToSelectedMessage();
      }
      return;
    }
    if (command == forwardCommand) {
      if (actions != null) {
        actions.forwardSelectedMessage();
      }
      return;
    }
    if (command == deleteCommand) {
      if (actions != null) {
        actions.deleteSelectedMessage();
      }
      return;
    }
    if (command == mediaCommand) {
      showMedia();
      return;
    }
    if (command == peerCommand) {
      if (actions != null) {
        actions.openPeerForSelectedDialog();
      }
      return;
    }
    if (command == reconnectCommand) {
      if (actions != null) {
        actions.reconnectSession();
      }
      return;
    }
    if (command == composeCommand) {
      if (actions != null) {
        actions.openCompose();
      } else {
        model.openComposeForSelectedDialog();
        showCompose();
      }
      return;
    }
    if (displayable == composeForm && command == sendCommand) {
      if (actions != null) {
        actions.sendCurrentCompose(composeField != null ? composeField.getString() : "");
      }
      return;
    }
    if (command == statusCommand) {
      showStatus();
      return;
    }
    if (command == dialogsCommand) {
      showDialogs();
      return;
    }
    if (command == searchBackCommand) {
      showHistory();
      return;
    }
    if (command == historyCommand) {
      showHistory();
    }
  }
}
