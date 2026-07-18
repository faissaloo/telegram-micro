import java.io.IOException;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import bouncycastle.BigInteger;

import config.AppCredentials;
import config.RuntimeDeviceProfile;
import crypto.SHA256;
import crypto.SecureRandomPlus;
import discovery.DiscoveryStateModel;
import media.MediaPlanQueue;
import media.MediaReference;
import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import mtproto.MTProtoConnection;
import mtproto.RPCResponse;
import mtproto.Response;
import mtproto.handle.MTProtoCallback;
import mtproto.recieve.RecieveRPCError;
import mtproto.recieve.auth.RecievePasswordState;
import mtproto.recieve.auth.RecieveSentCode;
import mtproto.recieve.help.RecieveConfig;
import mtproto.recieve.messages.RecieveAffectedMessages;
import mtproto.recieve.messages.ReaderResultParser;
import mtproto.recieve.messages.SecretChatResultParser;
import mtproto.recieve.service.RecieveUpdateShortSentMessage;
import mtproto.recieve.upload.RecieveUploadFile;
import mtproto.recieve.updates.RecieveShortTextUpdate;
import mtproto.recieve.updates.RecieveUpdatesDifference;
import mtproto.recieve.updates.RecieveUpdatesState;
import mtproto.secret.SecretChatBootstrap;
import mtproto.send.auth.SendGetPassword;
import mtproto.send.contacts.SendSearch;
import mtproto.send.channels.SendGetFullChannel;
import mtproto.send.channels.SendJoinChannel;
import mtproto.send.channels.SendLeaveChannel;
import mtproto.send.messages.SendGetDialogs;
import mtproto.send.messages.SendForwardMessages;
import mtproto.send.messages.SendAcceptEncryption;
import mtproto.send.messages.SendDiscardEncryption;
import mtproto.send.messages.SendDeleteChannelMessages;
import mtproto.send.messages.SendDeleteMessages;
import mtproto.send.messages.SendGetFullChat;
import mtproto.send.messages.SendGetHistory;
import mtproto.send.messages.SendReadHistory;
import mtproto.send.messages.SendRequestEncryption;
import mtproto.send.messages.SendSendMessage;
import mtproto.send.upload.SendUploadGetFile;
import mtproto.send.updates.SendGetChannelDifference;
import mtproto.send.updates.SendGetDifference;
import mtproto.send.updates.SendGetState;
import mtproto.send.users.SendGetFullUser;

import reader.DialogListModel;
import reader.PeerDirectory;
import peer.PeerActionStateModel;
import reader.SecretChatDirectory;
import session.FailureLog;
import session.FailureLogStore;
import session.EntropySeedCodec;
import session.EntropyStore;
import session.KnownDcHost;
import session.OutboundRecoveryState;
import session.ResumeState;
import session.SessionSnapshot;
import session.SessionStore;
import session.WarmStateSnapshot;
import session.WarmStateStore;
import sync.ChannelSyncState;
import sync.SyncState;
import ui.AppShellActions;
import ui.AppShellModel;
import ui.AppShellScreen;
import ui.EntropyCanvas;
import ui.EntropyScreenHost;
import ui.LoginScreen;
import ui.LoginScreenHost;
import support.RandomPlus;

public class TelegramMicro extends MIDlet implements AppShellActions, LoginScreenHost, EntropyScreenHost {
  private Display display;
  private LoginScreen loginScreen;
  private AppShellModel appShellModel;
  private AppShellScreen appShellScreen;
  private MTProtoConnection connection;
  private ReaderResultParser readerParser;
  private SessionStore sessionStore;
  private WarmStateStore warmStateStore;
  private FailureLog failureLog;
  private FailureLogStore failureLogStore;
  private EntropyStore entropyStore;
  private EntropyCanvas entropyCanvas;
  private SecureRandomPlus secureRandom;
  private SessionSnapshot savedSession;
  private ResumeState resumeState;
  private boolean started = false;
  private boolean restoreAuthorizedPending = false;
  private boolean reconnectAuthorizedPending = false;
  private boolean syncStateRequestInFlight = false;
  private boolean differenceRequestInFlight = false;
  private boolean channelDifferenceRequestInFlight = false;
  private boolean refreshSelectedHistoryAfterDialogs = false;
  private static final int PEER_ACTION_NONE = 0;
  private static final int PEER_ACTION_JOIN = 1;
  private static final int PEER_ACTION_LEAVE = 2;
  private static final int PEER_ACTION_SECRET = 3;
  private static final int PEER_ACTION_SECRET_DISCARD = 4;
  private static final String READ_ACK_WAIT_DETAIL =
    "Wait for the read acknowledgement, then retry.";
  private int pendingSelectedPeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingSelectedPeerId = 0;
  private int pendingHistoryPeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingHistoryPeerId = 0;
  private int pendingDeletePeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingDeletePeerId = 0;
  private int pendingDeleteMessageId = 0;
  private int pendingReadPeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingReadPeerId = 0;
  private int pendingReadMaxId = 0;
  private int pendingReadPreviousInboxMaxId = 0;
  private int pendingReadPreviousUnreadCount = 0;
  private int pendingSendPeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingSendPeerId = 0;
  private long pendingSendRandomId = 0;
  private String pendingDiscoveryQuery = "";
  private int pendingPeerInfoKind = DialogListModel.PEER_UNKNOWN;
  private long pendingPeerInfoId = 0;
  private int pendingPeerActionType = PEER_ACTION_NONE;
  private int pendingPeerActionKind = DialogListModel.PEER_UNKNOWN;
  private long pendingPeerActionId = 0;
  private long pendingPeerActionStartedAtMs = 0;
  private long pendingChannelDifferencePeerId = 0;
  private boolean resumeOutboundAfterDialogs = false;
  private boolean outboundResumeSyncReady = false;
  private int pendingRouteAfterDialogs = 0;
  private final AppCredentials bundledCredentials = AppCredentials.bundled();
  private RuntimeDeviceProfile runtimeDeviceProfile;
  private SyncState syncState;
  private String authHelpUrl;
  private String connectionHost = "149.154.175.100";
  private String connectionPort = "5222";
  private int connectionGeneration = 0;
  private MTProtoConnection.PendingRetryRequest reconnectRetryRequest;
  private final byte[] secretExponentScratch = new byte[SecretChatBootstrap.EXPONENT_BYTES];
  private final byte[] pendingSecretExponent = new byte[SecretChatBootstrap.EXPONENT_BYTES];
  private boolean pendingSecretExponentValid = false;
  private long pendingSecretExpectedKeyFingerprint = 0;
  private static final int MEDIA_DOWNLOAD_CHUNK_BYTES = SendUploadGetFile.DEFAULT_LIMIT;
  private static final int MEDIA_DOWNLOAD_MAX_BYTES = MediaPlanQueue.DATA_BYTES_MAX;
  private int pendingMediaPeerKind = DialogListModel.PEER_UNKNOWN;
  private long pendingMediaPeerId = 0;
  private int pendingMediaMessageId = 0;
  private final MediaReference pendingMediaReference = new MediaReference();
  private byte[] pendingMediaBuffer = null;
  private int pendingMediaBufferLength = 0;
  private int pendingMediaExpectedBytes = 0;

  private String appPropertyOrNull(String key) {
    try {
      return getAppProperty(key);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static String safeAscii(support.Utf8String value) {
    try {
      return value.ascii_string();
    } catch (Exception e) {
      return "<invalid encoding>";
    }
  }

  private static String trimOrEmpty(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private boolean initializeSecureRandomFromStore() {
    byte[] seed = new byte[EntropySeedCodec.SEED_BYTES];
    if (entropyStore == null) {
      entropyStore = new EntropyStore();
    }
    if (!entropyStore.load(seed)) {
      clearBytes(seed);
      return false;
    }
    return installSecureRandom(seed);
  }

  private boolean installSecureRandom(byte[] seed) {
    if (seed == null || seed.length != EntropySeedCodec.SEED_BYTES) {
      clearBytes(seed);
      return false;
    }
    if (entropyStore == null) {
      entropyStore = new EntropyStore();
    }

    byte[] material = new byte[EntropySeedCodec.SEED_BYTES + 17];
    System.arraycopy(seed, 0, material, 0, seed.length);
    putLong(material, seed.length, System.currentTimeMillis());
    putLong(material, seed.length + 8, Runtime.getRuntime().freeMemory());
    material[material.length - 1] = 0x52;
    SHA256 sha256 = new SHA256();
    byte[] runtimeSeed = sha256.digest(material);

    material[material.length - 1] = 0x50;
    byte[] nextStoredSeed = sha256.digest(material);
    boolean saved = entropyStore.save(nextStoredSeed);
    clearBytes(nextStoredSeed);
    clearBytes(material);
    clearBytes(seed);
    if (!saved) {
      clearBytes(runtimeSeed);
      return false;
    }

    secureRandom = new SecureRandomPlus(runtimeSeed);
    clearBytes(runtimeSeed);
    return true;
  }

  private void showInitialScreen() {
    if (savedSession != null && savedSession.authorized) {
      appShellModel.onResumeDetected();
      if (loadWarmState()) {
        appShellScreen.show();
      } else {
        appShellScreen.showStatus();
      }
    } else if (appShellModel.authorized) {
      appShellScreen.show();
    } else {
      loadWarmState();
      loginScreen.show();
    }
  }

  public void onEntropyCollected(byte[] seed) {
    if (!installSecureRandom(seed)) {
      if (entropyCanvas != null) {
        entropyCanvas.showFailure("Secure state could not be saved.");
      }
      return;
    }

    entropyCanvas = null;
    showInitialScreen();
    startConnectionAsync(savedSession != null && savedSession.shouldResumeSession());
  }

  private static void putLong(byte[] out, int offset, long value) {
    for (int i = 7; i >= 0; --i) {
      out[offset + i] = (byte)(value & 0xff);
      value >>>= 8;
    }
  }

  private static void clearBytes(byte[] value) {
    if (value == null) {
      return;
    }
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }

  private RuntimeDeviceProfile detectRuntimeDeviceProfile() {
    if (runtimeDeviceProfile == null) {
      runtimeDeviceProfile = RuntimeDeviceProfile.detect();
    }
    return runtimeDeviceProfile;
  }

  private void ensureSyncState() {
    if (syncState == null) {
      syncState = new SyncState();
    }
  }

  private void clearSyncState() {
    ensureSyncState();
    syncState.clear();
    syncStateRequestInFlight = false;
    differenceRequestInFlight = false;
    channelDifferenceRequestInFlight = false;
    clearPendingDiscoverySearch();
    clearPendingPeerInfo();
    clearPendingPeerAction();
    clearPendingDeleteState();
    clearPendingReadState();
    pendingSelectedPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingSelectedPeerId = 0;
    pendingChannelDifferencePeerId = 0;
    refreshSelectedHistoryAfterDialogs = false;
    syncShellRuntimeState();
  }

  private void clearPendingSendState() {
    pendingSendPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingSendPeerId = 0;
    pendingSendRandomId = 0;
  }

  private void clearPendingHistoryState() {
    pendingHistoryPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingHistoryPeerId = 0;
  }

  private void clearPendingDiscoverySearch() {
    pendingDiscoveryQuery = "";
  }

  private void clearPendingPeerInfo() {
    pendingPeerInfoKind = DialogListModel.PEER_UNKNOWN;
    pendingPeerInfoId = 0;
  }

  private void clearPendingDeleteState() {
    pendingDeletePeerKind = DialogListModel.PEER_UNKNOWN;
    pendingDeletePeerId = 0;
    pendingDeleteMessageId = 0;
  }

  private void clearPendingReadState() {
    pendingReadPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingReadPeerId = 0;
    pendingReadMaxId = 0;
    pendingReadPreviousInboxMaxId = 0;
    pendingReadPreviousUnreadCount = 0;
  }

  private boolean readRequestInFlight() {
    return pendingReadPeerKind != DialogListModel.PEER_UNKNOWN && pendingReadMaxId > 0;
  }

  private boolean hasRpcConflictForVisibleRead() {
    return syncStateRequestInFlight ||
      differenceRequestInFlight ||
      channelDifferenceRequestInFlight ||
      (syncState != null && syncState.needsDifference) ||
      pendingHistoryPeerKind != DialogListModel.PEER_UNKNOWN ||
      (appShellModel != null &&
        (appShellModel.reader.loadingDialogs || appShellModel.reader.loadingHistory)) ||
      pendingDeleteMessageId != 0 ||
      pendingSendPeerKind != DialogListModel.PEER_UNKNOWN ||
      pendingDiscoveryQuery.length() > 0 ||
      pendingPeerInfoKind != DialogListModel.PEER_UNKNOWN ||
      pendingPeerActionType != PEER_ACTION_NONE ||
      pendingMediaMessageId != 0;
  }

  private void clearPendingSecretAction() {
    pendingSecretExponentValid = false;
    pendingSecretExpectedKeyFingerprint = 0;
  }

  private void clearPendingPeerAction() {
    pendingPeerActionType = PEER_ACTION_NONE;
    pendingPeerActionKind = DialogListModel.PEER_UNKNOWN;
    pendingPeerActionId = 0;
    pendingPeerActionStartedAtMs = 0;
    clearPendingSecretAction();
  }

  private void rememberPendingPeerAction(int actionType, int peerKind, long peerId) {
    pendingPeerActionType = actionType;
    pendingPeerActionKind = peerKind;
    pendingPeerActionId = peerId;
    pendingPeerActionStartedAtMs = System.currentTimeMillis();
  }

  private void clearRememberedSelectedDialog() {
    pendingSelectedPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingSelectedPeerId = 0;
  }

  private void rememberSelectedDialogForRefresh() {
    DialogListModel.Item selected;

    if (appShellModel == null) {
      clearRememberedSelectedDialog();
      return;
    }

    selected = appShellModel.selectedDialog();
    if (selected == null) {
      clearRememberedSelectedDialog();
      return;
    }

    pendingSelectedPeerKind = selected.peerKind;
    pendingSelectedPeerId = selected.peerId;
  }

  private void restoreSelectedDialogAfterRefresh() {
    int index;

    if (appShellModel == null ||
        pendingSelectedPeerKind == DialogListModel.PEER_UNKNOWN ||
        pendingSelectedPeerId == 0) {
      clearRememberedSelectedDialog();
      return;
    }

    index = appShellModel.reader.dialogIndexForPeer(
      pendingSelectedPeerKind,
      pendingSelectedPeerId
    );
    clearRememberedSelectedDialog();
    if (index >= 0) {
      appShellModel.selectDialog(index);
    }
  }

  private void clearReconnectFlags() {
    reconnectAuthorizedPending = false;
    resumeOutboundAfterDialogs = false;
    outboundResumeSyncReady = false;
    pendingRouteAfterDialogs = 0;
  }

  private long nextOutboundRandomId() {
    if (connection == null || connection.random_number_generator == null) {
      throw new IllegalStateException("Secure randomness is unavailable.");
    }

    long randomId = 0;
    while (randomId == 0) {
      randomId = connection.random_number_generator.nextLong();
    }
    return randomId;
  }

  private boolean shouldAttemptAuthorizedReconnect() {
    return savedSession != null &&
      savedSession.shouldResumeSession() &&
      (restoreAuthorizedPending || reconnectAuthorizedPending ||
        (appShellModel != null && appShellModel.authorized));
  }

  private boolean shouldRefreshSelectedHistoryAfterReconnect() {
    int screen;

    if (appShellModel == null || appShellModel.selectedDialog() == null) {
      return false;
    }

    screen = appShellModel.router.currentScreen;
    return screen == AppShellModel.SCREEN_HISTORY ||
      screen == AppShellModel.SCREEN_COMPOSE;
  }

  private boolean scheduleAuthorizedReconnect(String detail, int errorCode) {
    MTProtoConnection oldConnection;

    if (!shouldAttemptAuthorizedReconnect()) {
      return false;
    }

    if (resumeState != null) {
      resumeState.noteDisconnect(errorCode);
      if (resumeState.reconnectAttempts > 3) {
        resumeState.noteFailure(errorCode);
        syncShellRuntimeState();
        appShellModel.onReaderFailure(detail + " Reconnect limit reached.");
        appShellScreen.showStatus();
        return false;
      }
      syncShellRuntimeState();
    }

    // A reconnecting dialog refresh reconciles an ambiguously acknowledged read RPC.
    clearPendingReadState();
    reconnectAuthorizedPending = true;
    resumeOutboundAfterDialogs = appShellModel != null && appShellModel.hasRecoverableOutboundSend();
    outboundResumeSyncReady = false;
    // The old transport can no longer own these volatile RPC slots. The
    // compose/outbound warm state remains the durable send job and retains its
    // original random_id for an idempotent retry on the new connection.
    clearPendingSendState();
    clearPendingHistoryState();
    syncStateRequestInFlight = false;
    differenceRequestInFlight = false;
    channelDifferenceRequestInFlight = false;
    pendingChannelDifferencePeerId = 0;
    syncShellRuntimeState();
    persistWarmState();
    if (appShellModel != null) {
      pendingRouteAfterDialogs = appShellModel.router.currentScreen;
      refreshSelectedHistoryAfterDialogs =
        refreshSelectedHistoryAfterDialogs || shouldRefreshSelectedHistoryAfterReconnect();
      appShellModel.activeDetail = detail != null ? detail : "Reconnecting authorized session...";
    }
    if (appShellScreen != null) {
      appShellScreen.showStatus();
    }
    System.out.println("RECONNECT START: " + (detail != null ? detail : "Reconnecting authorized session."));

    oldConnection = connection;
    connection = null;
    loginScreen.setConnection(null);
    if (oldConnection != null) {
      try {
        oldConnection.close();
      } catch (IOException e) {
      }
    }
    startConnectionAsync(true);
    return true;
  }

  private void bindConnectionCallbacks(MTProtoConnection connection, LoginScreen activeLoginScreen) {
    connection.bind_callback(new HandleRecieveNewSessionCreatedUi(connection, activeLoginScreen, appShellModel, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveBootstrapConfigUi(CombinatorIds.config, connection, activeLoginScreen, appShellModel, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveBootstrapConfigUi(CombinatorIds.config_modern, connection, activeLoginScreen, appShellModel, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveRPCErrorUi(connection, activeLoginScreen, appShellModel, appShellScreen, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSentCodeUi(connection, activeLoginScreen));
    connection.bind_callback(new HandleRecievePasswordStateUi(connection, activeLoginScreen));
    connection.bind_callback(new HandleRecieveAuthorizedUi(
      CombinatorIds.auth_sent_code_success,
      connection,
      activeLoginScreen,
      appShellModel,
      appShellScreen,
      TelegramMicro.this
    ));
    connection.bind_callback(new HandleRecieveAuthorizedUi(
      CombinatorIds.auth_authorization,
      connection,
      activeLoginScreen,
      appShellModel,
      appShellScreen,
      TelegramMicro.this
    ));
    connection.bind_callback(new HandleRecieveSignUpRequiredUi(connection, activeLoginScreen));
    connection.bind_callback(new HandleRecieveDialogsUi(CombinatorIds.messages_dialogs, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveDialogsUi(CombinatorIds.messages_dialogs_slice, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveDialogsUi(CombinatorIds.messages_dialogs_not_modified, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_messages, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_messages_legacy, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_messages_slice, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_messages_slice_legacy, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_messages_not_modified, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveHistoryUi(CombinatorIds.messages_channel_messages, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveDeleteMessagesUi(connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveShortSentMessageUi(connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUpdatesStateUi(connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUpdatesDifferenceUi(CombinatorIds.updates_difference_empty, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUpdatesDifferenceUi(CombinatorIds.updates_difference, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUpdatesDifferenceUi(CombinatorIds.updates_difference_slice, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUpdatesDifferenceUi(CombinatorIds.updates_difference_too_long, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveChannelDifferenceUi(CombinatorIds.updates_channel_difference_empty, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveChannelDifferenceUi(CombinatorIds.updates_channel_difference, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveChannelDifferenceUi(CombinatorIds.updates_channel_difference_too_long, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.updates, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.updates_combined, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.updates_too_long, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.update_short, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.update_short_message, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveAuxiliaryUpdateUi(CombinatorIds.update_short_chat_message, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveDiscoveryFoundUi(connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecievePeerInfoUi(CombinatorIds.users_user_full, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecievePeerInfoUi(CombinatorIds.messages_chat_full, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSecretChatUi(CombinatorIds.encrypted_chat_waiting, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSecretChatUi(CombinatorIds.encrypted_chat_requested, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSecretChatUi(CombinatorIds.encrypted_chat, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSecretDiscardUi(CombinatorIds.bool_true, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveSecretDiscardUi(CombinatorIds.bool_false, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUploadFileUi(CombinatorIds.upload_file, connection, TelegramMicro.this));
    connection.bind_callback(new HandleRecieveUploadFileUi(CombinatorIds.upload_file_cdn_redirect, connection, TelegramMicro.this));
  }

  private void onDirectAuthorizedRestoreReady() {
    boolean reconnecting = reconnectAuthorizedPending;

    if (!restoreAuthorizedPending && !reconnectAuthorizedPending) {
      return;
    }

    if (resumeState != null) {
      if (savedSession != null) {
        resumeState.dcId = savedSession.dcId;
      }
      resumeState.noteSuccess();
      syncShellRuntimeState();
    }
    if (savedSession != null && appShellModel != null) {
      appShellModel.onBootstrapReady(savedSession.dcId, connectionHost + ":" + connectionPort, 1);
    }

    restoreAuthorizedPending = false;
    reconnectAuthorizedPending = false;
    appShellModel.onResumeAuthorized();
    if (pendingRouteAfterDialogs == 0 && appShellModel != null) {
      pendingRouteAfterDialogs = appShellModel.router.currentScreen;
    }
    appShellScreen.show();
    resumeOutboundAfterDialogs = appShellModel != null && appShellModel.hasRecoverableOutboundSend();
    outboundResumeSyncReady = false;
    if (reconnecting) {
      System.out.println("RECONNECT READY: requesting dialogs on saved session.");
    } else {
      System.out.println("RESTORE READY: requesting dialogs on saved session.");
    }
    persistSessionState();
    requestDialogs();
    requestAuthorizedRecoverySync();
  }

  private void startConnectionAsync(final boolean restoreSavedSession) {
    if (secureRandom == null) {
      throw new IllegalStateException("Network startup requires initialized secure randomness.");
    }
    final LoginScreen activeLoginScreen = loginScreen;
    final SecureRandomPlus activeSecureRandom = secureRandom;
    final int generation = ++connectionGeneration;
    final String connectHost = connectionHost;
    final String connectPort = connectionPort;
    final String connectHelpUrl = authHelpUrl;

    new Thread(new Runnable() {
      public void run() {
        MTProtoConnection threadConnection = null;
        boolean useDirectAuthorizedRestore = false;

        try {
          threadConnection = new MTProtoConnection(
            connectHost,
            connectPort,
            connectHelpUrl,
            activeSecureRandom
          );
          if (generation != connectionGeneration) {
            threadConnection.close();
            return;
          }

          connection = threadConnection;
          activeLoginScreen.setConnection(threadConnection);
          if (restoreSavedSession && savedSession != null && savedSession.shouldResumeSession()) {
            threadConnection.applySavedSession(savedSession);
            /*
             * Match the PS2 restore path: bring up a fresh authorized socket
             * and issue the first reader query directly instead of inserting an
             * extra authorized help.getConfig round-trip first.
             */
            threadConnection.prepareRestoredSession(false);
            useDirectAuthorizedRestore = true;
          }

          bindConnectionCallbacks(threadConnection, activeLoginScreen);
          if (useDirectAuthorizedRestore) {
            onDirectAuthorizedRestoreReady();
          }
          threadConnection.main_loop();
        } catch (IOException e) {
          if (generation != connectionGeneration) {
            return;
          }
          recordFailure(FailureLog.CATEGORY_NETWORK, FailureLog.SEVERITY_ERROR, -301, generation);
          if (shouldAttemptAuthorizedReconnect()) {
            if (scheduleAuthorizedReconnect("Connection failed: " + e.toString(), -301)) {
              return;
            }
            if (resumeState != null) {
              syncShellRuntimeState();
            }
            if (appShellModel != null) {
              appShellModel.onReaderFailure("Connection failed: " + e.toString());
              appShellScreen.showStatus();
            }
          } else {
            activeLoginScreen.onConnectionFailed(e.toString());
          }
          e.printStackTrace();
        }
      }
    }).start();
  }

  public static class HandleRecieveNewSessionCreatedUi extends MTProtoCallback {
    private LoginScreen loginScreen;
    private MTProtoConnection connection;
    private AppShellModel appShellModel;
    private TelegramMicro midlet;

    public HandleRecieveNewSessionCreatedUi(MTProtoConnection connection, LoginScreen loginScreen, AppShellModel appShellModel, TelegramMicro midlet) {
      super(CombinatorIds.new_session_created, connection);
      this.loginScreen = loginScreen;
      this.connection = connection;
      this.appShellModel = appShellModel;
      this.midlet = midlet;
    }

    public void execute(Response response) {
      long elapsedMs = System.currentTimeMillis() - connection.handshake_start;
      loginScreen.onSessionCreated(elapsedMs);
      appShellModel.onSessionCreated(elapsedMs);
      midlet.persistSessionState();
    }
  }

  public static class HandleRecieveBootstrapConfigUi extends MTProtoCallback {
    private LoginScreen loginScreen;
    private AppShellModel appShellModel;
    private TelegramMicro midlet;

    public HandleRecieveBootstrapConfigUi(int combinator_id, MTProtoConnection connection, LoginScreen loginScreen, AppShellModel appShellModel, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.loginScreen = loginScreen;
      this.appShellModel = appShellModel;
      this.midlet = midlet;
    }

    public void execute(Response response) {
      RecieveConfig config = RecieveConfig.from_rpc_response((RPCResponse)response);
      String endpoint = "";
      RecieveConfig.DCOption current_dc = config.find_this_dc_option();
      if (current_dc != null) {
        endpoint = current_dc.ip_address + ":" + Integer.toString(current_dc.port);
      }
      loginScreen.onBootstrapReady(config.this_dc, endpoint, config.dc_options.length);
      appShellModel.onBootstrapReady(config.this_dc, endpoint, config.dc_options.length);
      midlet.onBootstrapReady(config);
    }
  }

  public static class HandleRecieveRPCErrorUi extends MTProtoCallback {
    private LoginScreen loginScreen;
    private MTProtoConnection connection;
    private AppShellModel appShellModel;
    private AppShellScreen appShellScreen;
    private TelegramMicro midlet;

    public HandleRecieveRPCErrorUi(
      MTProtoConnection connection,
      LoginScreen loginScreen,
      AppShellModel appShellModel,
      AppShellScreen appShellScreen,
      TelegramMicro midlet
    ) {
      super(CombinatorIds.rpc_error, connection);
      this.loginScreen = loginScreen;
      this.connection = connection;
      this.appShellModel = appShellModel;
      this.appShellScreen = appShellScreen;
      this.midlet = midlet;
    }

    public void execute(Response response) {
      RecieveRPCError rpc_error = RecieveRPCError.from_rpc_response((RPCResponse)response);
      String error_message = safeAscii(rpc_error.error_message);
      if ("SESSION_PASSWORD_NEEDED".equals(error_message) &&
          !midlet.isAuthorizedSessionActive()) {
        connection.clearPendingRetryRequest();
        loginScreen.onPasswordParametersRequested();
        (new SendGetPassword()).send(connection);
        return;
      }
      if (midlet.handleLoginDcMigration(rpc_error, error_message)) {
        return;
      }
      if (midlet.handleResumeRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handleOutboundRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handleReadHistoryRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handleDiscoveryRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handlePeerRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handleDeleteRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      if (midlet.handleMediaRpcError(rpc_error.error_code, error_message)) {
        connection.clearPendingRetryRequest();
        return;
      }
      connection.clearPendingRetryRequest();
      if (appShellModel.authorized) {
        midlet.recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, rpc_error.error_code, 0);
        appShellModel.onReaderFailure("RPC " + Integer.toString(rpc_error.error_code) + ": " + error_message);
        appShellScreen.showCurrent();
        return;
      }
      loginScreen.onRpcError(rpc_error.error_code, error_message);
    }
  }

  public static class HandleRecieveSentCodeUi extends MTProtoCallback {
    private LoginScreen loginScreen;

    public HandleRecieveSentCodeUi(MTProtoConnection connection, LoginScreen loginScreen) {
      super(CombinatorIds.auth_sent_code, connection);
      this.loginScreen = loginScreen;
    }

    public void execute(Response response) {
      RecieveSentCode sent_code = RecieveSentCode.from_rpc_response((RPCResponse)response);
      loginScreen.onSentCode(
        sent_code.phone_code_hash,
        sent_code.has_timeout ? sent_code.timeout_seconds : 0
      );
    }
  }

  public static class HandleRecieveAuthorizedUi extends MTProtoCallback {
    private LoginScreen loginScreen;
    private AppShellModel appShellModel;
    private AppShellScreen appShellScreen;
    private TelegramMicro midlet;

    public HandleRecieveAuthorizedUi(
      int combinator_id,
      MTProtoConnection connection,
      LoginScreen loginScreen,
      AppShellModel appShellModel,
      AppShellScreen appShellScreen,
      TelegramMicro midlet
    ) {
      super(combinator_id, connection);
      this.loginScreen = loginScreen;
      this.appShellModel = appShellModel;
      this.appShellScreen = appShellScreen;
      this.midlet = midlet;
    }

    public void execute(Response response) {
      loginScreen.onAuthorized();
      appShellModel.onAuthorized();
      appShellScreen.show();
      System.out.println("AUTHORIZED: loading dialogs...");
      midlet.restoreAuthorizedPending = false;
      midlet.clearWarmState();
      midlet.clearSyncState();
      midlet.persistSessionState();
      midlet.requestDialogs();
      midlet.requestUpdatesState();
    }
  }

  public static class HandleRecievePasswordStateUi extends MTProtoCallback {
    private LoginScreen loginScreen;
    private MTProtoConnection connection;

    public HandleRecievePasswordStateUi(MTProtoConnection connection, LoginScreen loginScreen) {
      super(CombinatorIds.account_password, connection);
      this.loginScreen = loginScreen;
      this.connection = connection;
    }

    public void execute(Response response) {
      RecievePasswordState password_state = RecievePasswordState.from_rpc_response((RPCResponse)response);
      connection.current_password_state = password_state;
      loginScreen.onPasswordStateLoaded(password_state);
    }
  }

  public static class HandleRecieveSignUpRequiredUi extends MTProtoCallback {
    private LoginScreen loginScreen;

    public HandleRecieveSignUpRequiredUi(MTProtoConnection connection, LoginScreen loginScreen) {
      super(CombinatorIds.auth_authorization_sign_up_required, connection);
      this.loginScreen = loginScreen;
    }

    public void execute(Response response) {
      loginScreen.onSignUpRequired();
    }
  }

  public static class HandleRecieveDialogsUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveDialogsUi(int combinator_id, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyDialogs((RPCResponse)response);
    }
  }

  public static class HandleRecieveHistoryUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveHistoryUi(int combinator_id, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyHistory((RPCResponse)response);
    }
  }

  public static class HandleRecieveDeleteMessagesUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveDeleteMessagesUi(MTProtoConnection connection, TelegramMicro midlet) {
      super(CombinatorIds.messages_affected_messages, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyDeleteMessages((RPCResponse)response);
    }
  }

  public static class HandleRecieveShortSentMessageUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveShortSentMessageUi(MTProtoConnection connection, TelegramMicro midlet) {
      super(CombinatorIds.update_short_sent_message, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyShortSentMessage((RPCResponse)response);
    }
  }

  public static class HandleRecieveUpdatesStateUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveUpdatesStateUi(MTProtoConnection connection, TelegramMicro midlet) {
      super(CombinatorIds.updates_state, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyUpdatesState((RPCResponse)response);
    }
  }

  public static class HandleRecieveUpdatesDifferenceUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveUpdatesDifferenceUi(int combinator_id, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyUpdatesDifference((RPCResponse)response);
    }
  }

  public static class HandleRecieveAuxiliaryUpdateUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveAuxiliaryUpdateUi(int combinator_id, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyAuxiliaryUpdate(response);
    }
  }

  public static class HandleRecieveChannelDifferenceUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveChannelDifferenceUi(int combinator_id, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinator_id, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyChannelDifference((RPCResponse)response);
    }
  }

  public static class HandleRecieveDiscoveryFoundUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveDiscoveryFoundUi(MTProtoConnection connection, TelegramMicro midlet) {
      super(CombinatorIds.contacts_found, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyContactsFound((RPCResponse)response);
    }
  }

  public static class HandleRecievePeerInfoUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecievePeerInfoUi(int combinatorId, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinatorId, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyPeerInfo((RPCResponse)response);
    }
  }

  public static class HandleRecieveSecretChatUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveSecretChatUi(int combinatorId, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinatorId, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applySecretChat((RPCResponse)response);
    }
  }

  public static class HandleRecieveSecretDiscardUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveSecretDiscardUi(int combinatorId, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinatorId, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applySecretDiscardResult((RPCResponse)response);
    }
  }

  public static class HandleRecieveUploadFileUi extends MTProtoCallback {
    private TelegramMicro midlet;

    public HandleRecieveUploadFileUi(int combinatorId, MTProtoConnection connection, TelegramMicro midlet) {
      super(combinatorId, connection);
      this.midlet = midlet;
    }

    public void execute(Response response) {
      midlet.applyMediaFile((RPCResponse)response);
    }
  }

  public TelegramMicro() {
    super();
  }

  private void syncShellRuntimeState() {
    if (appShellModel == null) {
      return;
    }
    appShellModel.onResumeStateChanged(resumeState);
    appShellModel.onSyncStateChanged(syncState, syncStateRequestInFlight, differenceRequestInFlight);
    appShellModel.onFailureLogChanged(failureLog);
  }

  private void ensureFailureLog() {
    if (failureLog == null) {
      failureLog = new FailureLog();
    }
    if (failureLogStore == null) {
      failureLogStore = new FailureLogStore();
    }
  }

  private void loadFailureLog() {
    ensureFailureLog();
    failureLogStore.load(failureLog);
    syncShellRuntimeState();
  }

  private void persistFailureLog() {
    if (failureLogStore == null || failureLog == null) {
      return;
    }
    failureLogStore.save(failureLog);
  }

  private boolean loadWarmState() {
    WarmStateSnapshot snapshot;

    if (warmStateStore == null) {
      warmStateStore = new WarmStateStore();
    }
    if (appShellModel == null) {
      return false;
    }

    snapshot = new WarmStateSnapshot();
    if (!warmStateStore.load(snapshot) || !snapshot.hasUsefulState()) {
      return false;
    }

    if (savedSession != null && savedSession.authorized && snapshot.hasUsefulShellState()) {
      appShellModel.applyWarmState(snapshot, true);
      pendingRouteAfterDialogs = snapshot.route;
      return true;
    }
    if ((savedSession == null || !savedSession.authorized) &&
        loginScreen != null && snapshot.hasUsefulLoginState()) {
      return loginScreen.applyWarmState(snapshot);
    }
    return false;
  }

  private void persistWarmState() {
    WarmStateSnapshot snapshot;

    if (warmStateStore == null) {
      return;
    }

    snapshot = appShellModel != null ? appShellModel.captureWarmState() : new WarmStateSnapshot();
    if (loginScreen != null) {
      loginScreen.captureWarmState(snapshot);
    }
    if (!snapshot.hasUsefulState()) {
      if (!warmStateStore.clear()) {
        recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -204, 1);
      }
      return;
    }

    if (!warmStateStore.save(snapshot)) {
      recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -204, 0);
    }
  }

  private void clearWarmState() {
    if (warmStateStore != null && !warmStateStore.clear()) {
      recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -205, 0);
    }
    clearReconnectFlags();
  }

  private void recordFailure(int category, int severity, int code, int detail) {
    ensureFailureLog();
    failureLog.push(category, severity, code, detail);
    persistFailureLog();
    syncShellRuntimeState();
  }

  private boolean loadSavedSession() {
    SessionSnapshot snapshot;
    String knownHost;

    if (sessionStore == null) {
      sessionStore = new SessionStore();
    }
    if (savedSession != null && savedSession.shouldResumeSession()) {
      syncShellRuntimeState();
      return true;
    }

    snapshot = new SessionSnapshot();
    if (!sessionStore.load(snapshot) || !snapshot.hasUsableAuth()) {
      savedSession = null;
      clearSyncState();
      resumeState.clear();
      syncShellRuntimeState();
      return false;
    }

    if (!snapshot.shouldResumeSession()) {
      if (!sessionStore.clear()) {
        recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -201, 0);
      }
      savedSession = null;
      clearSyncState();
      resumeState.clear();
      syncShellRuntimeState();
      return false;
    }

    if (snapshot.hasTransportReplayState()) {
      snapshot.clearTransportReplayState();
      if (!sessionStore.save(snapshot)) {
        recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -206, snapshot.dcId);
      }
    }

    savedSession = snapshot;
    knownHost = KnownDcHost.production(savedSession.dcId);
    if (knownHost != null) {
      connectionHost = knownHost;
      connectionPort = "5222";
    }
    resumeState.load(savedSession);
    restoreAuthorizedPending = savedSession.authorized;
    syncShellRuntimeState();
    return true;
  }

  private void clearSavedSession(String detail) {
    if (sessionStore != null) {
      if (!sessionStore.clear()) {
        recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -202, 0);
      }
    }
    if (savedSession != null) {
      savedSession.clear();
      savedSession = null;
    }
    clearWarmState();
    clearSyncState();
    restoreAuthorizedPending = false;
    clearReconnectFlags();
    resumeState.clear();
    if (appShellModel != null) {
      appShellModel.onResumeFallbackToLogin(detail);
    }
    syncShellRuntimeState();
  }

  private void persistSessionState() {
    SessionSnapshot liveSnapshot;
    SessionSnapshot durableSnapshot;

    if (sessionStore == null || connection == null) {
      return;
    }

    liveSnapshot = connection.captureSessionSnapshot(appShellModel != null && appShellModel.authorized, appShellModel != null ? appShellModel.currentDc : 0);
    if (!liveSnapshot.shouldResumeSession()) {
      return;
    }

    // Persist durable auth state, but start a fresh MTProto transport on the next socket.
    durableSnapshot = new SessionSnapshot();
    durableSnapshot.copyFrom(liveSnapshot);
    durableSnapshot.clearTransportReplayState();

    if (!sessionStore.save(durableSnapshot)) {
      recordFailure(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_ERROR, -203, durableSnapshot.dcId);
    }
    if (savedSession == null) {
      savedSession = new SessionSnapshot();
    }
    savedSession.copyFrom(durableSnapshot);
    resumeState.load(liveSnapshot);
    syncShellRuntimeState();
  }

  private void onBootstrapReady(RecieveConfig config) {
    boolean reconnecting = reconnectAuthorizedPending;
    boolean restoring = restoreAuthorizedPending;

    if (resumeState != null) {
      resumeState.dcId = config.this_dc;
      resumeState.noteSuccess();
      syncShellRuntimeState();
    }
    persistSessionState();
    if (reconnectRetryRequest != null && connection != null) {
      MTProtoConnection.PendingRetryRequest retry = reconnectRetryRequest;
      reconnectRetryRequest = null;
      connection.restorePendingRetryRequest(retry);
      if (connection.retryPendingRequest()) {
        loginScreen.onUserVisibleError(
          "Retrying request",
          "Telegram requested DC " + Integer.toString(config.this_dc) + ". Retrying the last login request."
        );
        loginScreen.show();
        return;
      }
    }
    if (!restoreAuthorizedPending && !reconnectAuthorizedPending) {
      return;
    }

    restoreAuthorizedPending = false;
    reconnectAuthorizedPending = false;
    appShellModel.onResumeAuthorized();
    if (pendingRouteAfterDialogs == 0 && appShellModel != null) {
      pendingRouteAfterDialogs = appShellModel.router.currentScreen;
    }
    appShellScreen.show();
    resumeOutboundAfterDialogs = appShellModel.hasRecoverableOutboundSend();
    outboundResumeSyncReady = false;
    if (reconnecting) {
      System.out.println("RECONNECT READY: requesting dialogs on saved session.");
    } else if (restoring) {
      System.out.println("RESTORE READY: requesting dialogs on saved session.");
    }
    persistSessionState();
    requestDialogs();
    requestAuthorizedRecoverySync();
  }

  private boolean handleResumeRpcError(int code, String message) {
    boolean rejectingSavedAuthorization = restoreAuthorizedPending ||
      reconnectAuthorizedPending ||
      (savedSession != null && savedSession.authorized && appShellModel != null && appShellModel.authorized);

    if (!rejectingSavedAuthorization) {
      return false;
    }
    if (code != 401 &&
        !"AUTH_KEY_UNREGISTERED".equals(message) &&
        !"SESSION_REVOKED".equals(message) &&
        !"SESSION_PASSWORD_NEEDED".equals(message)) {
      return false;
    }

    if (resumeState != null) {
      resumeState.noteFailure(code);
      syncShellRuntimeState();
    }
    recordFailure(FailureLog.CATEGORY_BOOTSTRAP, FailureLog.SEVERITY_WARN, code, savedSession != null ? savedSession.dcId : 0);
    clearSavedSession("Saved authorization was rejected. Starting a fresh login.");
    if (connection != null) {
      MTProtoConnection oldConnection = connection;
      connection = null;
      loginScreen.setConnection(null);
      try {
        oldConnection.close();
      } catch (IOException e) {
      }
      startConnectionAsync(false);
    }
    loginScreen.onUserVisibleError("Saved session cleared", "Telegram rejected the saved authorization.");
    loginScreen.show();
    return true;
  }

  private boolean isAuthorizedSessionActive() {
    return restoreAuthorizedPending || reconnectAuthorizedPending ||
      (appShellModel != null && appShellModel.authorized);
  }

  private DialogListModel.Item findDialog(int peerKind, long peerId) {
    if (appShellModel == null) {
      return null;
    }

    for (int i = 0; i < appShellModel.reader.dialogs.count; ++i) {
      DialogListModel.Item item = appShellModel.reader.dialogs.items[i];
      if (item.peerKind == peerKind && item.peerId == peerId) {
        return item;
      }
    }
    return null;
  }

  private boolean shouldResumeWarmOutbound() {
    return appShellModel != null &&
      connection != null &&
      pendingSendPeerKind == DialogListModel.PEER_UNKNOWN &&
      pendingSendPeerId == 0 &&
      appShellModel.hasRecoverableOutboundSend();
  }

  private boolean outboundResumePrerequisitesReady() {
    return shouldResumeWarmOutbound() &&
      appShellModel.authorized &&
      OutboundRecoveryState.resumePrerequisitesReady(
        outboundResumeSyncReady,
        appShellModel.reader.loadingDialogs,
        appShellModel.reader.loadingHistory,
        pendingHistoryPeerKind != DialogListModel.PEER_UNKNOWN,
        syncStateRequestInFlight,
        differenceRequestInFlight,
        channelDifferenceRequestInFlight,
        syncState != null && syncState.needsDifference
      );
  }

  /**
   * Retries only after dialog/access-hash refresh and update reconciliation
   * have settled. The flag remains set while either response is outstanding,
   * so callback ordering cannot silently lose an interrupted send.
   */
  private boolean maybeResumeInterruptedOutbound() {
    boolean started;

    if (!resumeOutboundAfterDialogs || appShellModel == null) {
      return false;
    }
    if (!appShellModel.hasRecoverableOutboundSend()) {
      resumeOutboundAfterDialogs = false;
      outboundResumeSyncReady = false;
      if (appShellModel.isOutboundSendLocked()) {
        appShellModel.onComposeFailed(0, AppShellModel.OUTBOUND_RECOVERY_INVALID_DETAIL);
        persistWarmState();
        if (appShellScreen != null) {
          appShellScreen.showCurrent();
        }
      }
      return false;
    }
    if (!outboundResumePrerequisitesReady()) {
      return false;
    }

    started = tryResumeInterruptedOutbound();
    if (started || !appShellModel.hasRecoverableOutboundSend()) {
      resumeOutboundAfterDialogs = false;
      outboundResumeSyncReady = false;
    }
    if (started && appShellScreen != null) {
      appShellScreen.showCurrent();
    }
    return started;
  }

  private boolean tryResumeInterruptedOutbound() {
    SendGetDialogs.DialogInputPeer fromPeer;
    SendGetDialogs.DialogInputPeer peer;
    long randomId;

    if (!shouldResumeWarmOutbound()) {
      return false;
    }

    try {
      peer = toInputPeer(appShellModel.compose.peerKind, appShellModel.compose.peerId);
    } catch (IllegalArgumentException e) {
      appShellModel.onComposeFailed(0, "Interrupted outbound message cannot be retried: " + e.getMessage());
      persistWarmState();
      appShellScreen.showCurrent();
      return false;
    }

    randomId = appShellModel.compose.lastRandomId;
    if (randomId == 0) {
      appShellModel.onComposeFailed(0, AppShellModel.OUTBOUND_RECOVERY_INVALID_DETAIL);
      persistWarmState();
      appShellScreen.showCurrent();
      return false;
    }
    if (!appShellModel.resumeInterruptedComposeSend(randomId)) {
      return false;
    }

    pendingSendPeerKind = appShellModel.compose.peerKind;
    pendingSendPeerId = appShellModel.compose.peerId;
    pendingSendRandomId = randomId;
    persistWarmState();
    if (appShellModel.compose.hasForwardSource()) {
      try {
        fromPeer = toInputPeer(
          appShellModel.compose.forwardFromPeerKind,
          appShellModel.compose.forwardFromPeerId
        );
      } catch (IllegalArgumentException e) {
        appShellModel.onComposeFailed(0, "Interrupted forward cannot be retried: " + e.getMessage());
        persistWarmState();
        appShellScreen.showCurrent();
        clearPendingSendState();
        return false;
      }
      (new SendForwardMessages(
        fromPeer,
        appShellModel.compose.forwardMessageId,
        randomId,
        peer
      )).send(connection);
    } else {
      (new SendSendMessage(
        peer,
        appShellModel.compose.text,
        randomId,
        appShellModel.compose.replyToMessageId
      )).send(connection);
    }
    return true;
  }

  private boolean handleLoginDcMigration(RecieveRPCError rpcError, String message) {
    RecieveConfig.DCOption targetDc;
    MTProtoConnection.PendingRetryRequest retryRequest;
    MTProtoConnection oldConnection;
    int migrateDcId = rpcError.migrate_dc_id();

    if (migrateDcId == 0 || connection == null || connection.current_config == null) {
      return false;
    }
    if (appShellModel != null && appShellModel.authorized) {
      return false;
    }

    targetDc = connection.current_config.find_preferred_dc_option(migrateDcId);
    if (targetDc == null) {
      connection.clearPendingRetryRequest();
      loginScreen.onUserVisibleError(
        "DC migration unavailable",
        "Telegram requested DC " + Integer.toString(migrateDcId) + " but no endpoint was available."
      );
      loginScreen.show();
      return true;
    }

    retryRequest = connection.capturePendingRetryRequest();
    if (retryRequest == null && loginScreen != null) {
      retryRequest = loginScreen.capturePendingRetryRequest();
    }
    if (retryRequest == null) {
      connection.clearPendingRetryRequest();
      loginScreen.onUserVisibleError("DC migration required", message);
      loginScreen.show();
      return true;
    }

    reconnectRetryRequest = retryRequest;
    System.out.println("LOGIN MIGRATE: scheduling retry on DC " + Integer.toString(migrateDcId));
    connectionHost = targetDc.ip_address;
    if (connectionPort == null || connectionPort.length() == 0) {
      connectionPort = Integer.toString(targetDc.port);
    }

    loginScreen.onUserVisibleError(
      "Switching DC",
      "Retrying on DC " + Integer.toString(migrateDcId) + " via " + connectionHost + ":" + connectionPort + "."
    );
    loginScreen.show();

    oldConnection = connection;
    connection = null;
    loginScreen.setConnection(null);
    try {
      oldConnection.close();
    } catch (IOException e) {
    }
    startConnectionAsync(false);
    return true;
  }

  public void startApp() {
    display = Display.getDisplay(this);

    if (appShellModel == null) {
      appShellModel = new AppShellModel();
      appShellModel.setRuntimeDeviceProfile(detectRuntimeDeviceProfile());
    }
    ensureSyncState();

    if (loginScreen == null) {
      loginScreen = new LoginScreen(this, display);
      loginScreen.setBundledCredentials(bundledCredentials);
    }

    if (appShellScreen == null) {
      appShellScreen = new AppShellScreen(this, display, appShellModel, this);
    }

    if (readerParser == null) {
      readerParser = new ReaderResultParser(
        appShellModel.reader.dialogs.capacity(),
        appShellModel.reader.history.capacity(),
        appShellModel.reader.peers.items.length
      );
    }

    if (resumeState == null) {
      resumeState = new ResumeState();
    }
    if (warmStateStore == null) {
      warmStateStore = new WarmStateStore();
    }

    loadFailureLog();
    loadSavedSession();

    if (started) {
      if (secureRandom == null && entropyCanvas != null) {
        display.setCurrent(entropyCanvas);
      } else {
        showInitialScreen();
      }
      return;
    }
    started = true;

    authHelpUrl = appPropertyOrNull("authHelpUrl");

    if (!initializeSecureRandomFromStore()) {
      System.out.println("ENTROPY REQUIRED: network startup deferred.");
      entropyCanvas = new EntropyCanvas(this);
      display.setCurrent(entropyCanvas);
      return;
    }

    showInitialScreen();
    startConnectionAsync(savedSession != null && savedSession.shouldResumeSession());
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {
    if (appShellScreen != null) {
      appShellScreen.syncVisibleState();
    }
    persistWarmState();
    persistSessionState();
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException e) {
      }
    }
    notifyDestroyed();
  }

  private SendGetDialogs.DialogInputPeer toInputPeer(DialogListModel.Item dialog) {
    if (dialog == null) {
      throw new IllegalArgumentException("No dialog selected.");
    }
    if (dialog.peerKind == DialogListModel.PEER_USER) {
      if (!dialog.hasAccessHash) {
        throw new IllegalArgumentException("Selected user dialog is missing an access hash.");
      }
      return SendGetDialogs.DialogInputPeer.user(dialog.peerId, dialog.accessHash);
    }
    if (dialog.peerKind == DialogListModel.PEER_CHAT) {
      return SendGetDialogs.DialogInputPeer.chat(dialog.peerId);
    }
    if (dialog.peerKind == DialogListModel.PEER_CHANNEL) {
      if (!dialog.hasAccessHash) {
        throw new IllegalArgumentException("Selected channel dialog is missing an access hash.");
      }
      return SendGetDialogs.DialogInputPeer.channel(dialog.peerId, dialog.accessHash);
    }
    throw new IllegalArgumentException("Unsupported dialog peer kind.");
  }

  private PeerDirectory.Item findPeerIdentity(int peerKind, long peerId) {
    if (appShellModel == null) {
      return null;
    }
    return appShellModel.reader.peers.find(peerKind, peerId);
  }

  private long resolveAccessHash(int peerKind, long peerId) {
    DialogListModel.Item dialog = findDialog(peerKind, peerId);
    PeerDirectory.Item peer = findPeerIdentity(peerKind, peerId);

    if (dialog != null && dialog.hasAccessHash) {
      return dialog.accessHash;
    }
    if (peer != null && peer.hasAccessHash) {
      return peer.accessHash;
    }
    return 0;
  }

  private SendGetDialogs.DialogInputPeer toInputPeer(int peerKind, long peerId) {
    DialogListModel.Item dialog = findDialog(peerKind, peerId);
    long accessHash;

    if (dialog != null) {
      return toInputPeer(dialog);
    }

    accessHash = resolveAccessHash(peerKind, peerId);
    if (peerKind == DialogListModel.PEER_USER) {
      if (accessHash == 0) {
        throw new IllegalArgumentException("Selected user peer is missing an access hash.");
      }
      return SendGetDialogs.DialogInputPeer.user(peerId, accessHash);
    }
    if (peerKind == DialogListModel.PEER_CHAT) {
      return SendGetDialogs.DialogInputPeer.chat(peerId);
    }
    if (peerKind == DialogListModel.PEER_CHANNEL) {
      if (accessHash == 0) {
        throw new IllegalArgumentException("Selected channel peer is missing an access hash.");
      }
      return SendGetDialogs.DialogInputPeer.channel(peerId, accessHash);
    }
    throw new IllegalArgumentException("Unsupported peer kind.");
  }

  private int mergePeerDirectory(PeerDirectory source) {
    int merged = 0;
    int i;

    if (appShellModel == null || source == null) {
      return 0;
    }

    for (i = 0; i < source.count; ++i) {
      PeerDirectory.Item peer = source.items[i];
      if (appShellModel.reader.peers.upsertIdentity(
          peer.peerKind,
          peer.peerId,
          peer.accessHash,
          peer.hasAccessHash,
          peer.minAccessHash,
          peer.title,
          peer.username)) {
        merged += 1;
      }
    }
    return merged;
  }

  private void requestDiscoverySearch(String query) {
    String trimmed = trimOrEmpty(query);

    if (connection == null || appShellModel == null || trimmed.length() == 0) {
      clearPendingDiscoverySearch();
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.discovery.detail = READ_ACK_WAIT_DETAIL;
      appShellModel.activeDetail = READ_ACK_WAIT_DETAIL;
      if (appShellScreen != null) {
        appShellScreen.showDiscovery();
      }
      return;
    }

    pendingDiscoveryQuery = trimmed;
    appShellModel.discovery.detail = "Searching Telegram for " + trimmed + "...";
    appShellModel.activeDetail = appShellModel.discovery.detail;
    persistWarmState();
    if (appShellScreen != null) {
      appShellScreen.showDiscovery();
    }
    (new SendSearch(trimmed, DiscoveryStateModel.RESULT_CAPACITY)).send(connection);
  }

  private void requestSelectedPeerInfo(String detail) {
    long accessHash;

    if (appShellModel == null || appShellScreen == null || appShellModel.peer.peerId == 0) {
      clearPendingPeerInfo();
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }

    pendingPeerInfoKind = appShellModel.peer.peerKind;
    pendingPeerInfoId = appShellModel.peer.peerId;

    switch (appShellModel.peer.peerKind) {
      case DialogListModel.PEER_USER:
        accessHash = resolveAccessHash(appShellModel.peer.peerKind, appShellModel.peer.peerId);
        if (connection == null || accessHash == 0) {
          appShellModel.notePeerReady("USER", "Cached user data only.");
          appShellScreen.showPeer();
          clearPendingPeerInfo();
          return;
        }
        appShellModel.notePeerAction("INFO", detail != null ? detail : "Requesting Telegram user details...");
        persistWarmState();
        appShellScreen.showPeer();
        (new SendGetFullUser(appShellModel.peer.peerId, accessHash)).send(connection);
        return;
      case DialogListModel.PEER_CHAT:
        if (connection == null) {
          appShellModel.notePeerReady("GROUP", "Cached group data only.");
          appShellScreen.showPeer();
          clearPendingPeerInfo();
          return;
        }
        appShellModel.notePeerAction("INFO", detail != null ? detail : "Requesting Telegram group details...");
        persistWarmState();
        appShellScreen.showPeer();
        (new SendGetFullChat(appShellModel.peer.peerId)).send(connection);
        return;
      case DialogListModel.PEER_CHANNEL:
        accessHash = resolveAccessHash(appShellModel.peer.peerKind, appShellModel.peer.peerId);
        if (connection == null || accessHash == 0) {
          appShellModel.notePeerReady("CHANNEL", "Cached channel data only.");
          appShellScreen.showPeer();
          clearPendingPeerInfo();
          return;
        }
        appShellModel.notePeerAction("INFO", detail != null ? detail : "Requesting Telegram channel details...");
        persistWarmState();
        appShellScreen.showPeer();
        (new SendGetFullChannel(appShellModel.peer.peerId, accessHash)).send(connection);
        return;
      default:
        appShellModel.notePeerError("Select a peer before requesting details.");
        appShellScreen.showPeer();
        clearPendingPeerInfo();
    }
  }

  private void applyContactsFound(RPCResponse response) {
    PeerDirectory found;
    int merged;
    String query = pendingDiscoveryQuery;

    clearPendingDiscoverySearch();
    if (readerParser == null || appShellModel == null) {
      return;
    }
    if (query.length() > 0 && !query.equals(appShellModel.discovery.query)) {
      return;
    }

    found = new PeerDirectory(DiscoveryStateModel.RESULT_CAPACITY * 2);
    if (!readerParser.mergeContactsFound(response, found)) {
      appShellModel.discovery.detail = "Telegram search parse failed: " + readerParser.lastError();
      appShellModel.activeDetail = appShellModel.discovery.detail;
      if (appShellScreen != null) {
        appShellScreen.showDiscovery();
      }
      return;
    }

    merged = mergePeerDirectory(found);
    appShellModel.discovery.search(appShellModel.reader);
    appShellModel.discovery.detail = appShellModel.discovery.count > 0
      ? "Merged Telegram matches for " + appShellModel.discovery.query + "."
      : "No Telegram matches for " + appShellModel.discovery.query + ".";
    appShellModel.activeDetail = appShellModel.discovery.detail +
      (merged > 0 ? " Cached " + Integer.toString(merged) + " peers." : "");
    persistWarmState();
    if (appShellScreen != null && appShellModel.router.currentScreen == AppShellModel.SCREEN_DISCOVERY) {
      appShellScreen.showDiscovery();
    }
  }

  private void applyPeerInfo(RPCResponse response) {
    ReaderResultParser.PeerInfoResult info = new ReaderResultParser.PeerInfoResult();
    String detail;
    int expectedPeerKind = pendingPeerInfoKind;
    long expectedPeerId = pendingPeerInfoId;

    clearPendingPeerInfo();
    if (readerParser == null || appShellModel == null || expectedPeerId == 0) {
      return;
    }
    if (!readerParser.parsePeerInfo(response, expectedPeerKind, expectedPeerId, info)) {
      appShellModel.notePeerError("Peer info parse failed: " + readerParser.lastError());
      if (appShellScreen != null && appShellModel.router.currentScreen == AppShellModel.SCREEN_PEER) {
        appShellScreen.showPeer();
      }
      return;
    }
    if (appShellModel.peer.peerKind != expectedPeerKind || appShellModel.peer.peerId != expectedPeerId) {
      return;
    }

    detail = info.participantCountKnown
      ? "Fetched Telegram peer details (" + Integer.toString(info.participantCount) + ")."
      : "Fetched Telegram peer details.";
    appShellModel.applyPeerInfo(info.aboutKnown ? info.about : "", info.participantCount, info.participantCountKnown, detail);
    persistWarmState();
    if (appShellScreen != null && appShellModel.router.currentScreen == AppShellModel.SCREEN_PEER) {
      appShellScreen.showPeer();
    }
  }

  private void applySecretChat(RPCResponse response) {
    SecretChatResultParser parser;
    SecretChatResultParser.Result result;

    if (appShellModel == null || appShellScreen == null || pendingPeerActionType != PEER_ACTION_SECRET) {
      return;
    }

    parser = new SecretChatResultParser();
    result = new SecretChatResultParser.Result();
    if (!parser.parse(response, result)) {
      appShellModel.notePeerError("Secret chat parse failed: " + parser.lastError());
      clearPendingPeerAction();
      appShellScreen.showPeer();
      return;
    }

    applySecretChatUpdate(result, pendingPeerActionKind, pendingPeerActionId, true);
    clearPendingPeerAction();
    appShellScreen.showPeer();
  }

  private int toReaderSecretState(int parserState) {
    if (parserState == SecretChatResultParser.STATE_WAITING) {
      return SecretChatDirectory.STATE_WAITING;
    }
    if (parserState == SecretChatResultParser.STATE_REQUESTED) {
      return SecretChatDirectory.STATE_REQUESTED;
    }
    if (parserState == SecretChatResultParser.STATE_CLOSED) {
      return SecretChatDirectory.STATE_CLOSED;
    }
    return SecretChatDirectory.STATE_READY;
  }

  private String secretPeerTitle(int peerKind, long peerId) {
    if (appShellModel == null) {
      return "";
    }
    return appShellModel.reader.peers.titleOrFallback(peerKind, peerId);
  }

  private String applySecretChatUpdate(
    SecretChatResultParser.Result result,
    int fallbackPeerKind,
    long fallbackPeerId,
    boolean fromPendingAction
  ) {
    SecretChatDirectory.Item existing;
    boolean activity;
    int peerKind = fallbackPeerKind != DialogListModel.PEER_UNKNOWN
      ? fallbackPeerKind
      : DialogListModel.PEER_USER;
    long peerId = fallbackPeerId;
    boolean outbound = fromPendingAction && pendingPeerActionType == PEER_ACTION_SECRET;
    int secretState;
    long accessHash;
    long keyFingerprint;
    int eventDate;
    byte[] pendingKeyData = null;
    int pendingKeyLength = 0;
    String detail;
    String title;

    if (appShellModel == null || result == null) {
      return "";
    }

    activity = result.state == SecretChatResultParser.STATE_ACTIVITY;
    existing = appShellModel.reader.secretChats.findByChatId(result.chatId);
    if (activity && existing == null) {
      return "";
    }
    secretState = activity
      ? existing.state
      : toReaderSecretState(result.state);
    if (existing != null) {
      peerKind = existing.peerKind;
      peerId = existing.peerId;
      outbound = existing.outbound;
    } else if (secretState == SecretChatDirectory.STATE_REQUESTED && result.adminId != 0) {
      peerKind = DialogListModel.PEER_USER;
      peerId = result.adminId;
      outbound = false;
    } else if (secretState == SecretChatDirectory.STATE_WAITING && result.participantId != 0) {
      peerKind = DialogListModel.PEER_USER;
      peerId = result.participantId;
      outbound = true;
    }

    if (peerId == 0 && result.participantId != 0) {
      peerKind = DialogListModel.PEER_USER;
      peerId = result.participantId;
    }
    if (peerId == 0 && result.adminId != 0) {
      peerKind = DialogListModel.PEER_USER;
      peerId = result.adminId;
    }
    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      return "";
    }

    accessHash = result.accessHash;
    if (accessHash == 0 && existing != null) {
      accessHash = existing.accessHash;
    }

    keyFingerprint = result.keyFingerprint;
    if (keyFingerprint == 0 && existing != null) {
      keyFingerprint = existing.keyFingerprint;
    }
    eventDate = result.date;
    if (eventDate == 0 && existing != null) {
      eventDate = existing.date;
    }

    if (activity) {
      if (existing != null && existing.pendingKeyData != null) {
        pendingKeyData = existing.pendingKeyData;
        pendingKeyLength = existing.pendingKeyData.length;
      }
      detail = result.service
        ? "Secret chat service activity was updated."
        : "Secret chat activity was updated.";
    } else if (secretState == SecretChatDirectory.STATE_WAITING && pendingSecretExponentValid) {
      pendingKeyData = pendingSecretExponent;
      pendingKeyLength = pendingSecretExponent.length;
      detail = "Secret chat request sent. Waiting for peer acceptance.";
    } else if (secretState == SecretChatDirectory.STATE_REQUESTED && result.exchangeLength > 0) {
      pendingKeyData = result.exchangeBytes;
      pendingKeyLength = result.exchangeLength;
      detail = "Inbound secret chat request is ready to accept.";
    } else if (secretState == SecretChatDirectory.STATE_READY) {
      if (keyFingerprint == 0 && pendingSecretExpectedKeyFingerprint != 0) {
        keyFingerprint = pendingSecretExpectedKeyFingerprint;
      }
      if (keyFingerprint == 0 &&
          existing != null &&
          existing.outbound &&
          existing.pendingKeyData != null &&
          result.exchangeLength > 0) {
        try {
          keyFingerprint = SecretChatBootstrap.fingerprintForSharedKey(
            SecretChatBootstrap.sharedKeyForPeerPublic(
              result.exchangeBytes,
              new BigInteger(1, existing.pendingKeyData)
            )
          );
        } catch (IllegalArgumentException e) {
          recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_WARN, -470, result.chatId);
        }
      }
      detail = keyFingerprint != 0
        ? "Secret chat is ready with a stored key fingerprint."
        : "Secret chat is ready locally.";
    } else if (secretState == SecretChatDirectory.STATE_CLOSED) {
      keyFingerprint = 0;
      detail = "Secret chat was discarded.";
    } else {
      detail = "Secret chat state was updated.";
    }

    appShellModel.reader.secretChats.upsert(
      result.chatId,
      peerKind,
      peerId,
      accessHash,
      secretState,
      outbound,
      eventDate,
      keyFingerprint,
      pendingKeyData,
      pendingKeyLength
    );
    title = secretPeerTitle(peerKind, peerId);
    if (appShellModel.peer.peerKind == peerKind && appShellModel.peer.peerId == peerId) {
      appShellModel.peer.applyExistingSecret(result.chatId, secretState, outbound, keyFingerprint);
      appShellModel.notePeerReady("SECRET", detail);
    } else {
      appShellModel.activeDetail = detail;
    }
    persistWarmState();
    return title;
  }

  private String applyLiveSecretUpdates(SecretChatResultParser.Result[] updates, int count) {
    String title = "";

    if (updates == null || count <= 0) {
      return title;
    }

    for (int i = 0; i < count; ++i) {
      String appliedTitle = applySecretChatUpdate(
        updates[i],
        DialogListModel.PEER_UNKNOWN,
        0,
        false
      );
      if (appliedTitle.length() > 0) {
        title = appliedTitle;
      }
    }
    return title;
  }

  private void applySecretDiscardResult(RPCResponse response) {
    SecretChatDirectory.Item existing;
    SecretChatResultParser.Result result;

    if (pendingReadPeerKind == DialogListModel.PEER_CHANNEL && pendingReadMaxId > 0) {
      applyReadHistoryBoolean(response);
      return;
    }
    if (appShellModel == null ||
        appShellScreen == null ||
        pendingPeerActionType != PEER_ACTION_SECRET_DISCARD) {
      return;
    }
    if (response == null || response.type != CombinatorIds.bool_true) {
      appShellModel.notePeerError("Telegram refused to discard the secret chat.");
      clearPendingPeerAction();
      appShellScreen.showPeer();
      return;
    }

    existing = appShellModel.reader.secretChats.findByPeer(pendingPeerActionKind, pendingPeerActionId);
    if (existing == null || existing.chatId == 0) {
      appShellModel.notePeerError("No tracked secret chat was available to discard.");
      clearPendingPeerAction();
      appShellScreen.showPeer();
      return;
    }

    result = new SecretChatResultParser.Result();
    result.state = SecretChatResultParser.STATE_CLOSED;
    result.chatId = existing.chatId;
    result.accessHash = existing.accessHash;
    result.date = (int)(System.currentTimeMillis() / 1000L);
    applySecretChatUpdate(result, existing.peerKind, existing.peerId, false);
    clearPendingPeerAction();
    appShellScreen.showPeer();
  }

  private void applyDeleteMessages(RPCResponse response) {
    RecieveAffectedMessages affected;
    int peerKind = pendingDeletePeerKind;
    long peerId = pendingDeletePeerId;
    int messageId = pendingDeleteMessageId;
    boolean ptsContinuous = true;
    boolean removed;
    String title;

    if (pendingReadPeerKind != DialogListModel.PEER_UNKNOWN && pendingReadMaxId > 0) {
      applyReadHistoryAffected(response);
      return;
    }
    if (appShellModel == null || appShellScreen == null || messageId == 0) {
      return;
    }

    try {
      affected = RecieveAffectedMessages.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      clearPendingDeleteState();
      appShellModel.activeDetail = "Delete parse failed: " + e.getMessage();
      appShellScreen.showHistory();
      return;
    }

    removed = appShellModel.reader.deleteMessageForPeer(peerKind, peerId, messageId);
    clearPendingDeleteState();
    ensureSyncState();
    if (peerKind == DialogListModel.PEER_CHANNEL) {
      ptsContinuous = appShellModel.reader.channelSync.noteAcknowledgement(
        peerId,
        affected.pts,
        affected.ptsCount
      );
    } else if (affected.pts != 0 || affected.ptsCount != 0) {
      ptsContinuous = syncState.notePtsAcknowledgement(
        affected.pts,
        affected.ptsCount,
        syncState.date
      );
    }
    title = appShellModel.reader.peers.titleOrFallback(peerKind, peerId);
    appShellModel.updateReaderSummary();
    appShellModel.recomputeWorkingSet();
    appShellModel.activeDetail = !ptsContinuous
      ? "Deleted message from " + title + "; catching up an update gap."
      : (removed
          ? "Deleted message from " + title + "."
          : "Telegram deleted the message; local cache was already clear.");
    syncShellRuntimeState();
    persistWarmState();
    appShellScreen.showHistory();
    if (!ptsContinuous) {
      if (peerKind == DialogListModel.PEER_CHANNEL) {
        requestChannelDifferenceFor(
          findDialog(peerKind, peerId),
          "Catching up channel updates after deleting a message..."
        );
      } else {
        requestUpdatesDifference("Catching up Telegram updates after deleting a message...");
      }
    } else {
      requestVisibleHistoryRead();
    }
  }

  private void applyReadHistoryAffected(RPCResponse response) {
    RecieveAffectedMessages affected;

    try {
      affected = RecieveAffectedMessages.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      rollbackPendingRead("Read acknowledgement parse failed: " + e.getMessage());
      return;
    }
    finishPendingRead(affected.pts, affected.ptsCount);
  }

  private void applyReadHistoryBoolean(RPCResponse response) {
    if (response == null || response.type != CombinatorIds.bool_true) {
      rollbackPendingRead("Telegram did not acknowledge the read request.");
      return;
    }
    finishPendingRead(0, 0);
  }

  private void finishPendingRead(int pts, int ptsCount) {
    int peerKind = pendingReadPeerKind;
    long peerId = pendingReadPeerId;
    int maxId = pendingReadMaxId;
    boolean ptsContinuous = true;
    String title;

    if (appShellModel == null || peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || maxId <= 0) {
      clearPendingReadState();
      return;
    }

    appShellModel.reader.applyLiveInboxRead(peerKind, peerId, maxId, 0);
    title = appShellModel.reader.peers.titleOrFallback(peerKind, peerId);
    clearPendingReadState();
    ensureSyncState();
    if (pts != 0 || ptsCount != 0) {
      ptsContinuous = syncState.notePtsAcknowledgement(pts, ptsCount, syncState.date);
      noteChannelLiveUpdate(peerKind, peerId, pts);
    }
    appShellModel.updateReaderSummary();
    appShellModel.activeDetail = ptsContinuous
      ? "Marked visible history read for " + title + "."
      : "Marked history read for " + title + "; catching up an update gap.";
    syncShellRuntimeState();
    persistWarmState();
    System.out.println("HISTORY READ: " + title + " #" + Integer.toString(maxId));
    if (appShellScreen != null) {
      appShellScreen.syncVisibleState();
      appShellScreen.showCurrent();
    }
    resumeAfterPendingRead(true);
  }

  private void rollbackPendingRead(String detail) {
    DialogListModel.Item dialog = findDialog(pendingReadPeerKind, pendingReadPeerId);

    if (dialog != null) {
      if (dialog.readInboxMaxId <= pendingReadMaxId) {
        dialog.readInboxMaxId = pendingReadPreviousInboxMaxId;
        dialog.unreadCount = pendingReadPreviousUnreadCount;
      }
    }
    clearPendingReadState();
    if (appShellModel != null) {
      appShellModel.updateReaderSummary();
      appShellModel.activeDetail = detail != null ? detail : "Unable to mark visible history read.";
      persistWarmState();
    }
    if (appShellScreen != null) {
      appShellScreen.syncVisibleState();
      appShellScreen.showCurrent();
    }
    resumeAfterPendingRead(false);
  }

  private void resumeAfterPendingRead(boolean retryVisibleRead) {
    DialogListModel.Item selected;
    ChannelSyncState.Entry channelState;

    if (connection == null || appShellModel == null || !appShellModel.authorized) {
      return;
    }
    if (syncState != null && syncState.needsDifference) {
      requestUpdatesDifference("Catching up Telegram updates after marking history read...");
      return;
    }

    selected = selectedDialogForPeer(
      appShellModel.reader.historyPeerKind,
      appShellModel.reader.historyPeerId
    );
    if (selected != null && selected.peerKind == DialogListModel.PEER_CHANNEL) {
      channelState = appShellModel.reader.channelSync.find(selected.peerId);
      if (channelState != null && channelState.needsDifference) {
        requestChannelDifferenceFor(selected, "Continuing channel catch-up after marking history read...");
        return;
      }
    }

    if (retryVisibleRead) {
      requestVisibleHistoryRead();
      if (readRequestInFlight()) {
        return;
      }
    }
    maybeStartQueuedMediaDownload();
  }

  private void applyDialogs(RPCResponse response) {
    if (readerParser == null || appShellModel == null) {
      return;
    }
    if (!readerParser.applyDialogs(response, appShellModel.reader)) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, -401, response.type);
      System.out.println("DIALOGS FAIL: " + readerParser.lastError());
      appShellModel.onReaderFailure("Dialog parse failed: " + readerParser.lastError());
      appShellScreen.showCurrent();
      return;
    }
    if (response.type == CombinatorIds.messages_dialogs_not_modified) {
      appShellModel.finishDialogsUnchanged();
      System.out.println("DIALOGS READY: unchanged");
    } else {
      appShellModel.finishDialogsLoad();
      System.out.println("DIALOGS READY: count=" + Integer.toString(appShellModel.reader.dialogs.count));
    }
    restoreSelectedDialogAfterRefresh();
    persistWarmState();
    if (refreshSelectedHistoryAfterDialogs) {
      DialogListModel.Item selected = appShellModel.selectedDialog();

      refreshSelectedHistoryAfterDialogs = false;
      if (selected != null) {
        requestHistoryFor(selected);
        return;
      }
    }
    if (maybeResumeInterruptedOutbound()) {
      if (pendingRouteAfterDialogs != 0) {
        appShellModel.router.init(pendingRouteAfterDialogs);
      }
      pendingRouteAfterDialogs = 0;
      appShellScreen.showCurrent();
      return;
    }
    if (pendingRouteAfterDialogs != 0) {
      appShellModel.router.init(pendingRouteAfterDialogs);
      pendingRouteAfterDialogs = 0;
      appShellScreen.showCurrent();
      return;
    }
    appShellScreen.showDialogs();
  }

  private void applyHistory(RPCResponse response) {
    int routeAfterHistory = pendingRouteAfterDialogs;

    if (readerParser == null || appShellModel == null) {
      return;
    }
    if (!readerParser.applyHistory(response, appShellModel.reader, pendingHistoryPeerKind, pendingHistoryPeerId)) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, -402, response.type);
      System.out.println("HISTORY FAIL: " + readerParser.lastError());
      clearPendingHistoryState();
      appShellModel.onReaderFailure("History parse failed: " + readerParser.lastError());
      appShellScreen.showCurrent();
      maybeResumeInterruptedOutbound();
      return;
    }
    clearPendingHistoryState();
    if (response.type == CombinatorIds.messages_messages_not_modified) {
      appShellModel.finishHistoryUnchanged();
      System.out.println("HISTORY READY: unchanged");
    } else {
      appShellModel.finishHistoryLoad();
      System.out.println("HISTORY READY: count=" + Integer.toString(appShellModel.reader.history.count));
    }
    persistWarmState();
    pendingRouteAfterDialogs = 0;
    appShellScreen.showHistory();
    if (routeAfterHistory != 0 && routeAfterHistory != AppShellModel.SCREEN_HISTORY) {
      appShellModel.router.init(routeAfterHistory);
      appShellScreen.showCurrent();
    }
    if (maybeResumeInterruptedOutbound()) {
      return;
    }
    if (appShellModel.reader.historyPeerKind == DialogListModel.PEER_CHANNEL &&
        appShellModel.reader.historyPeerId != 0) {
      requestChannelDifferenceFor(
        selectedDialogForPeer(appShellModel.reader.historyPeerKind, appShellModel.reader.historyPeerId),
        "Syncing selected channel..."
      );
    } else {
      requestVisibleHistoryRead();
    }
  }

  private void applyShortSentMessage(RPCResponse response) {
    RecieveUpdateShortSentMessage sent;

    if (appShellModel == null) {
      return;
    }
    if (pendingSendPeerKind == DialogListModel.PEER_UNKNOWN || pendingSendPeerId == 0) {
      return;
    }

    try {
      sent = RecieveUpdateShortSentMessage.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, -403, response.type);
      appShellModel.onComposeFailed(0, e.getMessage());
      appShellScreen.showCurrent();
      clearPendingSendState();
      return;
    }

    ensureSyncState();
    syncState.noteShortSent(sent);
    syncShellRuntimeState();

    if (!appShellModel.reader.markLatestPendingOutboundSent(
          pendingSendPeerKind,
          pendingSendPeerId,
          sent.messageId,
          sent.date)) {
      appShellModel.reader.appendOutboundText(
        pendingSendPeerKind,
        pendingSendPeerId,
        "YOU",
        appShellModel.compose.text,
        sent.date
      );
    }
    appShellModel.onComposeSent(sent.messageId);
    persistWarmState();
    resumeOutboundAfterDialogs = false;
    outboundResumeSyncReady = false;
    appShellScreen.showCurrent();
    clearPendingSendState();
    if (syncState.needsDifference) {
      requestUpdatesDifference("Catching up Telegram updates after sending a message...");
    } else {
      requestVisibleHistoryRead();
    }
  }

  private boolean applyPendingOutboundMessageId(ReaderResultParser.LiveMessageEvent event) {
    boolean runtimeSendPending;

    if (event == null || event.kind != ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
      return false;
    }

    runtimeSendPending = pendingSendPeerKind != DialogListModel.PEER_UNKNOWN &&
      pendingSendPeerId != 0 && pendingSendRandomId != 0;
    if (runtimeSendPending) {
      if (appShellModel == null ||
          pendingSendPeerKind != appShellModel.compose.peerKind ||
          pendingSendPeerId != appShellModel.compose.peerId ||
          event.randomId != pendingSendRandomId) {
        return false;
      }
    } else if (!resumeOutboundAfterDialogs) {
      return false;
    }

    if (appShellModel == null ||
        !appShellModel.reconcileOutboundMessageId(event.randomId, event.messageId)) {
      return false;
    }
    persistWarmState();
    resumeOutboundAfterDialogs = false;
    outboundResumeSyncReady = false;
    clearPendingSendState();
    return true;
  }

  private boolean shouldRefreshSelectedHistoryAfterSync() {
    int screen;

    if (appShellModel == null || appShellModel.selectedDialog() == null) {
      return false;
    }

    screen = appShellModel.router.currentScreen;
    return screen == AppShellModel.SCREEN_HISTORY;
  }

  private boolean isViewingPeer(int peerKind, long peerId) {
    DialogListModel.Item selected;
    int screen;

    if (appShellModel == null) {
      return false;
    }

    selected = appShellModel.selectedDialog();
    if (selected == null ||
        selected.peerKind != peerKind ||
        selected.peerId != peerId) {
      return false;
    }

    screen = appShellModel.router.currentScreen;
    return screen == AppShellModel.SCREEN_HISTORY ||
      screen == AppShellModel.SCREEN_COMPOSE;
  }

  private DialogListModel.Item selectedDialogForPeer(int peerKind, long peerId) {
    DialogListModel.Item selected;

    if (appShellModel == null) {
      return null;
    }

    selected = appShellModel.selectedDialog();
    if (selected == null ||
        selected.peerKind != peerKind ||
        selected.peerId != peerId) {
      return null;
    }
    return selected;
  }

  private void noteChannelLiveUpdate(int peerKind, long peerId, int pts) {
    if (appShellModel == null || peerKind != DialogListModel.PEER_CHANNEL || peerId == 0 || pts <= 0) {
      return;
    }
    appShellModel.reader.channelSync.noteLiveUpdate(peerId, pts);
  }

  private void scheduleReaderRefreshFromSync(String detail) {
    if (connection == null || appShellModel == null || !appShellModel.authorized) {
      return;
    }

    refreshSelectedHistoryAfterDialogs =
      refreshSelectedHistoryAfterDialogs || shouldRefreshSelectedHistoryAfterSync();
    if (pendingRouteAfterDialogs == 0) {
      pendingRouteAfterDialogs = appShellModel.router.currentScreen;
    }
    if (detail != null && detail.length() > 0) {
      appShellModel.activeDetail = detail;
    }
    if (!appShellModel.reader.loadingDialogs) {
      requestDialogs();
    }
  }

  private void requestUpdatesState() {
    if (connection == null || appShellModel == null || !appShellModel.authorized) {
      return;
    }
    if (readRequestInFlight() || syncStateRequestInFlight) {
      return;
    }

    ensureSyncState();
    if (resumeOutboundAfterDialogs) {
      outboundResumeSyncReady = false;
    }
    syncStateRequestInFlight = true;
    syncShellRuntimeState();
    (new SendGetState()).send(connection);
  }

  private void requestAuthorizedRecoverySync() {
    ensureSyncState();
    if (OutboundRecoveryState.shouldRequestDifferenceBeforeResume(
        resumeOutboundAfterDialogs,
        syncState.hasState)) {
      syncState.markNeedsDifference();
      syncShellRuntimeState();
      requestUpdatesDifference("Reconciling interrupted outbound acknowledgement...");
      return;
    }
    requestUpdatesState();
  }

  private void requestUpdatesDifference(String reason) {
    SyncState.DifferenceArgs args;

    if (connection == null || appShellModel == null || !appShellModel.authorized) {
      return;
    }
    if (readRequestInFlight() || differenceRequestInFlight || syncStateRequestInFlight) {
      return;
    }

    ensureSyncState();
    if (!syncState.canBuildDifferenceQuery()) {
      requestUpdatesState();
      return;
    }

    args = syncState.buildDifferenceArgs();
    if (resumeOutboundAfterDialogs) {
      outboundResumeSyncReady = false;
    }
    differenceRequestInFlight = true;
    if (reason != null && reason.length() > 0) {
      appShellModel.activeDetail = reason;
    }
    syncShellRuntimeState();
    (new SendGetDifference(args)).send(connection);
  }

  private void requestChannelDifferenceFor(DialogListModel.Item selected, String reason) {
    ChannelSyncState.DifferenceArgs args;
    PeerDirectory.Item peer;
    long accessHash;

    if (connection == null || appShellModel == null || selected == null) {
      return;
    }
    if (selected.peerKind != DialogListModel.PEER_CHANNEL || selected.peerId == 0) {
      return;
    }
    if (readRequestInFlight() || channelDifferenceRequestInFlight) {
      return;
    }

    accessHash = selected.hasAccessHash ? selected.accessHash : 0;
    if (accessHash == 0) {
      peer = appShellModel.reader.peers.find(DialogListModel.PEER_CHANNEL, selected.peerId);
      if (peer != null && peer.hasAccessHash) {
        accessHash = peer.accessHash;
      }
    }
    if (accessHash == 0) {
      return;
    }

    args = appShellModel.reader.channelSync.buildDifferenceArgs(
      selected.peerId,
      appShellModel.reader.history.capacity()
    );
    channelDifferenceRequestInFlight = true;
    pendingChannelDifferencePeerId = selected.peerId;
    if (reason != null && reason.length() > 0) {
      appShellModel.activeDetail = reason;
    }
    (new SendGetChannelDifference(selected.peerId, accessHash, args)).send(connection);
  }

  private void applyUpdatesState(RPCResponse response) {
    RecieveUpdatesState state;

    syncStateRequestInFlight = false;
    ensureSyncState();

    try {
      state = RecieveUpdatesState.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_WARN, -404, response != null ? response.type : 0);
      if (appShellModel != null) {
        appShellModel.activeDetail = "Updates state parse failed: " + e.getMessage();
      }
      syncShellRuntimeState();
      return;
    }

    syncState.applyUpdatesState(state);
    System.out.println("SYNC READY: pts=" + Integer.toString(syncState.pts) +
      " qts=" + Integer.toString(syncState.qts) +
      " seq=" + Integer.toString(syncState.seq));
    syncShellRuntimeState();
    if (syncState.needsDifference) {
      requestUpdatesDifference("Applying queued updates catch-up...");
      return;
    }
    outboundResumeSyncReady = true;
    maybeResumeInterruptedOutbound();
  }

  private boolean mergeLiveBatchPeers(ReaderResultParser.LiveUpdateBatch batch) {
    PeerDirectory.Item peer;
    int i;

    if (batch == null || appShellModel == null) {
      return false;
    }

    for (i = 0; i < batch.peers.count; ++i) {
      peer = batch.peers.items[i];
      if (!appShellModel.reader.peers.upsertIdentity(
          peer.peerKind,
          peer.peerId,
          peer.accessHash,
          peer.hasAccessHash,
          peer.minAccessHash,
          peer.title,
          peer.username)) {
        return false;
      }
    }
    return true;
  }

  private boolean tryApplyDifferenceBatch(RPCResponse response) {
    ReaderResultParser.LiveUpdateBatch batch;
    ReaderResultParser.LiveMessageEvent event;
    String title;
    boolean anyApplied;
    boolean applied;
    int unreadDelta;
    int pass;
    int i;

    if (response == null || response.body == null ||
        readerParser == null || appShellModel == null) {
      return false;
    }

    batch = new ReaderResultParser.LiveUpdateBatch(
      appShellModel.reader.dialogs.capacity() + appShellModel.reader.historyCapacityPerSlot(),
      PeerDirectory.DEFAULT_CAPACITY
    );
    if (!readerParser.parseDifferenceResult(response.type, response.body, batch)) {
      return false;
    }
    if (!mergeLiveBatchPeers(batch)) {
      return false;
    }

    title = "";
    anyApplied = false;
    title = applyLiveSecretUpdates(batch.secretEvents, batch.secretCount);
    if (title.length() > 0) {
      anyApplied = true;
    }
    for (pass = 0; pass < 2; ++pass) {
      for (i = 0; i < batch.count; ++i) {
        event = batch.events[i];
        if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
          if (pass != 0) {
            continue;
          }
          applied = applyPendingOutboundMessageId(event);
          if (applied) {
            anyApplied = true;
            title = appShellModel.compose.peerTitle;
          }
          continue;
        }
        if (pass == 0) {
          if (event.kind != ReaderResultParser.LiveMessageEvent.KIND_DELETE &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_READ &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
            continue;
          }
        } else {
          if (event.kind != ReaderResultParser.LiveMessageEvent.KIND_APPEND &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
            continue;
          }
        }

        applied = false;
        if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_DELETE) {
          applied = event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0
            ? appShellModel.reader.deleteMessageForPeer(event.peerKind, event.peerId, event.messageId)
            : appShellModel.reader.deleteMessageGlobally(event.messageId) > 0;
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ) {
          applied = appShellModel.reader.applyLiveInboxRead(
            event.peerKind,
            event.peerId,
            event.messageId,
            isViewingPeer(event.peerKind, event.peerId) ? 0 : event.unreadCount
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
          applied = appShellModel.reader.applyLiveOutboxRead(
            event.peerKind,
            event.peerId,
            event.messageId
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_APPEND) {
          unreadDelta = (!event.outgoing && !isViewingPeer(event.peerKind, event.peerId)) ? 1 : 0;
          applied = appShellModel.reader.applyLiveTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            event.date,
            unreadDelta
          );
          if (!applied) {
            return false;
          }
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
          applied = appShellModel.reader.applyLiveEditedTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            event.date
          );
          if (!applied) {
            return false;
          }
        }

        if (applied) {
          anyApplied = true;
          noteChannelLiveUpdate(event.peerKind, event.peerId, event.pts);
          if (event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0) {
            title = appShellModel.reader.peers.titleOrFallback(event.peerKind, event.peerId);
          }
        }
      }
    }

    appShellModel.updateReaderSummary();
    appShellModel.recomputeWorkingSet();
    appShellModel.activeDetail = anyApplied
      ? (title.length() > 0
          ? "Applied Telegram catch-up for " + title + "."
          : "Applied Telegram catch-up to the bounded reader cache.")
      : "Applied Telegram catch-up state to the bounded reader cache.";
    return true;
  }

  private void applyUpdatesDifference(RPCResponse response) {
    RecieveUpdatesDifference difference;
    boolean appliedDirectDifference = false;

    differenceRequestInFlight = false;
    ensureSyncState();

    try {
      difference = RecieveUpdatesDifference.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_WARN, -405, response != null ? response.type : 0);
      if (appShellModel != null) {
        appShellModel.activeDetail = "Updates difference parse failed: " + e.getMessage();
      }
      syncShellRuntimeState();
      return;
    }

    if (difference.type == RecieveUpdatesDifference.TYPE_FULL ||
        difference.type == RecieveUpdatesDifference.TYPE_SLICE) {
      appliedDirectDifference = tryApplyDifferenceBatch(response);
    }

    syncState.noteDifferenceResult(difference);
    syncShellRuntimeState();
    if (difference.type == RecieveUpdatesDifference.TYPE_EMPTY) {
      System.out.println("SYNC DIFF: empty");
      if (appShellModel != null) {
        appShellModel.activeDetail = "Updates are in sync.";
      }
      outboundResumeSyncReady = true;
      if (maybeResumeInterruptedOutbound()) {
        return;
      }
      requestVisibleHistoryRead();
      return;
    }

    if (difference.type == RecieveUpdatesDifference.TYPE_TOO_LONG) {
      outboundResumeSyncReady = false;
      System.out.println("SYNC DIFF: too long pts=" + Integer.toString(difference.pts));
      scheduleReaderRefreshFromSync("Telegram reported updatesTooLong; refreshing reader.");
      requestUpdatesState();
      return;
    }

    if (appliedDirectDifference) {
      System.out.println("SYNC DIFF: applied " +
        (difference.type == RecieveUpdatesDifference.TYPE_SLICE ? "slice" : "full") +
        " pts=" + Integer.toString(difference.pts));
      persistWarmState();
      if (appShellScreen != null) {
        appShellScreen.syncVisibleState();
        appShellScreen.showCurrent();
      }
      if (difference.type == RecieveUpdatesDifference.TYPE_SLICE) {
        requestUpdatesDifference("Applying next Telegram catch-up slice...");
      } else {
        outboundResumeSyncReady = true;
        if (maybeResumeInterruptedOutbound()) {
          return;
        }
        requestVisibleHistoryRead();
      }
      return;
    }

    System.out.println("SYNC DIFF: " +
      (difference.type == RecieveUpdatesDifference.TYPE_SLICE ? "slice" : "full") +
      " pts=" + Integer.toString(difference.pts));
    outboundResumeSyncReady = false;
    scheduleReaderRefreshFromSync("Applying Telegram catch-up to reader.");
    requestUpdatesState();
  }

  private void applyChannelDifference(RPCResponse response) {
    ReaderResultParser.ChannelDifferenceBatch difference;
    ReaderResultParser.LiveMessageEvent event;
    DialogListModel.Item selected;
    String title;
    boolean anyApplied;
    boolean applied;
    int unreadDelta;
    int pass;
    int i;
    long peerId = pendingChannelDifferencePeerId;

    channelDifferenceRequestInFlight = false;
    pendingChannelDifferencePeerId = 0;
    if (response == null || response.body == null ||
        readerParser == null || appShellModel == null || peerId == 0) {
      return;
    }

    difference = new ReaderResultParser.ChannelDifferenceBatch(
      appShellModel.reader.dialogs.capacity() + appShellModel.reader.historyCapacityPerSlot(),
      PeerDirectory.DEFAULT_CAPACITY
    );
    if (!readerParser.parseChannelDifference(response.type, response.body, peerId, difference)) {
      recordFailure(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_WARN, -406, response.type);
      selected = selectedDialogForPeer(DialogListModel.PEER_CHANNEL, peerId);
      if (selected != null) {
        appShellModel.activeDetail = "Channel catch-up parse failed; refreshing history.";
        requestHistoryFor(selected);
      }
      return;
    }
    if (!mergeLiveBatchPeers(difference.updates)) {
      return;
    }
    appShellModel.reader.channelSync.noteDifference(
      difference.peerId,
      difference.pts,
      difference.timeout,
      difference.isFinal
    );

    if (difference.type == ReaderResultParser.ChannelDifferenceBatch.TYPE_EMPTY) {
      title = appShellModel.reader.peers.titleOrFallback(DialogListModel.PEER_CHANNEL, difference.peerId);
      appShellModel.activeDetail = "Channel sync is current for " + title + ".";
      System.out.println("CHANNEL DIFF: empty pts=" + Integer.toString(difference.pts));
      if (maybeResumeInterruptedOutbound()) {
        return;
      }
      requestVisibleHistoryRead();
      return;
    }

    if (difference.type == ReaderResultParser.ChannelDifferenceBatch.TYPE_TOO_LONG) {
      selected = selectedDialogForPeer(DialogListModel.PEER_CHANNEL, difference.peerId);
      title = appShellModel.reader.peers.titleOrFallback(DialogListModel.PEER_CHANNEL, difference.peerId);
      System.out.println("CHANNEL DIFF: too long pts=" + Integer.toString(difference.pts));
      if (selected != null) {
        appShellModel.activeDetail = "Refreshing bounded history for " + title + ".";
        requestHistoryFor(selected);
      } else {
        appShellModel.activeDetail = "Channel catch-up grew too large for direct apply.";
      }
      return;
    }

    title = "";
    anyApplied = false;
    for (pass = 0; pass < 2; ++pass) {
      for (i = 0; i < difference.updates.count; ++i) {
        event = difference.updates.events[i];
        if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
          if (pass != 0) {
            continue;
          }
          applied = applyPendingOutboundMessageId(event);
          if (applied) {
            anyApplied = true;
            title = appShellModel.compose.peerTitle;
          }
          continue;
        }
        if (pass == 0) {
          if (event.kind != ReaderResultParser.LiveMessageEvent.KIND_DELETE &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_READ &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
            continue;
          }
        } else {
          if (event.kind != ReaderResultParser.LiveMessageEvent.KIND_APPEND &&
              event.kind != ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
            continue;
          }
        }

        applied = false;
        if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_DELETE) {
          applied = event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0
            ? appShellModel.reader.deleteMessageForPeer(event.peerKind, event.peerId, event.messageId)
            : appShellModel.reader.deleteMessageGlobally(event.messageId) > 0;
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ) {
          applied = appShellModel.reader.applyLiveInboxRead(
            event.peerKind,
            event.peerId,
            event.messageId,
            isViewingPeer(event.peerKind, event.peerId) ? 0 : event.unreadCount
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
          applied = appShellModel.reader.applyLiveOutboxRead(
            event.peerKind,
            event.peerId,
            event.messageId
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_APPEND) {
          unreadDelta = (!event.outgoing && !isViewingPeer(event.peerKind, event.peerId)) ? 1 : 0;
          applied = appShellModel.reader.applyLiveTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            event.date,
            unreadDelta
          );
          if (!applied) {
            selected = selectedDialogForPeer(DialogListModel.PEER_CHANNEL, difference.peerId);
            if (selected != null) {
              requestHistoryFor(selected);
            }
            return;
          }
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
          applied = appShellModel.reader.applyLiveEditedTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            event.date
          );
          if (!applied) {
            selected = selectedDialogForPeer(DialogListModel.PEER_CHANNEL, difference.peerId);
            if (selected != null) {
              requestHistoryFor(selected);
            }
            return;
          }
        }

        if (applied) {
          anyApplied = true;
          noteChannelLiveUpdate(event.peerKind, event.peerId, difference.pts);
          if (event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0) {
            title = appShellModel.reader.peers.titleOrFallback(event.peerKind, event.peerId);
          }
        }
      }
    }

    if (title.length() == 0) {
      title = appShellModel.reader.peers.titleOrFallback(DialogListModel.PEER_CHANNEL, difference.peerId);
    }
    appShellModel.updateReaderSummary();
    appShellModel.recomputeWorkingSet();
    appShellModel.activeDetail = anyApplied
      ? "Applied channel catch-up for " + title + "."
      : "Channel catch-up state updated for " + title + ".";
    System.out.println("CHANNEL DIFF: applied pts=" + Integer.toString(difference.pts) +
      " final=" + (difference.isFinal ? "1" : "0"));
    persistWarmState();
    if (appShellScreen != null) {
      appShellScreen.syncVisibleState();
      appShellScreen.showCurrent();
    }
    if (!difference.isFinal) {
      requestChannelDifferenceFor(
        selectedDialogForPeer(DialogListModel.PEER_CHANNEL, difference.peerId),
        "Applying next channel catch-up slice..."
      );
    } else {
      if (maybeResumeInterruptedOutbound()) {
        return;
      }
      requestVisibleHistoryRead();
    }
  }

  private boolean tryApplyDirectShortUpdate(Response response) {
    EncryptedResponse encrypted;
    RecieveShortTextUpdate update;
    ReaderResultParser.LiveUpdateBatch batch;
    ReaderResultParser.LiveMessageEvent wrapped;
    ReaderResultParser.LiveMessageEvent event;
    SecretChatResultParser.Result secretWrapped;
    PeerDirectory.Item peer;
    String title;
    String secretTitle;
    int unreadDelta;
    int eventDate;
    int lastPts;
    int lastDate;
    int i;
    boolean applied;
    boolean anyApplied;

    if (!(response instanceof EncryptedResponse) || appShellModel == null) {
      return false;
    }

    encrypted = (EncryptedResponse)response;
    if (response.type == CombinatorIds.update_short_message ||
        response.type == CombinatorIds.update_short_chat_message) {
      try {
        update = RecieveShortTextUpdate.from_encrypted_message(encrypted);
      } catch (IllegalArgumentException e) {
        return false;
      }

      unreadDelta = (!update.outgoing && !isViewingPeer(update.peerKind, update.peerId)) ? 1 : 0;
      applied = appShellModel.reader.applyLiveTextUpdate(
        update.peerKind,
        update.peerId,
        update.senderPeerKind,
        update.senderPeerId,
        update.messageId,
        update.text,
        update.outgoing,
        false,
        "",
        "",
        null,
        update.date,
        unreadDelta
      );
      if (!applied) {
        return false;
      }

      ensureSyncState();
      syncState.noteShortUpdate(update.pts, update.date);
      noteChannelLiveUpdate(update.peerKind, update.peerId, update.pts);
      title = appShellModel.reader.peers.titleOrFallback(update.peerKind, update.peerId);
      appShellModel.updateReaderSummary();
      appShellModel.recomputeWorkingSet();
      appShellModel.activeDetail = update.outgoing
        ? "Live send delivered to " + title + "."
        : "Live message from " + title + ".";
      syncShellRuntimeState();
      persistWarmState();
      System.out.println("UPDATE LIVE: " + title + " #" + Integer.toString(update.messageId));
      if (appShellScreen != null) {
        appShellScreen.syncVisibleState();
        appShellScreen.showCurrent();
      }
      requestVisibleHistoryRead();
      return true;
    }

    if ((response.type == CombinatorIds.updates ||
         response.type == CombinatorIds.updates_combined) &&
        readerParser != null) {
      batch = new ReaderResultParser.LiveUpdateBatch(
        appShellModel.reader.dialogs.capacity() + appShellModel.reader.historyCapacityPerSlot(),
        PeerDirectory.DEFAULT_CAPACITY
      );
      if (!readerParser.parseUpdateCollection(response.type, encrypted.data, batch)) {
        return false;
      }
      if (!mergeLiveBatchPeers(batch)) {
        return false;
      }

      anyApplied = false;
      title = "";
      secretTitle = applyLiveSecretUpdates(batch.secretEvents, batch.secretCount);
      if (secretTitle.length() > 0) {
        anyApplied = true;
        title = secretTitle;
      }
      lastPts = 0;
      lastDate = batch.date;
      for (i = 0; i < batch.count; ++i) {
        event = batch.events[i];
        if (event.kind != ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
          continue;
        }
        if (applyPendingOutboundMessageId(event)) {
          anyApplied = true;
          title = appShellModel.compose.peerTitle;
        }
      }
      for (i = 0; i < batch.count; ++i) {
        event = batch.events[i];
        eventDate = event.date != 0 ? event.date : batch.date;
        applied = false;

        if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
          continue;
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_APPEND) {
          unreadDelta = (!event.outgoing && !isViewingPeer(event.peerKind, event.peerId)) ? 1 : 0;
          applied = appShellModel.reader.applyLiveTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            eventDate,
            unreadDelta
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
          applied = appShellModel.reader.applyLiveEditedTextUpdate(
            event.peerKind,
            event.peerId,
            event.senderPeerKind,
            event.senderPeerId,
            event.messageId,
            event.text,
            event.outgoing,
            event.hasMedia,
            event.mediaLabel,
            event.mediaInfo,
            event.mediaRef,
            eventDate
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_DELETE) {
          applied = event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0
            ? appShellModel.reader.deleteMessageForPeer(event.peerKind, event.peerId, event.messageId)
            : appShellModel.reader.deleteMessageGlobally(event.messageId) > 0;
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ) {
          applied = appShellModel.reader.applyLiveInboxRead(
            event.peerKind,
            event.peerId,
            event.messageId,
            isViewingPeer(event.peerKind, event.peerId) ? 0 : event.unreadCount
          );
        } else if (event.kind == ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
          applied = appShellModel.reader.applyLiveOutboxRead(
            event.peerKind,
            event.peerId,
            event.messageId
          );
        }

        if (applied) {
          anyApplied = true;
          noteChannelLiveUpdate(event.peerKind, event.peerId, event.pts);
          if (event.pts != 0) {
            lastPts = event.pts;
          }
          if (eventDate != 0) {
            lastDate = eventDate;
          }
          if (event.peerKind != DialogListModel.PEER_UNKNOWN && event.peerId != 0) {
            title = appShellModel.reader.peers.titleOrFallback(event.peerKind, event.peerId);
          }
        }
      }

      if (!anyApplied) {
        return false;
      }

      ensureSyncState();
      syncState.noteShortUpdate(lastPts != 0 ? lastPts : syncState.pts, lastDate);
      appShellModel.updateReaderSummary();
      appShellModel.recomputeWorkingSet();
      appShellModel.activeDetail = title.length() > 0
        ? "Applied live updates for " + title + "."
        : "Applied live updates to the bounded reader cache.";
      syncShellRuntimeState();
      persistWarmState();
      System.out.println("UPDATE LIVE: vector " + Integer.toString(batch.count));
      if (appShellScreen != null) {
        appShellScreen.syncVisibleState();
        appShellScreen.showCurrent();
      }
      requestVisibleHistoryRead();
      return true;
    }

    if (response.type != CombinatorIds.update_short || readerParser == null) {
      return false;
    }

    secretWrapped = new SecretChatResultParser.Result();
    if (readerParser.parseShortSecretUpdate(response.type, encrypted.data, secretWrapped)) {
      title = applySecretChatUpdate(secretWrapped, DialogListModel.PEER_UNKNOWN, 0, false);
      if (title.length() == 0) {
        return false;
      }
      appShellModel.updateReaderSummary();
      appShellModel.recomputeWorkingSet();
      appShellModel.activeDetail = "Applied live secret update for " + title + ".";
      syncShellRuntimeState();
      persistWarmState();
      System.out.println("UPDATE SECRET: " + title + " #" + Integer.toString(secretWrapped.chatId));
      if (appShellScreen != null) {
        appShellScreen.syncVisibleState();
        appShellScreen.showCurrent();
      }
      return true;
    }

    wrapped = new ReaderResultParser.LiveMessageEvent();
    if (!readerParser.parseShortUpdateEnvelope(response.type, encrypted.data, wrapped)) {
      return false;
    }

    if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_APPEND) {
      unreadDelta = (!wrapped.outgoing && !isViewingPeer(wrapped.peerKind, wrapped.peerId)) ? 1 : 0;
      applied = appShellModel.reader.applyLiveTextUpdate(
        wrapped.peerKind,
        wrapped.peerId,
        wrapped.senderPeerKind,
        wrapped.senderPeerId,
        wrapped.messageId,
        wrapped.text,
        wrapped.outgoing,
        wrapped.hasMedia,
        wrapped.mediaLabel,
        wrapped.mediaInfo,
        wrapped.mediaRef,
        wrapped.date,
        unreadDelta
      );
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
      applied = appShellModel.reader.applyLiveEditedTextUpdate(
        wrapped.peerKind,
        wrapped.peerId,
        wrapped.senderPeerKind,
        wrapped.senderPeerId,
        wrapped.messageId,
        wrapped.text,
        wrapped.outgoing,
        wrapped.hasMedia,
        wrapped.mediaLabel,
        wrapped.mediaInfo,
        wrapped.mediaRef,
        wrapped.date
      );
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_DELETE) {
      applied = wrapped.peerKind != DialogListModel.PEER_UNKNOWN && wrapped.peerId != 0
        ? appShellModel.reader.deleteMessageForPeer(wrapped.peerKind, wrapped.peerId, wrapped.messageId)
        : appShellModel.reader.deleteMessageGlobally(wrapped.messageId) > 0;
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_READ) {
      applied = appShellModel.reader.applyLiveInboxRead(
        wrapped.peerKind,
        wrapped.peerId,
        wrapped.messageId,
        isViewingPeer(wrapped.peerKind, wrapped.peerId) ? 0 : wrapped.unreadCount
      );
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
      applied = appShellModel.reader.applyLiveOutboxRead(
        wrapped.peerKind,
        wrapped.peerId,
        wrapped.messageId
      );
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
      applied = applyPendingOutboundMessageId(wrapped);
    } else {
      return false;
    }
    if (!applied) {
      return false;
    }

    ensureSyncState();
    syncState.noteShortUpdate(
      wrapped.pts != 0 ? wrapped.pts : syncState.pts,
      wrapped.date != 0 ? wrapped.date : syncState.date
    );
    noteChannelLiveUpdate(wrapped.peerKind, wrapped.peerId, wrapped.pts);
    title = appShellModel.reader.peers.titleOrFallback(wrapped.peerKind, wrapped.peerId);
    appShellModel.updateReaderSummary();
    appShellModel.recomputeWorkingSet();
    if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_EDIT) {
      appShellModel.activeDetail = "Live edit in " + title + ".";
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX) {
      appShellModel.activeDetail = "Read state advanced in " + title + ".";
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_READ) {
      appShellModel.activeDetail = "Unread state updated for " + title + ".";
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_DELETE) {
      appShellModel.activeDetail = "Live delete in " + title + ".";
    } else if (wrapped.kind == ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID) {
      appShellModel.activeDetail = "Telegram acknowledged the outbound message.";
    } else {
      appShellModel.activeDetail = wrapped.outgoing
        ? "Live send delivered to " + title + "."
        : "Live message from " + title + ".";
    }
    syncShellRuntimeState();
    persistWarmState();
    System.out.println("UPDATE LIVE: " + title + " #" + Integer.toString(wrapped.messageId));
    if (appShellScreen != null) {
      appShellScreen.syncVisibleState();
      appShellScreen.showCurrent();
    }
    requestVisibleHistoryRead();
    return true;
  }

  private void applyAuxiliaryUpdate(Response response) {
    if (response == null || connection == null || appShellModel == null || !appShellModel.authorized) {
      return;
    }
    if (handlePendingPeerActionUpdate(response)) {
      return;
    }
    if (tryApplyDirectShortUpdate(response)) {
      return;
    }

    ensureSyncState();
    syncState.markNeedsDifference();
    if (response.type == CombinatorIds.updates_too_long && syncState.hasState) {
      syncState.noteDifferenceTooLong(syncState.pts);
    }
    if (response.type == CombinatorIds.updates_too_long) {
      appShellModel.activeDetail = "Telegram reported updatesTooLong; requesting catch-up.";
    } else {
      appShellModel.activeDetail = "Telegram pushed updates; requesting catch-up.";
    }
    syncShellRuntimeState();
    requestUpdatesDifference("Fetching Telegram updates catch-up...");
  }

  private boolean handlePendingPeerActionUpdate(Response response) {
    boolean joined;
    String detail;
    int route;

    if (response == null || appShellModel == null) {
      return false;
    }
    if (pendingPeerActionType != PEER_ACTION_JOIN && pendingPeerActionType != PEER_ACTION_LEAVE) {
      return false;
    }
    if (pendingPeerActionStartedAtMs == 0 ||
        (System.currentTimeMillis() - pendingPeerActionStartedAtMs) > 15000L) {
      return false;
    }
    if (response.type != CombinatorIds.updates &&
        response.type != CombinatorIds.updates_combined &&
        response.type != CombinatorIds.update_short &&
        response.type != CombinatorIds.update_short_message &&
        response.type != CombinatorIds.update_short_chat_message &&
        response.type != CombinatorIds.updates_too_long) {
      return false;
    }

    joined = pendingPeerActionType == PEER_ACTION_JOIN;
    detail = joined
      ? "Channel join acknowledged. Refreshing dialogs..."
      : "Channel leave acknowledged. Refreshing dialogs...";
    if (appShellModel.peer.peerKind == pendingPeerActionKind &&
        appShellModel.peer.peerId == pendingPeerActionId) {
      appShellModel.notePeerMembershipChanged(joined, detail);
    }
    route = appShellModel.router.currentScreen;
    clearPendingPeerAction();
    persistWarmState();
    if (appShellScreen != null && route == AppShellModel.SCREEN_PEER) {
      appShellScreen.showPeer();
    }
    if (pendingRouteAfterDialogs == 0) {
      pendingRouteAfterDialogs = route;
    }
    requestDialogsInBackground(detail);
    return true;
  }

  private boolean handleOutboundRpcError(int errorCode, String errorMessage) {
    if (appShellModel == null ||
        pendingSendPeerKind == DialogListModel.PEER_UNKNOWN ||
        pendingSendPeerId == 0) {
      return false;
    }

    appShellModel.reader.removeLatestPendingOutbound(pendingSendPeerKind, pendingSendPeerId);
    appShellModel.onComposeFailed(
      errorCode,
      "Send failed: RPC " + Integer.toString(errorCode) + ": " + errorMessage
    );
    persistWarmState();
    appShellScreen.showCurrent();
    clearPendingSendState();
    return true;
  }

  private boolean handleDiscoveryRpcError(int errorCode, String errorMessage) {
    if (appShellModel == null || appShellScreen == null || pendingDiscoveryQuery.length() == 0) {
      return false;
    }

    clearPendingDiscoverySearch();
    appShellModel.discovery.detail = "Telegram search failed: RPC " +
      Integer.toString(errorCode) + ": " + errorMessage;
    appShellModel.activeDetail = appShellModel.discovery.detail;
    if (appShellModel.router.currentScreen == AppShellModel.SCREEN_DISCOVERY) {
      appShellScreen.showDiscovery();
    }
    return true;
  }

  private boolean handlePeerRpcError(int errorCode, String errorMessage) {
    boolean peerInfoPending;
    boolean peerActionVisible;
    boolean peerActionPending;

    if (appShellModel == null || appShellScreen == null) {
      return false;
    }

    peerInfoPending = pendingPeerInfoKind != DialogListModel.PEER_UNKNOWN && pendingPeerInfoId != 0;
    peerActionPending = pendingPeerActionType != PEER_ACTION_NONE &&
      pendingPeerActionKind != DialogListModel.PEER_UNKNOWN &&
      pendingPeerActionId != 0;
    peerActionVisible =
      appShellModel.router.currentScreen == AppShellModel.SCREEN_PEER &&
      appShellModel.peer.peerId != 0 &&
      (appShellModel.peer.status == PeerActionStateModel.STATUS_ACTION ||
        appShellModel.peer.status == PeerActionStateModel.STATUS_PENDING_SECRET);

    if (!peerInfoPending && !peerActionVisible && !peerActionPending) {
      return false;
    }

    clearPendingPeerInfo();
    clearPendingPeerAction();
    appShellModel.notePeerError("Peer action failed: RPC " +
      Integer.toString(errorCode) + ": " + errorMessage);
    appShellScreen.showPeer();
    return true;
  }

  private boolean handleDeleteRpcError(int errorCode, String errorMessage) {
    if (appShellModel == null || appShellScreen == null || pendingDeleteMessageId == 0) {
      return false;
    }

    clearPendingDeleteState();
    appShellModel.activeDetail = "Delete failed: RPC " +
      Integer.toString(errorCode) + ": " + errorMessage;
    appShellScreen.showHistory();
    return true;
  }

  private boolean handleReadHistoryRpcError(int errorCode, String errorMessage) {
    if (pendingReadPeerKind == DialogListModel.PEER_UNKNOWN || pendingReadMaxId == 0) {
      return false;
    }

    rollbackPendingRead("Mark read failed: RPC " +
      Integer.toString(errorCode) + ": " + errorMessage);
    return true;
  }

  private boolean handleMediaRpcError(int errorCode, String errorMessage) {
    if (appShellModel == null || appShellScreen == null || pendingMediaMessageId == 0) {
      return false;
    }

    failActiveMediaDownload("Save failed: RPC " + Integer.toString(errorCode) + ": " + errorMessage);
    return true;
  }

  private void maybeStartQueuedMediaDownload() {
    MediaPlanQueue.Entry entry;

    if (connection == null || appShellModel == null || pendingMediaMessageId != 0) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.activeDetail = READ_ACK_WAIT_DETAIL;
      return;
    }
    entry = appShellModel.mediaQueue.nextQueued();
    if (entry == null) {
      return;
    }
    startMediaDownload(entry);
  }

  private void startMediaDownload(MediaPlanQueue.Entry entry) {
    if (entry == null || connection == null || appShellModel == null) {
      return;
    }
    if (!entry.mediaRef.usable()) {
      appShellModel.mediaQueue.markFailed(entry.peerKind, entry.peerId, entry.messageId, "Missing media reference.");
      appShellModel.updateMediaSummary();
      appShellModel.activeDetail = "This media item cannot be saved yet.";
      appShellScreen.showCurrent();
      return;
    }
    if (entry.mediaRef.expectedSize > MEDIA_DOWNLOAD_MAX_BYTES) {
      failMediaEntry(entry, "Media exceeds the device cache limit.");
      return;
    }
    if (connection.current_config != null &&
        connection.current_config.this_dc > 0 &&
        entry.mediaRef.dcId > 0 &&
        entry.mediaRef.dcId != connection.current_config.this_dc) {
      failMediaEntry(entry, "Cross-DC media fetch is not supported yet.");
      return;
    }

    pendingMediaPeerKind = entry.peerKind;
    pendingMediaPeerId = entry.peerId;
    pendingMediaMessageId = entry.messageId;
    pendingMediaReference.copyFrom(entry.mediaRef);
    pendingMediaBufferLength = 0;
    pendingMediaExpectedBytes = entry.mediaRef.expectedSize;
    pendingMediaBuffer = new byte[
      pendingMediaExpectedBytes > 0 ? pendingMediaExpectedBytes : MEDIA_DOWNLOAD_MAX_BYTES
    ];
    appShellModel.mediaQueue.markLoading(
      entry.peerKind,
      entry.peerId,
      entry.messageId,
      pendingMediaExpectedBytes
    );
    appShellModel.updateMediaSummary();
    appShellModel.activeDetail = "Saving media for " + entry.peerTitle + "...";
    appShellScreen.showCurrent();
    (new SendUploadGetFile(pendingMediaReference, 0, MEDIA_DOWNLOAD_CHUNK_BYTES)).send(connection);
  }

  private void failMediaEntry(MediaPlanQueue.Entry entry, String detail) {
    if (appShellModel == null || appShellScreen == null || entry == null) {
      return;
    }
    appShellModel.mediaQueue.markFailed(entry.peerKind, entry.peerId, entry.messageId, detail);
    appShellModel.updateMediaSummary();
    appShellModel.activeDetail = detail;
    appShellScreen.showCurrent();
  }

  private void failActiveMediaDownload(String detail) {
    if (appShellModel == null || appShellScreen == null || pendingMediaMessageId == 0) {
      clearPendingMediaDownload();
      return;
    }
    appShellModel.mediaQueue.markFailed(
      pendingMediaPeerKind,
      pendingMediaPeerId,
      pendingMediaMessageId,
      detail
    );
    appShellModel.updateMediaSummary();
    appShellModel.activeDetail = detail;
    clearPendingMediaDownload();
    appShellScreen.showCurrent();
    requestVisibleHistoryRead();
    if (!readRequestInFlight()) {
      maybeStartQueuedMediaDownload();
    }
  }

  private void clearPendingMediaDownload() {
    pendingMediaPeerKind = DialogListModel.PEER_UNKNOWN;
    pendingMediaPeerId = 0;
    pendingMediaMessageId = 0;
    pendingMediaReference.clear();
    pendingMediaBuffer = null;
    pendingMediaBufferLength = 0;
    pendingMediaExpectedBytes = 0;
  }

  private void applyMediaFile(RPCResponse response) {
    RecieveUploadFile upload;
    byte[] data;
    byte[] exact;

    if (appShellModel == null || appShellScreen == null || pendingMediaMessageId == 0) {
      return;
    }

    try {
      upload = RecieveUploadFile.from_rpc_response(response);
    } catch (IllegalArgumentException e) {
      failActiveMediaDownload("Save failed: " + e.getMessage());
      return;
    }

    if (upload.kind == RecieveUploadFile.KIND_CDN_REDIRECT) {
      failActiveMediaDownload("CDN media fetch is not supported yet.");
      return;
    }

    data = upload.bytes;
    if (data.length > 0) {
      if (pendingMediaBuffer == null ||
          data.length > pendingMediaBuffer.length - pendingMediaBufferLength) {
        failActiveMediaDownload("Media exceeds the device cache limit.");
        return;
      }
      System.arraycopy(data, 0, pendingMediaBuffer, pendingMediaBufferLength, data.length);
      pendingMediaBufferLength += data.length;
    }

    appShellModel.mediaQueue.markProgress(
      pendingMediaPeerKind,
      pendingMediaPeerId,
      pendingMediaMessageId,
      pendingMediaBufferLength,
      pendingMediaExpectedBytes
    );
    appShellModel.updateMediaSummary();

    if (data.length == MEDIA_DOWNLOAD_CHUNK_BYTES &&
        pendingMediaBufferLength < MEDIA_DOWNLOAD_MAX_BYTES &&
        (pendingMediaExpectedBytes <= 0 || pendingMediaBufferLength < pendingMediaExpectedBytes)) {
      (new SendUploadGetFile(
        pendingMediaReference,
        pendingMediaBufferLength,
        MEDIA_DOWNLOAD_CHUNK_BYTES
      )).send(connection);
      return;
    }

    if (pendingMediaBufferLength <= 0) {
      failActiveMediaDownload("No media payload was returned.");
      return;
    }

    if (pendingMediaBufferLength == pendingMediaBuffer.length) {
      exact = pendingMediaBuffer;
    } else {
      exact = new byte[pendingMediaBufferLength];
      System.arraycopy(pendingMediaBuffer, 0, exact, 0, pendingMediaBufferLength);
    }
    pendingMediaBuffer = null;
    appShellModel.mediaQueue.markReadyOwned(
      pendingMediaPeerKind,
      pendingMediaPeerId,
      pendingMediaMessageId,
      exact,
      pendingMediaExpectedBytes
    );
    appShellModel.updateMediaSummary();
    appShellModel.activeDetail = "Saved " + Integer.toString(pendingMediaBufferLength) + " bytes into the bounded media cache.";
    clearPendingMediaDownload();
    appShellScreen.showCurrent();
    requestVisibleHistoryRead();
    if (!readRequestInFlight()) {
      maybeStartQueuedMediaDownload();
    }
  }

  private void requestDialogs() {
    if (connection == null || appShellModel == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.activeDetail = READ_ACK_WAIT_DETAIL;
      appShellScreen.showCurrent();
      return;
    }
    rememberSelectedDialogForRefresh();
    appShellModel.beginDialogsLoad();
    appShellScreen.showDialogs();
    (new SendGetDialogs(appShellModel.reader.dialogs.capacity())).send(connection);
  }

  private void requestDialogsInBackground(String detail) {
    if (connection == null || appShellModel == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.activeDetail = READ_ACK_WAIT_DETAIL;
      return;
    }
    rememberSelectedDialogForRefresh();
    appShellModel.beginDialogsLoad();
    if (detail != null && detail.length() > 0) {
      appShellModel.activeDetail = detail;
    }
    (new SendGetDialogs(appShellModel.reader.dialogs.capacity())).send(connection);
  }

  private void requestHistoryFor(DialogListModel.Item selected) {
    SendGetDialogs.DialogInputPeer peer;

    if (connection == null || appShellModel == null || selected == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.activeDetail = READ_ACK_WAIT_DETAIL;
      appShellScreen.showCurrent();
      return;
    }

    try {
      peer = toInputPeer(selected);
    } catch (IllegalArgumentException e) {
      appShellModel.onReaderFailure(e.getMessage());
      appShellScreen.showCurrent();
      return;
    }

    appShellModel.beginHistoryLoadForSelected();
    pendingHistoryPeerKind = selected.peerKind;
    pendingHistoryPeerId = selected.peerId;
    appShellScreen.showHistory();
    (new SendGetHistory(peer, appShellModel.reader.history.capacity())).send(connection);
  }

  private void requestVisibleHistoryRead() {
    DialogListModel.Item selected;
    ChannelSyncState.Entry channelState;
    SendGetDialogs.DialogInputPeer peer;
    int maxMessageId;

    if (connection == null || appShellModel == null || !appShellModel.authorized ||
        readRequestInFlight() || hasRpcConflictForVisibleRead()) {
      return;
    }
    selected = selectedDialogForPeer(
      appShellModel.reader.historyPeerKind,
      appShellModel.reader.historyPeerId
    );
    if (selected == null || !isViewingPeer(selected.peerKind, selected.peerId)) {
      return;
    }
    if (selected.peerKind == DialogListModel.PEER_CHANNEL) {
      channelState = appShellModel.reader.channelSync.find(selected.peerId);
      if (channelState != null && channelState.needsDifference) {
        return;
      }
    }
    maxMessageId = appShellModel.reader.latestIncomingMessageIdForPeer(
      selected.peerKind,
      selected.peerId
    );
    if (maxMessageId <= 0 ||
        (selected.readInboxMaxId >= maxMessageId && selected.unreadCount <= 0)) {
      return;
    }

    try {
      peer = toInputPeer(selected);
    } catch (IllegalArgumentException e) {
      appShellModel.activeDetail = "Unable to mark history read: " + e.getMessage();
      return;
    }

    pendingReadPeerKind = selected.peerKind;
    pendingReadPeerId = selected.peerId;
    pendingReadMaxId = maxMessageId;
    pendingReadPreviousInboxMaxId = selected.readInboxMaxId;
    pendingReadPreviousUnreadCount = selected.unreadCount;
    appShellModel.reader.applyLiveInboxRead(selected.peerKind, selected.peerId, maxMessageId, 0);
    persistWarmState();

    try {
      (new SendReadHistory(peer, maxMessageId)).send(connection);
    } catch (IllegalArgumentException e) {
      rollbackPendingRead("Unable to mark history read: " + e.getMessage());
    }
  }

  private void requestSendCurrentCompose(String text) {
    SendGetDialogs.DialogInputPeer fromPeer = null;
    SendGetDialogs.DialogInputPeer peer;
    String message = trimOrEmpty(text);
    int peerKind;
    long peerId;
    long randomId;

    if (connection == null || appShellModel == null) {
      return;
    }
    if (pendingSendPeerKind != DialogListModel.PEER_UNKNOWN ||
        pendingSendPeerId != 0 ||
        pendingSendRandomId != 0 ||
        appShellModel.isOutboundSendLocked()) {
      appShellModel.activeDetail = AppShellModel.OUTBOUND_WAIT_DETAIL;
      appShellScreen.showCurrent();
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.onComposeFailed(0, READ_ACK_WAIT_DETAIL);
      appShellScreen.showCompose();
      return;
    }

    peerKind = appShellModel.compose.peerKind;
    peerId = appShellModel.compose.peerId;
    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      DialogListModel.Item selected = appShellModel.selectedDialog();
      if (selected != null) {
        peerKind = selected.peerKind;
        peerId = selected.peerId;
      }
    }
    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      appShellModel.onComposeFailed(0, "Select a dialog or peer before sending.");
      appShellScreen.showCompose();
      return;
    }
    if (!appShellModel.compose.hasForwardSource() && message.length() == 0) {
      appShellModel.onComposeFailed(0, "Compose text is empty.");
      appShellScreen.showCompose();
      return;
    }
    if (appShellModel.compose.hasForwardSource() && message.length() > 0) {
      appShellModel.onComposeFailed(0, "Direct forward does not support extra text yet.");
      appShellScreen.showCompose();
      return;
    }

    try {
      peer = toInputPeer(peerKind, peerId);
    } catch (IllegalArgumentException e) {
      appShellModel.onComposeFailed(0, e.getMessage());
      appShellScreen.showCompose();
      return;
    }

    randomId = nextOutboundRandomId();
    if (appShellModel.compose.hasForwardSource()) {
      try {
        fromPeer = toInputPeer(
          appShellModel.compose.forwardFromPeerKind,
          appShellModel.compose.forwardFromPeerId
        );
      } catch (IllegalArgumentException e) {
        appShellModel.onComposeFailed(0, e.getMessage());
        appShellScreen.showCompose();
        return;
      }
      if (!appShellModel.beginComposeForward(randomId)) {
        appShellScreen.showCompose();
        return;
      }
    } else {
      if (!appShellModel.beginComposeSend(message, randomId)) {
        appShellScreen.showCompose();
        return;
      }
    }

    pendingSendPeerKind = peerKind;
    pendingSendPeerId = peerId;
    pendingSendRandomId = randomId;
    persistWarmState();
    appShellScreen.showHistory();
    if (appShellModel.compose.hasForwardSource()) {
      (new SendForwardMessages(
        fromPeer,
        appShellModel.compose.forwardMessageId,
        randomId,
        peer
      )).send(connection);
    } else {
      (new SendSendMessage(peer, message, randomId, appShellModel.compose.replyToMessageId)).send(connection);
    }
  }

  public void openDialog(int index) {
    DialogListModel.Item selected;

    if (connection == null || appShellModel == null) {
      return;
    }
    if (!appShellModel.selectDialog(index)) {
      return;
    }

    selected = appShellModel.selectedDialog();
    requestHistoryFor(selected);
  }

  public void openDiscovery() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    appShellModel.openDiscovery();
    persistWarmState();
    appShellScreen.showDiscovery();
  }

  public void searchDiscovery(String query) {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.searchDiscovery(query)) {
      appShellScreen.showDiscoveryQuery();
      return;
    }
    persistWarmState();
    appShellScreen.showDiscovery();
    requestDiscoverySearch(appShellModel.discovery.query);
  }

  public void openSelectedDiscoveryResult() {
    DiscoveryStateModel.Result selected;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    selected = appShellModel.selectedDiscoveryResult();
    if (selected == null) {
      appShellScreen.showDiscovery();
      return;
    }
    if (selected.hasDialog()) {
      openDialog(selected.dialogIndex);
      return;
    }
    if (!appShellModel.openSelectedDiscoveryResult()) {
      appShellScreen.showDiscovery();
      return;
    }
    persistWarmState();
    appShellScreen.showCurrent();
    if (appShellModel.router.currentScreen == AppShellModel.SCREEN_PEER) {
      requestSelectedPeerInfo("Requesting Telegram peer details...");
    }
  }

  public void openComposeForSelectedDiscoveryResult() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.openComposeForSelectedDiscoveryResult()) {
      appShellScreen.showDiscovery();
      return;
    }
    persistWarmState();
    appShellScreen.showCompose();
  }

  public void openPeerForSelectedDiscoveryResult() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.openPeerForSelectedDiscoveryResult()) {
      appShellScreen.showDiscovery();
      return;
    }
    persistWarmState();
    appShellScreen.showPeer();
    requestSelectedPeerInfo("Requesting Telegram peer details...");
  }

  public void openCompose() {
    if (appShellModel == null) {
      return;
    }
    if (!appShellModel.openComposeForSelectedDialog()) {
      appShellScreen.showCurrent();
      return;
    }
    appShellScreen.showCompose();
  }

  public void replyToSelectedMessage() {
    if (appShellModel == null) {
      return;
    }
    if (!appShellModel.prepareReplyToSelectedMessage()) {
      appShellScreen.showCurrent();
      return;
    }
    appShellScreen.showCompose();
  }

  public void forwardSelectedMessage() {
    if (appShellModel == null) {
      return;
    }
    if (!appShellModel.prepareForwardSelectedMessage()) {
      appShellScreen.showCurrent();
      return;
    }
    appShellScreen.showCompose();
  }

  public void openSelectedMessageMediaExternally() {
    String target;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }

    target = appShellModel.selectedMessageMediaExternalTarget();
    if (target.length() == 0) {
      appShellModel.activeDetail = "This media item has no external target.";
      appShellScreen.showCurrent();
      return;
    }

    try {
      platformRequest(target);
      appShellModel.activeDetail = "Requested external open for " + target + ".";
    } catch (Exception e) {
      appShellModel.activeDetail = "External open failed: " + e.getMessage();
    }
    appShellScreen.showCurrent();
  }

  public void planSelectedMessageMedia() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (appShellModel.planSelectedMessageMedia()) {
      maybeStartQueuedMediaDownload();
    }
    appShellScreen.showCurrent();
  }

  public void clearMediaQueue() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    clearPendingMediaDownload();
    appShellModel.clearMediaQueue();
    appShellScreen.showCurrent();
  }

  public void deleteSelectedMessage() {
    DialogListModel.Item dialog;
    reader.MessageListModel.Item message;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    dialog = appShellModel.selectedDialog();
    message = appShellModel.selectedMessage();
    if (connection == null || dialog == null || message == null) {
      appShellModel.activeDetail = "Select a synced message before deleting.";
      appShellScreen.showCurrent();
      return;
    }
    if (message.messageId == 0 || message.pending) {
      appShellModel.activeDetail = "This message cannot be deleted yet.";
      appShellScreen.showHistory();
      return;
    }
    if (pendingDeleteMessageId != 0) {
      appShellModel.activeDetail = "A delete request is already in flight.";
      appShellScreen.showHistory();
      return;
    }
    if (pendingReadMaxId != 0) {
      appShellModel.activeDetail = "Wait for the read acknowledgement before deleting.";
      appShellScreen.showHistory();
      return;
    }

    pendingDeletePeerKind = dialog.peerKind;
    pendingDeletePeerId = dialog.peerId;
    pendingDeleteMessageId = message.messageId;

    try {
      if (dialog.peerKind == DialogListModel.PEER_CHANNEL) {
        long accessHash = dialog.hasAccessHash ? dialog.accessHash : resolveAccessHash(dialog.peerKind, dialog.peerId);
        (new SendDeleteChannelMessages(dialog.peerId, accessHash, message.messageId)).send(connection);
      } else {
        (new SendDeleteMessages(message.messageId, true)).send(connection);
      }
    } catch (IllegalArgumentException e) {
      clearPendingDeleteState();
      appShellModel.activeDetail = "Delete failed: " + e.getMessage();
      appShellScreen.showHistory();
      return;
    }
    appShellModel.activeDetail = "Deleting the selected message...";
    appShellScreen.showHistory();
  }

  public void openPeerForSelectedDialog() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.openPeerForSelectedDialog()) {
      appShellScreen.showCurrent();
      return;
    }
    persistWarmState();
    appShellScreen.showPeer();
    requestSelectedPeerInfo("Requesting Telegram peer details...");
  }

  public void openPeerDialog() {
    DialogListModel.Item selected;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.openPeerDialog()) {
      appShellScreen.showPeer();
      return;
    }
    selected = appShellModel.selectedDialog();
    if (selected != null) {
      requestHistoryFor(selected);
    } else {
      appShellScreen.showHistory();
    }
  }

  public void openComposeForSelectedPeer() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (!appShellModel.openComposeForSelectedPeer()) {
      appShellScreen.showPeer();
      return;
    }
    persistWarmState();
    appShellScreen.showCompose();
  }

  public void refreshSelectedPeerInfo() {
    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    requestSelectedPeerInfo("Refreshing Telegram peer details...");
  }

  public void joinSelectedPeer() {
    long accessHash;

    if (connection == null || appShellModel == null || appShellScreen == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }
    if (appShellModel.peer.peerKind != DialogListModel.PEER_CHANNEL || appShellModel.peer.peerId == 0) {
      appShellModel.notePeerError("Select a channel before joining.");
      appShellScreen.showPeer();
      return;
    }

    accessHash = resolveAccessHash(appShellModel.peer.peerKind, appShellModel.peer.peerId);
    if (accessHash == 0) {
      appShellModel.notePeerError("Channel join requires a full access hash.");
      appShellScreen.showPeer();
      return;
    }

    appShellModel.notePeerAction("JOIN", "Requesting Telegram channel join...");
    rememberPendingPeerAction(PEER_ACTION_JOIN, appShellModel.peer.peerKind, appShellModel.peer.peerId);
    persistWarmState();
    appShellScreen.showPeer();
    (new SendJoinChannel(appShellModel.peer.peerId, accessHash)).send(connection);
  }

  public void leaveSelectedPeer() {
    long accessHash;

    if (connection == null || appShellModel == null || appShellScreen == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }
    if (appShellModel.peer.peerKind != DialogListModel.PEER_CHANNEL || appShellModel.peer.peerId == 0) {
      appShellModel.notePeerError("Select a channel before leaving.");
      appShellScreen.showPeer();
      return;
    }

    accessHash = resolveAccessHash(appShellModel.peer.peerKind, appShellModel.peer.peerId);
    if (accessHash == 0) {
      appShellModel.notePeerError("Channel leave requires a full access hash.");
      appShellScreen.showPeer();
      return;
    }

    appShellModel.notePeerAction("LEAVE", "Requesting Telegram channel leave...");
    rememberPendingPeerAction(PEER_ACTION_LEAVE, appShellModel.peer.peerKind, appShellModel.peer.peerId);
    persistWarmState();
    appShellScreen.showPeer();
    (new SendLeaveChannel(appShellModel.peer.peerId, accessHash)).send(connection);
  }

  public void startSecretChatWithSelectedPeer() {
    long accessHash;
    RandomPlus random;
    int randomId;
    byte[] ga;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }
    if (appShellModel.peer.peerKind != DialogListModel.PEER_USER || appShellModel.peer.peerId == 0) {
      appShellModel.notePeerError("Select a user before opening a secret chat.");
      appShellScreen.showPeer();
      return;
    }

    accessHash = resolveAccessHash(appShellModel.peer.peerKind, appShellModel.peer.peerId);
    if (accessHash == 0) {
      appShellModel.notePeerError("Secret chats require a full cached user access hash.");
      appShellScreen.showPeer();
      return;
    }
    if (connection == null) {
      appShellModel.notePeerError("Secret chats require an active Telegram connection.");
      appShellScreen.showPeer();
      return;
    }

    appShellModel.noteSecretChatPending(
      "Requesting Telegram secret chat bootstrap..."
    );
    rememberPendingPeerAction(PEER_ACTION_SECRET, appShellModel.peer.peerKind, appShellModel.peer.peerId);
    persistWarmState();
    appShellScreen.showPeer();
    if (connection.random_number_generator == null) {
      clearPendingPeerAction();
      appShellModel.notePeerError("Secure randomness is unavailable.");
      appShellScreen.showPeer();
      return;
    }
    random = connection.random_number_generator;
    randomId = 0;
    while (randomId == 0) {
      randomId = random.nextInt();
    }
    try {
      ga = SecretChatBootstrap.generatePublicValue(random, secretExponentScratch);
      System.arraycopy(secretExponentScratch, 0, pendingSecretExponent, 0, pendingSecretExponent.length);
      pendingSecretExponentValid = true;
      pendingSecretExpectedKeyFingerprint = 0;
      (new SendRequestEncryption(appShellModel.peer.peerId, accessHash, randomId, ga)).send(connection);
    } catch (IllegalArgumentException e) {
      clearPendingPeerAction();
      appShellModel.notePeerError(e.getMessage());
      appShellScreen.showPeer();
    }
  }

  public void acceptSelectedPeerSecretChat() {
    SecretChatDirectory.Item secretChat;
    RandomPlus random;
    BigInteger exponent;
    byte[] gb;
    long keyFingerprint;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }
    if (appShellModel.peer.peerKind != DialogListModel.PEER_USER || appShellModel.peer.peerId == 0) {
      appShellModel.notePeerError("Select a user before accepting a secret chat.");
      appShellScreen.showPeer();
      return;
    }
    if (connection == null) {
      appShellModel.notePeerError("Secret chats require an active Telegram connection.");
      appShellScreen.showPeer();
      return;
    }

    secretChat = appShellModel.reader.secretChats.findByPeer(appShellModel.peer.peerKind, appShellModel.peer.peerId);
    if (secretChat == null ||
        secretChat.state != SecretChatDirectory.STATE_REQUESTED ||
        secretChat.outbound ||
        secretChat.chatId == 0 ||
        secretChat.accessHash == 0 ||
        secretChat.pendingKeyData == null ||
        secretChat.pendingKeyData.length == 0) {
      appShellModel.notePeerError("No inbound secret chat request is ready to accept.");
      appShellScreen.showPeer();
      return;
    }

    appShellModel.noteSecretChatPending("Accepting Telegram secret chat...");
    rememberPendingPeerAction(PEER_ACTION_SECRET, appShellModel.peer.peerKind, appShellModel.peer.peerId);
    persistWarmState();
    appShellScreen.showPeer();

    if (connection.random_number_generator == null) {
      clearPendingPeerAction();
      appShellModel.notePeerError("Secure randomness is unavailable.");
      appShellScreen.showPeer();
      return;
    }
    random = connection.random_number_generator;
    try {
      SecretChatBootstrap.generateExponent(random, secretExponentScratch);
      exponent = new BigInteger(1, secretExponentScratch);
      gb = SecretChatBootstrap.publicValueForExponent(exponent);
      keyFingerprint = SecretChatBootstrap.fingerprintForSharedKey(
        SecretChatBootstrap.sharedKeyForPeerPublic(secretChat.pendingKeyData, exponent)
      );
      pendingSecretExponentValid = false;
      pendingSecretExpectedKeyFingerprint = keyFingerprint;
      (new SendAcceptEncryption(secretChat.chatId, secretChat.accessHash, gb, keyFingerprint)).send(connection);
    } catch (IllegalArgumentException e) {
      clearPendingPeerAction();
      appShellModel.notePeerError(e.getMessage());
      appShellScreen.showPeer();
    }
  }

  public void discardSelectedPeerSecretChat() {
    SecretChatDirectory.Item secretChat;

    if (appShellModel == null || appShellScreen == null) {
      return;
    }
    if (readRequestInFlight()) {
      appShellModel.notePeerError(READ_ACK_WAIT_DETAIL);
      appShellScreen.showPeer();
      return;
    }
    if (appShellModel.peer.peerKind != DialogListModel.PEER_USER || appShellModel.peer.peerId == 0) {
      appShellModel.notePeerError("Select a user before closing a secret chat.");
      appShellScreen.showPeer();
      return;
    }
    if (connection == null) {
      appShellModel.notePeerError("Secret chats require an active Telegram connection.");
      appShellScreen.showPeer();
      return;
    }

    secretChat = appShellModel.reader.secretChats.findByPeer(appShellModel.peer.peerKind, appShellModel.peer.peerId);
    if (secretChat == null ||
        secretChat.chatId == 0 ||
        secretChat.state == SecretChatDirectory.STATE_CLOSED) {
      appShellModel.notePeerError("No tracked secret chat is available to discard.");
      appShellScreen.showPeer();
      return;
    }

    appShellModel.noteSecretChatPending("Discarding Telegram secret chat...");
    rememberPendingPeerAction(PEER_ACTION_SECRET_DISCARD, appShellModel.peer.peerKind, appShellModel.peer.peerId);
    persistWarmState();
    appShellScreen.showPeer();
    try {
      (new SendDiscardEncryption(secretChat.chatId, false)).send(connection);
    } catch (IllegalArgumentException e) {
      clearPendingPeerAction();
      appShellModel.notePeerError(e.getMessage());
      appShellScreen.showPeer();
    }
  }

  public void refreshDialogs() {
    requestDialogs();
  }

  public void refreshCurrentHistory() {
    if (appShellModel == null) {
      return;
    }
    requestHistoryFor(appShellModel.selectedDialog());
  }

  public void sendCurrentCompose(String text) {
    requestSendCurrentCompose(text);
  }

  public void onLoginWarmStateChanged() {
    persistWarmState();
  }

  public void reconnectSession() {
    persistSessionState();
    if (!scheduleAuthorizedReconnect("Manual reconnect requested.", -302) &&
        appShellModel != null) {
      appShellModel.activeDetail = "Reconnect is unavailable until authorization is saved.";
      if (appShellScreen != null) {
        appShellScreen.showCurrent();
      }
    }
  }
}
