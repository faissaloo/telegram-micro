package test;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.Serializer;
import mtproto.recieve.messages.ReaderResultParser;
import mtproto.send.messages.SendGetDialogs;
import mtproto.send.messages.SendGetHistory;
import mtproto.send.messages.SendReadHistory;
import reader.DialogListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;
import support.Decode;

public class ReaderContext {
  private static final int TG_PHOTO_ID = (int)0xfb197a65L;
  private static final int TG_PHOTO_SIZE_OLD_ID = (int)0x75c78e60L;
  private static final int TG_PHOTO_SIZE_PROGRESSIVE_ID = (int)0xfa3efb95L;
  private static final int TG_PHOTO_STRIPPED_SIZE_ID = (int)0xe0b0bc2eL;
  private static final int TG_VIDEO_SIZE_ID = (int)0xde33b094L;
  private static final int TG_MESSAGE_ENTITY_BOLD_ID = (int)0xbd610bc9L;
  private static final int TG_DOCUMENT_ID = (int)0x8fd4c4d8L;
  private static final int TG_DOCUMENT_ATTRIBUTE_VIDEO_ID = (int)0x43c57c48L;
  private static final int TG_DOCUMENT_ATTRIBUTE_FILENAME_ID = (int)0x15590068L;
  private static final int TG_DOCUMENT_ATTRIBUTE_CUSTOM_EMOJI_ID = (int)0xfd149899L;
  private static final int TG_INPUT_STICKER_SET_SHORT_NAME_ID = (int)0x861cc8a0L;

  private static byte[] vector(byte[][] items) {
    Serializer serializer = new Serializer().append_int(CombinatorIds.vector).append_int(items.length);
    int i;

    for (i = 0; i < items.length; ++i) {
      serializer.append_raw_bytes(items[i]);
    }
    return serializer.end();
  }

  private static byte[] peerUser(long userId) {
    return peer(ReaderResultParser.TG_PEER_USER_ID, userId);
  }

  private static byte[] peerChat(long chatId) {
    return peer(ReaderResultParser.TG_PEER_CHAT_ID, chatId);
  }

  private static byte[] peerChannel(long channelId) {
    return peer(ReaderResultParser.TG_PEER_CHANNEL_ID, channelId);
  }

  private static byte[] peer(int constructor, long peerId) {
    return (new Serializer())
      .append_int(constructor)
      .append_long(peerId)
      .end();
  }

  private static byte[] notifySettings() {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_PEER_NOTIFY_SETTINGS_ID)
      .append_int(0)
      .end();
  }

  private static byte[] dialog(byte[] peer, int topMessageId, int unreadCount) {
    return dialog(peer, topMessageId, 0, 0, unreadCount);
  }

  private static byte[] dialog(
    byte[] peer,
    int topMessageId,
    int readInboxMaxId,
    int readOutboxMaxId,
    int unreadCount
  ) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_DIALOG_ID)
      .append_int(0)
      .append_raw_bytes(peer)
      .append_int(topMessageId)
      .append_int(readInboxMaxId)
      .append_int(readOutboxMaxId)
      .append_int(unreadCount)
      .append_int(0)
      .append_int(0)
      .append_raw_bytes(notifySettings())
      .end();
  }

  private static byte[] currentDraftMessage(String message) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_DRAFT_MESSAGE_ID)
      .append_int(0)
      .append_string(message)
      .append_int(1700000000)
      .end();
  }

  private static byte[] dialogWithCurrentDraft(byte[] peer, int topMessageId, int unreadCount, String draft) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_DIALOG_ID)
      .append_int(1 << 1)
      .append_raw_bytes(peer)
      .append_int(topMessageId)
      .append_int(0)
      .append_int(0)
      .append_int(unreadCount)
      .append_int(0)
      .append_int(0)
      .append_raw_bytes(notifySettings())
      .append_raw_bytes(currentDraftMessage(draft))
      .end();
  }

  private static byte[] userDialog(long peerId, int topMessageId, int unreadCount) {
    return dialog(peerUser(peerId), topMessageId, unreadCount);
  }

  private static byte[] channelDialog(long peerId, int topMessageId, int unreadCount) {
    return dialog(peerChannel(peerId), topMessageId, unreadCount);
  }

  private static byte[] user(long userId, long accessHash, String firstName) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_USER_ID)
      .append_int((1 << 0) | (1 << 1))
      .append_int(0)
      .append_long(userId)
      .append_long(accessHash)
      .append_string(firstName)
      .end();
  }

  private static byte[] channel(long channelId, long accessHash, String title, String username) {
    int flags = (1 << 13);

    if (username != null && username.length() > 0) {
      flags |= (1 << 6);
    }

    Serializer serializer = new Serializer()
      .append_int(ReaderResultParser.TG_CHANNEL_ID)
      .append_int(flags)
      .append_int(0)
      .append_long(channelId)
      .append_long(accessHash)
      .append_string(title != null ? title : "");

    if ((flags & (1 << 6)) != 0) {
      serializer.append_string(username);
    }

    return serializer
      .append_int((int)0x37c1011cL)
      .append_int(0)
      .end();
  }

  private static byte[] contactsFoundPayload() {
    return (new Serializer())
      .append_raw_bytes(vector(new byte[][] {
        peerUser(1001L)
      }))
      .append_raw_bytes(vector(new byte[][] {
        peerUser(1001L),
        peerChannel(4001L)
      }))
      .append_raw_bytes(vector(new byte[][] {
        channel(4001L, 0x0102030405060708L, "News Feed", "newsfeed")
      }))
      .append_raw_bytes(vector(new byte[][] {
        user(1001L, 0x1122334455667788L, "Alice")
      }))
      .end();
  }

  private static byte[] userFullPayload(long userId, String about) {
    int flags = about != null && about.length() > 0 ? (1 << 1) : 0;
    Serializer serializer = new Serializer()
      .append_int(ReaderResultParser.TG_USER_FULL_ID)
      .append_int(flags)
      .append_int(0)
      .append_long(userId);

    if ((flags & (1 << 1)) != 0) {
      serializer.append_string(about);
    }
    return serializer.end();
  }

  private static byte[] chatFullPayload(long chatId, String about, int participantCount) {
    byte[][] participants = new byte[participantCount][];
    int i;

    for (i = 0; i < participantCount; ++i) {
      participants[i] = (new Serializer())
        .append_int((int)0xe1f867b8L)
        .append_int(0)
        .append_long(1001L + i)
        .end();
    }

    return (new Serializer())
      .append_int(ReaderResultParser.TG_CHAT_FULL_ID)
      .append_int(0)
      .append_long(chatId)
      .append_string(about)
      .append_int((int)0x3cbc93f8L)
      .append_long(chatId)
      .append_raw_bytes(vector(participants))
      .append_int(1)
      .end();
  }

  private static byte[] channelFullPayload(long channelId, String about, int participantCount) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_CHANNEL_FULL_ID)
      .append_int(1 << 0)
      .append_int(0)
      .append_long(channelId)
      .append_string(about)
      .append_int(participantCount)
      .end();
  }

  private static byte[] textMessageWithConstructor(
    int constructor, int messageId, byte[] fromPeer, byte[] peer, String text) {
    return textMessageWithConstructorAtDate(
      constructor,
      messageId,
      fromPeer,
      peer,
      text,
      1700000000
    );
  }

  private static byte[] textMessageWithConstructorAtDate(
    int constructor,
    int messageId,
    byte[] fromPeer,
    byte[] peer,
    String text,
    int date
  ) {
    return (new Serializer())
      .append_int(constructor)
      .append_int(1 << 8)
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(fromPeer)
      .append_raw_bytes(peer)
      .append_int(date)
      .append_string(text)
      .end();
  }

  private static byte[] textMessage(int messageId, byte[] fromPeer, byte[] peer, String text) {
    return textMessageWithConstructor(
      ReaderResultParser.TG_MESSAGE_ID, messageId, fromPeer, peer, text);
  }

  private static byte[] legacyTextMessage(int messageId, byte[] fromPeer, byte[] peer, String text) {
    return textMessageWithConstructor(
      ReaderResultParser.TG_MESSAGE_LEGACY_ID, messageId, fromPeer, peer, text);
  }

  private static byte[] textUserMessage(int messageId, long fromPeerId, long peerId, String text) {
    return textMessage(messageId, peerUser(fromPeerId), peerUser(peerId), text);
  }

  private static byte[] unsupportedMediaMessageWithConstructor(
    int constructor, int messageId, byte[] fromPeer, byte[] peer) {
    return (new Serializer())
      .append_int(constructor)
      .append_int((1 << 8) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(fromPeer)
      .append_raw_bytes(peer)
      .append_int(1700000001)
      .append_string("")
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_UNSUPPORTED_ID)
      .end();
  }

  private static byte[] unsupportedMediaMessage(int messageId, byte[] fromPeer, byte[] peer) {
    return unsupportedMediaMessageWithConstructor(
      ReaderResultParser.TG_MESSAGE_ID, messageId, fromPeer, peer);
  }

  private static byte[] photoStrippedSize(String type, byte[] bytes) {
    return (new Serializer())
      .append_int(TG_PHOTO_STRIPPED_SIZE_ID)
      .append_string(type)
      .append_byte_string(bytes)
      .end();
  }

  private static byte[] photoWithSizesNoStickerFlag() {
    return (new Serializer())
      .append_int(TG_PHOTO_ID)
      .append_int(0)
      .append_long(0x1111222233334444L)
      .append_long(0x5555666677778888L)
      .append_byte_string(new byte[] {1, 2, 3, 4})
      .append_int(1700000001)
      .append_raw_bytes(vector(new byte[][] {
        photoStrippedSize("i", new byte[] {10, 20, 30, 40})
      }))
      .append_int(4)
      .end();
  }

  private static byte[] photoWithLegacySizesNoStickerFlag() {
    return (new Serializer())
      .append_int(TG_PHOTO_ID)
      .append_int(0)
      .append_long(0x1111222233334445L)
      .append_long(0x5555666677778889L)
      .append_byte_string(new byte[] {5, 6, 7, 8})
      .append_int(1700000002)
      .append_raw_bytes(vector(new byte[][] {
        (new Serializer())
          .append_int(TG_PHOTO_SIZE_OLD_ID)
          .append_string("x")
          .append_int(640)
          .append_int(400)
          .append_int(12345)
          .end()
      }))
      .append_int(4)
      .end();
  }

  private static byte[] photoWithVideoSizesNoStickerFlag() {
    return (new Serializer())
      .append_int(TG_PHOTO_ID)
      .append_int(1 << 1)
      .append_long(0x1111222233334446L)
      .append_long(0x5555666677778890L)
      .append_byte_string(new byte[] {9, 10, 11, 12})
      .append_int(1700000004)
      .append_raw_bytes(vector(new byte[][] {
        photoStrippedSize("i", new byte[] {50, 60, 70, 80})
      }))
      .append_raw_bytes(vector(new byte[][] {
        (new Serializer())
          .append_int(TG_VIDEO_SIZE_ID)
          .append_int(0)
          .append_string("v")
          .append_int(600)
          .append_int(400)
          .append_int(123456)
          .end()
      }))
      .append_int(4)
      .end();
  }

  private static byte[] photoWithProgressiveSizesNoStickerFlag() {
    return (new Serializer())
      .append_int(TG_PHOTO_ID)
      .append_int(0)
      .append_long(0x1111222233334447L)
      .append_long(0x5555666677778891L)
      .append_byte_string(new byte[] {13, 14, 15, 16})
      .append_int(1700000006)
      .append_raw_bytes(vector(new byte[][] {
        (new Serializer())
          .append_int(TG_PHOTO_SIZE_PROGRESSIVE_ID)
          .append_string("x")
          .append_int(600)
          .append_int(400)
          .append_raw_bytes(vector(new byte[][] {
            (new Serializer()).append_int(4408).end(),
            (new Serializer()).append_int(9160).end(),
            (new Serializer()).append_int(17937).end()
          }))
          .end()
      }))
      .append_int(4)
      .end();
  }

  private static byte[] textUserMessageWithPhotoAndEntities(int messageId, long fromPeerId, long peerId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int((1 << 8) | (1 << 7) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000000)
      .append_string(text)
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_PHOTO_ID)
      .append_int(1 << 0)
      .append_raw_bytes(photoWithSizesNoStickerFlag())
      .append_raw_bytes(vector(new byte[][] {
        (new Serializer())
          .append_int(TG_MESSAGE_ENTITY_BOLD_ID)
          .append_int(0)
          .append_int(text.length())
          .end()
      }))
      .end();
  }

  private static byte[] textUserMessageWithLegacyPhotoSize(int messageId, long fromPeerId, long peerId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int((1 << 8) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000003)
      .append_string(text)
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_PHOTO_ID)
      .append_int(1 << 0)
      .append_raw_bytes(photoWithLegacySizesNoStickerFlag())
      .end();
  }

  private static byte[] textUserMessageWithPhotoVideoSizes(int messageId, long fromPeerId, long peerId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int((1 << 8) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000005)
      .append_string(text)
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_PHOTO_ID)
      .append_int(1 << 0)
      .append_raw_bytes(photoWithVideoSizesNoStickerFlag())
      .end();
  }

  private static byte[] textUserMessageWithProgressivePhotoSize(int messageId, long fromPeerId, long peerId, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int((1 << 8) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000007)
      .append_string(text)
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_PHOTO_ID)
      .append_int(1 << 0)
      .append_raw_bytes(photoWithProgressiveSizesNoStickerFlag())
      .end();
  }

  private static byte[] inputStickerSetShortName(String name) {
    return (new Serializer())
      .append_int(TG_INPUT_STICKER_SET_SHORT_NAME_ID)
      .append_string(name)
      .end();
  }

  private static byte[] documentAttributeFilename(String fileName) {
    return (new Serializer())
      .append_int(TG_DOCUMENT_ATTRIBUTE_FILENAME_ID)
      .append_string(fileName)
      .end();
  }

  private static byte[] documentAttributeVideoWithCodec(String codec) {
    return (new Serializer())
      .append_int(TG_DOCUMENT_ATTRIBUTE_VIDEO_ID)
      .append_int((1 << 2) | (1 << 4) | (1 << 5))
      .append_long(0L)
      .append_int(240)
      .append_int(320)
      .append_int(16)
      .append_long(0L)
      .append_string(codec)
      .end();
  }

  private static byte[] documentAttributeCustomEmoji(String alt, String stickerSetName) {
    return (new Serializer())
      .append_int(TG_DOCUMENT_ATTRIBUTE_CUSTOM_EMOJI_ID)
      .append_int(0)
      .append_string(alt)
      .append_raw_bytes(inputStickerSetShortName(stickerSetName))
      .end();
  }

  private static byte[] documentWithAttributes(byte[][] attributes) {
    return (new Serializer())
      .append_int(TG_DOCUMENT_ID)
      .append_int(0)
      .append_long(0x1020304050607080L)
      .append_long(0x1122334455667788L)
      .append_byte_string(new byte[] {1, 2, 3, 4})
      .append_int(1700000008)
      .append_string("video/mp4")
      .append_long(4096L)
      .append_int(4)
      .append_raw_bytes(vector(attributes))
      .end();
  }

  private static byte[] textUserMessageWithDocument(int messageId, long fromPeerId, long peerId, String text, byte[] document) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int((1 << 8) | (1 << 9))
      .append_int(0)
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000008)
      .append_string(text)
      .append_int(ReaderResultParser.TG_MESSAGE_MEDIA_DOCUMENT_ID)
      .append_int(1 << 0)
      .append_raw_bytes(document)
      .end();
  }

  private static byte[] unsupportedUserMediaMessage(int messageId, long fromPeerId, long peerId) {
    return unsupportedMediaMessage(messageId, peerUser(fromPeerId), peerUser(peerId));
  }

  private static byte[] legacyTextUserMessage(int messageId, long fromPeerId, long peerId, String text) {
    return legacyTextMessage(messageId, peerUser(fromPeerId), peerUser(peerId), text);
  }

  private static byte[] rankedCurrentTextMessage(
    int messageId, long fromPeerId, long peerId, String rank, String text) {
    return (new Serializer())
      .append_int(ReaderResultParser.TG_MESSAGE_ID)
      .append_int(1 << 8)
      .append_int((1 << 10) | (1 << 11) | (1 << 12))
      .append_int(messageId)
      .append_raw_bytes(peerUser(fromPeerId))
      .append_string(rank)
      .append_raw_bytes(peerUser(peerId))
      .append_int(1700000000)
      .append_string(text)
      .append_int(3600)
      .append_string("en")
      .end();
  }

  private static byte[] messagesNotModified(int count) {
    return (new Serializer())
      .append_int(count)
      .end();
  }

  public static class BuildGetDialogsRequestTest extends Test {
    public String label() {
      return "Can build messages.getDialogs requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendGetDialogs(5)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_get_dialogs);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_empty);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 5);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0L);
      offset += 8;

      expect(offset, body.length);
    }
  }

  public static class BuildGetHistoryRequestTest extends Test {
    public String label() {
      return "Can build messages.getHistory requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendGetHistory(
        SendGetDialogs.DialogInputPeer.channel(77L, 0x1122334455667788L),
        20
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_get_history);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 20);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0L);
      offset += 8;

      expect(offset, body.length);
    }
  }

  public static class BuildReadHistoryRequestTest extends Test {
    public String label() {
      return "Can build bounded private and channel read-history requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendReadHistory(
        SendGetDialogs.DialogInputPeer.user(77L, 0x1122334455667788L),
        91
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_read_history);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 91);
      offset += 4;
      expect(offset, body.length);

      body = (new SendReadHistory(
        SendGetDialogs.DialogInputPeer.channel(88L, 0x0102030405060708L),
        92
      )).message_data.end();
      offset = 0;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.channels_read_history);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 88L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 92);
      offset += 4;
      expect(offset, body.length);

      try {
        new SendReadHistory(SendGetDialogs.DialogInputPeer.chat(9L), 0);
        expect(true, false);
      } catch (IllegalArgumentException expected) {
        expect(true, true);
      }
    }
  }

  public static class ParseDialogsPayloadTest extends Test {
    public String label() {
      return "Can parse messages.dialogs payloads into bounded reader state";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          dialog(peerUser(1001L), 77, 73, 75, 3)
        }))
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(77, 1001L, 1001L, "Hello")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, 0x1122334455667788L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginDialogsLoad();
      expect(parser.applyDialogs(new RPCResponse(CombinatorIds.messages_dialogs, body), reader), true);
      reader.finishDialogsLoad();

      expect(reader.dialogs.count, 1);
      expect(reader.dialogs.items[0].peerKind, DialogListModel.PEER_USER);
      expect(reader.dialogs.items[0].peerId, 1001L);
      expect(reader.dialogs.items[0].hasAccessHash, true);
      expect(reader.dialogs.items[0].accessHash, 0x1122334455667788L);
      expect(reader.dialogs.items[0].topMessageId, 77);
      expect(reader.dialogs.items[0].readInboxMaxId, 73);
      expect(reader.dialogs.items[0].readOutboxMaxId, 75);
      expect(reader.dialogs.items[0].title, "Alice");
      expect(reader.dialogs.items[0].preview, "Hello");
      expect(reader.dialogs.items[0].unreadCount, 3);
      expect(reader.peers.count, 1);
    }
  }

  public static class RejectNegativeDialogReadWatermarkTest extends Test {
    public String label() {
      return "Dialog parsing rejects negative read watermarks before reader mutation";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          dialog(peerUser(1001L), 77, -1, 75, 3)
        }))
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(77, 1001L, 1001L, "Hello")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, 0x1122334455667788L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(4, 4, 8);

      reader.beginDialogsLoad();
      expect(parser.applyDialogs(new RPCResponse(CombinatorIds.messages_dialogs, body), reader), false);
      expect(reader.dialogs.count, 0);
      expect(reader.peers.count, 0);
    }
  }

  public static class ParseHistoryPayloadTest extends Test {
    public String label() {
      return "Can parse messages.Messages payloads into bounded history state";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(90, 2001L, 3001L, "hi"),
          unsupportedUserMediaMessage(91, 2001L, 3001L)
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(new RPCResponse(CombinatorIds.messages_messages, body), reader, DialogListModel.PEER_USER, 3001L), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 2);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "hi");
      expect(reader.history.items[0].messageId, 90);
      expect(reader.history.items[0].dateSeconds, 1700000000);
      expect(reader.history.items[0].hasMedia, false);
      expect(reader.history.items[1].sender, "Bob");
      expect(reader.history.items[1].text, "DOCUMENT");
      expect(reader.history.items[1].messageId, 91);
      expect(reader.history.items[1].dateSeconds, 1700000001);
      expect(reader.history.items[1].hasMedia, true);
      expect(reader.historyPeerKind, DialogListModel.PEER_USER);
      expect(reader.historyPeerId, 3001L);
    }
  }

  public static class RejectNegativeHistoryMessageDateTest extends Test {
    public String label() {
      return "History parsing rejects a negative message date before reader mutation";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textMessageWithConstructorAtDate(
            ReaderResultParser.TG_MESSAGE_ID,
            91,
            peerUser(2001L),
            peerUser(3001L),
            "invalid",
            -1
          )
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.appendDialog(DialogListModel.PEER_USER, 3001L, "Alice", "old", 0, false);
      reader.selectDialog(0);
      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      reader.appendHistoryMessage(90, "Bob", "kept", false, false, false);
      reader.finishHistoryLoad();

      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), false);
      expect(reader.history.count, 1);
      expect(reader.history.items[0].messageId, 90);
      expect(reader.history.items[0].text, "kept");
      expect(reader.dialogs.items[0].preview, "old");
      expect(parser.lastError().indexOf("Message date must not be negative.") >= 0, true);
    }
  }

  public static class ParseLegacyMessageConstructorTest extends Test {
    public String label() {
      return "Can parse legacy message constructors for reader compatibility";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          legacyTextUserMessage(90, 2001L, 3001L, "hi")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "hi");
      expect(reader.history.items[0].messageId, 90);
    }
  }

  public static class ParseCurrentMessageOptionalFieldsTest extends Test {
    public String label() {
      return "Can parse current message optional fields without misalignment";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          rankedCurrentTextMessage(90, 2001L, 3001L, "admin", "hi")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "hi");
      expect(reader.history.items[0].messageId, 90);
    }
  }

  public static class ParseCurrentPhotoMediaWithEntitiesTest extends Test {
    public String label() {
      return "Can parse current photo media messages with entities when photo flags are clear";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithPhotoAndEntities(92, 2001L, 3001L, "photo")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "photo");
      expect(reader.history.items[0].messageId, 92);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "PHOTO");
      expect(reader.history.items[0].mediaInfo, "");
    }
  }

  public static class ParseLegacyPhotoSizeCompatibilityTest extends Test {
    public String label() {
      return "Can parse legacy photoSize constructors nested in current photos";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithLegacyPhotoSize(93, 2001L, 3001L, "web")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "web");
      expect(reader.history.items[0].messageId, 93);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "PHOTO");
      expect(reader.history.items[0].mediaInfo, "640x400");
    }
  }

  public static class ParsePhotoVideoSizeCompatibilityTest extends Test {
    public String label() {
      return "Can parse current videoSize constructors nested in photos";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithPhotoVideoSizes(94, 2001L, 3001L, "anim")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "anim");
      expect(reader.history.items[0].messageId, 94);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "PHOTO");
      expect(reader.history.items[0].mediaInfo, "600x400");
    }
  }

  public static class ParseProgressivePhotoSizeCompatibilityTest extends Test {
    public String label() {
      return "Can parse progressive photoSize constructors nested in photos";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithProgressivePhotoSize(95, 2001L, 3001L, "prog")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "prog");
      expect(reader.history.items[0].messageId, 95);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "PHOTO");
      expect(reader.history.items[0].mediaInfo, "600x400");
    }
  }

  public static class ParseDocumentVideoCodecCompatibilityTest extends Test {
    public String label() {
      return "Can parse current documentAttributeVideo payloads with optional codec fields";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithDocument(
            96,
            2001L,
            3001L,
            "clip",
            documentWithAttributes(new byte[][] {
              documentAttributeVideoWithCodec("avc1"),
              documentAttributeFilename("clip.mp4")
            })
          )
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].text, "clip");
      expect(reader.history.items[0].messageId, 96);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "VIDEO");
      expect(reader.history.items[0].mediaInfo, "clip.mp4 240x320 4KB");
    }
  }

  public static class ParseDocumentCustomEmojiCompatibilityTest extends Test {
    public String label() {
      return "Can parse current documentAttributeCustomEmoji payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessageWithDocument(
            97,
            2001L,
            3001L,
            "emoji",
            documentWithAttributes(new byte[][] {
              documentAttributeCustomEmoji(":wave:", "telegrammicro-emoji"),
              documentAttributeFilename("emoji.webm")
            })
          )
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages, body),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].text, "emoji");
      expect(reader.history.items[0].messageId, 97);
      expect(reader.history.items[0].hasMedia, true);
      expect(reader.history.items[0].mediaLabel, "EMOJI");
      expect(reader.history.items[0].mediaInfo, ":wave: 4KB");
    }
  }

  public static class ParseLegacyHistoryPayloadTest extends Test {
    public String label() {
      return "Can parse legacy messages.Messages payloads for compatibility";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(90, 2001L, 3001L, "hi")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(new RPCResponse(CombinatorIds.messages_messages_legacy, body), reader, DialogListModel.PEER_USER, 3001L), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "hi");
    }
  }

  public static class ParseMessagesSlicePayloadTest extends Test {
    public String label() {
      return "Can parse current messages.messagesSlice payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(0)
        .append_int(2)
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(90, 2001L, 3001L, "hi")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Bob"),
          user(3001L, 0x2222222222222222L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      expect(parser.applyHistory(new RPCResponse(CombinatorIds.messages_messages_slice, body), reader, DialogListModel.PEER_USER, 3001L), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "hi");
    }
  }

  public static class ParseDialogsSliceChannelPayloadTest extends Test {
    public String label() {
      return "Can parse messages.dialogsSlice channel payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(1)
        .append_raw_bytes(vector(new byte[][] {
          channelDialog(4001L, 201, 6)
        }))
        .append_raw_bytes(vector(new byte[][] {
          textMessage(201, peerUser(2001L), peerChannel(4001L), "Broadcast")
        }))
        .append_raw_bytes(vector(new byte[][] {
          channel(4001L, 0x0102030405060708L, "News Feed", "newsfeed")
        }))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Poster")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginDialogsLoad();
      expect(parser.applyDialogs(new RPCResponse(CombinatorIds.messages_dialogs_slice, body), reader), true);
      reader.finishDialogsLoad();

      expect(reader.dialogs.count, 1);
      expect(reader.dialogs.items[0].peerKind, DialogListModel.PEER_CHANNEL);
      expect(reader.dialogs.items[0].peerId, 4001L);
      expect(reader.dialogs.items[0].hasAccessHash, true);
      expect(reader.dialogs.items[0].accessHash, 0x0102030405060708L);
      expect(reader.dialogs.items[0].title, "News Feed");
      expect(reader.dialogs.items[0].preview, "Broadcast");
      expect(reader.dialogs.items[0].unreadCount, 6);
    }
  }

  public static class ParseDialogsPayloadWithCurrentDraftTest extends Test {
    public String label() {
      return "Can parse dialogs payloads that include current draft constructors";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_raw_bytes(vector(new byte[][] {
          dialogWithCurrentDraft(peerUser(1001L), 77, 3, "draft")
        }))
        .append_raw_bytes(vector(new byte[][] {
          textUserMessage(77, 1001L, 1001L, "Hello")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          user(1001L, 0x1122334455667788L, "Alice")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginDialogsLoad();
      expect(parser.applyDialogs(new RPCResponse(CombinatorIds.messages_dialogs, body), reader), true);
      reader.finishDialogsLoad();

      expect(reader.dialogs.count, 1);
      expect(reader.dialogs.items[0].peerId, 1001L);
      expect(reader.dialogs.items[0].preview, "Hello");
      expect(reader.dialogs.items[0].unreadCount, 3);
    }
  }

  public static class ParseChannelHistoryPayloadTest extends Test {
    public String label() {
      return "Can parse messages.channelMessages payloads into bounded history";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(0)
        .append_int(77)
        .append_int(1)
        .append_raw_bytes(vector(new byte[][] {
          textMessage(301, peerUser(2001L), peerChannel(4001L), "Channel hello")
        }))
        .append_raw_bytes(vector(new byte[][] {}))
        .append_raw_bytes(vector(new byte[][] {
          channel(4001L, 0x0102030405060708L, "News Feed", "newsfeed")
        }))
        .append_raw_bytes(vector(new byte[][] {
          user(2001L, 0x1111111111111111L, "Poster")
        }))
        .end();
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.beginHistoryLoad(DialogListModel.PEER_CHANNEL, 4001L);
      expect(parser.applyHistory(new RPCResponse(CombinatorIds.messages_channel_messages, body), reader, DialogListModel.PEER_CHANNEL, 4001L), true);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Poster");
      expect(reader.history.items[0].text, "Channel hello");
      expect(reader.history.items[0].messageId, 301);
      expect(reader.historyPeerKind, DialogListModel.PEER_CHANNEL);
      expect(reader.historyPeerId, 4001L);
      expect(reader.peers.titleOrFallback(DialogListModel.PEER_CHANNEL, 4001L), "News Feed");
    }
  }

  public static class ParseDialogsNotModifiedPayloadTest extends Test {
    public String label() {
      return "Can preserve dialogs on messages.dialogsNotModified";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.appendDialog(DialogListModel.PEER_USER, 1001L, "Alice", "Hello", 1, false);
      expect(parser.applyDialogs(
        new RPCResponse(CombinatorIds.messages_dialogs_not_modified, messagesNotModified(1)),
        reader
      ), true);

      expect(reader.dialogs.count, 1);
      expect(reader.dialogs.items[0].title, "Alice");
    }
  }

  public static class ParseHistoryNotModifiedPayloadTest extends Test {
    public String label() {
      return "Can preserve history on messages.messagesNotModified";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(4, 4);
      ReaderResultParser parser = new ReaderResultParser(
        reader.dialogs.capacity(),
        reader.history.capacity(),
        reader.peers.items.length
      );

      reader.appendDialog(DialogListModel.PEER_USER, 3001L, "Bob", "hi", 0, false);
      reader.selectDialog(0);
      reader.beginHistoryLoad(DialogListModel.PEER_USER, 3001L);
      reader.appendHistoryMessage(90, "Bob", "hi", false, false, false);
      reader.finishHistoryLoad();

      expect(parser.applyHistory(
        new RPCResponse(CombinatorIds.messages_messages_not_modified, messagesNotModified(1)),
        reader,
        DialogListModel.PEER_USER,
        3001L
      ), true);

      expect(reader.history.count, 1);
      expect(reader.history.items[0].text, "hi");
      expect(reader.historyPeerKind, DialogListModel.PEER_USER);
      expect(reader.historyPeerId, 3001L);
    }
  }

  public static class ParseContactsFoundPayloadTest extends Test {
    public String label() {
      return "Can parse contacts.found payloads into the bounded peer directory";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, PeerDirectory.DEFAULT_CAPACITY);
      PeerDirectory peers = new PeerDirectory();

      expect(parser.mergeContactsFound(
        new RPCResponse(CombinatorIds.contacts_found, contactsFoundPayload()),
        peers
      ), true);
      expect(peers.find(DialogListModel.PEER_USER, 1001L) != null, true);
      expect(peers.find(DialogListModel.PEER_CHANNEL, 4001L) != null, true);
      expect(peers.find(DialogListModel.PEER_CHANNEL, 4001L).title, "News Feed");
    }
  }

  public static class ParseUserFullPeerInfoTest extends Test {
    public String label() {
      return "Can parse users.userFull peer info payloads";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, PeerDirectory.DEFAULT_CAPACITY);
      ReaderResultParser.PeerInfoResult info = new ReaderResultParser.PeerInfoResult();

      expect(parser.parsePeerInfo(
        new RPCResponse(CombinatorIds.users_user_full, userFullPayload(1001L, "Telegram bio")),
        DialogListModel.PEER_USER,
        1001L,
        info
      ), true);
      expect(info.peerKind, DialogListModel.PEER_USER);
      expect(info.peerId, 1001L);
      expect(info.aboutKnown, true);
      expect(info.about, "Telegram bio");
      expect(info.participantCountKnown, false);
    }
  }

  public static class ParseChatFullPeerInfoTest extends Test {
    public String label() {
      return "Can parse messages.chatFull group info payloads";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, PeerDirectory.DEFAULT_CAPACITY);
      ReaderResultParser.PeerInfoResult info = new ReaderResultParser.PeerInfoResult();

      expect(parser.parsePeerInfo(
        new RPCResponse(CombinatorIds.messages_chat_full, chatFullPayload(88L, "Group bio", 2)),
        DialogListModel.PEER_CHAT,
        88L,
        info
      ), true);
      expect(info.peerKind, DialogListModel.PEER_CHAT);
      expect(info.peerId, 88L);
      expect(info.about, "Group bio");
      expect(info.participantCountKnown, true);
      expect(info.participantCount, 2);
    }
  }

  public static class ParseChannelFullPeerInfoTest extends Test {
    public String label() {
      return "Can parse messages.chatFull channel info payloads";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, PeerDirectory.DEFAULT_CAPACITY);
      ReaderResultParser.PeerInfoResult info = new ReaderResultParser.PeerInfoResult();

      expect(parser.parsePeerInfo(
        new RPCResponse(CombinatorIds.messages_chat_full, channelFullPayload(4001L, "Channel bio", 1234)),
        DialogListModel.PEER_CHANNEL,
        4001L,
        info
      ), true);
      expect(info.peerKind, DialogListModel.PEER_CHANNEL);
      expect(info.peerId, 4001L);
      expect(info.about, "Channel bio");
      expect(info.participantCountKnown, true);
      expect(info.participantCount, 1234);
    }
  }
}
