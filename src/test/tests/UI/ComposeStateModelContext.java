package test;

import compose.ComposeStateModel;
import reader.DialogListModel;

public class ComposeStateModelContext {
  public static class BindPeerResetsDraftTest extends Test {
    public String label() {
      return "Compose state resets its draft when binding a different peer";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.replaceText("draft"), true);
      compose.bindPeer(DialogListModel.PEER_CHAT, 2, "Builders");

      expect(compose.peerKind, DialogListModel.PEER_CHAT);
      expect(compose.peerId, 2L);
      expect(compose.text, "");
      expect(compose.status, ComposeStateModel.STATUS_IDLE);
    }
  }

  public static class SendTransitionsClearDraftOnSuccessTest extends Test {
    public String label() {
      return "Compose state clears the draft once a send succeeds";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.replaceText("hello"), true);
      compose.markSending(99L);
      compose.markSent(77);

      expect(compose.status, ComposeStateModel.STATUS_SENT);
      expect(compose.lastRandomId, 99L);
      expect(compose.lastMessageId, 77);
      expect(compose.text, "");
    }
  }

  public static class FailedSendRetainsDraftTest extends Test {
    public String label() {
      return "Compose state keeps the draft for retry when a send fails";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.replaceText("retry me"), true);
      compose.markSending(88L);
      compose.markFailed(420, "RPC 420");

      expect(compose.status, ComposeStateModel.STATUS_FAILED);
      expect(compose.text, "retry me");
      expect(compose.lastErrorCode, 420);
      expect(compose.lastErrorMessage, "RPC 420");
      expect(compose.canSend(), true);
    }
  }

  public static class ReplyContextTracksSelectedMessageTest extends Test {
    public String label() {
      return "Compose state can hold reply context separately from the draft";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.replaceText("hello"), true);
      compose.prepareReplyContext(77, "Alice", "Ping");

      expect(compose.contextMode, ComposeStateModel.CONTEXT_REPLY);
      expect(compose.replyToMessageId, 77);
      expect(compose.contextLabel, "Alice");
      expect(compose.contextPreview, "Ping");
      expect(compose.text, "hello");
    }
  }

  public static class ForwardContextCanSendWithoutDraftTextTest extends Test {
    public String label() {
      return "Compose state can hold forward context without a typed draft";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.prepareForwardContext(
        DialogListModel.PEER_USER,
        2L,
        77,
        "Bob",
        "Longer forward body"
      ), true);

      expect(compose.contextMode, ComposeStateModel.CONTEXT_FORWARD);
      expect(compose.forwardFromPeerKind, DialogListModel.PEER_USER);
      expect(compose.forwardFromPeerId, 2L);
      expect(compose.forwardMessageId, 77);
      expect(compose.contextLabel, "Bob");
      expect(compose.contextPreview, "Longer forward body");
      expect(compose.text, "");
      expect(compose.canSend(), true);
    }
  }

  public static class ForwardContextSurvivesPeerRebindTest extends Test {
    public String label() {
      return "Compose state can preserve forward context while changing destination peer";
    }

    public void test() throws TestFailureException {
      ComposeStateModel compose = new ComposeStateModel();

      compose.bindPeer(DialogListModel.PEER_USER, 1, "Alice");
      expect(compose.prepareForwardContext(
        DialogListModel.PEER_USER,
        1L,
        44,
        "Alice",
        "Forward me"
      ), true);
      compose.rebindPeer(DialogListModel.PEER_CHANNEL, 9L, "News", true, true);

      expect(compose.peerKind, DialogListModel.PEER_CHANNEL);
      expect(compose.peerId, 9L);
      expect(compose.peerTitle, "News");
      expect(compose.contextMode, ComposeStateModel.CONTEXT_FORWARD);
      expect(compose.forwardMessageId, 44);
      expect(compose.canSend(), true);
    }
  }
}
