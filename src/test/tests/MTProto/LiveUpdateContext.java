package test;

import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import mtproto.Serializer;
import mtproto.recieve.messages.ReaderResultParser;
import mtproto.recieve.updates.RecieveShortTextUpdate;
import reader.DialogListModel;
import sync.SyncState;

public class LiveUpdateContext {
  private static final int TG_DIALOG_ID = (int)0xd58a08c6L;
  private static final int TG_PEER_NOTIFY_SETTINGS_ID = (int)0x99622c0cL;
  private static final int TG_ENCRYPTED_MESSAGE_ID = (int)0xed18c118L;
  private static final int TG_ENCRYPTED_MESSAGE_SERVICE_ID = (int)0x23734b06L;
  private static final int TG_ENCRYPTED_FILE_EMPTY_ID = (int)0xc21f497eL;

  private static byte[] vector(byte[][] items) {
    Serializer serializer = new Serializer()
      .append_int(CombinatorIds.vector)
      .append_int(items.length);
    int i;

    for (i = 0; i < items.length; ++i) {
      serializer.append_raw_bytes(items[i]);
    }
    return serializer.end();
  }

  private static byte[] encodedString(String value) {
    return (new Serializer()).append_string(value).end();
  }

  private static byte[] peerUser(long userId) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_PEER_USER_ID)
      .append_long(userId)
      .end();
  }

  private static byte[] peerChannel(long channelId) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_PEER_CHANNEL_ID)
      .append_long(channelId)
      .end();
  }

  private static byte[] notifySettings() {
    return (new Serializer())
      .append_int(TG_PEER_NOTIFY_SETTINGS_ID)
      .append_int(0)
      .end();
  }

  private static byte[] textMessage(int messageId, long fromPeerId, long peerId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int(1 << 8)
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000000)
      .append_string(text)
      .end();
  }

  private static byte[] textChannelMessage(int messageId, long fromPeerId, long channelId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int(1 << 8)
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerChannel(channelId))
      .append_int(1700000004)
      .append_string(text)
      .end();
  }

  private static byte[] dialogChannel(long channelId, int topMessageId, int unreadCount, int pts) {
    return (new Serializer())
      .append_int(TG_DIALOG_ID)
      .append_int(1)
      .append_raw_bytes(peerChannel(channelId))
      .append_int(topMessageId)
      .append_int(0)
      .append_int(0)
      .append_int(unreadCount)
      .append_int(0)
      .append_int(0)
      .append_raw_bytes(notifySettings())
      .append_int(pts)
      .end();
  }

  private static byte[] user(long userId, String firstName) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_USER_ID)
      .append_int((1 << 1))
      .append_int(0)
      .append_long(userId)
      .append_string(firstName)
      .end();
  }

  private static byte[] updatesState(int pts, int qts, int date, int seq, int unreadCount) {
    return (new Serializer())
      .append_int(CombinatorIds.updates_state)
      .append_int(pts)
      .append_int(qts)
      .append_int(date)
      .append_int(seq)
      .append_int(unreadCount)
      .end();
  }

  private static byte[] encryptedFileEmpty() {
    return (new Serializer())
      .append_int(TG_ENCRYPTED_FILE_EMPTY_ID)
      .end();
  }

  private static byte[] encryptedMessage(int chatId, long randomId, int date, byte[] payload) {
    return (new Serializer())
      .append_int(TG_ENCRYPTED_MESSAGE_ID)
      .append_long(randomId)
      .append_int(chatId)
      .append_int(date)
      .append_byte_string(payload)
      .append_raw_bytes(encryptedFileEmpty())
      .end();
  }

  private static byte[] encryptedServiceMessage(int chatId, long randomId, int date, byte[] payload) {
    return (new Serializer())
      .append_int(TG_ENCRYPTED_MESSAGE_SERVICE_ID)
      .append_long(randomId)
      .append_int(chatId)
      .append_int(date)
      .append_byte_string(payload)
      .end();
  }

  public static class ParseShortPrivateUpdatePayloadTest extends Test {
    public String label() {
      return "Can parse updateShortMessage payloads for live private messages";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int((1 << 13) | (1 << 7))
        .append_int(77)
        .append_long(1001L)
        .append_raw_bytes(encodedString("hello world"))
        .append_int(900)
        .append_int(1)
        .append_int(1700000000)
        .append_int(CombinatorIds.vector)
        .append_int(1)
        .append_int(0x6ed02538)
        .append_int(0)
        .append_int(5)
        .end();
      RecieveShortTextUpdate update = RecieveShortTextUpdate.from_encrypted_message(
        new EncryptedResponse(0, 0, 0, 0, CombinatorIds.update_short_message, body)
      );

      expect(update.peerKind, DialogListModel.PEER_USER);
      expect(update.peerId, 1001L);
      expect(update.senderPeerKind, DialogListModel.PEER_USER);
      expect(update.senderPeerId, 1001L);
      expect(update.messageId, 77);
      expect(update.text, "hello world");
      expect(update.outgoing, false);
      expect(update.pts, 900);
      expect(update.ptsCount, 1);
      expect(update.date, 1700000000);
    }
  }

  public static class RejectNegativeShortUpdateDateTest extends Test {
    public String label() {
      return "Short updates reject negative server dates";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(0)
        .append_int(77)
        .append_long(1001L)
        .append_raw_bytes(encodedString("invalid"))
        .append_int(900)
        .append_int(1)
        .append_int(-1)
        .end();
      boolean rejected = false;

      try {
        RecieveShortTextUpdate.from_encrypted_message(
          new EncryptedResponse(0, 0, 0, 0, CombinatorIds.update_short_message, body)
        );
      } catch (IllegalArgumentException e) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class ParseShortChatUpdatePayloadTest extends Test {
    public String label() {
      return "Can parse updateShortChatMessage payloads for live group messages";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int((1 << 1) | (1 << 11))
        .append_int(88)
        .append_long(3003L)
        .append_long(2002L)
        .append_raw_bytes(encodedString("sent"))
        .append_int(901)
        .append_int(1)
        .append_int(1700000001)
        .append_long(44L)
        .end();
      RecieveShortTextUpdate update = RecieveShortTextUpdate.from_encrypted_message(
        new EncryptedResponse(0, 0, 0, 0, CombinatorIds.update_short_chat_message, body)
      );

      expect(update.peerKind, DialogListModel.PEER_CHAT);
      expect(update.peerId, 2002L);
      expect(update.senderPeerKind, DialogListModel.PEER_USER);
      expect(update.senderPeerId, 3003L);
      expect(update.outgoing, true);
      expect(update.text, "sent");
      expect(update.pts, 901);
    }
  }

  public static class SyncStateTracksAppliedShortUpdatesTest extends Test {
    public String label() {
      return "Sync state accepts directly applied short updates";
    }

    public void test() throws TestFailureException {
      SyncState state = new SyncState();

      state.noteShortUpdate(1234, 1700000123);

      expect(state.hasState, true);
      expect(state.needsDifference, false);
      expect(state.pts, 1234);
      expect(state.date, 1700000123);
      expect(state.ptsLimitHint, SyncState.DEFAULT_PTS_LIMIT_HINT);
    }
  }

  public static class ParseWrappedShortNewMessagePayloadTest extends Test {
    public String label() {
      return "Can parse updateShort wrappers carrying updateNewMessage";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveMessageEvent event = new ReaderResultParser.LiveMessageEvent();
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.update_new_message)
        .append_raw_bytes(textMessage(501, 1001L, 1001L, "wrapped"))
        .append_int(902)
        .append_int(1)
        .append_int(1700000002)
        .end();

      expect(parser.parseShortUpdateEnvelope(CombinatorIds.update_short, body, event), true);
      expect(event.kind, ReaderResultParser.LiveMessageEvent.KIND_APPEND);
      expect(event.peerKind, DialogListModel.PEER_USER);
      expect(event.peerId, 1001L);
      expect(event.senderPeerKind, DialogListModel.PEER_USER);
      expect(event.senderPeerId, 1001L);
      expect(event.messageId, 501);
      expect(event.text, "wrapped");
      expect(event.pts, 902);
      expect(event.date, 1700000002);
    }
  }

  public static class ParseWrappedShortEditMessagePayloadTest extends Test {
    public String label() {
      return "Can parse updateShort wrappers carrying updateEditMessage";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveMessageEvent event = new ReaderResultParser.LiveMessageEvent();
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.update_edit_message)
        .append_raw_bytes(textMessage(777, 2002L, 2002L, "edited"))
        .append_int(903)
        .append_int(1)
        .append_int(1700000003)
        .end();

      expect(parser.parseShortUpdateEnvelope(CombinatorIds.update_short, body, event), true);
      expect(event.kind, ReaderResultParser.LiveMessageEvent.KIND_EDIT);
      expect(event.peerKind, DialogListModel.PEER_USER);
      expect(event.peerId, 2002L);
      expect(event.messageId, 777);
      expect(event.text, "edited");
      expect(event.pts, 903);
      expect(event.date, 1700000003);
    }
  }

  public static class ParseWrappedShortOutboxReadPayloadTest extends Test {
    public String label() {
      return "Can parse updateShort wrappers carrying outbox read state";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveMessageEvent event = new ReaderResultParser.LiveMessageEvent();
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.update_read_history_outbox)
        .append_raw_bytes(peerUser(2002L))
        .append_int(777)
        .append_int(904)
        .append_int(1)
        .append_int(1700000004)
        .end();

      expect(parser.parseShortUpdateEnvelope(CombinatorIds.update_short, body, event), true);
      expect(event.kind, ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX);
      expect(event.peerKind, DialogListModel.PEER_USER);
      expect(event.peerId, 2002L);
      expect(event.messageId, 777);
      expect(event.pts, 904);
      expect(event.ptsCount, 1);
      expect(event.date, 1700000004);
    }
  }

  public static class ParseUpdatesVectorPayloadTest extends Test {
    public String label() {
      return "Can parse updates vectors with append and read-state events";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveUpdateBatch batch = new ReaderResultParser.LiveUpdateBatch(8, 8);
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(CombinatorIds.update_new_message)
            .append_raw_bytes(textMessage(901, 1001L, 1001L, "vector"))
            .append_int(910)
            .append_int(1)
            .end(),
          (new Serializer())
            .append_int(CombinatorIds.update_read_history_inbox)
            .append_int(0)
            .append_raw_bytes(peerUser(1001L))
            .append_int(901)
            .append_int(0)
            .append_int(911)
            .append_int(1)
            .end(),
          (new Serializer())
            .append_int(CombinatorIds.update_read_history_outbox)
            .append_raw_bytes(peerUser(1001L))
            .append_int(899)
            .append_int(912)
            .append_int(1)
            .end(),
          (new Serializer())
            .append_int(CombinatorIds.update_read_channel_outbox)
            .append_long(4001L)
            .append_int(700)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, "Alice")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_int(1700000010)
        .append_int(77)
        .end();

      expect(parser.parseUpdateCollection(CombinatorIds.updates, body, batch), true);
      expect(batch.count, 4);
      expect(batch.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_APPEND);
      expect(batch.events[0].messageId, 901);
      expect(batch.events[0].date, 1700000000);
      expect(batch.events[1].kind, ReaderResultParser.LiveMessageEvent.KIND_READ);
      expect(batch.events[1].peerKind, DialogListModel.PEER_USER);
      expect(batch.events[1].peerId, 1001L);
      expect(batch.events[1].messageId, 901);
      expect(batch.events[1].unreadCount, 0);
      expect(batch.events[2].kind, ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX);
      expect(batch.events[2].peerKind, DialogListModel.PEER_USER);
      expect(batch.events[2].peerId, 1001L);
      expect(batch.events[2].messageId, 899);
      expect(batch.events[2].pts, 912);
      expect(batch.events[2].ptsCount, 1);
      expect(batch.events[3].kind, ReaderResultParser.LiveMessageEvent.KIND_READ_OUTBOX);
      expect(batch.events[3].peerKind, DialogListModel.PEER_CHANNEL);
      expect(batch.events[3].peerId, 4001L);
      expect(batch.events[3].messageId, 700);
      expect(batch.date, 1700000010);
      expect(batch.seq, 77);
      expect(batch.peers.count, 1);
      expect(batch.peers.items[0].title, "Alice");
    }
  }

  public static class ParseUpdatesVectorMessageIdPayloadTest extends Test {
    public String label() {
      return "Can parse updateMessageID items from updates vectors";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveUpdateBatch batch = new ReaderResultParser.LiveUpdateBatch(8, 8);
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(CombinatorIds.update_message_id)
            .append_int(901)
            .append_long(0x0102030405060708L)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_int(1700000010)
        .append_int(77)
        .end();

      expect(parser.parseUpdateCollection(CombinatorIds.updates, body, batch), true);
      expect(batch.count, 1);
      expect(batch.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_MESSAGE_ID);
      expect(batch.events[0].messageId, 901);
      expect(batch.events[0].randomId, 0x0102030405060708L);
    }
  }

  public static class ParseUpdatesCombinedDeletePayloadTest extends Test {
    public String label() {
      return "Can parse updatesCombined vectors with bounded delete events";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveUpdateBatch batch = new ReaderResultParser.LiveUpdateBatch(8, 8);
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(CombinatorIds.update_delete_channel_messages)
            .append_long(555L)
            .append_raw_bytes(vector(new byte[][] {
              (new Serializer()).append_int(33).end(),
              (new Serializer()).append_int(34).end()
            }))
            .append_int(920)
            .append_int(2)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_int(1700000011)
        .append_int(70)
        .append_int(71)
        .end();

      expect(parser.parseUpdateCollection(CombinatorIds.updates_combined, body, batch), true);
      expect(batch.count, 2);
      expect(batch.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_DELETE);
      expect(batch.events[0].peerKind, DialogListModel.PEER_CHANNEL);
      expect(batch.events[0].peerId, 555L);
      expect(batch.events[0].messageId, 33);
      expect(batch.events[1].messageId, 34);
      expect(batch.seqStart, 70);
      expect(batch.seq, 71);
    }
  }

  public static class ParseUpdatesDifferencePayloadTest extends Test {
    public String label() {
      return "Can parse updates.difference payloads into bounded live events";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveUpdateBatch batch = new ReaderResultParser.LiveUpdateBatch(8, 8);
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textMessage(930, 1001L, 1001L, "difference")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(CombinatorIds.update_read_history_inbox)
            .append_int(0)
            .append_raw_bytes(peerUser(1001L))
            .append_int(930)
            .append_int(0)
            .append_int(931)
            .append_int(1)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, "Alice")
        }))
        .append_raw_bytes(updatesState(940, 0, 1700000020, 88, 0))
        .end();

      expect(parser.parseDifferenceResult(CombinatorIds.updates_difference, body, batch), true);
      expect(batch.count, 2);
      expect(batch.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_APPEND);
      expect(batch.events[0].messageId, 930);
      expect(batch.events[0].date, 1700000000);
      expect(batch.events[1].kind, ReaderResultParser.LiveMessageEvent.KIND_READ);
      expect(batch.events[1].peerKind, DialogListModel.PEER_USER);
      expect(batch.events[1].peerId, 1001L);
      expect(batch.peers.count, 1);
      expect(batch.peers.items[0].title, "Alice");
      expect(batch.date, 1700000020);
      expect(batch.seq, 88);
    }
  }

  public static class ParseUpdatesDifferenceEncryptedPayloadTest extends Test {
    public String label() {
      return "Can parse encrypted metadata from updates.difference payloads";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.LiveUpdateBatch batch = new ReaderResultParser.LiveUpdateBatch(8, 8);
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          encryptedMessage(77, 0x0102030405060708L, 1700000021, new byte[] {1, 2, 3}),
          encryptedServiceMessage(78, 0x1112131415161718L, 1700000022, new byte[] {4, 5})
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(updatesState(940, 0, 1700000023, 88, 0))
        .end();

      expect(parser.parseDifferenceResult(CombinatorIds.updates_difference, body, batch), true);
      expect(batch.count, 0);
      expect(batch.secretCount, 2);
      expect(batch.secretEvents[0].state, mtproto.recieve.messages.SecretChatResultParser.STATE_ACTIVITY);
      expect(batch.secretEvents[0].chatId, 77);
      expect(batch.secretEvents[0].randomId, 0x0102030405060708L);
      expect(batch.secretEvents[0].date, 1700000021);
      expect(batch.secretEvents[0].service, false);
      expect(batch.secretEvents[1].state, mtproto.recieve.messages.SecretChatResultParser.STATE_ACTIVITY);
      expect(batch.secretEvents[1].chatId, 78);
      expect(batch.secretEvents[1].randomId, 0x1112131415161718L);
      expect(batch.secretEvents[1].date, 1700000022);
      expect(batch.secretEvents[1].service, true);
      expect(batch.date, 1700000023);
      expect(batch.seq, 88);
    }
  }

  public static class ParseChannelDifferencePayloadTest extends Test {
    public String label() {
      return "Can parse updates.channelDifference payloads into bounded live events";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.ChannelDifferenceBatch batch =
        new ReaderResultParser.ChannelDifferenceBatch(8, 8);
      byte[] body = (new Serializer())
        .append_int(0)
        .append_int(950)
        .append_raw_bytes(vector(new byte[][] {
          textChannelMessage(941, 1001L, 555L, "channel diff")
        }))
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(CombinatorIds.update_read_channel_inbox)
            .append_int(0)
            .append_long(555L)
            .append_int(941)
            .append_int(0)
            .append_int(951)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {
          (new Serializer())
            .append_int(ReaderResultParser.TG_CHANNEL_ID)
            .append_int(1 << 13)
            .append_int(0)
            .append_long(555L)
            .append_long(999L)
            .append_string("News")
            .append_int((int)0x37c1011cL)
            .append_int(0)
            .end()
        }))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, "Alice")
        }))
        .end();

      expect(parser.parseChannelDifference(CombinatorIds.updates_channel_difference, body, 555L, batch), true);
      expect(batch.type, ReaderResultParser.ChannelDifferenceBatch.TYPE_FULL);
      expect(batch.peerId, 555L);
      expect(batch.pts, 950);
      expect(batch.updates.count, 2);
      expect(batch.updates.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_APPEND);
      expect(batch.updates.events[0].peerKind, DialogListModel.PEER_CHANNEL);
      expect(batch.updates.events[0].peerId, 555L);
      expect(batch.updates.events[1].kind, ReaderResultParser.LiveMessageEvent.KIND_READ);
      expect(batch.updates.events[1].peerId, 555L);
      expect(batch.updates.peers.count, 2);
    }
  }

  public static class ParseChannelDifferenceTooLongPayloadTest extends Test {
    public String label() {
      return "Can parse updates.channelDifferenceTooLong metadata";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);
      ReaderResultParser.ChannelDifferenceBatch batch =
        new ReaderResultParser.ChannelDifferenceBatch(8, 8);
      byte[] body = (new Serializer())
        .append_int(1 << 1)
        .append_int(15)
        .append_raw_bytes(dialogChannel(555L, 942, 3, 960))
        .append_raw_bytes(vector(new byte[][] {
          textChannelMessage(942, 1001L, 555L, "too long")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, "Alice")
        }))
        .end();

      expect(parser.parseChannelDifference(CombinatorIds.updates_channel_difference_too_long, body, 555L, batch), true);
      expect(batch.type, ReaderResultParser.ChannelDifferenceBatch.TYPE_TOO_LONG);
      expect(batch.peerId, 555L);
      expect(batch.pts, 960);
      expect(batch.timeout, 15);
      expect(batch.updates.count, 1);
      expect(batch.updates.events[0].kind, ReaderResultParser.LiveMessageEvent.KIND_READ);
      expect(batch.updates.events[0].peerKind, DialogListModel.PEER_CHANNEL);
      expect(batch.updates.events[0].unreadCount, 3);
    }
  }
}
