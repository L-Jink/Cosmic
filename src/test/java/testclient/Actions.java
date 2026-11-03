package testclient;

import net.opcodes.RecvOpcode;
import net.opcodes.SendOpcode;

import java.io.IOException;

import static testclient.ScenarioContext.check;

/**
 * In-map actions a player can do, one method per client packet, plus helpers that decode the matching server
 * reply. Add new actions here (see tools/agent/testclient/README.md, "Adding an action").
 */
public final class Actions {
    private Actions() {
    }

    // ---------------------------------------------------------------- chat

    /** GENERAL_CHAT (0x31): {@code string text, byte show}. Mirrors GeneralChatHandler. */
    public static void chat(HeadlessClient c, String text) throws IOException {
        c.send(RecvOpcode.GENERAL_CHAT, w -> w.writeString(text).writeByte(0));
    }

    /**
     * Waits for the CHATTEXT (0xA2) broadcast: {@code int charId, bool gmWhiteChat, string text, byte show}.
     * Returns the sender's character id after checking the text.
     */
    public static int expectChat(HeadlessClient c, String text) throws IOException {
        PacketReader p = c.expect(SendOpcode.CHATTEXT, r -> {
            r.readInt();
            r.readByte();
            return r.readString().equals(text);
        });
        int sender = p.readInt();
        p.readByte(); // gm white chat
        check(p.readString().equals(text), "chat text changed in transit");
        return sender;
    }

    // ---------------------------------------------------------------- move

    /**
     * MOVE_PLAYER (0x29): 9 bytes the server skips, then the movement list {@code byte count, then per command:
     * byte cmd=0 (absolute), short x, short y, short xWobble, short yWobble, short foothold, byte stance, short ms}.
     * Mirrors MovePlayerHandler / AbstractMovementPacketHandler.updatePosition; the server trusts the last absolute
     * position (ChangeMapHandler later checks portal distance against it).
     */
    public static void walkTo(HeadlessClient c, int x, int y) throws IOException {
        c.send(RecvOpcode.MOVE_PLAYER, w -> w.writeBytes(new byte[9])
                .writeByte(1)           // one movement command
                .writeByte(0)           // absolute move
                .writeShort(x).writeShort(y)
                .writeShort(0).writeShort(0)   // wobble
                .writeShort(0)          // foothold
                .writeByte(2)           // stance (2 = standing, facing left)
                .writeShort(100));      // duration ms
    }

    /**
     * Waits for the MOVE_PLAYER (0xB9) rebroadcast about {@code charId}: {@code int charId, int 0, byte count,
     * byte cmd, short x, short y, ...}. Returns {x, y} of the first command. The server never echoes this to the
     * mover, so a second client has to be the observer.
     */
    public static int[] expectMove(HeadlessClient c, int charId) throws IOException {
        PacketReader p = c.expect(SendOpcode.MOVE_PLAYER, r -> r.readInt() == charId);
        p.readInt(); // charId
        p.readInt(); // 0
        check(p.readByte() >= 1, "empty movement list");
        p.readByte(); // command
        return new int[]{(short) p.readShort(), (short) p.readShort()};
    }

    // ---------------------------------------------------------------- change map

    /**
     * CHANGE_MAP (0x26): {@code byte fromDying=0, int targetMapId=-1 (portal), string portalName, byte unused,
     * byte wheel=0, byte chasing=0}. Mirrors ChangeMapHandler (non-GM characters can only use real portals, and must
     * be within ~630 px of it, so {@link #walkTo} first).
     */
    public static void usePortal(HeadlessClient c, String portalName) throws IOException {
        c.send(RecvOpcode.CHANGE_MAP, w -> w.writeByte(0).writeInt(-1).writeString(portalName)
                .writeByte(0).writeByte(0).writeByte(0));
    }

    /**
     * Waits for the SET_FIELD (0x7D) the server sends on a map change: {@code int channel0, int 0, byte 0,
     * int mapId, byte spawnPortal, short hp, ...}. Returns the new map id. (Only for map changes - the first
     * SET_FIELD after login is the long char-info form.)
     */
    public static int expectWarp(HeadlessClient c) throws IOException {
        PacketReader p = c.expect(SendOpcode.SET_FIELD);
        p.readInt();
        p.readInt();
        p.readByte();
        int mapId = p.readInt();
        c.send(RecvOpcode.PLAYER_MAP_TRANSFER, w -> { }); // client ack of the map load (PlayerMapTransitionHandler)
        return mapId;
    }
}
