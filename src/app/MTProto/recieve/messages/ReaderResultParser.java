package mtproto.recieve.messages;

import media.MediaReference;
import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import reader.DialogListModel;
import reader.MessageListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;
import reader.SecretChatDirectory;
import search.MessageSearchStateModel;

public final class ReaderResultParser {
  public static final int TG_VECTOR_ID = (int)0x1cb5c415L;
  public static final int TG_PEER_USER_ID = (int)0x59511722L;
  public static final int TG_PEER_CHAT_ID = (int)0x36c6019aL;
  public static final int TG_PEER_CHANNEL_ID = (int)0xa2a5371eL;
  public static final int TG_USER_ID = (int)0x020b1422L;
  public static final int TG_USER_EMPTY_ID = (int)0xd3bc4b7aL;
  public static final int TG_USER_FULL_ID = (int)0xa02bc13eL;
  public static final int TG_CHAT_ID = (int)0x41cbf256L;
  public static final int TG_CHAT_EMPTY_ID = (int)0x29562865L;
  public static final int TG_CHAT_FORBIDDEN_ID = (int)0x6592a1a7L;
  public static final int TG_CHAT_FULL_ID = (int)0x2633421bL;
  public static final int TG_CHANNEL_ID = (int)0xfe685355L;
  public static final int TG_CHANNEL_FORBIDDEN_ID = (int)0x17d493d5L;
  public static final int TG_CHANNEL_FULL_ID = (int)0xe4e0b29dL;
  public static final int TG_DIALOG_ID = (int)0xd58a08c6L;
  public static final int TG_PEER_NOTIFY_SETTINGS_ID = (int)0x99622c0cL;
  public static final int TG_PEER_NOTIFY_SETTINGS_LEGACY_ID = (int)0x995c3d14L;
  public static final int TG_DRAFT_MESSAGE_EMPTY_ID = (int)0x1b0c841aL;
  public static final int TG_DRAFT_MESSAGE_ID = (int)0x96eaa5ebL;
  public static final int TG_DRAFT_MESSAGE_LEGACY_ID = (int)0x96e61141L;
  public static final int TG_MESSAGE_ID = (int)0x9815cec8L;
  public static final int TG_MESSAGE_COMPAT_ID = (int)0x3ae56482L;
  public static final int TG_MESSAGE_LEGACY_ID = (int)0x980f8cc8L;
  public static final int TG_MESSAGE_SERVICE_ID = (int)0x7a800e0aL;
  public static final int TG_MESSAGE_EMPTY_ID = (int)0x90a6ca84L;
  public static final int TG_MESSAGE_MEDIA_EMPTY_ID = (int)0x3ded6320L;
  public static final int TG_MESSAGE_MEDIA_PHOTO_ID = (int)0x695150d7L;
  public static final int TG_MESSAGE_MEDIA_DOCUMENT_ID = (int)0x52d8ccd9L;
  public static final int TG_MESSAGE_MEDIA_UNSUPPORTED_ID = (int)0x9f84f49eL;
  public static final int TG_MESSAGE_MEDIA_WEBPAGE_ID = (int)0xddf10c3bL;

  private static final int TG_CHAT_PHOTO_EMPTY_ID = (int)0x37c1011cL;
  private static final int TG_CHAT_PHOTO_ID = (int)0x1c6e1c11L;
  private static final int TG_CHAT_PARTICIPANTS_FORBIDDEN_ID = (int)0x8763d3e1L;
  private static final int TG_CHAT_PARTICIPANTS_ID = (int)0x3cbc93f8L;
  private static final int TG_CHAT_ADMIN_RIGHTS_ID = (int)0x5fb224d5L;
  private static final int TG_CHAT_BANNED_RIGHTS_ID = (int)0x9f120418L;
  private static final int TG_USERNAME_ID = (int)0xb4073647L;
  private static final int TG_PEER_COLOR_ID = (int)0xb54b5acfL;
  private static final int TG_EMOJI_STATUS_EMPTY_ID = (int)0x2de11aaeL;
  private static final int TG_EMOJI_STATUS_ID = (int)0xe7ff068aL;
  private static final int TG_EMOJI_STATUS_COLLECTIBLE_ID = (int)0x7184603bL;
  private static final int TG_USER_PROFILE_PHOTO_EMPTY_ID = (int)0x4f11bae1L;
  private static final int TG_USER_PROFILE_PHOTO_ID = (int)0x82d1f706L;
  private static final int TG_USER_STATUS_EMPTY_ID = (int)0x09d05049L;
  private static final int TG_USER_STATUS_ONLINE_ID = (int)0xedb93949L;
  private static final int TG_USER_STATUS_OFFLINE_ID = (int)0x008c703fL;
  private static final int TG_USER_STATUS_RECENTLY_ID = (int)0x7b197dc8L;
  private static final int TG_USER_STATUS_LAST_WEEK_ID = (int)0x541a1d1aL;
  private static final int TG_USER_STATUS_LAST_MONTH_ID = (int)0x65899777L;
  private static final int TG_NOTIFICATION_SOUND_DEFAULT_ID = (int)0x97e8bebeL;
  private static final int TG_NOTIFICATION_SOUND_NONE_ID = (int)0x6f0c34dfL;
  private static final int TG_NOTIFICATION_SOUND_LOCAL_ID = (int)0x830b9ae4L;
  private static final int TG_NOTIFICATION_SOUND_RINGTONE_ID = (int)0xff6c8049L;
  private static final int TG_NOTIFICATION_SOUND_DEFAULT_LEGACY_ID = (int)0x97e8b51eL;
  private static final int TG_NOTIFICATION_SOUND_NONE_LEGACY_ID = (int)0x6f9393bdL;
  private static final int TG_NOTIFICATION_SOUND_LOCAL_LEGACY_ID = (int)0x83070ff4L;
  private static final int TG_NOTIFICATION_SOUND_RINGTONE_LEGACY_ID = (int)0xff6c7cb9L;
  private static final int TG_RESTRICTION_REASON_ID = (int)0xd072acb4L;
  private static final int TG_MESSAGE_ENTITY_UNKNOWN_ID = (int)0xbb92ba95L;
  private static final int TG_MESSAGE_ENTITY_MENTION_ID = (int)0xfa04579dL;
  private static final int TG_MESSAGE_ENTITY_HASHTAG_ID = (int)0x6f635b0dL;
  private static final int TG_MESSAGE_ENTITY_BOT_COMMAND_ID = (int)0x6cef8ac7L;
  private static final int TG_MESSAGE_ENTITY_URL_ID = (int)0x6ed02538L;
  private static final int TG_MESSAGE_ENTITY_EMAIL_ID = (int)0x64e475c2L;
  private static final int TG_MESSAGE_ENTITY_BOLD_ID = (int)0xbd610bc9L;
  private static final int TG_MESSAGE_ENTITY_ITALIC_ID = (int)0x826f8b60L;
  private static final int TG_MESSAGE_ENTITY_CODE_ID = (int)0x28a20571L;
  private static final int TG_MESSAGE_ENTITY_PRE_ID = (int)0x73924be0L;
  private static final int TG_MESSAGE_ENTITY_TEXT_URL_ID = (int)0x76a6d327L;
  private static final int TG_MESSAGE_ENTITY_MENTION_NAME_ID = (int)0xdc7b1140L;
  private static final int TG_INPUT_MESSAGE_ENTITY_MENTION_NAME_ID = (int)0x208e68c9L;
  private static final int TG_MESSAGE_ENTITY_PHONE_ID = (int)0x9b69e34bL;
  private static final int TG_MESSAGE_ENTITY_CASHTAG_ID = (int)0x4c4e743fL;
  private static final int TG_MESSAGE_ENTITY_UNDERLINE_ID = (int)0x9c4e7e8bL;
  private static final int TG_MESSAGE_ENTITY_STRIKE_ID = (int)0xbf0693d4L;
  private static final int TG_MESSAGE_ENTITY_BLOCKQUOTE_ID = (int)0xf1ccaaacL;
  private static final int TG_MESSAGE_ENTITY_BLOCKQUOTE_OLD_ID = (int)0x020df5d0L;
  private static final int TG_MESSAGE_ENTITY_BANK_CARD_ID = (int)0x761e6af4L;
  private static final int TG_MESSAGE_ENTITY_SPOILER_ID = (int)0x32ca960fL;
  private static final int TG_MESSAGE_ENTITY_CUSTOM_EMOJI_ID = (int)0xc8cf05f8L;
  private static final int TG_MESSAGE_FWD_HEADER_ID = (int)0x4e4df4bbL;
  private static final int TG_MESSAGE_REPLY_HEADER_ID = (int)0x6917560bL;
  private static final int TG_MESSAGE_REPLY_STORY_HEADER_ID = (int)0x0e5af939L;
  private static final int TG_MESSAGE_REPLIES_ID = (int)0x83d60fc2L;
  private static final int TG_MESSAGE_REACTIONS_ID = (int)0x0a339f0bL;
  private static final int TG_REACTION_COUNT_ID = (int)0xa3d1cb80L;
  private static final int TG_MESSAGE_PEER_REACTION_ID = (int)0x8c79b63cL;
  private static final int TG_MESSAGE_REACTOR_ID = (int)0x4ba3a95aL;
  private static final int TG_REACTION_EMPTY_ID = (int)0x79f5d419L;
  private static final int TG_REACTION_EMOJI_ID = (int)0x1b2286b8L;
  private static final int TG_REACTION_CUSTOM_EMOJI_ID = (int)0x8935fc73L;
  private static final int TG_REACTION_PAID_ID = (int)0x523da4ebL;
  private static final int TG_REPLY_KEYBOARD_HIDE_ID = (int)0xa03e5b85L;
  private static final int TG_REPLY_KEYBOARD_FORCE_REPLY_ID = (int)0x86b40b08L;
  private static final int TG_REPLY_KEYBOARD_MARKUP_ID = (int)0x85dd99d1L;
  private static final int TG_REPLY_INLINE_MARKUP_ID = (int)0x48a30254L;
  private static final int TG_KEYBOARD_BUTTON_ROW_ID = (int)0x77608b83L;
  private static final int TG_FACT_CHECK_ID = (int)0xb89bfccfL;
  private static final int TG_SUGGESTED_POST_ID = (int)0x0e8e37e5L;
  private static final int TG_TEXT_EMPTY_ID = (int)0xdc3d824fL;
  private static final int TG_TEXT_PLAIN_ID = (int)0x744694e0L;
  private static final int TG_TEXT_BOLD_ID = (int)0x6724abc4L;
  private static final int TG_TEXT_ITALIC_ID = (int)0xd912a59cL;
  private static final int TG_TEXT_UNDERLINE_ID = (int)0xc12622c4L;
  private static final int TG_TEXT_STRIKE_ID = (int)0x9bf8bb95L;
  private static final int TG_TEXT_FIXED_ID = (int)0x6c3f19b9L;
  private static final int TG_TEXT_URL_ID = (int)0x3c2884c1L;
  private static final int TG_TEXT_EMAIL_ID = (int)0xde5a0dd6L;
  private static final int TG_TEXT_CONCAT_ID = (int)0x7e6260d7L;
  private static final int TG_TEXT_SUBSCRIPT_ID = (int)0xed6a8504L;
  private static final int TG_TEXT_SUPERSCRIPT_ID = (int)0xc7fb5e01L;
  private static final int TG_TEXT_MARKED_ID = (int)0x034b8621L;
  private static final int TG_TEXT_PHONE_ID = (int)0x1ccb966aL;
  private static final int TG_TEXT_IMAGE_ID = (int)0x081ccf4fL;
  private static final int TG_TEXT_ANCHOR_ID = (int)0x35553762L;
  private static final int TG_PAGE_CAPTION_ID = (int)0x6f747657L;
  private static final int TG_PAGE_LIST_ITEM_TEXT_ID = (int)0xb92fb6cdL;
  private static final int TG_PAGE_LIST_ITEM_BLOCKS_ID = (int)0x25e073fcL;
  private static final int TG_PAGE_LIST_ORDERED_ITEM_TEXT_ID = (int)0x5e068047L;
  private static final int TG_PAGE_LIST_ORDERED_ITEM_BLOCKS_ID = (int)0x98dd8936L;
  private static final int TG_PAGE_RELATED_ARTICLE_ID = (int)0xb390dc08L;
  private static final int TG_PAGE_TABLE_CELL_ID = (int)0x34566b6aL;
  private static final int TG_PAGE_TABLE_ROW_ID = (int)0xe0c0c5e5L;
  private static final int TG_PAGE_BLOCK_UNSUPPORTED_ID = (int)0x13567e8aL;
  private static final int TG_PAGE_BLOCK_TITLE_ID = (int)0x70abc3fdL;
  private static final int TG_PAGE_BLOCK_SUBTITLE_ID = (int)0x8ffa9a1fL;
  private static final int TG_PAGE_BLOCK_AUTHOR_DATE_ID = (int)0xbaafe5e0L;
  private static final int TG_PAGE_BLOCK_HEADER_ID = (int)0xbfd064ecL;
  private static final int TG_PAGE_BLOCK_SUBHEADER_ID = (int)0xf12bb6e1L;
  private static final int TG_PAGE_BLOCK_PARAGRAPH_ID = (int)0x467a0766L;
  private static final int TG_PAGE_BLOCK_PREFORMATTED_ID = (int)0xc070d93eL;
  private static final int TG_PAGE_BLOCK_FOOTER_ID = (int)0x48870999L;
  private static final int TG_PAGE_BLOCK_DIVIDER_ID = (int)0xdb20b188L;
  private static final int TG_PAGE_BLOCK_ANCHOR_ID = (int)0xce0d37b0L;
  private static final int TG_PAGE_BLOCK_LIST_ID = (int)0xe4e88011L;
  private static final int TG_PAGE_BLOCK_BLOCKQUOTE_ID = (int)0x263d7c26L;
  private static final int TG_PAGE_BLOCK_PULLQUOTE_ID = (int)0x4f4456d3L;
  private static final int TG_PAGE_BLOCK_PHOTO_ID = (int)0x1759c560L;
  private static final int TG_PAGE_BLOCK_VIDEO_ID = (int)0x7c8fe7b6L;
  private static final int TG_PAGE_BLOCK_COVER_ID = (int)0x39f23300L;
  private static final int TG_PAGE_BLOCK_EMBED_ID = (int)0xa8718dc5L;
  private static final int TG_PAGE_BLOCK_EMBED_POST_ID = (int)0xf259a80bL;
  private static final int TG_PAGE_BLOCK_COLLAGE_ID = (int)0x65a0fa4dL;
  private static final int TG_PAGE_BLOCK_SLIDESHOW_ID = (int)0x031f9590L;
  private static final int TG_PAGE_BLOCK_AUDIO_ID = (int)0x804361eaL;
  private static final int TG_PAGE_BLOCK_KICKER_ID = (int)0x1e148390L;
  private static final int TG_PAGE_BLOCK_TABLE_ID = (int)0xbf4dea82L;
  private static final int TG_PAGE_BLOCK_ORDERED_LIST_ID = (int)0x9a8ae1e1L;
  private static final int TG_PAGE_BLOCK_DETAILS_ID = (int)0x76768bedL;
  private static final int TG_PAGE_BLOCK_RELATED_ARTICLES_ID = (int)0x16115a96L;
  private static final int TG_PAGE_BLOCK_MAP_ID = (int)0xa44f3ef6L;
  private static final int TG_PHOTO_EMPTY_ID = (int)0x2331b22dL;
  private static final int TG_PHOTO_ID = (int)0xfb197a65L;
  private static final int TG_PHOTO_SIZE_EMPTY_ID = (int)0x0e17e23cL;
  private static final int TG_PHOTO_SIZE_ID = (int)0x77bfb61bL;
  private static final int TG_PHOTO_SIZE_OLD_ID = (int)0x75c78e60L;
  private static final int TG_PHOTO_CACHED_SIZE_ID = (int)0xe9a734faL;
  private static final int TG_PHOTO_CACHED_SIZE_OLD_ID = (int)0x021e1ad6L;
  private static final int TG_PHOTO_STRIPPED_SIZE_ID = (int)0xe0b0bc2eL;
  private static final int TG_PHOTO_SIZE_PROGRESSIVE_ID = (int)0xfa3efb95L;
  private static final int TG_PHOTO_PATH_SIZE_ID = (int)0xd8214d41L;
  private static final int TG_FILE_LOCATION_ID = (int)0x53d69076L;
  private static final int TG_FILE_LOCATION_UNAVAILABLE_ID = (int)0x7c596b46L;
  private static final int TG_VIDEO_SIZE_ID = (int)0xde33b094L;
  private static final int TG_VIDEO_SIZE_EMOJI_MARKUP_ID = (int)0xf85c413cL;
  private static final int TG_VIDEO_SIZE_STICKER_MARKUP_ID = (int)0x0da082feL;
  private static final int TG_INPUT_STICKER_SET_EMPTY_ID = (int)0xffb62b95L;
  private static final int TG_INPUT_STICKER_SET_ID_ID = (int)0x9de7a269L;
  private static final int TG_INPUT_STICKER_SET_SHORT_NAME_ID = (int)0x861cc8a0L;
  private static final int TG_INPUT_STICKER_SET_ANIMATED_EMOJI_ID = (int)0x028703c8L;
  private static final int TG_INPUT_STICKER_SET_DICE_ID = (int)0xe67f520eL;
  private static final int TG_INPUT_STICKER_SET_ANIMATED_EMOJI_ANIMATIONS_ID = (int)0x0cde3739L;
  private static final int TG_INPUT_STICKER_SET_PREMIUM_GIFTS_ID = (int)0xc88b3b02L;
  private static final int TG_INPUT_STICKER_SET_EMOJI_GENERIC_ANIMATIONS_ID = (int)0x04c4d4ceL;
  private static final int TG_INPUT_STICKER_SET_EMOJI_DEFAULT_STATUSES_ID = (int)0x29d0f5eeL;
  private static final int TG_INPUT_STICKER_SET_EMOJI_DEFAULT_TOPIC_ICONS_ID = (int)0x44c1f8e9L;
  private static final int TG_INPUT_STICKER_SET_EMOJI_CHANNEL_DEFAULT_STATUSES_ID = (int)0x49748553L;
  private static final int TG_INPUT_STICKER_SET_TON_GIFTS_ID = (int)0x1cf671a0L;
  private static final int TG_DOCUMENT_EMPTY_ID = (int)0x36f8c871L;
  private static final int TG_DOCUMENT_ID = (int)0x8fd4c4d8L;
  private static final int TG_ENCRYPTED_MESSAGE_ID = (int)0xed18c118L;
  private static final int TG_ENCRYPTED_MESSAGE_SERVICE_ID = (int)0x23734b06L;
  private static final int TG_ENCRYPTED_FILE_EMPTY_ID = (int)0xc21f497eL;
  private static final int TG_ENCRYPTED_FILE_ID = (int)0xa8008cd8L;
  private static final int TG_WEBPAGE_EMPTY_ID = (int)0x211a1788L;
  private static final int TG_WEBPAGE_PENDING_ID = (int)0xb0d13e47L;
  private static final int TG_WEBPAGE_ID = (int)0xe89c45b2L;
  private static final int TG_WEBPAGE_NOT_MODIFIED_ID = (int)0x7311ca11L;
  private static final int TG_PAGE_ID = (int)0x98657f0dL;
  private static final int TG_GEO_POINT_EMPTY_ID = (int)0x1117dd5fL;
  private static final int TG_GEO_POINT_ID = (int)0xb2a2f663L;
  private static final int TG_FORUM_TOPIC_ID = (int)0x71676a89L;
  private static final int TG_MASK_COORDS_ID = (int)0xaed6dbb2L;
  private static final int TG_DOCUMENT_ATTRIBUTE_IMAGE_SIZE_ID = (int)0x6c37c15cL;
  private static final int TG_DOCUMENT_ATTRIBUTE_ANIMATED_ID = (int)0x11b58939L;
  private static final int TG_DOCUMENT_ATTRIBUTE_STICKER_ID = (int)0x6319d612L;
  private static final int TG_DOCUMENT_ATTRIBUTE_VIDEO_ID = (int)0x43c57c48L;
  private static final int TG_DOCUMENT_ATTRIBUTE_AUDIO_ID = (int)0x9852f9c6L;
  private static final int TG_DOCUMENT_ATTRIBUTE_FILENAME_ID = (int)0x15590068L;
  private static final int TG_DOCUMENT_ATTRIBUTE_HAS_STICKERS_ID = (int)0x9801d2f7L;
  private static final int TG_DOCUMENT_ATTRIBUTE_CUSTOM_EMOJI_ID = (int)0xfd149899L;
  private static final int TG_MESSAGE_ACTION_EMPTY_ID = (int)0xbfe9a518L;
  private static final int TG_MESSAGE_ACTION_CHAT_CREATE_ID = (int)0xbd47cbadL;
  private static final int TG_MESSAGE_ACTION_CHAT_EDIT_TITLE_ID = (int)0xb5a1ce5aL;
  private static final int TG_MESSAGE_ACTION_CHAT_EDIT_PHOTO_ID = (int)0x7fcb13a8L;
  private static final int TG_MESSAGE_ACTION_CHAT_DELETE_PHOTO_ID = (int)0x95e3fbefL;
  private static final int TG_MESSAGE_ACTION_CHAT_ADD_USER_ID = (int)0x15cefd00L;
  private static final int TG_MESSAGE_ACTION_CHAT_DELETE_USER_ID = (int)0xa43f30ccL;
  private static final int TG_MESSAGE_ACTION_CHAT_JOINED_BY_LINK_ID = (int)0x031224c3L;
  private static final int TG_MESSAGE_ACTION_CHANNEL_CREATE_ID = (int)0x95d2ac92L;
  private static final int TG_MESSAGE_ACTION_CHAT_MIGRATE_TO_ID = (int)0xe1037f92L;
  private static final int TG_MESSAGE_ACTION_CHANNEL_MIGRATE_FROM_ID = (int)0xea3948e9L;
  private static final int TG_MESSAGE_ACTION_PIN_MESSAGE_ID = (int)0x94bd38edL;
  private static final int TG_MESSAGE_ACTION_HISTORY_CLEAR_ID = (int)0x9fbab604L;
  private static final int TG_MESSAGE_ACTION_SCREENSHOT_TAKEN_ID = (int)0x4792929bL;
  private static final int TG_MESSAGE_ACTION_CONTACT_SIGN_UP_ID = (int)0xf3f25f76L;
  private static final int TG_MESSAGE_ACTION_GEO_PROXIMITY_REACHED_ID = (int)0x98e0d697L;
  private static final int TG_MESSAGE_ACTION_GROUP_CALL_ID = (int)0x7a0d7f42L;
  private static final int TG_MESSAGE_ACTION_INVITE_TO_GROUP_CALL_ID = (int)0x502f92f7L;
  private static final int TG_MESSAGE_ACTION_SET_MESSAGES_TTL_ID = (int)0x3c134d7bL;
  private static final int TG_MESSAGE_ACTION_GROUP_CALL_SCHEDULED_ID = (int)0xb3a07661L;
  private static final int TG_MESSAGE_ACTION_SET_CHAT_THEME_ID = (int)0xb91bbd3aL;
  private static final int TG_MESSAGE_ACTION_CHAT_JOINED_BY_REQUEST_ID = (int)0xebbca3cbL;
  private static final int TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ME_ID = (int)0x47dd8079L;
  private static final int TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ID = (int)0xb4c38cb5L;
  private static final int TG_MESSAGE_ACTION_TOPIC_CREATE_ID = (int)0x0d999256L;
  private static final int TG_MESSAGE_ACTION_TOPIC_EDIT_ID = (int)0xc0944820L;
  private static final int TG_MESSAGE_ACTION_SUGGEST_PROFILE_PHOTO_ID = (int)0x57de635eL;
  private static final int TL_BOOL_TRUE_ID = (int)0x997275b5L;
  private static final int TL_BOOL_FALSE_ID = (int)0xbc799737L;

  private final DialogScratch[] dialogScratch;
  private final MessageScratch[] messageScratch;
  private final PeerDirectory scratchPeers;
  private final DialogScratch parsedDialogScratch = new DialogScratch();
  private final MessageScratch parsedMessageScratch = new MessageScratch();
  private int parsedMessageCount = 0;
  private int parsedMessageTotalCount = 0;
  private boolean parsedMessagesNotModified = false;
  private String lastError = "";

  public static final class LiveMessageEvent {
    public static final int KIND_NONE = 0;
    public static final int KIND_APPEND = 1;
    public static final int KIND_EDIT = 2;
    public static final int KIND_DELETE = 3;
    public static final int KIND_READ = 4;
    public static final int KIND_MESSAGE_ID = 5;
    public static final int KIND_READ_OUTBOX = 6;

    public int kind = KIND_NONE;
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public int senderPeerKind = DialogListModel.PEER_UNKNOWN;
    public long senderPeerId = 0;
    public int messageId = 0;
    public long randomId = 0;
    public int pts = 0;
    public int ptsCount = 0;
    public int date = 0;
    public int unreadCount = 0;
    public String text = "";
    public String mediaLabel = "";
    public String mediaInfo = "";
    public final MediaReference mediaRef = new MediaReference();
    public boolean outgoing = false;
    public boolean hasMedia = false;

    public void clear() {
      kind = KIND_NONE;
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      senderPeerKind = DialogListModel.PEER_UNKNOWN;
      senderPeerId = 0;
      messageId = 0;
      randomId = 0;
      pts = 0;
      ptsCount = 0;
      date = 0;
      unreadCount = 0;
      text = "";
      mediaLabel = "";
      mediaInfo = "";
      mediaRef.clear();
      outgoing = false;
      hasMedia = false;
    }
  }

  public static final class LiveUpdateBatch {
    public final LiveMessageEvent[] events;
    public final SecretChatResultParser.Result[] secretEvents;
    public final PeerDirectory peers;
    public int count = 0;
    public int secretCount = 0;
    public int date = 0;
    public int seq = 0;
    public int seqStart = 0;

    public LiveUpdateBatch(int eventCapacity, int peerCapacity) {
      int i;

      if (eventCapacity <= 0 || peerCapacity <= 0) {
        throw new IllegalArgumentException("Live update batch capacities must be positive.");
      }

      events = new LiveMessageEvent[eventCapacity];
      for (i = 0; i < events.length; ++i) {
        events[i] = new LiveMessageEvent();
      }
      secretEvents = new SecretChatResultParser.Result[Math.min(SecretChatDirectory.DEFAULT_CAPACITY, eventCapacity)];
      for (i = 0; i < secretEvents.length; ++i) {
        secretEvents[i] = new SecretChatResultParser.Result();
      }
      peers = new PeerDirectory(peerCapacity);
    }

    public void clear() {
      int i;

      count = 0;
      secretCount = 0;
      date = 0;
      seq = 0;
      seqStart = 0;
      peers.clear();
      for (i = 0; i < events.length; ++i) {
        events[i].clear();
      }
      for (i = 0; i < secretEvents.length; ++i) {
        secretEvents[i].clear();
      }
    }

    LiveMessageEvent addEvent() {
      LiveMessageEvent event;

      if (count >= events.length) {
        return null;
      }

      event = events[count];
      count += 1;
      event.clear();
      return event;
    }

    SecretChatResultParser.Result addSecretEvent() {
      SecretChatResultParser.Result event;

      if (secretCount >= secretEvents.length) {
        return null;
      }

      event = secretEvents[secretCount];
      secretCount += 1;
      event.clear();
      return event;
    }
  }

  public static final class ChannelDifferenceBatch {
    public static final int TYPE_EMPTY = 1;
    public static final int TYPE_FULL = 2;
    public static final int TYPE_TOO_LONG = 3;

    public final LiveUpdateBatch updates;
    public int type = TYPE_EMPTY;
    public long peerId = 0;
    public int pts = 0;
    public int timeout = 0;
    public boolean isFinal = true;

    public ChannelDifferenceBatch(int eventCapacity, int peerCapacity) {
      updates = new LiveUpdateBatch(eventCapacity, peerCapacity);
    }

    public void clear() {
      updates.clear();
      type = TYPE_EMPTY;
      peerId = 0;
      pts = 0;
      timeout = 0;
      isFinal = true;
    }
  }

  public static final class PeerInfoResult {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public boolean aboutKnown = false;
    public String about = "";
    public boolean participantCountKnown = false;
    public int participantCount = 0;

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      aboutKnown = false;
      about = "";
      participantCountKnown = false;
      participantCount = 0;
    }
  }

  public ReaderResultParser(int dialogCapacity, int historyCapacity, int peerCapacity) {
    int i;
    int messageCapacity = dialogCapacity > historyCapacity ? dialogCapacity : historyCapacity;

    if (dialogCapacity <= 0 || historyCapacity <= 0 || peerCapacity <= 0) {
      throw new IllegalArgumentException("Parser capacities must be positive.");
    }

    dialogScratch = new DialogScratch[dialogCapacity];
    for (i = 0; i < dialogScratch.length; ++i) {
      dialogScratch[i] = new DialogScratch();
    }

    messageScratch = new MessageScratch[messageCapacity];
    for (i = 0; i < messageScratch.length; ++i) {
      messageScratch[i] = new MessageScratch();
    }

    scratchPeers = new PeerDirectory(peerCapacity);
  }

  public String lastError() {
    return lastError;
  }

  public boolean parseShortUpdateEnvelope(int outerType, byte[] body, LiveMessageEvent out) {
    TLReader reader;
    int innerType;
    MessageScratch message;
    boolean messageUpdate;

    if (out == null) {
      lastError = "Live update target is missing.";
      return false;
    }
    out.clear();
    if (body == null) {
      lastError = "Live update payload is missing.";
      return false;
    }
    if (outerType != CombinatorIds.update_short) {
      lastError = "Unexpected short-update wrapper: 0x" + Integer.toHexString(outerType);
      return false;
    }

    try {
      resetScratch();
      reader = new TLReader(body);
      innerType = reader.readInt();

      messageUpdate = innerType == CombinatorIds.update_new_message ||
        innerType == CombinatorIds.update_new_channel_message ||
        innerType == CombinatorIds.update_edit_message ||
        innerType == CombinatorIds.update_edit_channel_message;
      if (!messageUpdate &&
          innerType != CombinatorIds.update_read_history_inbox &&
          innerType != CombinatorIds.update_read_history_outbox &&
          innerType != CombinatorIds.update_read_channel_inbox &&
          innerType != CombinatorIds.update_read_channel_outbox) {
        lastError = "Unsupported wrapped update type: 0x" + Integer.toHexString(innerType);
        return false;
      }

      if (!messageUpdate) {
        parseWrappedReadUpdate(reader, innerType, out);
        out.date = reader.readInt();
        if (!reader.atEnd()) {
          throw new IllegalArgumentException("Wrapped read update did not fully parse.");
        }
        lastError = "";
        return true;
      }

      parsedMessageScratch.clear();
      parseMessageObject(reader, parsedMessageScratch);
      out.pts = reader.readInt();
      out.ptsCount = reader.readInt();
      out.date = reader.readInt();
      if (!reader.atEnd()) {
        throw new IllegalArgumentException("Wrapped short update did not fully parse.");
      }

      message = parsedMessageScratch;
      out.kind =
        (innerType == CombinatorIds.update_edit_message ||
         innerType == CombinatorIds.update_edit_channel_message)
          ? LiveMessageEvent.KIND_EDIT
          : LiveMessageEvent.KIND_APPEND;
      out.peerKind = message.peer.peerKind;
      out.peerId = message.peer.peerId;
      out.senderPeerKind = message.hasFromPeer ? message.fromPeer.peerKind : message.peer.peerKind;
      out.senderPeerId = message.hasFromPeer ? message.fromPeer.peerId : message.peer.peerId;
      out.messageId = message.id;
      out.text = message.previewText();
      out.mediaLabel = message.mediaLabel;
      out.mediaInfo = message.mediaInfo;
      out.outgoing = message.outgoing;
      out.hasMedia = message.hasMedia;
      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  private void parseWrappedReadUpdate(TLReader reader, int innerType, LiveMessageEvent out) {
    PeerRef peer = parsedMessageScratch.peer;
    int flags;

    peer.clear();
    if (innerType == CombinatorIds.update_read_history_inbox) {
      flags = reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      parsePeerRef(reader, peer);
      if ((flags & (1 << 1)) != 0) {
        reader.readInt();
      }
      out.kind = LiveMessageEvent.KIND_READ;
      out.peerKind = peer.peerKind;
      out.peerId = peer.peerId;
      out.messageId = reader.readInt();
      out.unreadCount = reader.readInt();
      out.pts = reader.readInt();
      out.ptsCount = reader.readInt();
      if (out.messageId < 0 || out.unreadCount < 0) {
        throw new IllegalArgumentException("Wrapped inbox read update has invalid counters.");
      }
      return;
    }
    if (innerType == CombinatorIds.update_read_history_outbox) {
      parsePeerRef(reader, peer);
      out.kind = LiveMessageEvent.KIND_READ_OUTBOX;
      out.peerKind = peer.peerKind;
      out.peerId = peer.peerId;
      out.messageId = reader.readInt();
      out.pts = reader.readInt();
      out.ptsCount = reader.readInt();
      if (out.messageId < 0) {
        throw new IllegalArgumentException("Wrapped outbox read update has a negative max id.");
      }
      return;
    }

    flags = innerType == CombinatorIds.update_read_channel_inbox
      ? reader.readInt()
      : 0;
    if ((flags & (1 << 0)) != 0) {
      reader.readInt();
    }
    out.kind = innerType == CombinatorIds.update_read_channel_inbox
      ? LiveMessageEvent.KIND_READ
      : LiveMessageEvent.KIND_READ_OUTBOX;
    out.peerKind = DialogListModel.PEER_CHANNEL;
    out.peerId = reader.readLong();
    out.messageId = reader.readInt();
    if (innerType == CombinatorIds.update_read_channel_inbox) {
      out.unreadCount = reader.readInt();
      out.pts = reader.readInt();
    }
    if (out.peerId == 0 || out.messageId < 0 || out.unreadCount < 0) {
      throw new IllegalArgumentException("Wrapped channel read update has invalid counters.");
    }
  }

  public boolean parseShortSecretUpdate(int outerType, byte[] body, SecretChatResultParser.Result out) {
    TLReader reader;
    int innerType;
    int outerDate;

    if (out == null) {
      lastError = "Secret update target is missing.";
      return false;
    }
    out.clear();
    if (body == null) {
      lastError = "Secret update payload is missing.";
      return false;
    }
    if (outerType != CombinatorIds.update_short) {
      lastError = "Unexpected short-update wrapper: 0x" + Integer.toHexString(outerType);
      return false;
    }

    try {
      reader = new TLReader(body);
      innerType = reader.readInt();
      if (innerType != CombinatorIds.update_encryption) {
        lastError = "Unsupported wrapped secret update type: 0x" + Integer.toHexString(innerType);
        return false;
      }
      parseUpdateEncryption(reader, out);
      outerDate = reader.readInt();
      if (out.date == 0) {
        out.date = outerDate;
      }
      if (!reader.atEnd()) {
        throw new IllegalArgumentException("Wrapped secret update did not fully parse.");
      }
      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean parseUpdateCollection(int outerType, byte[] body, LiveUpdateBatch out) {
    TLReader reader;
    int updateCount;
    int userCount;
    int chatCount;
    int i;

    if (out == null) {
      lastError = "Live update batch target is missing.";
      return false;
    }
    out.clear();
    if (body == null) {
      lastError = "Live update collection payload is missing.";
      return false;
    }
    if (outerType != CombinatorIds.updates &&
        outerType != CombinatorIds.updates_combined) {
      lastError = "Unexpected updates collection: 0x" + Integer.toHexString(outerType);
      return false;
    }

    try {
      resetScratch();
      reader = new TLReader(body);
      updateCount = reader.readVectorCount();
      for (i = 0; i < updateCount; ++i) {
        parseUpdateCollectionEntry(reader, out);
      }

      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, out.peers);
      }

      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, out.peers);
      }

      out.date = reader.readInt();
      if (outerType == CombinatorIds.updates_combined) {
        out.seqStart = reader.readInt();
      }
      out.seq = reader.readInt();
      if (!reader.atEnd()) {
        throw new IllegalArgumentException("Updates collection did not fully parse.");
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean parseDifferenceResult(int outerType, byte[] body, LiveUpdateBatch out) {
    TLReader reader;
    int newMessageCount;
    int encryptedCount;
    int updateCount;
    int chatCount;
    int userCount;
    int i;

    if (out == null) {
      lastError = "Difference batch target is missing.";
      return false;
    }
    out.clear();
    if (body == null) {
      lastError = "Difference payload is missing.";
      return false;
    }
    if (outerType != CombinatorIds.updates_difference &&
        outerType != CombinatorIds.updates_difference_slice) {
      lastError = "Unexpected updates.difference payload: 0x" + Integer.toHexString(outerType);
      return false;
    }

    try {
      resetScratch();
      reader = new TLReader(body);

      newMessageCount = reader.readVectorCount();
      for (i = 0; i < newMessageCount; ++i) {
        parsedMessageScratch.clear();
        parseMessageObject(reader, parsedMessageScratch);
        fillLiveMessageEvent(
          requireLiveEvent(out),
          LiveMessageEvent.KIND_APPEND,
          parsedMessageScratch,
          0,
          0
        );
      }

      encryptedCount = reader.readVectorCount();
      for (i = 0; i < encryptedCount; ++i) {
        parseEncryptedDifferenceMessage(reader, requireSecretEvent(out));
      }

      updateCount = reader.readVectorCount();
      for (i = 0; i < updateCount; ++i) {
        parseUpdateCollectionEntry(reader, out);
      }

      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, out.peers);
      }

      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, out.peers);
      }

      if (reader.readInt() != CombinatorIds.updates_state) {
        throw new IllegalArgumentException("updates.Difference does not end with updates.State.");
      }
      reader.readInt();
      reader.readInt();
      out.date = reader.readInt();
      out.seq = reader.readInt();
      reader.readInt();
      if (!reader.atEnd()) {
        throw new IllegalArgumentException("updates.Difference payload did not fully parse.");
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean parseChannelDifference(int outerType, byte[] body, long expectedPeerId, ChannelDifferenceBatch out) {
    TLReader reader;
    int flags;
    int messageCount;
    int updateCount;
    int chatCount;
    int userCount;
    int i;

    if (out == null) {
      lastError = "Channel difference target is missing.";
      return false;
    }
    out.clear();
    if (body == null) {
      lastError = "Channel difference payload is missing.";
      return false;
    }
    if (expectedPeerId == 0) {
      lastError = "Channel difference peer id is missing.";
      return false;
    }
    if (outerType != CombinatorIds.updates_channel_difference_empty &&
        outerType != CombinatorIds.updates_channel_difference &&
        outerType != CombinatorIds.updates_channel_difference_too_long) {
      lastError = "Unexpected channel difference payload: 0x" + Integer.toHexString(outerType);
      return false;
    }

    try {
      resetScratch();
      reader = new TLReader(body);
      flags = reader.readInt();
      out.peerId = expectedPeerId;
      out.isFinal = (flags & 1) != 0;
      if ((flags & (1 << 1)) != 0) {
        out.timeout = reader.readInt();
      }

      if (outerType == CombinatorIds.updates_channel_difference_empty) {
        out.type = ChannelDifferenceBatch.TYPE_EMPTY;
        out.pts = reader.readInt();
        if (!reader.atEnd()) {
          throw new IllegalArgumentException("updates.channelDifferenceEmpty did not fully parse.");
        }
        lastError = "";
        return true;
      }

      if (outerType == CombinatorIds.updates_channel_difference_too_long) {
        out.type = ChannelDifferenceBatch.TYPE_TOO_LONG;
        parsedDialogScratch.clear();
        parseDialogItem(reader, parsedDialogScratch);
        if (parsedDialogScratch.peer.peerKind != DialogListModel.PEER_CHANNEL ||
            parsedDialogScratch.peer.peerId != expectedPeerId) {
          throw new IllegalArgumentException("Channel difference dialog peer mismatch.");
        }
        if (parsedDialogScratch.pts <= 0) {
          throw new IllegalArgumentException("Channel difference dialog did not include pts.");
        }
        out.pts = parsedDialogScratch.pts;
        messageCount = parseMessagesVector(reader);
        chatCount = reader.readVectorCount();
        for (i = 0; i < chatCount; ++i) {
          parseChatToPeers(reader, out.updates.peers);
        }
        userCount = reader.readVectorCount();
        for (i = 0; i < userCount; ++i) {
          parseUserToPeers(reader, out.updates.peers);
        }
        if (!reader.atEnd()) {
          throw new IllegalArgumentException("updates.channelDifferenceTooLong did not fully parse.");
        }
        if (messageCount > 0) {
          LiveMessageEvent event = requireLiveEvent(out.updates);
          event.kind = LiveMessageEvent.KIND_READ;
          event.peerKind = DialogListModel.PEER_CHANNEL;
          event.peerId = expectedPeerId;
          event.unreadCount = parsedDialogScratch.unreadCount;
        }
        lastError = "";
        return true;
      }

      out.type = ChannelDifferenceBatch.TYPE_FULL;
      out.pts = reader.readInt();
      messageCount = reader.readVectorCount();
      for (i = 0; i < messageCount; ++i) {
        parsedMessageScratch.clear();
        parseMessageObject(reader, parsedMessageScratch);
        fillLiveMessageEvent(
          requireLiveEvent(out.updates),
          LiveMessageEvent.KIND_APPEND,
          parsedMessageScratch,
          out.pts,
          0
        );
      }
      updateCount = reader.readVectorCount();
      for (i = 0; i < updateCount; ++i) {
        parseUpdateCollectionEntry(reader, out.updates);
      }
      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, out.updates.peers);
      }
      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, out.updates.peers);
      }
      if (!reader.atEnd()) {
        throw new IllegalArgumentException("updates.channelDifference did not fully parse.");
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean mergeContactsFound(RPCResponse response, PeerDirectory peers) {
    TLReader reader;
    int chatCount;
    int userCount;
    int i;

    if (response == null || peers == null || response.body == null) {
      lastError = "contacts.Found payload is missing.";
      return false;
    }
    if (response.type != CombinatorIds.contacts_found) {
      lastError = "Unexpected contacts.Found constructor: 0x" + Integer.toHexString(response.type);
      return false;
    }

    try {
      reader = new TLReader(response.body);
      skipPeerRefsVector(reader);
      skipPeerRefsVector(reader);

      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, peers);
      }

      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, peers);
      }

      if (!reader.atEnd()) {
        throw new IllegalArgumentException("contacts.Found payload did not fully parse.");
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean parsePeerInfo(
    RPCResponse response,
    int expectedPeerKind,
    long expectedPeerId,
    PeerInfoResult out
  ) {
    TLReader reader;
    int constructor;
    int flags;
    int flags2;
    long peerId;

    if (response == null || out == null || response.body == null) {
      lastError = "Peer info payload is missing.";
      return false;
    }
    if (expectedPeerKind == DialogListModel.PEER_UNKNOWN || expectedPeerId == 0) {
      lastError = "Expected peer identity is missing.";
      return false;
    }

    out.clear();

    try {
      reader = new TLReader(response.body);
      constructor = reader.readInt();

      if (response.type == CombinatorIds.users_user_full) {
        if (expectedPeerKind != DialogListModel.PEER_USER) {
          lastError = "users.userFull did not match the requested peer kind.";
          return false;
        }
        if (constructor != TG_USER_FULL_ID) {
          throw new IllegalArgumentException("Unexpected UserFull constructor.");
        }

        flags = reader.readInt();
        flags2 = reader.readInt();
        peerId = reader.readLong();
        if (peerId != expectedPeerId) {
          throw new IllegalArgumentException("users.userFull peer mismatch.");
        }

        out.peerKind = expectedPeerKind;
        out.peerId = peerId;
        if ((flags & (1 << 1)) != 0) {
          out.about = reader.readDisplayString();
          out.aboutKnown = true;
        }
        lastError = "";
        return true;
      }

      if (response.type != CombinatorIds.messages_chat_full) {
        lastError = "Unexpected peer info constructor: 0x" + Integer.toHexString(response.type);
        return false;
      }

      if (constructor == TG_CHAT_FULL_ID) {
        if (expectedPeerKind != DialogListModel.PEER_CHAT) {
          lastError = "chatFull did not match the requested peer kind.";
          return false;
        }

        flags = reader.readInt();
        peerId = reader.readLong();
        if (peerId != expectedPeerId) {
          throw new IllegalArgumentException("chatFull peer mismatch.");
        }

        out.peerKind = expectedPeerKind;
        out.peerId = peerId;
        out.about = reader.readDisplayString();
        out.aboutKnown = true;
        out.participantCount = parseChatParticipantsCount(reader);
        out.participantCountKnown = true;
        lastError = "";
        return true;
      }

      if (constructor != TG_CHANNEL_FULL_ID) {
        throw new IllegalArgumentException("Unexpected ChatFull constructor.");
      }
      if (expectedPeerKind != DialogListModel.PEER_CHANNEL) {
        lastError = "channelFull did not match the requested peer kind.";
        return false;
      }

      flags = reader.readInt();
      flags2 = reader.readInt();
      peerId = reader.readLong();
      if (peerId != expectedPeerId) {
        throw new IllegalArgumentException("channelFull peer mismatch.");
      }

      out.peerKind = expectedPeerKind;
      out.peerId = peerId;
      out.about = reader.readDisplayString();
      out.aboutKnown = true;
      if ((flags & (1 << 0)) != 0) {
        out.participantCount = reader.readInt();
        out.participantCountKnown = true;
      }
      if ((flags & (1 << 1)) != 0) {
        reader.readInt();
      }
      if ((flags & (1 << 2)) != 0) {
        reader.readInt();
        reader.readInt();
      }
      if ((flags & (1 << 13)) != 0) {
        reader.readInt();
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean applyDialogs(RPCResponse response, ReaderStateModel state) {
    TLReader reader;
    int constructor;
    int rawDialogCount;
    int keptMessageCount;
    int chatCount;
    int userCount;
    int i;

    if (response == null || state == null || response.body == null) {
      lastError = "Dialogs payload is missing.";
      return false;
    }

    try {
      resetScratch();
      scratchPeers.clear();
      reader = new TLReader(response.body);
      constructor = response.type;

      if (constructor != CombinatorIds.messages_dialogs &&
          constructor != CombinatorIds.messages_dialogs_slice &&
          constructor != CombinatorIds.messages_dialogs_not_modified) {
        lastError = "Unexpected dialogs constructor: 0x" + Integer.toHexString(constructor);
        return false;
      }

      if (constructor == CombinatorIds.messages_dialogs_not_modified) {
        reader.readInt();
        if (!reader.atEnd()) {
          throw new IllegalArgumentException("Dialogs-not-modified payload did not fully parse.");
        }
        lastError = "";
        return true;
      }

      if (constructor == CombinatorIds.messages_dialogs_slice) {
        reader.readInt();
      }

      rawDialogCount = parseDialogsVector(reader);
      keptMessageCount = parseMessagesVector(reader);

      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, scratchPeers);
      }

      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, scratchPeers);
      }

      if (!reader.atEnd()) {
        throw new IllegalArgumentException("Dialogs payload did not fully parse.");
      }

      if (!state.peers.copyFrom(scratchPeers)) {
        lastError = "Peer directory capacity exceeded during dialog load.";
        return false;
      }
      for (i = 0; i < rawDialogCount; ++i) {
        DialogScratch dialog = dialogScratch[i];
        MessageScratch previewMessage = findMessage(dialog.topMessageId, keptMessageCount);
        PeerDirectory.Item peer = state.peers.find(dialog.peer.peerKind, dialog.peer.peerId);
        String preview = previewMessage != null ? previewMessage.previewText() : "";
        boolean hasMedia = previewMessage != null && previewMessage.hasMedia;
        long accessHash = peer != null && peer.hasAccessHash ? peer.accessHash : 0;
        boolean hasAccessHash = peer != null && peer.hasAccessHash;
        String title = state.peers.titleOrFallback(dialog.peer.peerKind, dialog.peer.peerId);

        if (!state.appendDialog(
            dialog.peer.peerKind,
            dialog.peer.peerId,
            accessHash,
            hasAccessHash,
            dialog.topMessageId,
            dialog.readInboxMaxId,
            dialog.readOutboxMaxId,
            title,
            preview,
            dialog.unreadCount,
            hasMedia)) {
          lastError = "Dialog list capacity exceeded.";
          return false;
        }
        if (dialog.peer.peerKind == DialogListModel.PEER_CHANNEL && dialog.pts > 0) {
          state.channelSync.seedFromDialog(dialog.peer.peerId, dialog.pts);
        }
      }

      lastError = "";
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  public boolean applyHistory(RPCResponse response, ReaderStateModel state, int peerKind, long peerId) {
    int i;

    if (response == null || state == null || response.body == null) {
      lastError = "History payload is missing.";
      return false;
    }

    if (!parseMessagesPayload(
        response,
        state.peers,
        true,
        state.history.capacity(),
        "History")) {
      return false;
    }
    if (parsedMessagesNotModified) {
      state.historyPeerKind = peerKind;
      state.historyPeerId = peerId;
      lastError = "";
      return true;
    }
    if (!state.peers.copyFrom(scratchPeers)) {
      lastError = "Peer directory capacity exceeded during history load.";
      return false;
    }
    for (i = 0; i < parsedMessageCount; ++i) {
      MessageScratch message = messageScratch[i];
      PeerRef senderPeer = message.hasFromPeer ? message.fromPeer : message.peer;
      String sender = state.peers.titleOrFallback(senderPeer.peerKind, senderPeer.peerId);

      if (!state.appendHistoryMessage(
          message.id,
          sender,
          message.previewText(),
          message.outgoing,
          false,
          message.hasMedia,
          message.mediaLabel,
          message.mediaInfo,
          message.mediaRef,
          message.date)) {
        lastError = "History capacity exceeded.";
        return false;
      }
    }

    state.historyPeerKind = peerKind;
    state.historyPeerId = peerId;
    lastError = "";
    return true;
  }

  public boolean applyMessageSearch(
    RPCResponse response,
    ReaderStateModel state,
    MessageSearchStateModel search
  ) {
    int i;

    if (response == null || state == null || search == null || response.body == null) {
      lastError = "Search payload is missing.";
      return false;
    }
    if (!search.loading) {
      lastError = "Search result arrived without a pending bounded query.";
      return false;
    }
    if (!parseMessagesPayload(
        response,
        state.peers,
        false,
        search.results.capacity(),
        "Search")) {
      return false;
    }
    for (i = 0; i < parsedMessageCount; ++i) {
      if (messageScratch[i].id <= 0) {
        lastError = "Search result contains an invalid message id.";
        return false;
      }
    }
    if (!state.peers.copyFrom(scratchPeers)) {
      lastError = "Peer directory capacity exceeded during message search.";
      return false;
    }
    for (i = 0; i < parsedMessageCount; ++i) {
      MessageScratch message = messageScratch[i];
      PeerRef senderPeer = message.hasFromPeer ? message.fromPeer : message.peer;
      String sender = state.peers.titleOrFallback(senderPeer.peerKind, senderPeer.peerId);

      if (!search.appendResult(
          message.id,
          sender,
          message.previewText(),
          message.outgoing,
          message.hasMedia,
          message.mediaLabel,
          message.mediaInfo,
          message.date)) {
        lastError = "Search result page could not be retained boundedly.";
        return false;
      }
    }
    search.finish(parsedMessageTotalCount);
    lastError = "";
    return true;
  }

  private boolean parseMessagesPayload(
    RPCResponse response,
    PeerDirectory seedPeers,
    boolean allowNotModified,
    int maxMessageCount,
    String purpose
  ) {
    TLReader reader;
    int constructor;
    int chatCount;
    int userCount;
    int i;

    parsedMessageCount = 0;
    parsedMessageTotalCount = 0;
    parsedMessagesNotModified = false;
    try {
      resetScratch();
      if (!scratchPeers.copyFrom(seedPeers)) {
        lastError = "Could not seed " + purpose.toLowerCase() + " peer cache.";
        return false;
      }

      reader = new TLReader(response.body);
      constructor = response.type;
      if (constructor == CombinatorIds.messages_messages_not_modified) {
        if (!allowNotModified) {
          lastError = "Unexpected search constructor: 0x" + Integer.toHexString(constructor);
          return false;
        }
        reader.readInt();
        if (!reader.atEnd()) {
          throw new IllegalArgumentException("History-not-modified payload did not fully parse.");
        }
        parsedMessagesNotModified = true;
        return true;
      }

      if (constructor == CombinatorIds.messages_messages_slice) {
        int flags = reader.readInt();
        parsedMessageTotalCount = reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readInt();
        }
        if ((flags & (1 << 2)) != 0) {
          reader.readInt();
        }
        if ((flags & (1 << 3)) != 0) {
          skipSearchPostsFlood(reader);
        }
        if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3))) != 0) {
          throw new IllegalArgumentException("messages.messagesSlice flags are not supported.");
        }
      } else if (constructor == CombinatorIds.messages_messages_slice_legacy) {
        parsedMessageTotalCount = reader.readInt();
      } else if (constructor == CombinatorIds.messages_channel_messages) {
        int flags = reader.readInt();
        reader.readInt();
        parsedMessageTotalCount = reader.readInt();
        if ((flags & (1 << 2)) != 0) {
          reader.readInt();
        }
        if ((flags & ~((1 << 1) | (1 << 2))) != 0) {
          throw new IllegalArgumentException("messages.channelMessages flags are not supported.");
        }
      } else if (constructor != CombinatorIds.messages_messages &&
                 constructor != CombinatorIds.messages_messages_legacy) {
        lastError = "Unexpected " + purpose.toLowerCase() + " constructor: 0x" +
          Integer.toHexString(constructor);
        return false;
      }

      if (parsedMessageTotalCount < 0) {
        throw new IllegalArgumentException("Message result total must not be negative.");
      }
      parsedMessageCount = parseMessagesVector(reader, maxMessageCount, purpose);
      if (parsedMessageTotalCount < parsedMessageCount) {
        parsedMessageTotalCount = parsedMessageCount;
      }

      if (constructor == CombinatorIds.messages_messages ||
          constructor == CombinatorIds.messages_messages_slice ||
          constructor == CombinatorIds.messages_channel_messages) {
        int topicCount = reader.readVectorCount();
        for (i = 0; i < topicCount; ++i) {
          skipForumTopic(reader);
        }
      }

      chatCount = reader.readVectorCount();
      for (i = 0; i < chatCount; ++i) {
        parseChatToPeers(reader, scratchPeers);
      }
      userCount = reader.readVectorCount();
      for (i = 0; i < userCount; ++i) {
        parseUserToPeers(reader, scratchPeers);
      }
      if (!reader.atEnd()) {
        throw new IllegalArgumentException(purpose + " payload did not fully parse.");
      }
      return true;
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }
  }

  private void resetScratch() {
    int i;

    for (i = 0; i < dialogScratch.length; ++i) {
      dialogScratch[i].clear();
    }
    for (i = 0; i < messageScratch.length; ++i) {
      messageScratch[i].clear();
    }
  }

  private int parseDialogsVector(TLReader reader) {
    int count = reader.readVectorCount();
    int kept = 0;
    int i;

    for (i = 0; i < count; ++i) {
      parsedDialogScratch.clear();
      parseDialogItem(reader, parsedDialogScratch);
      if (kept < dialogScratch.length) {
        dialogScratch[kept].copyFrom(parsedDialogScratch);
        kept += 1;
      }
    }
    return kept;
  }

  private int parseMessagesVector(TLReader reader) {
    return parseMessagesVector(reader, Integer.MAX_VALUE, "Message");
  }

  private int parseMessagesVector(TLReader reader, int maximumCount, String purpose) {
    int count = reader.readVectorCount();
    int kept = 0;
    int i;

    if (maximumCount <= 0 || count > maximumCount) {
      throw new IllegalArgumentException(
        purpose + " result page exceeds its bounded capacity."
      );
    }

    for (i = 0; i < count; ++i) {
      int messageOffset = reader.offset();

      parsedMessageScratch.clear();
      try {
        parseMessageObject(reader, parsedMessageScratch);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
          "Message[" + Integer.toString(i) +
          "] at 0x" + Integer.toHexString(messageOffset) +
          " bytes[" + reader.hexWindow(messageOffset, 24) + "]: " +
          e.getMessage());
      }
      if (kept < messageScratch.length) {
        messageScratch[kept].copyFrom(parsedMessageScratch);
        kept += 1;
      }
    }
    return kept;
  }

  private void parseUpdateCollectionEntry(TLReader reader, LiveUpdateBatch out) {
    int constructor = reader.readInt();
    int i;
    int start;
    int count;
    int pts;
    int ptsCount;
    int flags;
    int maxId;
    LiveMessageEvent event;
    PeerRef peer;

    if (constructor == CombinatorIds.update_encryption) {
      parseUpdateEncryption(reader, requireSecretEvent(out));
      return;
    }

    if (constructor == CombinatorIds.update_new_message ||
        constructor == CombinatorIds.update_new_channel_message ||
        constructor == CombinatorIds.update_edit_message ||
        constructor == CombinatorIds.update_edit_channel_message) {
      parsedMessageScratch.clear();
      parseMessageObject(reader, parsedMessageScratch);
      pts = reader.readInt();
      ptsCount = reader.readInt();
      event = requireLiveEvent(out);
      fillLiveMessageEvent(
        event,
        (constructor == CombinatorIds.update_edit_message ||
         constructor == CombinatorIds.update_edit_channel_message)
          ? LiveMessageEvent.KIND_EDIT
          : LiveMessageEvent.KIND_APPEND,
        parsedMessageScratch,
        pts,
        ptsCount
      );
      return;
    }

    if (constructor == CombinatorIds.update_delete_messages) {
      start = out.count;
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        event = requireLiveEvent(out);
        event.kind = LiveMessageEvent.KIND_DELETE;
        event.messageId = reader.readInt();
      }
      pts = reader.readInt();
      ptsCount = reader.readInt();
      for (i = start; i < out.count; ++i) {
        out.events[i].pts = pts;
        out.events[i].ptsCount = ptsCount;
      }
      return;
    }

    if (constructor == CombinatorIds.update_delete_channel_messages) {
      start = out.count;
      peer = new PeerRef();
      peer.peerKind = DialogListModel.PEER_CHANNEL;
      peer.peerId = reader.readLong();
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        event = requireLiveEvent(out);
        event.kind = LiveMessageEvent.KIND_DELETE;
        event.peerKind = peer.peerKind;
        event.peerId = peer.peerId;
        event.messageId = reader.readInt();
      }
      pts = reader.readInt();
      ptsCount = reader.readInt();
      for (i = start; i < out.count; ++i) {
        out.events[i].pts = pts;
        out.events[i].ptsCount = ptsCount;
      }
      return;
    }

    if (constructor == CombinatorIds.update_read_history_inbox) {
      flags = reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      peer = new PeerRef();
      parsePeerRef(reader, peer);
      if ((flags & (1 << 1)) != 0) {
        reader.readInt();
      }
      maxId = reader.readInt();
      event = requireLiveEvent(out);
      event.kind = LiveMessageEvent.KIND_READ;
      event.peerKind = peer.peerKind;
      event.peerId = peer.peerId;
      event.messageId = maxId;
      event.unreadCount = reader.readInt();
      if (event.messageId < 0 || event.unreadCount < 0) {
        throw new IllegalArgumentException("Inbox read update has invalid counters.");
      }
      event.pts = reader.readInt();
      event.ptsCount = reader.readInt();
      return;
    }

    if (constructor == CombinatorIds.update_read_channel_inbox) {
      flags = reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      event = requireLiveEvent(out);
      event.kind = LiveMessageEvent.KIND_READ;
      event.peerKind = DialogListModel.PEER_CHANNEL;
      event.peerId = reader.readLong();
      event.messageId = reader.readInt();
      event.unreadCount = reader.readInt();
      if (event.peerId == 0 || event.messageId < 0 || event.unreadCount < 0) {
        throw new IllegalArgumentException("Channel inbox read update has invalid counters.");
      }
      event.pts = reader.readInt();
      event.ptsCount = 0;
      return;
    }

    if (constructor == CombinatorIds.update_read_history_outbox) {
      peer = new PeerRef();
      parsePeerRef(reader, peer);
      event = requireLiveEvent(out);
      event.kind = LiveMessageEvent.KIND_READ_OUTBOX;
      event.peerKind = peer.peerKind;
      event.peerId = peer.peerId;
      event.messageId = reader.readInt();
      if (event.messageId < 0) {
        throw new IllegalArgumentException("Outbox read update has a negative max id.");
      }
      event.pts = reader.readInt();
      event.ptsCount = reader.readInt();
      return;
    }

    if (constructor == CombinatorIds.update_read_channel_outbox) {
      event = requireLiveEvent(out);
      event.kind = LiveMessageEvent.KIND_READ_OUTBOX;
      event.peerKind = DialogListModel.PEER_CHANNEL;
      event.peerId = reader.readLong();
      event.messageId = reader.readInt();
      if (event.peerId == 0 || event.messageId < 0) {
        throw new IllegalArgumentException("Channel outbox read update has invalid identity.");
      }
      return;
    }

    if (constructor == CombinatorIds.update_message_id) {
      event = requireLiveEvent(out);
      event.kind = LiveMessageEvent.KIND_MESSAGE_ID;
      event.messageId = reader.readInt();
      event.randomId = reader.readLong();
      return;
    }

    throw new IllegalArgumentException(
      "Unsupported updates vector item: 0x" + Integer.toHexString(constructor));
  }

  private void parseUpdateEncryption(TLReader reader, SecretChatResultParser.Result out) {
    SecretChatResultParser parser = new SecretChatResultParser();
    int constructor = reader.readInt();
    int date;

    if (!parser.parseBody(constructor, reader, out)) {
      throw new IllegalArgumentException(parser.lastError());
    }
    date = reader.readInt();
    if (out.date == 0) {
      out.date = date;
    }
  }

  private void parseEncryptedDifferenceMessage(TLReader reader, SecretChatResultParser.Result out) {
    int constructor = reader.readInt();

    if (constructor != TG_ENCRYPTED_MESSAGE_ID &&
        constructor != TG_ENCRYPTED_MESSAGE_SERVICE_ID) {
      throw new IllegalArgumentException(
        "Unsupported encrypted difference message constructor: 0x" +
        Integer.toHexString(constructor));
    }

    out.state = SecretChatResultParser.STATE_ACTIVITY;
    out.service = constructor == TG_ENCRYPTED_MESSAGE_SERVICE_ID;
    out.randomId = reader.readLong();
    out.chatId = reader.readInt();
    out.date = reader.readInt();
    reader.skipBytes();
    if (!out.service) {
      skipEncryptedFile(reader);
    }
  }

  private void skipEncryptedFile(TLReader reader) {
    int constructor = reader.readInt();

    if (constructor == TG_ENCRYPTED_FILE_EMPTY_ID) {
      return;
    }
    if (constructor == TG_ENCRYPTED_FILE_ID) {
      reader.readLong();
      reader.readLong();
      reader.readLong();
      reader.readInt();
      reader.readInt();
      return;
    }

    throw new IllegalArgumentException(
      "Unsupported encrypted file constructor: 0x" +
      Integer.toHexString(constructor));
  }

  private LiveMessageEvent requireLiveEvent(LiveUpdateBatch out) {
    LiveMessageEvent event = out.addEvent();

    if (event == null) {
      throw new IllegalArgumentException("Live update batch capacity exceeded.");
    }
    return event;
  }

  private SecretChatResultParser.Result requireSecretEvent(LiveUpdateBatch out) {
    SecretChatResultParser.Result event = out.addSecretEvent();

    if (event == null) {
      throw new IllegalArgumentException("Live secret update batch capacity exceeded.");
    }
    return event;
  }

  private void fillLiveMessageEvent(
    LiveMessageEvent event,
    int kind,
    MessageScratch message,
    int pts,
    int ptsCount
  ) {
    event.kind = kind;
    event.peerKind = message.peer.peerKind;
    event.peerId = message.peer.peerId;
    event.senderPeerKind = message.hasFromPeer ? message.fromPeer.peerKind : message.peer.peerKind;
    event.senderPeerId = message.hasFromPeer ? message.fromPeer.peerId : message.peer.peerId;
    event.messageId = message.id;
    event.pts = pts;
    event.ptsCount = ptsCount;
    event.date = message.date;
    event.text = message.previewText();
    event.mediaLabel = message.mediaLabel;
    event.mediaInfo = message.mediaInfo;
    event.mediaRef.copyFrom(message.mediaRef);
    event.outgoing = message.outgoing;
    event.hasMedia = message.hasMedia;
  }

  private MessageScratch findMessage(int messageId, int count) {
    int i;

    for (i = 0; i < count; ++i) {
      if (messageScratch[i].id == messageId) {
        return messageScratch[i];
      }
    }
    return null;
  }

  private void parseDialogItem(TLReader reader, DialogScratch out) {
    int constructor = reader.readInt();
    int flags;

    if (constructor != TG_DIALOG_ID) {
      throw new IllegalArgumentException("Expected dialog.");
    }

    flags = reader.readInt();
    parsePeerRef(reader, out.peer);
    out.topMessageId = reader.readInt();
    out.readInboxMaxId = reader.readInt();
    out.readOutboxMaxId = reader.readInt();
    out.unreadCount = reader.readInt();
    if (out.readInboxMaxId < 0 || out.readOutboxMaxId < 0 || out.unreadCount < 0) {
      throw new IllegalArgumentException("Dialog read counters must not be negative.");
    }
    reader.readInt();
    reader.readInt();
    skipPeerNotifySettings(reader);

    if ((flags & (1 << 0)) != 0) {
      out.pts = reader.readInt();
    }
    if ((flags & (1 << 1)) != 0) {
      skipDraftMessage(reader);
    }
    if ((flags & (1 << 4)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 5)) != 0) {
      reader.readInt();
    }

    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5) | (1 << 6))) != 0) {
      throw new IllegalArgumentException("Unsupported dialog flags.");
    }
  }

  private void parsePeerRef(TLReader reader, PeerRef out) {
    int constructor = reader.readInt();
    long peerId = reader.readLong();

    if (constructor == TG_PEER_USER_ID) {
      out.peerKind = DialogListModel.PEER_USER;
    } else if (constructor == TG_PEER_CHAT_ID) {
      out.peerKind = DialogListModel.PEER_CHAT;
    } else if (constructor == TG_PEER_CHANNEL_ID) {
      out.peerKind = DialogListModel.PEER_CHANNEL;
    } else {
      throw new IllegalArgumentException("Unsupported peer reference.");
    }

    out.peerId = peerId;
  }

  private void skipPeerRefsVector(TLReader reader) {
    int count = reader.readVectorCount();
    int i;

    for (i = 0; i < count; ++i) {
      parsePeerRef(reader, new PeerRef());
    }
  }

  private int parseChatParticipantsCount(TLReader reader) {
    int constructor = reader.readInt();
    int count;
    int flags;
    int i;

    if (constructor == TG_CHAT_PARTICIPANTS_ID) {
      reader.readLong();
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipChatParticipant(reader);
      }
      reader.readInt();
      return count;
    }
    if (constructor == TG_CHAT_PARTICIPANTS_FORBIDDEN_ID) {
      flags = reader.readInt();
      reader.readLong();
      if ((flags & 1) != 0) {
        skipChatParticipant(reader);
      }
      return 0;
    }
    throw new IllegalArgumentException("Unsupported chat participants constructor.");
  }

  private void skipChatParticipant(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    switch (constructor) {
      case (int)0x38e79fdeL:
        flags = reader.readInt();
        reader.readLong();
        reader.readLong();
        reader.readInt();
        if ((flags & 1) != 0) {
          reader.readDisplayString();
        }
        return;
      case (int)0xe1f867b8L:
        flags = reader.readInt();
        reader.readLong();
        if ((flags & 1) != 0) {
          reader.readDisplayString();
        }
        return;
      case (int)0x360d5d2L:
        flags = reader.readInt();
        reader.readLong();
        reader.readLong();
        reader.readInt();
        if ((flags & 1) != 0) {
          reader.readDisplayString();
        }
        return;
      default:
        throw new IllegalArgumentException("Unsupported chat participant constructor.");
    }
  }

  private void parseUserToPeers(TLReader reader, PeerDirectory peers) {
    int constructor = reader.readInt();
    int flags;
    int flags2;
    long userId;
    long accessHash = 0;
    boolean hasAccessHash = false;
    boolean minAccessHash = false;
    String firstName = "";
    String lastName = "";
    String username = "";
    String phone = "";
    String title;

    if (constructor == TG_USER_EMPTY_ID) {
      peers.upsertIdentity(DialogListModel.PEER_USER, reader.readLong(), 0, false, false, "DELETED", "");
      return;
    }
    if (constructor != TG_USER_ID) {
      throw new IllegalArgumentException("Unsupported user constructor.");
    }

    flags = reader.readInt();
    flags2 = reader.readInt();
    userId = reader.readLong();

    if ((flags & (1 << 0)) != 0) {
      accessHash = reader.readLong();
      hasAccessHash = true;
    }
    if ((flags & (1 << 1)) != 0) {
      firstName = reader.readDisplayString();
    }
    if ((flags & (1 << 2)) != 0) {
      lastName = reader.readDisplayString();
    }
    if ((flags & (1 << 3)) != 0) {
      username = reader.readDisplayString();
    }
    if ((flags & (1 << 4)) != 0) {
      phone = reader.readDisplayString();
    }
    if ((flags & (1 << 5)) != 0) {
      skipUserProfilePhoto(reader);
    }
    if ((flags & (1 << 6)) != 0) {
      skipUserStatus(reader);
    }
    if ((flags & (1 << 14)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 18)) != 0) {
      skipRestrictionReasons(reader);
    }
    if ((flags & (1 << 19)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 22)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 30)) != 0) {
      skipEmojiStatus(reader);
    }
    if ((flags2 & (1 << 0)) != 0) {
      int count = reader.readVectorCount();
      int i;
      for (i = 0; i < count; ++i) {
        skipUsername(reader);
      }
    }
    if ((flags2 & (1 << 5)) != 0) {
      reader.readInt();
    }
    if ((flags2 & (1 << 8)) != 0) {
      skipPeerColor(reader);
    }
    if ((flags2 & (1 << 9)) != 0) {
      skipPeerColor(reader);
    }
    if ((flags2 & (1 << 12)) != 0) {
      reader.readInt();
    }
    if ((flags2 & (1 << 14)) != 0) {
      reader.readLong();
    }
    if ((flags2 & (1 << 15)) != 0) {
      reader.readLong();
    }
    if ((flags2 & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5) |
                    (1 << 8) | (1 << 9) | (1 << 10) | (1 << 11) | (1 << 12) |
                    (1 << 13) | (1 << 14) | (1 << 15))) != 0) {
      throw new IllegalArgumentException("Unsupported user flags2.");
    }

    if (username.length() > 0) {
      title = username;
    } else {
      title = joinNames(firstName, lastName, "USER");
    }
    minAccessHash = ((flags & (1 << 20)) != 0) && (((flags & (1 << 4)) == 0) || phone.length() > 0);
    peers.upsertIdentity(DialogListModel.PEER_USER, userId, accessHash, hasAccessHash, minAccessHash, title, username);
  }

  private void parseChatToPeers(TLReader reader, PeerDirectory peers) {
    int constructor = reader.readInt();
    int flags;
    int flags2;
    long chatId;
    long accessHash = 0;
    String title = "";
    String username = "";

    if (constructor == TG_CHAT_EMPTY_ID) {
      peers.upsertIdentity(DialogListModel.PEER_CHAT, reader.readLong(), 0, false, false, "GROUP", "");
      return;
    }
    if (constructor == TG_CHAT_FORBIDDEN_ID) {
      chatId = reader.readLong();
      title = reader.readDisplayString();
      peers.upsertIdentity(DialogListModel.PEER_CHAT, chatId, 0, false, false, title, "");
      return;
    }
    if (constructor == TG_CHANNEL_FORBIDDEN_ID) {
      flags = reader.readInt();
      chatId = reader.readLong();
      accessHash = reader.readLong();
      title = reader.readDisplayString();
      if ((flags & (1 << 16)) != 0) {
        reader.readInt();
      }
      peers.upsertIdentity(DialogListModel.PEER_CHANNEL, chatId, accessHash, true, false, title, "");
      return;
    }
    if (constructor == TG_CHANNEL_ID) {
      flags = reader.readInt();
      flags2 = reader.readInt();
      chatId = reader.readLong();
      if ((flags & (1 << 13)) != 0) {
        accessHash = reader.readLong();
      }
      title = reader.readDisplayString();
      if ((flags & (1 << 6)) != 0) {
        username = reader.readDisplayString();
      }
      skipChatPhoto(reader);
      reader.readInt();
      if ((flags & (1 << 9)) != 0) {
        skipRestrictionReasons(reader);
      }
      if ((flags & (1 << 14)) != 0) {
        skipChatAdminRights(reader);
      }
      if ((flags & (1 << 15)) != 0) {
        skipChatBannedRights(reader);
      }
      if ((flags & (1 << 18)) != 0) {
        skipChatBannedRights(reader);
      }
      if ((flags & (1 << 17)) != 0) {
        reader.readInt();
      }
      if ((flags2 & (1 << 0)) != 0) {
        int count = reader.readVectorCount();
        int i;
        for (i = 0; i < count; ++i) {
          skipUsername(reader);
        }
      }
      if ((flags2 & (1 << 4)) != 0) {
        reader.readInt();
      }
      if ((flags2 & (1 << 7)) != 0) {
        skipPeerColor(reader);
      }
      if ((flags2 & (1 << 8)) != 0) {
        skipPeerColor(reader);
      }
      if ((flags2 & (1 << 9)) != 0) {
        skipEmojiStatus(reader);
      }
      if ((flags2 & (1 << 10)) != 0) {
        reader.readInt();
      }
      if ((flags2 & (1 << 11)) != 0) {
        reader.readInt();
      }
      if ((flags2 & (1 << 13)) != 0) {
        reader.readLong();
      }
      if ((flags2 & (1 << 14)) != 0) {
        reader.readLong();
      }
      if ((flags2 & (1 << 18)) != 0) {
        reader.readLong();
      }
      peers.upsertIdentity(
        DialogListModel.PEER_CHANNEL,
        chatId,
        accessHash,
        (flags & (1 << 13)) != 0,
        (flags & (1 << 12)) != 0,
        title,
        username
      );
      return;
    }
    if (constructor != TG_CHAT_ID) {
      throw new IllegalArgumentException("Unsupported chat constructor.");
    }

    flags = reader.readInt();
    chatId = reader.readLong();
    title = reader.readDisplayString();
    skipChatPhoto(reader);
    reader.readInt();
    reader.readInt();
    reader.readInt();
    if ((flags & (1 << 6)) != 0) {
      reader.readInt();
      reader.readLong();
      reader.readLong();
    }
    if ((flags & (1 << 14)) != 0) {
      skipChatAdminRights(reader);
    }
    if ((flags & (1 << 18)) != 0) {
      skipChatBannedRights(reader);
    }

    peers.upsertIdentity(DialogListModel.PEER_CHAT, chatId, 0, false, false, title, "");
  }

  private void parseMessageObject(TLReader reader, MessageScratch out) {
    int constructor = reader.readInt();

    if (constructor == TG_MESSAGE_EMPTY_ID) {
      int flags = reader.readInt();
      reader.readInt();
      if ((flags & 1) != 0) {
        PeerRef ignored = new PeerRef();
        parsePeerRef(reader, ignored);
      }
      return;
    }
    if (constructor == TG_MESSAGE_ID ||
        constructor == TG_MESSAGE_COMPAT_ID ||
        constructor == TG_MESSAGE_LEGACY_ID) {
      parseMessageCommon(reader, out, false);
      return;
    }
    if (constructor == TG_MESSAGE_SERVICE_ID) {
      parseMessageCommon(reader, out, true);
      return;
    }
    throw new IllegalArgumentException(
      "Unsupported message constructor: 0x" + Integer.toHexString(constructor));
  }

  private void parseMessageCommon(TLReader reader, MessageScratch out, boolean service) {
    int flags = reader.readInt();
    int flags2 = service ? 0 : reader.readInt();

    out.id = reader.readInt();
    out.outgoing = (flags & (1 << 1)) != 0;
    if ((flags & (1 << 8)) != 0) {
      parsePeerRef(reader, out.fromPeer);
      out.hasFromPeer = true;
    }
    if ((flags & (1 << 29)) != 0) {
      reader.readInt();
    }
    if (!service && (flags2 & (1 << 12)) != 0) {
      reader.readDisplayString();
    }
    parsePeerRef(reader, out.peer);
    if ((flags & (1 << 28)) != 0) {
      PeerRef ignoredSaved = new PeerRef();
      parsePeerRef(reader, ignoredSaved);
    }
    if ((flags & (1 << 2)) != 0) {
      skipMessageFwdHeader(reader);
    }
    if ((flags & (1 << 11)) != 0) {
      reader.readLong();
    }
    if (!service && (flags2 & (1 << 0)) != 0) {
      reader.readLong();
    }
    if ((flags & (1 << 3)) != 0) {
      skipMessageReplyHeader(reader);
    }
    out.date = reader.readInt();
    if (out.date < 0) {
      throw new IllegalArgumentException("Message date must not be negative.");
    }

    if (service) {
      if ((flags & (1 << 20)) != 0) {
        skipMessageReactions(reader);
      }
      if ((flags & (1 << 25)) != 0) {
        reader.readInt();
      }
      out.text = parseMessageActionText(reader);
      return;
    }

    out.text = reader.readDisplayString();
    if ((flags & (1 << 9)) != 0) {
      try {
        parseMediaItem(reader, out);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "media", e);
      }
    }
    if ((flags & (1 << 6)) != 0) {
      try {
        skipReplyMarkup(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "reply_markup", e);
      }
    }
    if ((flags & (1 << 7)) != 0) {
      try {
        skipMessageEntities(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "entities", e);
      }
    }
    if ((flags & (1 << 10)) != 0) {
      try {
        reader.readInt();
        reader.readInt();
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "views_forwards", e);
      }
    }
    if ((flags & (1 << 23)) != 0) {
      try {
        skipMessageReplies(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "replies", e);
      }
    }
    if ((flags & (1 << 15)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 16)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 17)) != 0) {
      reader.readLong();
    }
    if ((flags & (1 << 20)) != 0) {
      try {
        skipMessageReactions(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "reactions", e);
      }
    }
    if ((flags & (1 << 22)) != 0) {
      try {
        skipRestrictionReasons(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "restriction_reason", e);
      }
    }
    if ((flags & (1 << 25)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 30)) != 0) {
      reader.readInt();
    }
    if ((flags2 & (1 << 2)) != 0) {
      reader.readLong();
    }
    if ((flags2 & (1 << 3)) != 0) {
      try {
        skipFactCheck(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "factcheck", e);
      }
    }
    if ((flags2 & (1 << 5)) != 0) {
      reader.readInt();
    }
    if ((flags2 & (1 << 6)) != 0) {
      reader.readLong();
    }
    if ((flags2 & (1 << 7)) != 0) {
      try {
        skipSuggestedPost(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "suggested_post", e);
      }
    }
    if ((flags2 & (1 << 10)) != 0) {
      reader.readInt();
    }
    if ((flags2 & (1 << 11)) != 0) {
      reader.readDisplayString();
    }
    if ((flags2 & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) |
                    (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8) | (1 << 9) |
                    (1 << 10) | (1 << 11) | (1 << 12))) != 0) {
      throw new IllegalArgumentException("Unsupported message flags2.");
    }
  }

  private void parseMediaItem(TLReader reader, MessageScratch out) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_MESSAGE_MEDIA_EMPTY_ID) {
      return;
    }
    if (constructor == TG_MESSAGE_MEDIA_UNSUPPORTED_ID) {
      out.hasMedia = true;
      out.mediaLabel = "DOCUMENT";
      out.mediaInfo = "";
      return;
    }
    if (constructor == TG_MESSAGE_MEDIA_PHOTO_ID) {
      flags = reader.readInt();
      out.hasMedia = true;
      out.mediaLabel = "PHOTO";
      if ((flags & (1 << 0)) != 0) {
        try {
          out.mediaInfo = parsePhoto(reader, out.mediaRef);
        } catch (IllegalArgumentException e) {
          throw stageError(reader, "messageMediaPhoto.photo", e);
        }
      }
      if ((flags & (1 << 2)) != 0) {
        reader.readInt();
      }
      return;
    }
    if (constructor == TG_MESSAGE_MEDIA_DOCUMENT_ID) {
      DocumentMediaInfo documentInfo = new DocumentMediaInfo();

      flags = reader.readInt();
      out.hasMedia = true;
      out.mediaLabel =
        (flags & (1 << 8)) != 0 ? "VOICE" :
        ((flags & (1 << 6)) != 0 || (flags & (1 << 7)) != 0 ? "VIDEO" : "DOCUMENT");
      if ((flags & (1 << 0)) != 0) {
        try {
          parseDocument(reader, documentInfo, out.mediaRef);
        } catch (IllegalArgumentException e) {
          throw stageError(reader, "messageMediaDocument.document", e);
        }
      }
      if ((flags & (1 << 5)) != 0) {
        int count = reader.readVectorCount();
        int i;
        for (i = 0; i < count; ++i) {
          try {
            skipDocument(reader);
          } catch (IllegalArgumentException e) {
            throw stageError(reader, "messageMediaDocument.alt_documents[" + Integer.toString(i) + "]", e);
          }
        }
      }
      if ((flags & (1 << 9)) != 0) {
        try {
          skipPhoto(reader);
        } catch (IllegalArgumentException e) {
          throw stageError(reader, "messageMediaDocument.video_cover", e);
        }
      }
      if ((flags & (1 << 10)) != 0) {
        reader.readInt();
      }
      if ((flags & (1 << 2)) != 0) {
        reader.readInt();
      }
      out.mediaInfo = documentInfo.summary();
      if (documentInfo.label.length() > 0) {
        out.mediaLabel = documentInfo.label;
      }
      return;
    }
    if (constructor == TG_MESSAGE_MEDIA_WEBPAGE_ID) {
      flags = reader.readInt();
      out.hasMedia = true;
      out.mediaLabel = "LINK";
      try {
        out.mediaInfo = parseWebpageInfo(reader);
      } catch (IllegalArgumentException e) {
        throw stageError(reader, "messageMediaWebPage.webpage", e);
      }
      if ((flags & ~(1 << 0)) != 0 && flags != 0) {
        return;
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported media constructor.");
  }

  private IllegalArgumentException stageError(TLReader reader, String stage, IllegalArgumentException cause) {
    return new IllegalArgumentException(
      stage + " @0x" + Integer.toHexString(reader.offset()) +
      " bytes[" + reader.hexWindow(reader.offset(), 24) + "]: " +
      cause.getMessage());
  }

  private void skipUserProfilePhoto(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_USER_PROFILE_PHOTO_EMPTY_ID) {
      return;
    }
    if (constructor != TG_USER_PROFILE_PHOTO_ID) {
      throw new IllegalArgumentException("Unsupported user profile photo.");
    }

    flags = reader.readInt();
    reader.readLong();
    if ((flags & (1 << 1)) != 0) {
      reader.skipBytes();
    }
    reader.readInt();
  }

  private void skipUserStatus(TLReader reader) {
    int constructor = reader.readInt();

    if (constructor == TG_USER_STATUS_EMPTY_ID) {
      return;
    }
    if (constructor == TG_USER_STATUS_ONLINE_ID || constructor == TG_USER_STATUS_OFFLINE_ID) {
      reader.readInt();
      return;
    }
    if (constructor == TG_USER_STATUS_RECENTLY_ID ||
        constructor == TG_USER_STATUS_LAST_WEEK_ID ||
        constructor == TG_USER_STATUS_LAST_MONTH_ID) {
      int flags = reader.readInt();
      if ((flags & ~(1 << 0)) != 0) {
        throw new IllegalArgumentException("Unsupported user status flags.");
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported user status.");
  }

  private void skipRestrictionReasons(TLReader reader) {
    int count = reader.readVectorCount();
    int i;
    for (i = 0; i < count; ++i) {
      skipRestrictionReason(reader);
    }
  }

  private void skipRestrictionReason(TLReader reader) {
    if (reader.readInt() != TG_RESTRICTION_REASON_ID) {
      throw new IllegalArgumentException("Unsupported restriction reason.");
    }
    reader.readDisplayString();
    reader.readDisplayString();
    reader.readDisplayString();
  }

  private void skipChatPhoto(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_CHAT_PHOTO_EMPTY_ID) {
      return;
    }
    if (constructor != TG_CHAT_PHOTO_ID) {
      throw new IllegalArgumentException("Unsupported chat photo.");
    }

    flags = reader.readInt();
    reader.readLong();
    if ((flags & (1 << 1)) != 0) {
      reader.skipBytes();
    }
    reader.readInt();
  }

  private void skipChatAdminRights(TLReader reader) {
    if (reader.readInt() != TG_CHAT_ADMIN_RIGHTS_ID) {
      throw new IllegalArgumentException("Unsupported admin rights.");
    }
    reader.readInt();
  }

  private void skipChatBannedRights(TLReader reader) {
    if (reader.readInt() != TG_CHAT_BANNED_RIGHTS_ID) {
      throw new IllegalArgumentException("Unsupported banned rights.");
    }
    reader.readInt();
    reader.readInt();
  }

  private void skipUsername(TLReader reader) {
    if (reader.readInt() != TG_USERNAME_ID) {
      throw new IllegalArgumentException("Unsupported username.");
    }
    reader.readInt();
    reader.readDisplayString();
  }

  private void skipPeerColor(TLReader reader) {
    int flags;

    if (reader.readInt() != TG_PEER_COLOR_ID) {
      throw new IllegalArgumentException("Unsupported peer color.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 0)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 1)) != 0) {
      reader.readLong();
    }
    if ((flags & ~((1 << 0) | (1 << 1))) != 0) {
      throw new IllegalArgumentException("Unsupported peer color flags.");
    }
  }

  private void skipEmojiStatus(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_EMOJI_STATUS_EMPTY_ID) {
      return;
    }
    if (constructor == TG_EMOJI_STATUS_ID) {
      flags = reader.readInt();
      reader.readLong();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      if ((flags & ~(1 << 0)) != 0) {
        throw new IllegalArgumentException("Unsupported emoji status flags.");
      }
      return;
    }
    if (constructor == TG_EMOJI_STATUS_COLLECTIBLE_ID) {
      flags = reader.readInt();
      reader.readLong();
      reader.readLong();
      reader.readDisplayString();
      reader.readDisplayString();
      reader.readLong();
      reader.readInt();
      reader.readInt();
      reader.readInt();
      reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      if ((flags & ~(1 << 0)) != 0) {
        throw new IllegalArgumentException("Unsupported collectible emoji status flags.");
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported emoji status.");
  }

  private void skipPeerNotifySettings(TLReader reader) {
    int flags;

    int constructor = reader.readInt();

    if (constructor != TG_PEER_NOTIFY_SETTINGS_ID &&
        constructor != TG_PEER_NOTIFY_SETTINGS_LEGACY_ID) {
      throw new IllegalArgumentException("Unsupported notify settings.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 0)) != 0) {
      skipBoxedBool(reader);
    }
    if ((flags & (1 << 1)) != 0) {
      skipBoxedBool(reader);
    }
    if ((flags & (1 << 2)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 3)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & (1 << 4)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & (1 << 5)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & (1 << 6)) != 0) {
      skipBoxedBool(reader);
    }
    if ((flags & (1 << 7)) != 0) {
      skipBoxedBool(reader);
    }
    if ((flags & (1 << 8)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & (1 << 9)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & (1 << 10)) != 0) {
      skipNotificationSound(reader);
    }
    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) |
                   (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8) | (1 << 9) |
                   (1 << 10))) != 0) {
      throw new IllegalArgumentException("Unsupported notify setting flags.");
    }
  }

  private void skipBoxedBool(TLReader reader) {
    int constructor = reader.readInt();
    if (constructor != TL_BOOL_TRUE_ID && constructor != TL_BOOL_FALSE_ID) {
      throw new IllegalArgumentException("Unsupported bool object.");
    }
  }

  private void skipNotificationSound(TLReader reader) {
    int constructor = reader.readInt();

    if (constructor == TG_NOTIFICATION_SOUND_DEFAULT_ID ||
        constructor == TG_NOTIFICATION_SOUND_NONE_ID ||
        constructor == TG_NOTIFICATION_SOUND_DEFAULT_LEGACY_ID ||
        constructor == TG_NOTIFICATION_SOUND_NONE_LEGACY_ID) {
      return;
    }
    if (constructor == TG_NOTIFICATION_SOUND_LOCAL_ID ||
        constructor == TG_NOTIFICATION_SOUND_LOCAL_LEGACY_ID) {
      reader.readDisplayString();
      reader.readDisplayString();
      return;
    }
    if (constructor == TG_NOTIFICATION_SOUND_RINGTONE_ID ||
        constructor == TG_NOTIFICATION_SOUND_RINGTONE_LEGACY_ID) {
      reader.readLong();
      return;
    }
    throw new IllegalArgumentException("Unsupported notification sound.");
  }

  private void skipDraftMessage(TLReader reader) {
    int constructor = reader.readInt();
    int flags = reader.readInt();

    if (constructor == TG_DRAFT_MESSAGE_EMPTY_ID) {
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      if ((flags & ~(1 << 0)) != 0) {
        throw new IllegalArgumentException("Unsupported empty draft flags.");
      }
      return;
    }
    if (constructor == TG_DRAFT_MESSAGE_LEGACY_ID) {
      if ((flags & ((1 << 3) | (1 << 4) | (1 << 5) | (1 << 7) | (1 << 8))) != 0) {
        throw new IllegalArgumentException("Unsupported legacy draft flags.");
      }
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      if ((flags & (1 << 1)) != 0) {
        reader.readInt();
      }
      reader.readDisplayString();
      if ((flags & (1 << 2)) != 0) {
        skipMessageEntities(reader);
      }
      reader.readInt();
      return;
    }
    if (constructor != TG_DRAFT_MESSAGE_ID) {
      throw new IllegalArgumentException("Unsupported draft message.");
    }
    if ((flags & ((1 << 4) | (1 << 5) | (1 << 7) | (1 << 8))) != 0) {
      throw new IllegalArgumentException("Unsupported draft flags.");
    }
    reader.readDisplayString();
    if ((flags & (1 << 3)) != 0) {
      skipMessageEntities(reader);
    }
    reader.readInt();
  }

  private void skipMessageEntities(TLReader reader) {
    int count = reader.readVectorCount();
    int i;
    for (i = 0; i < count; ++i) {
      skipMessageEntity(reader);
    }
  }

  private void skipMessageEntity(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    switch (constructor) {
      case TG_MESSAGE_ENTITY_BLOCKQUOTE_ID:
        flags = reader.readInt();
        reader.readInt();
        reader.readInt();
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported blockquote entity flags.");
        }
        return;
      case TG_MESSAGE_ENTITY_BLOCKQUOTE_OLD_ID:
      case TG_MESSAGE_ENTITY_UNKNOWN_ID:
      case TG_MESSAGE_ENTITY_MENTION_ID:
      case TG_MESSAGE_ENTITY_HASHTAG_ID:
      case TG_MESSAGE_ENTITY_BOT_COMMAND_ID:
      case TG_MESSAGE_ENTITY_URL_ID:
      case TG_MESSAGE_ENTITY_EMAIL_ID:
      case TG_MESSAGE_ENTITY_BOLD_ID:
      case TG_MESSAGE_ENTITY_ITALIC_ID:
      case TG_MESSAGE_ENTITY_CODE_ID:
      case TG_MESSAGE_ENTITY_PHONE_ID:
      case TG_MESSAGE_ENTITY_CASHTAG_ID:
      case TG_MESSAGE_ENTITY_UNDERLINE_ID:
      case TG_MESSAGE_ENTITY_STRIKE_ID:
      case TG_MESSAGE_ENTITY_BANK_CARD_ID:
      case TG_MESSAGE_ENTITY_SPOILER_ID:
        reader.readInt();
        reader.readInt();
        return;
      case TG_MESSAGE_ENTITY_PRE_ID:
      case TG_MESSAGE_ENTITY_TEXT_URL_ID:
        reader.readInt();
        reader.readInt();
        reader.readDisplayString();
        return;
      case TG_MESSAGE_ENTITY_MENTION_NAME_ID:
        reader.readInt();
        reader.readInt();
        reader.readLong();
        return;
      case TG_INPUT_MESSAGE_ENTITY_MENTION_NAME_ID:
        throw new IllegalArgumentException("Input message entities are not supported.");
      case TG_MESSAGE_ENTITY_CUSTOM_EMOJI_ID:
        reader.readInt();
        reader.readInt();
        reader.readLong();
        return;
      default:
        throw new IllegalArgumentException("Unsupported message entity.");
    }
  }

  private void skipMessageFwdHeader(TLReader reader) {
    int flags;

    if (reader.readInt() != TG_MESSAGE_FWD_HEADER_ID) {
      throw new IllegalArgumentException("Unsupported forward header.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 0)) != 0) {
      PeerRef ignored = new PeerRef();
      parsePeerRef(reader, ignored);
    }
    if ((flags & (1 << 5)) != 0) {
      reader.readDisplayString();
    }
    reader.readInt();
    if ((flags & (1 << 2)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 3)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 4)) != 0) {
      PeerRef ignoredImported = new PeerRef();
      parsePeerRef(reader, ignoredImported);
      reader.readInt();
    }
    if ((flags & (1 << 8)) != 0) {
      PeerRef ignoredSaved = new PeerRef();
      parsePeerRef(reader, ignoredSaved);
    }
    if ((flags & (1 << 9)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 10)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 6)) != 0) {
      reader.readDisplayString();
    }
  }

  private void skipReplyMarkup(TLReader reader) {
    int constructor = reader.readInt();
    int flags;
    int count;
    int i;
    int buttonCount;
    int j;

    switch (constructor) {
      case TG_REPLY_KEYBOARD_HIDE_ID:
        reader.readInt();
        return;
      case TG_REPLY_KEYBOARD_FORCE_REPLY_ID:
        flags = reader.readInt();
        if ((flags & (1 << 3)) != 0) {
          reader.readDisplayString();
        }
        if ((flags & ~((1 << 1) | (1 << 2) | (1 << 3))) != 0) {
          throw new IllegalArgumentException("Unsupported force-reply flags.");
        }
        return;
      case TG_REPLY_KEYBOARD_MARKUP_ID:
        flags = reader.readInt();
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          if (reader.readInt() != TG_KEYBOARD_BUTTON_ROW_ID) {
            throw new IllegalArgumentException("Unsupported keyboard row.");
          }
          buttonCount = reader.readVectorCount();
          for (j = 0; j < buttonCount; ++j) {
            throw new IllegalArgumentException("Keyboard buttons are not supported.");
          }
        }
        if ((flags & (1 << 3)) != 0) {
          reader.readDisplayString();
        }
        if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4))) != 0) {
          throw new IllegalArgumentException("Unsupported keyboard markup flags.");
        }
        return;
      case TG_REPLY_INLINE_MARKUP_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          if (reader.readInt() != TG_KEYBOARD_BUTTON_ROW_ID) {
            throw new IllegalArgumentException("Unsupported inline keyboard row.");
          }
          buttonCount = reader.readVectorCount();
          for (j = 0; j < buttonCount; ++j) {
            throw new IllegalArgumentException("Inline keyboard buttons are not supported.");
          }
        }
        return;
      default:
        throw new IllegalArgumentException("Unsupported reply markup.");
    }
  }

  private void skipMessageReplyHeader(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_MESSAGE_REPLY_STORY_HEADER_ID) {
      PeerRef ignored = new PeerRef();
      parsePeerRef(reader, ignored);
      reader.readInt();
      return;
    }
    if (constructor != TG_MESSAGE_REPLY_HEADER_ID) {
      throw new IllegalArgumentException("Unsupported reply header.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 4)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 0)) != 0) {
      PeerRef ignored = new PeerRef();
      parsePeerRef(reader, ignored);
    }
    if ((flags & (1 << 5)) != 0) {
      skipMessageFwdHeader(reader);
    }
    if ((flags & (1 << 8)) != 0) {
      MessageScratch ignoredMedia = new MessageScratch();
      parseMediaItem(reader, ignoredMedia);
    }
    if ((flags & (1 << 1)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 6)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 7)) != 0) {
      skipMessageEntities(reader);
    }
    if ((flags & (1 << 10)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 11)) != 0) {
      reader.readInt();
    }
    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) |
                   (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8) | (1 << 9) |
                   (1 << 10) | (1 << 11))) != 0) {
      throw new IllegalArgumentException("Unsupported reply header flags.");
    }
  }

  private void skipMessageReplies(TLReader reader) {
    int flags;
    int count;
    int i;

    if (reader.readInt() != TG_MESSAGE_REPLIES_ID) {
      throw new IllegalArgumentException("Unsupported message replies object.");
    }
    flags = reader.readInt();
    reader.readInt();
    reader.readInt();
    if ((flags & (1 << 1)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        PeerRef ignored = new PeerRef();
        parsePeerRef(reader, ignored);
      }
    }
    if ((flags & (1 << 0)) != 0) {
      reader.readLong();
    }
    if ((flags & (1 << 2)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 3)) != 0) {
      reader.readInt();
    }
    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3))) != 0) {
      throw new IllegalArgumentException("Unsupported message replies flags.");
    }
  }

  private void skipMessageReactions(TLReader reader) {
    int flags;
    int count;
    int i;

    if (reader.readInt() != TG_MESSAGE_REACTIONS_ID) {
      throw new IllegalArgumentException("Unsupported message reactions object.");
    }
    flags = reader.readInt();
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      int reactionFlags;
      if (reader.readInt() != TG_REACTION_COUNT_ID) {
        throw new IllegalArgumentException("Unsupported reaction count object.");
      }
      reactionFlags = reader.readInt();
      if ((reactionFlags & (1 << 0)) != 0) {
        reader.readInt();
      }
      skipReaction(reader);
      reader.readInt();
    }
    if ((flags & (1 << 1)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        if (reader.readInt() != TG_MESSAGE_PEER_REACTION_ID) {
          throw new IllegalArgumentException("Unsupported peer reaction object.");
        }
        reader.readInt();
        {
          PeerRef ignored = new PeerRef();
          parsePeerRef(reader, ignored);
        }
        reader.readInt();
        skipReaction(reader);
      }
    }
    if ((flags & (1 << 4)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        int reactorFlags;
        if (reader.readInt() != TG_MESSAGE_REACTOR_ID) {
          throw new IllegalArgumentException("Unsupported message reactor object.");
        }
        reactorFlags = reader.readInt();
        if ((reactorFlags & (1 << 3)) != 0) {
          PeerRef ignored = new PeerRef();
          parsePeerRef(reader, ignored);
        }
        reader.readInt();
      }
    }
    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4))) != 0) {
      throw new IllegalArgumentException("Unsupported reactions flags.");
    }
  }

  private void skipReaction(TLReader reader) {
    int constructor = reader.readInt();

    if (constructor == TG_REACTION_EMPTY_ID || constructor == TG_REACTION_PAID_ID) {
      return;
    }
    if (constructor == TG_REACTION_EMOJI_ID) {
      reader.readDisplayString();
      return;
    }
    if (constructor == TG_REACTION_CUSTOM_EMOJI_ID) {
      reader.readLong();
      return;
    }
    throw new IllegalArgumentException("Unsupported reaction type.");
  }

  private String parseMessageActionText(TLReader reader) {
    int constructor = reader.readInt();
    int flags;
    int count;
    int i;
    String title;

    switch (constructor) {
      case TG_MESSAGE_ACTION_EMPTY_ID:
        return "SERVICE";
      case TG_MESSAGE_ACTION_CHAT_EDIT_TITLE_ID:
        return "TITLE " + reader.readDisplayString();
      case TG_MESSAGE_ACTION_CHAT_EDIT_PHOTO_ID:
        skipPhoto(reader);
        return "PHOTO UPDATED";
      case TG_MESSAGE_ACTION_CHAT_DELETE_PHOTO_ID:
        return "PHOTO REMOVED";
      case TG_MESSAGE_ACTION_CHAT_CREATE_ID:
        title = reader.readDisplayString();
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readLong();
        }
        return "CREATED " + title;
      case TG_MESSAGE_ACTION_CHAT_ADD_USER_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readLong();
        }
        return "USER ADDED";
      case TG_MESSAGE_ACTION_CHAT_DELETE_USER_ID:
        reader.readLong();
        return "USER REMOVED";
      case TG_MESSAGE_ACTION_CHAT_JOINED_BY_LINK_ID:
        reader.readLong();
        return "JOINED VIA LINK";
      case TG_MESSAGE_ACTION_CHANNEL_CREATE_ID:
        reader.readDisplayString();
        return "CHANNEL CREATED";
      case TG_MESSAGE_ACTION_CHAT_MIGRATE_TO_ID:
        reader.readLong();
        return "UPGRADED TO CHANNEL";
      case TG_MESSAGE_ACTION_CHANNEL_MIGRATE_FROM_ID:
        reader.readDisplayString();
        reader.readLong();
        return "MIGRATED FROM GROUP";
      case TG_MESSAGE_ACTION_PIN_MESSAGE_ID:
        return "PINNED MESSAGE";
      case TG_MESSAGE_ACTION_HISTORY_CLEAR_ID:
        return "HISTORY CLEARED";
      case TG_MESSAGE_ACTION_SCREENSHOT_TAKEN_ID:
        return "SCREENSHOT TAKEN";
      case TG_MESSAGE_ACTION_CONTACT_SIGN_UP_ID:
        return "CONTACT JOINED";
      case TG_MESSAGE_ACTION_CHAT_JOINED_BY_REQUEST_ID:
        return "JOINED";
      case TG_MESSAGE_ACTION_GEO_PROXIMITY_REACHED_ID:
        parsePeerRef(reader, new PeerRef());
        parsePeerRef(reader, new PeerRef());
        reader.readInt();
        return "LOCATION ALERT";
      case TG_MESSAGE_ACTION_GROUP_CALL_ID:
        flags = reader.readInt();
        skipInputGroupCall(reader);
        if ((flags & (1 << 0)) != 0) {
          reader.readInt();
        }
        return "GROUP CALL";
      case TG_MESSAGE_ACTION_INVITE_TO_GROUP_CALL_ID:
        skipInputGroupCall(reader);
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readLong();
        }
        return "GROUP CALL INVITE";
      case TG_MESSAGE_ACTION_SET_MESSAGES_TTL_ID:
        flags = reader.readInt();
        reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readLong();
        }
        return "AUTO-DELETE UPDATED";
      case TG_MESSAGE_ACTION_GROUP_CALL_SCHEDULED_ID:
        skipInputGroupCall(reader);
        reader.readInt();
        return "CALL SCHEDULED";
      case TG_MESSAGE_ACTION_SET_CHAT_THEME_ID:
        skipChatTheme(reader);
        return "THEME UPDATED";
      case TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ME_ID:
        reader.readDisplayString();
        reader.readDisplayString();
        return "WEB APP DATA";
      case TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ID:
        reader.readDisplayString();
        return "WEB APP DATA";
      case TG_MESSAGE_ACTION_TOPIC_CREATE_ID:
        flags = reader.readInt();
        title = reader.readDisplayString();
        reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readLong();
        }
        return "TOPIC " + (title.length() > 0 ? title : "CREATED");
      case TG_MESSAGE_ACTION_TOPIC_EDIT_ID:
        skipMessageAction(reader, constructor);
        return "TOPIC UPDATED";
      case TG_MESSAGE_ACTION_SUGGEST_PROFILE_PHOTO_ID:
        skipPhoto(reader);
        return "PROFILE PHOTO SUGGESTED";
      default:
        skipMessageAction(reader, constructor);
        return "SERVICE";
    }
  }

  private void skipMessageAction(TLReader reader, int constructor) {
    int flags;
    int count;
    int i;

    switch (constructor) {
      case TG_MESSAGE_ACTION_EMPTY_ID:
      case TG_MESSAGE_ACTION_CHAT_DELETE_PHOTO_ID:
      case TG_MESSAGE_ACTION_PIN_MESSAGE_ID:
      case TG_MESSAGE_ACTION_HISTORY_CLEAR_ID:
      case TG_MESSAGE_ACTION_SCREENSHOT_TAKEN_ID:
      case TG_MESSAGE_ACTION_CHAT_JOINED_BY_REQUEST_ID:
      case TG_MESSAGE_ACTION_CONTACT_SIGN_UP_ID:
        return;
      case TG_MESSAGE_ACTION_CHAT_EDIT_TITLE_ID:
      case TG_MESSAGE_ACTION_CHANNEL_CREATE_ID:
      case TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ID:
        reader.readDisplayString();
        return;
      case TG_MESSAGE_ACTION_CHAT_CREATE_ID:
        reader.readDisplayString();
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readLong();
        }
        return;
      case TG_MESSAGE_ACTION_CHAT_EDIT_PHOTO_ID:
      case TG_MESSAGE_ACTION_SUGGEST_PROFILE_PHOTO_ID:
        skipPhoto(reader);
        return;
      case TG_MESSAGE_ACTION_CHAT_ADD_USER_ID:
      case TG_MESSAGE_ACTION_INVITE_TO_GROUP_CALL_ID:
        if (constructor == TG_MESSAGE_ACTION_INVITE_TO_GROUP_CALL_ID) {
          skipInputGroupCall(reader);
        }
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readLong();
        }
        return;
      case TG_MESSAGE_ACTION_CHAT_DELETE_USER_ID:
      case TG_MESSAGE_ACTION_CHAT_JOINED_BY_LINK_ID:
      case TG_MESSAGE_ACTION_CHAT_MIGRATE_TO_ID:
        reader.readLong();
        return;
      case TG_MESSAGE_ACTION_CHANNEL_MIGRATE_FROM_ID:
        reader.readDisplayString();
        reader.readLong();
        return;
      case TG_MESSAGE_ACTION_GEO_PROXIMITY_REACHED_ID:
        parsePeerRef(reader, new PeerRef());
        parsePeerRef(reader, new PeerRef());
        reader.readInt();
        return;
      case TG_MESSAGE_ACTION_GROUP_CALL_ID:
        flags = reader.readInt();
        skipInputGroupCall(reader);
        if ((flags & (1 << 0)) != 0) {
          reader.readInt();
        }
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported group call action flags.");
        }
        return;
      case TG_MESSAGE_ACTION_SET_MESSAGES_TTL_ID:
        flags = reader.readInt();
        reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readLong();
        }
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported TTL action flags.");
        }
        return;
      case TG_MESSAGE_ACTION_GROUP_CALL_SCHEDULED_ID:
        skipInputGroupCall(reader);
        reader.readInt();
        return;
      case TG_MESSAGE_ACTION_SET_CHAT_THEME_ID:
        skipChatTheme(reader);
        return;
      case TG_MESSAGE_ACTION_WEBVIEW_DATA_SENT_ME_ID:
        reader.readDisplayString();
        reader.readDisplayString();
        return;
      case TG_MESSAGE_ACTION_TOPIC_CREATE_ID:
        flags = reader.readInt();
        reader.readDisplayString();
        reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readLong();
        }
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported topic create flags.");
        }
        return;
      case TG_MESSAGE_ACTION_TOPIC_EDIT_ID:
        flags = reader.readInt();
        if ((flags & (1 << 0)) != 0) {
          reader.readDisplayString();
        }
        if ((flags & (1 << 1)) != 0) {
          reader.readLong();
        }
        if ((flags & (1 << 2)) != 0) {
          skipBoxedBool(reader);
        }
        if ((flags & (1 << 3)) != 0) {
          skipBoxedBool(reader);
        }
        if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3))) != 0) {
          throw new IllegalArgumentException("Unsupported topic edit flags.");
        }
        return;
      default:
        throw new IllegalArgumentException("Unsupported message action.");
    }
  }

  private void skipInputGroupCall(TLReader reader) {
    int constructor = reader.readInt();
    if (constructor == 0xd8aa840f) {
      reader.readLong();
      reader.readLong();
      return;
    }
    if (constructor == 0xfe06823f) {
      reader.readDisplayString();
      return;
    }
    if (constructor == 0x8c10603f) {
      reader.readInt();
      return;
    }
    throw new IllegalArgumentException("Unsupported group call reference.");
  }

  private void skipChatTheme(TLReader reader) {
    int constructor = reader.readInt();
    if (constructor == 0xc3dffc04) {
      reader.readDisplayString();
      return;
    }
    if (constructor == 0x3458f9c8) {
      int count;
      int i;
      skipDocument(reader);
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        int themeFlags;
        reader.readInt();
        themeFlags = reader.readInt();
        reader.readDisplayString();
        reader.readInt();
        reader.readInt();
        reader.readInt();
        reader.readInt();
        reader.readInt();
        if ((themeFlags & (1 << 0)) != 0) {
          reader.readInt();
        }
        if ((themeFlags & (1 << 1)) != 0) {
          reader.readInt();
        }
        if ((themeFlags & (1 << 2)) != 0) {
          reader.readInt();
        }
        if ((themeFlags & (1 << 3)) != 0) {
          reader.readInt();
        }
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported chat theme.");
  }

  private void skipFactCheck(TLReader reader) {
    int flags;

    if (reader.readInt() != TG_FACT_CHECK_ID) {
      throw new IllegalArgumentException("Unsupported fact check.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 1)) != 0) {
      reader.readDisplayString();
      skipTextWithEntities(reader);
    }
    reader.readLong();
    if ((flags & ~((1 << 0) | (1 << 1))) != 0) {
      throw new IllegalArgumentException("Unsupported fact check flags.");
    }
  }

  private void skipSuggestedPost(TLReader reader) {
    int flags;

    if (reader.readInt() != TG_SUGGESTED_POST_ID) {
      throw new IllegalArgumentException("Unsupported suggested post.");
    }
    flags = reader.readInt();
    if ((flags & (1 << 3)) != 0) {
      int starsFlags;
      reader.readInt();
      starsFlags = reader.readInt();
      reader.readInt();
      if ((starsFlags & (1 << 0)) != 0) {
        reader.readLong();
      }
    }
    if ((flags & (1 << 0)) != 0) {
      reader.readInt();
    }
  }

  private void skipTextWithEntities(TLReader reader) {
    reader.readDisplayString();
    skipMessageEntities(reader);
  }

  private void skipRichText(TLReader reader) {
    int constructor = reader.readInt();
    int count;
    int i;

    switch (constructor) {
      case TG_TEXT_EMPTY_ID:
        return;
      case TG_TEXT_PLAIN_ID:
        reader.readDisplayString();
        return;
      case TG_TEXT_BOLD_ID:
      case TG_TEXT_ITALIC_ID:
      case TG_TEXT_UNDERLINE_ID:
      case TG_TEXT_STRIKE_ID:
      case TG_TEXT_FIXED_ID:
      case TG_TEXT_SUBSCRIPT_ID:
      case TG_TEXT_SUPERSCRIPT_ID:
      case TG_TEXT_MARKED_ID:
        skipRichText(reader);
        return;
      case TG_TEXT_URL_ID:
        skipRichText(reader);
        reader.readDisplayString();
        reader.readLong();
        return;
      case TG_TEXT_EMAIL_ID:
      case TG_TEXT_PHONE_ID:
      case TG_TEXT_ANCHOR_ID:
        skipRichText(reader);
        reader.readDisplayString();
        return;
      case TG_TEXT_CONCAT_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipRichText(reader);
        }
        return;
      case TG_TEXT_IMAGE_ID:
        reader.readLong();
        reader.readInt();
        reader.readInt();
        return;
      default:
        throw new IllegalArgumentException("Unsupported rich text object.");
    }
  }

  private void skipPageCaption(TLReader reader) {
    if (reader.readInt() != TG_PAGE_CAPTION_ID) {
      throw new IllegalArgumentException("Unsupported page caption.");
    }
    skipRichText(reader);
    skipRichText(reader);
  }

  private void skipPageListItem(TLReader reader) {
    int constructor = reader.readInt();
    int count;
    int i;

    if (constructor == TG_PAGE_LIST_ITEM_TEXT_ID) {
      skipRichText(reader);
      return;
    }
    if (constructor == TG_PAGE_LIST_ITEM_BLOCKS_ID) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipPageBlock(reader);
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported page list item.");
  }

  private void skipPageListOrderedItem(TLReader reader) {
    int constructor = reader.readInt();
    int count;
    int i;

    if (constructor == TG_PAGE_LIST_ORDERED_ITEM_TEXT_ID) {
      reader.readDisplayString();
      skipRichText(reader);
      return;
    }
    if (constructor == TG_PAGE_LIST_ORDERED_ITEM_BLOCKS_ID) {
      reader.readDisplayString();
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipPageBlock(reader);
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported page ordered list item.");
  }

  private void skipPageRelatedArticle(TLReader reader) {
    int flags;
    if (reader.readInt() != TG_PAGE_RELATED_ARTICLE_ID) {
      throw new IllegalArgumentException("Unsupported related article.");
    }
    flags = reader.readInt();
    reader.readDisplayString();
    reader.readLong();
    if ((flags & (1 << 0)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 1)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 2)) != 0) {
      reader.readLong();
    }
    if ((flags & (1 << 3)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 4)) != 0) {
      reader.readInt();
    }
  }

  private void skipPageTableRow(TLReader reader) {
    int count;
    int i;
    if (reader.readInt() != TG_PAGE_TABLE_ROW_ID) {
      throw new IllegalArgumentException("Unsupported page table row.");
    }
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      int flags;
      if (reader.readInt() != TG_PAGE_TABLE_CELL_ID) {
        throw new IllegalArgumentException("Unsupported page table cell.");
      }
      flags = reader.readInt();
      if ((flags & (1 << 7)) != 0) {
        skipRichText(reader);
      }
      if ((flags & (1 << 1)) != 0) {
        reader.readInt();
      }
      if ((flags & (1 << 2)) != 0) {
        reader.readInt();
      }
      if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) |
                     (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7))) != 0) {
        throw new IllegalArgumentException("Unsupported page table cell flags.");
      }
    }
  }

  private void skipGeoPoint(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor == TG_GEO_POINT_EMPTY_ID) {
      return;
    }
    if (constructor != TG_GEO_POINT_ID) {
      throw new IllegalArgumentException("Unsupported geo point.");
    }
    flags = reader.readInt();
    reader.readLong();
    reader.readLong();
    reader.readLong();
    if ((flags & (1 << 0)) != 0) {
      reader.readInt();
    }
    if ((flags & ~(1 << 0)) != 0) {
      throw new IllegalArgumentException("Unsupported geo point flags.");
    }
  }

  private void skipPageBlock(TLReader reader) {
    int constructor = reader.readInt();
    int flags;
    int count;
    int i;

    switch (constructor) {
      case TG_PAGE_BLOCK_UNSUPPORTED_ID:
      case TG_PAGE_BLOCK_DIVIDER_ID:
        return;
      case TG_PAGE_BLOCK_TITLE_ID:
      case TG_PAGE_BLOCK_SUBTITLE_ID:
      case TG_PAGE_BLOCK_HEADER_ID:
      case TG_PAGE_BLOCK_SUBHEADER_ID:
      case TG_PAGE_BLOCK_PARAGRAPH_ID:
      case TG_PAGE_BLOCK_FOOTER_ID:
      case TG_PAGE_BLOCK_KICKER_ID:
        skipRichText(reader);
        return;
      case TG_PAGE_BLOCK_AUTHOR_DATE_ID:
        skipRichText(reader);
        reader.readInt();
        return;
      case TG_PAGE_BLOCK_PREFORMATTED_ID:
        skipRichText(reader);
        reader.readDisplayString();
        return;
      case TG_PAGE_BLOCK_ANCHOR_ID:
        reader.readDisplayString();
        return;
      case TG_PAGE_BLOCK_LIST_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageListItem(reader);
        }
        return;
      case TG_PAGE_BLOCK_BLOCKQUOTE_ID:
      case TG_PAGE_BLOCK_PULLQUOTE_ID:
        skipRichText(reader);
        skipRichText(reader);
        return;
      case TG_PAGE_BLOCK_PHOTO_ID:
        flags = reader.readInt();
        reader.readLong();
        skipPageCaption(reader);
        if ((flags & (1 << 0)) != 0) {
          reader.readDisplayString();
          reader.readLong();
        }
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported photo block flags.");
        }
        return;
      case TG_PAGE_BLOCK_VIDEO_ID:
        flags = reader.readInt();
        reader.readLong();
        skipPageCaption(reader);
        if ((flags & ~((1 << 0) | (1 << 1))) != 0) {
          throw new IllegalArgumentException("Unsupported video block flags.");
        }
        return;
      case TG_PAGE_BLOCK_COVER_ID:
        skipPageBlock(reader);
        return;
      case TG_PAGE_BLOCK_EMBED_ID:
        flags = reader.readInt();
        if ((flags & (1 << 1)) != 0) {
          reader.readDisplayString();
        }
        if ((flags & (1 << 2)) != 0) {
          reader.readDisplayString();
        }
        if ((flags & (1 << 4)) != 0) {
          reader.readLong();
        }
        if ((flags & (1 << 5)) != 0) {
          reader.readInt();
          reader.readInt();
        }
        skipPageCaption(reader);
        if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5))) != 0) {
          throw new IllegalArgumentException("Unsupported embed block flags.");
        }
        return;
      case TG_PAGE_BLOCK_EMBED_POST_ID:
        reader.readDisplayString();
        reader.readLong();
        reader.readLong();
        reader.readDisplayString();
        reader.readInt();
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageBlock(reader);
        }
        skipPageCaption(reader);
        return;
      case TG_PAGE_BLOCK_COLLAGE_ID:
      case TG_PAGE_BLOCK_SLIDESHOW_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageBlock(reader);
        }
        skipPageCaption(reader);
        return;
      case TG_PAGE_BLOCK_AUDIO_ID:
        reader.readLong();
        skipPageCaption(reader);
        return;
      case TG_PAGE_BLOCK_TABLE_ID:
        flags = reader.readInt();
        skipRichText(reader);
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageTableRow(reader);
        }
        if ((flags & ~((1 << 0) | (1 << 1))) != 0) {
          throw new IllegalArgumentException("Unsupported table block flags.");
        }
        return;
      case TG_PAGE_BLOCK_ORDERED_LIST_ID:
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageListOrderedItem(reader);
        }
        return;
      case TG_PAGE_BLOCK_DETAILS_ID:
        flags = reader.readInt();
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageBlock(reader);
        }
        skipRichText(reader);
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported details block flags.");
        }
        return;
      case TG_PAGE_BLOCK_RELATED_ARTICLES_ID:
        skipRichText(reader);
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          skipPageRelatedArticle(reader);
        }
        return;
      case TG_PAGE_BLOCK_MAP_ID:
        skipGeoPoint(reader);
        reader.readInt();
        reader.readInt();
        reader.readInt();
        skipPageCaption(reader);
        return;
      default:
        throw new IllegalArgumentException("Unsupported page block.");
    }
  }

  private void skipPage(TLReader reader) {
    int flags;
    int count;
    int i;

    if (reader.readInt() != TG_PAGE_ID) {
      throw new IllegalArgumentException("Unsupported page object.");
    }
    flags = reader.readInt();
    reader.readDisplayString();
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      skipPageBlock(reader);
    }
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      skipPhoto(reader);
    }
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      skipDocument(reader);
    }
    if ((flags & (1 << 3)) != 0) {
      reader.readInt();
    }
  }

  private String parseWebpageInfo(TLReader reader) {
    int constructor = reader.readInt();
    int flags;
    String url = "";
    String displayUrl = "";
    String siteName = "";
    String title = "";
    String description = "";

    if (constructor == TG_WEBPAGE_EMPTY_ID) {
      flags = reader.readInt();
      reader.readLong();
      if ((flags & (1 << 0)) != 0) {
        url = reader.readDisplayString();
      }
      return compactUrl(url);
    }
    if (constructor == TG_WEBPAGE_PENDING_ID) {
      flags = reader.readInt();
      reader.readLong();
      if ((flags & (1 << 0)) != 0) {
        url = reader.readDisplayString();
      }
      reader.readInt();
      return compactUrl(url);
    }
    if (constructor == TG_WEBPAGE_NOT_MODIFIED_ID) {
      flags = reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readInt();
      }
      return "";
    }
    if (constructor != TG_WEBPAGE_ID) {
      throw new IllegalArgumentException("Unsupported webpage object.");
    }

    flags = reader.readInt();
    reader.readLong();
    url = reader.readDisplayString();
    displayUrl = reader.readDisplayString();
    reader.readInt();
    if ((flags & (1 << 0)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 1)) != 0) {
      siteName = reader.readDisplayString();
    }
    if ((flags & (1 << 2)) != 0) {
      title = reader.readDisplayString();
    }
    if ((flags & (1 << 3)) != 0) {
      description = reader.readDisplayString();
    }
    if ((flags & (1 << 4)) != 0) {
      skipPhoto(reader);
    }
    if ((flags & (1 << 5)) != 0) {
      reader.readDisplayString();
      reader.readDisplayString();
    }
    if ((flags & (1 << 6)) != 0) {
      reader.readInt();
      reader.readInt();
    }
    if ((flags & (1 << 7)) != 0) {
      reader.readInt();
    }
    if ((flags & (1 << 8)) != 0) {
      reader.readDisplayString();
    }
    if ((flags & (1 << 9)) != 0) {
      skipDocument(reader);
    }
    if ((flags & (1 << 10)) != 0) {
      skipPage(reader);
    }
    if ((flags & (1 << 12)) != 0) {
      throw new IllegalArgumentException("Cached webpage payloads are not supported.");
    }
    if ((flags & ~((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) |
                   (1 << 5) | (1 << 6) | (1 << 7) | (1 << 8) | (1 << 9) |
                   (1 << 10) | (1 << 12) | (1 << 13) | (1 << 14))) != 0) {
      throw new IllegalArgumentException("Unsupported webpage flags.");
    }
    return composeWebpageInfo(siteName, title, description, displayUrl, url);
  }

  private String parsePhotoInfo(TLReader reader) {
    return parsePhoto(reader, null);
  }

  private String parsePhoto(TLReader reader, MediaReference ref) {
    PhotoMediaInfo size = new PhotoMediaInfo();

    skipPhoto(reader, size, ref);
    return size.summary();
  }

  private void skipPhoto(TLReader reader) {
    skipPhoto(reader, null, null);
  }

  private void skipPhoto(TLReader reader, SizeAccumulator size) {
    skipPhoto(reader, size, null);
  }

  private void skipPhoto(TLReader reader, SizeAccumulator size, MediaReference ref) {
    int constructor = reader.readInt();
    int flags;
    int count;
    int i;
    long id;
    long accessHash;
    byte[] fileReference;
    int dcId;

    if (constructor == TG_PHOTO_EMPTY_ID) {
      reader.readLong();
      return;
    }
    if (constructor != TG_PHOTO_ID) {
      throw new IllegalArgumentException("Unsupported photo object.");
    }

    flags = reader.readInt();
    id = reader.readLong();
    accessHash = reader.readLong();
    fileReference = reader.readBytes();
    reader.readInt();
    count = reader.readVectorCount();
    for (i = 0; i < count; ++i) {
      skipPhotoSize(reader, size);
    }
    if ((flags & (1 << 1)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipVideoSize(reader, size);
      }
    }
    dcId = reader.readInt();
    if (ref != null && size instanceof PhotoMediaInfo) {
      ref.setPhoto(id, accessHash, dcId, fileReference, fileReference.length, ((PhotoMediaInfo)size).thumbSize);
    }
  }

  private void skipPhotoSize(TLReader reader) {
    skipPhotoSize(reader, null);
  }

  private void skipPhotoSize(TLReader reader, SizeAccumulator size) {
    int constructor = reader.readInt();
    int count;
    int i;
    int width;
    int height;
    String type;

    switch (constructor) {
      case TG_PHOTO_SIZE_EMPTY_ID:
        reader.readDisplayString();
        return;
      case TG_PHOTO_SIZE_ID:
        type = reader.readDisplayString();
        skipFileLocation(reader);
        width = reader.readInt();
        height = reader.readInt();
        if (size != null) {
          notePhotoSize(size, type, width, height);
        }
        reader.readInt();
        return;
      case TG_PHOTO_SIZE_OLD_ID:
        type = reader.readDisplayString();
        width = reader.readInt();
        height = reader.readInt();
        if (size != null) {
          notePhotoSize(size, type, width, height);
        }
        reader.readInt();
        return;
      case TG_PHOTO_CACHED_SIZE_ID:
        type = reader.readDisplayString();
        skipFileLocation(reader);
        width = reader.readInt();
        height = reader.readInt();
        if (size != null) {
          notePhotoSize(size, type, width, height);
        }
        reader.skipBytes();
        return;
      case TG_PHOTO_CACHED_SIZE_OLD_ID:
        type = reader.readDisplayString();
        width = reader.readInt();
        height = reader.readInt();
        if (size != null) {
          notePhotoSize(size, type, width, height);
        }
        reader.skipBytes();
        return;
      case TG_PHOTO_STRIPPED_SIZE_ID:
        reader.readDisplayString();
        reader.skipBytes();
        return;
      case TG_PHOTO_SIZE_PROGRESSIVE_ID:
        type = reader.readDisplayString();
        width = reader.readInt();
        height = reader.readInt();
        if (size != null) {
          notePhotoSize(size, type, width, height);
        }
        count = reader.readVectorCount();
        for (i = 0; i < count; ++i) {
          reader.readInt();
        }
        return;
      case TG_PHOTO_PATH_SIZE_ID:
        reader.readDisplayString();
        reader.skipBytes();
        return;
      default:
        throw new IllegalArgumentException("Unsupported photo size.");
    }
  }

  private void skipFileLocation(TLReader reader) {
    int constructor = reader.readInt();
    if (constructor == TG_FILE_LOCATION_UNAVAILABLE_ID) {
      reader.readLong();
      reader.readLong();
      return;
    }
    if (constructor != TG_FILE_LOCATION_ID) {
      throw new IllegalArgumentException(
        "Unsupported file location: 0x" + Integer.toHexString(constructor));
    }
    reader.readLong();
    reader.readLong();
    reader.skipBytes();
    reader.skipBytes();
    reader.readInt();
  }

  private void skipVideoSize(TLReader reader) {
    skipVideoSize(reader, null);
  }

  private void skipVideoSize(TLReader reader, SizeAccumulator size) {
    int constructor = reader.readInt();
    int flags;
    int width;
    int height;

    if (constructor == TG_VIDEO_SIZE_ID) {
      flags = reader.readInt();
      reader.readDisplayString();
      width = reader.readInt();
      height = reader.readInt();
      if (size != null) {
        size.note(width, height);
      }
      reader.readInt();
      if ((flags & (1 << 0)) != 0) {
        reader.readLong();
      }
      return;
    }
    if (constructor == TG_VIDEO_SIZE_EMOJI_MARKUP_ID) {
      reader.readLong();
      {
        int count = reader.readVectorCount();
        int i;
        for (i = 0; i < count; ++i) {
          reader.readInt();
        }
      }
      return;
    }
    if (constructor == TG_VIDEO_SIZE_STICKER_MARKUP_ID) {
      skipInputStickerSet(reader);
      reader.readLong();
      {
        int count = reader.readVectorCount();
        int i;
        for (i = 0; i < count; ++i) {
          reader.readInt();
        }
      }
      return;
    }
    throw new IllegalArgumentException("Unsupported video size.");
  }

  private void skipDocument(TLReader reader) {
    parseDocument(reader, null);
  }

  private void parseDocument(TLReader reader, DocumentMediaInfo info) {
    parseDocument(reader, info, null);
  }

  private void parseDocument(TLReader reader, DocumentMediaInfo info, MediaReference ref) {
    int constructor = reader.readInt();
    int flags;
    int count;
    int i;
    String mimeType;
    long size;
    long id;
    long accessHash;
    byte[] fileReference;
    int dcId;

    if (constructor == TG_DOCUMENT_EMPTY_ID) {
      reader.readLong();
      return;
    }
    if (constructor != TG_DOCUMENT_ID) {
      throw new IllegalArgumentException("Unsupported document object.");
    }

    flags = reader.readInt();
    id = reader.readLong();
    accessHash = reader.readLong();
    fileReference = reader.readBytes();
    reader.readInt();
    mimeType = reader.readDisplayString();
    size = reader.readLong();
    if (info != null) {
      info.noteMimeType(mimeType);
      info.size = size;
    }
    if ((flags & (1 << 0)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipPhotoSize(reader, info);
      }
    }
    if ((flags & (1 << 1)) != 0) {
      count = reader.readVectorCount();
      for (i = 0; i < count; ++i) {
        skipVideoSize(reader, info);
      }
    }
    dcId = reader.readInt();
    skipDocumentAttributes(reader, info);
    if (ref != null) {
      ref.setDocument(
        id,
        accessHash,
        dcId,
        fileReference,
        fileReference.length,
        size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)size,
        mimeType
      );
    }
  }

  private void skipDocumentAttributes(TLReader reader) {
    skipDocumentAttributes(reader, null);
  }

  private void skipDocumentAttributes(TLReader reader, DocumentMediaInfo info) {
    int count = reader.readVectorCount();
    int i;
    for (i = 0; i < count; ++i) {
      skipDocumentAttribute(reader, info);
    }
  }

  private void skipDocumentAttribute(TLReader reader) {
    skipDocumentAttribute(reader, null);
  }

  private void skipDocumentAttribute(TLReader reader, DocumentMediaInfo info) {
    int constructor = reader.readInt();
    int flags;
    int width;
    int height;
    int duration;
    String text;

    switch (constructor) {
      case TG_DOCUMENT_ATTRIBUTE_IMAGE_SIZE_ID:
        width = reader.readInt();
        height = reader.readInt();
        if (info != null) {
          info.image = true;
          info.noteDimensions(width, height);
        }
        return;
      case TG_DOCUMENT_ATTRIBUTE_ANIMATED_ID:
      case TG_DOCUMENT_ATTRIBUTE_HAS_STICKERS_ID:
        return;
      case TG_DOCUMENT_ATTRIBUTE_STICKER_ID:
        flags = reader.readInt();
        text = reader.readDisplayString();
        skipInputStickerSet(reader);
        if ((flags & (1 << 0)) != 0) {
          skipMaskCoords(reader);
        }
        if (info != null) {
          info.sticker = true;
          info.altText = text;
        }
        return;
      case TG_DOCUMENT_ATTRIBUTE_VIDEO_ID:
        flags = reader.readInt();
        reader.readLong();
        width = reader.readInt();
        height = reader.readInt();
        if (info != null) {
          info.video = true;
          info.noteDimensions(width, height);
        }
        if ((flags & (1 << 2)) != 0) {
          reader.readInt();
        }
        if ((flags & (1 << 4)) != 0) {
          reader.readLong();
        }
        if ((flags & (1 << 5)) != 0) {
          reader.readDisplayString();
        }
        return;
      case TG_DOCUMENT_ATTRIBUTE_AUDIO_ID:
        flags = reader.readInt();
        duration = reader.readInt();
        if (info != null) {
          info.audio = true;
          info.voice = (flags & (1 << 10)) != 0;
          info.durationSeconds = duration;
        }
        if ((flags & (1 << 0)) != 0) {
          text = reader.readDisplayString();
          if (info != null && text != null && text.length() > 0) {
            info.title = text;
          }
        }
        if ((flags & (1 << 1)) != 0) {
          text = reader.readDisplayString();
          if (info != null && text != null && text.length() > 0) {
            info.performer = text;
          }
        }
        if ((flags & (1 << 2)) != 0) {
          reader.skipBytes();
        }
        return;
      case TG_DOCUMENT_ATTRIBUTE_FILENAME_ID:
        text = reader.readDisplayString();
        if (info != null) {
          info.fileName = text;
        }
        return;
      case TG_DOCUMENT_ATTRIBUTE_CUSTOM_EMOJI_ID:
        flags = reader.readInt();
        text = reader.readDisplayString();
        skipInputStickerSet(reader);
        if (info != null) {
          info.customEmoji = true;
          info.altText = text;
        }
        return;
      default:
        throw new IllegalArgumentException("Unsupported document attribute.");
    }
  }

  private void skipMaskCoords(TLReader reader) {
    if (reader.readInt() != TG_MASK_COORDS_ID) {
      throw new IllegalArgumentException("Unsupported mask coords.");
    }
    reader.readInt();
    reader.readInt();
    reader.readInt();
    reader.readInt();
  }

  private void skipInputStickerSet(TLReader reader) {
    int constructor = reader.readInt();
    if (constructor == TG_INPUT_STICKER_SET_EMPTY_ID ||
        constructor == TG_INPUT_STICKER_SET_ANIMATED_EMOJI_ID ||
        constructor == TG_INPUT_STICKER_SET_DICE_ID ||
        constructor == TG_INPUT_STICKER_SET_ANIMATED_EMOJI_ANIMATIONS_ID ||
        constructor == TG_INPUT_STICKER_SET_PREMIUM_GIFTS_ID ||
        constructor == TG_INPUT_STICKER_SET_EMOJI_GENERIC_ANIMATIONS_ID ||
        constructor == TG_INPUT_STICKER_SET_EMOJI_DEFAULT_STATUSES_ID ||
        constructor == TG_INPUT_STICKER_SET_EMOJI_DEFAULT_TOPIC_ICONS_ID ||
        constructor == TG_INPUT_STICKER_SET_EMOJI_CHANNEL_DEFAULT_STATUSES_ID ||
        constructor == TG_INPUT_STICKER_SET_TON_GIFTS_ID) {
      return;
    }
    if (constructor == TG_INPUT_STICKER_SET_ID_ID) {
      reader.readLong();
      reader.readLong();
      return;
    }
    if (constructor == TG_INPUT_STICKER_SET_SHORT_NAME_ID) {
      reader.readDisplayString();
      return;
    }
    throw new IllegalArgumentException("Unsupported input sticker set.");
  }

  private void skipForumTopic(TLReader reader) {
    int flags;
    int constructor = reader.readInt();

    if (constructor != TG_FORUM_TOPIC_ID) {
      throw new IllegalArgumentException("Unsupported forum topic.");
    }
    flags = reader.readInt();
    reader.readInt();
    reader.readInt();
    reader.readDisplayString();
    reader.readInt();
    if ((flags & (1 << 0)) != 0) {
      reader.readLong();
    }
    reader.readInt();
    reader.readInt();
    reader.readInt();
    reader.readInt();
    reader.readInt();
    parsePeerRef(reader, new PeerRef());
    skipPeerNotifySettings(reader);
    if ((flags & (1 << 4)) != 0) {
      skipDraftMessage(reader);
    }
  }

  private void skipSearchPostsFlood(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    if (constructor != 0x3e0b5b6a) {
      throw new IllegalArgumentException("Unsupported search posts flood.");
    }
    flags = reader.readInt();
    reader.readInt();
    reader.readInt();
    if ((flags & (1 << 1)) != 0) {
      reader.readInt();
    }
    reader.readLong();
    if ((flags & ~((1 << 0) | (1 << 1))) != 0) {
      throw new IllegalArgumentException("Unsupported search posts flood flags.");
    }
  }

  private static String joinNames(String firstName, String lastName, String fallback) {
    if (firstName != null && firstName.length() > 0 && lastName != null && lastName.length() > 0) {
      return firstName + " " + lastName;
    }
    if (firstName != null && firstName.length() > 0) {
      return firstName;
    }
    if (lastName != null && lastName.length() > 0) {
      return lastName;
    }
    return fallback;
  }

  private static String compactUrl(String url) {
    String compact = url != null ? url : "";
    int scheme;
    int query;

    if (compact.length() == 0) {
      return "";
    }
    scheme = compact.indexOf("://");
    if (scheme >= 0 && scheme + 3 < compact.length()) {
      compact = compact.substring(scheme + 3);
    }
    if (compact.startsWith("www.")) {
      compact = compact.substring(4);
    }
    query = compact.indexOf('?');
    if (query >= 0) {
      compact = compact.substring(0, query);
    }
    return DialogListModel.limit(compact, MessageListModel.MEDIA_INFO_MAX);
  }

  private static String composeWebpageInfo(
    String siteName,
    String title,
    String description,
    String displayUrl,
    String url
  ) {
    StringBuffer out = new StringBuffer();
    String compactDisplayUrl = compactUrl(displayUrl);
    String compactUrlValue = compactUrl(url);

    if (siteName != null && siteName.length() > 0) {
      out.append(siteName);
    }
    if (title != null && title.length() > 0) {
      if (out.length() > 0) {
        out.append(": ");
      }
      out.append(title);
    } else if (description != null && description.length() > 0) {
      if (out.length() > 0) {
        out.append(": ");
      }
      out.append(description);
    }
    if (out.length() == 0 && compactDisplayUrl.length() > 0) {
      out.append(compactDisplayUrl);
    } else if (out.length() == 0 && compactUrlValue.length() > 0) {
      out.append(compactUrlValue);
    }
    return DialogListModel.limit(out.toString(), MessageListModel.MEDIA_INFO_MAX);
  }

  private static String compactBytes(long bytes) {
    if (bytes <= 0) {
      return "";
    }
    if (bytes >= (1024L * 1024L)) {
      return Long.toString(bytes / (1024L * 1024L)) + "MB";
    }
    if (bytes >= 1024L) {
      return Long.toString(bytes / 1024L) + "KB";
    }
    return Long.toString(bytes) + "B";
  }

  private static String compactDuration(int seconds) {
    int minutes;
    int remainder;

    if (seconds <= 0) {
      return "";
    }
    minutes = seconds / 60;
    remainder = seconds % 60;
    return minutes + ":" + (remainder < 10 ? "0" : "") + remainder;
  }

  private static void appendSummaryPart(StringBuffer out, String part) {
    if (part == null || part.length() == 0) {
      return;
    }
    if (out.length() > 0) {
      out.append(' ');
    }
    out.append(part);
  }

  private static class SizeAccumulator {
    int width = 0;
    int height = 0;

    void note(int width, int height) {
      int currentArea;
      int nextArea;

      if (width <= 0 || height <= 0) {
        return;
      }
      currentArea = this.width * this.height;
      nextArea = width * height;
      if (nextArea > currentArea || (nextArea == currentArea && width > this.width)) {
        this.width = width;
        this.height = height;
      }
    }

    String summary() {
      if (width <= 0 || height <= 0) {
        return "";
      }
      return Integer.toString(width) + "x" + Integer.toString(height);
    }
  }

  private static final class PhotoMediaInfo extends SizeAccumulator {
    String thumbSize = "";

    void note(String type, int width, int height) {
      int currentArea;
      int nextArea;

      if (width <= 0 || height <= 0) {
        return;
      }
      currentArea = this.width * this.height;
      nextArea = width * height;
      if (nextArea > currentArea || (nextArea == currentArea && width > this.width)) {
        this.width = width;
        this.height = height;
        this.thumbSize = type != null ? type : "";
      }
    }
  }

  private static void notePhotoSize(SizeAccumulator size, String type, int width, int height) {
    if (size instanceof PhotoMediaInfo) {
      ((PhotoMediaInfo)size).note(type, width, height);
    } else {
      size.note(width, height);
    }
  }

  private static final class DocumentMediaInfo extends SizeAccumulator {
    String label = "";
    String fileName = "";
    String altText = "";
    String title = "";
    String performer = "";
    String mimeType = "";
    long size = 0;
    int durationSeconds = 0;
    boolean image = false;
    boolean video = false;
    boolean audio = false;
    boolean voice = false;
    boolean sticker = false;
    boolean customEmoji = false;

    void noteDimensions(int width, int height) {
      note(width, height);
    }

    void noteMimeType(String mimeType) {
      this.mimeType = mimeType != null ? mimeType : "";
      if (this.mimeType.startsWith("image/")) {
        image = true;
      } else if (this.mimeType.startsWith("video/")) {
        video = true;
      } else if (this.mimeType.startsWith("audio/")) {
        audio = true;
      }
    }

    String summary() {
      StringBuffer out = new StringBuffer();
      String primary = "";

      if (customEmoji) {
        label = "EMOJI";
      } else if (sticker) {
        label = "STICKER";
      } else if (voice) {
        label = "VOICE";
      } else if (video) {
        label = "VIDEO";
      } else if (audio) {
        label = "AUDIO";
      } else if (image) {
        label = "IMAGE";
      } else {
        label = "DOCUMENT";
      }

      if (altText != null && altText.length() > 0) {
        primary = altText;
      } else if (fileName != null && fileName.length() > 0) {
        primary = fileName;
      } else if (title != null && title.length() > 0) {
        primary = title;
      } else if (performer != null && performer.length() > 0) {
        primary = performer;
      }
      appendSummaryPart(out, primary);
      appendSummaryPart(out, super.summary());
      appendSummaryPart(out, compactDuration(durationSeconds));
      appendSummaryPart(out, compactBytes(size));
      return DialogListModel.limit(out.toString(), MessageListModel.MEDIA_INFO_MAX);
    }
  }

  private static final class PeerRef {
    int peerKind = DialogListModel.PEER_UNKNOWN;
    long peerId = 0;

    void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
    }

    void copyFrom(PeerRef other) {
      peerKind = other.peerKind;
      peerId = other.peerId;
    }
  }

  private static final class DialogScratch {
    final PeerRef peer = new PeerRef();
    int topMessageId = 0;
    int readInboxMaxId = 0;
    int readOutboxMaxId = 0;
    int unreadCount = 0;
    int pts = 0;

    void clear() {
      peer.clear();
      topMessageId = 0;
      readInboxMaxId = 0;
      readOutboxMaxId = 0;
      unreadCount = 0;
      pts = 0;
    }

    void copyFrom(DialogScratch other) {
      peer.copyFrom(other.peer);
      topMessageId = other.topMessageId;
      readInboxMaxId = other.readInboxMaxId;
      readOutboxMaxId = other.readOutboxMaxId;
      unreadCount = other.unreadCount;
      pts = other.pts;
    }
  }

  private static final class MessageScratch {
    int id = 0;
    int date = 0;
    final PeerRef fromPeer = new PeerRef();
    boolean hasFromPeer = false;
    final PeerRef peer = new PeerRef();
    String text = "";
    String mediaLabel = "";
    String mediaInfo = "";
    final MediaReference mediaRef = new MediaReference();
    boolean hasMedia = false;
    boolean outgoing = false;

    void clear() {
      id = 0;
      date = 0;
      fromPeer.clear();
      hasFromPeer = false;
      peer.clear();
      text = "";
      mediaLabel = "";
      mediaInfo = "";
      mediaRef.clear();
      hasMedia = false;
      outgoing = false;
    }

    void copyFrom(MessageScratch other) {
      id = other.id;
      date = other.date;
      fromPeer.copyFrom(other.fromPeer);
      hasFromPeer = other.hasFromPeer;
      peer.copyFrom(other.peer);
      text = other.text;
      mediaLabel = other.mediaLabel;
      mediaInfo = other.mediaInfo;
      mediaRef.copyFrom(other.mediaRef);
      hasMedia = other.hasMedia;
      outgoing = other.outgoing;
    }

    String previewText() {
      if (text != null && text.length() > 0) {
        return text;
      }
      if (hasMedia) {
        if (mediaLabel != null && mediaLabel.length() > 0) {
          if (mediaInfo != null && mediaInfo.length() > 0) {
            return mediaLabel + " " + mediaInfo;
          }
          return mediaLabel;
        }
        if (mediaInfo != null && mediaInfo.length() > 0) {
          return mediaInfo;
        }
      }
      return "";
    }
  }
}
