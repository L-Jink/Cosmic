package testclient;

import net.opcodes.RecvOpcode;
import net.opcodes.SendOpcode;

import java.io.Closeable;
import java.net.InetAddress;

import static testclient.ScenarioContext.check;

/**
 * A logged-in character on a channel server: the product of the login half of every scenario. Use
 * {@link #enterMap} to get one, then drive {@link #client} (send packets, {@code expect} replies). Closing the session
 * closes the channel socket, which is the clean logout (the server saves the character on channelInactive).
 */
public final class GameSession implements Closeable {
    // The real client sends these; the server only validates their format (12 hex mac + '_' + 8 hex hwid).
    private static final String MACS = "00-11-22-33-44-55";
    private static final String HOST_STRING = "001122334455_AABBCCDD";

    public final HeadlessClient client;
    public final int charId;
    public final String charName;
    /** Map id from the character list (where the character was saved), or -1 if the layout was not parseable. */
    public final int listedMapId;
    public final String label;

    private GameSession(HeadlessClient client, int charId, String charName, int listedMapId, String label) {
        this.client = client;
        this.charId = charId;
        this.charName = charName;
        this.listedMapId = listedMapId;
        this.label = label;
    }

    /**
     * Full login: login server (password, world list, char list, select; creates a Beginner called
     * {@code newCharName} if the account has none), then channel server PLAYER_LOGGEDIN and waits for SET_FIELD.
     * Unknown accounts auto-register (server flag AUTOMATIC_REGISTER).
     */
    public static GameSession enterMap(ScenarioContext ctx, String account, String password, String newCharName)
            throws Exception {
        int charId;
        String charName;
        int mapId;
        String channelHost;
        int channelPort;
        String label = account;

        try (HeadlessClient login = new HeadlessClient(ctx.host, ctx.loginPort, ctx.logger())) {
            login.send(RecvOpcode.LOGIN_PASSWORD, w -> w.writeString(account).writeString(password)
                    .writeBytes(new byte[6])                              // masked part of the hwid
                    .writeBytes(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD})); // hwid nibbles
            PacketReader status = login.expect(SendOpcode.LOGIN_STATUS);
            int code = status.readByte();
            if (code == 23) { // terms of service not yet accepted for this account
                ctx.log(label + ": accepting terms of service");
                login.send(RecvOpcode.ACCEPT_TOS, w -> w.writeByte(1));
                status = login.expect(SendOpcode.LOGIN_STATUS);
                code = status.readByte();
            }
            check(code == 0, "login rejected, LOGIN_STATUS code " + code + " (4=bad password, 5=no such account, 7=already logged in)");
            ctx.log(label + ": logged in");

            login.send(RecvOpcode.SERVERLIST_REQUEST, w -> { });
            int worlds = 0;
            while (true) {
                PacketReader sl = login.expect(SendOpcode.SERVERLIST);
                int id = sl.readByte();
                if (id == 0xFF) {
                    break; // end-of-list marker
                }
                sl.readString();
                worlds++;
            }
            check(worlds > 0, "server list was empty");

            login.send(RecvOpcode.CHARLIST_REQUEST, w -> w.writeByte(2).writeByte(ctx.world).writeByte(ctx.channel));
            PacketReader list = login.expect(SendOpcode.CHARLIST);
            check(list.readByte() == 0, "CHARLIST status not 0");
            int count = list.readByte();
            ctx.log(label + ": account has " + count + " character(s)");

            PacketReader entry;
            if (count == 0) {
                entry = createCharacter(login, ctx, newCharName, label);
            } else {
                entry = list; // cursor is at the first entry; later entries are ignored on purpose
            }
            charId = entry.readInt();
            charName = entry.readFixedString(13);
            mapId = readMapId(entry);
            ctx.log(label + ": selecting character " + charName + " (id " + charId + ", map " + mapId + ")");

            final int selectedId = charId;
            login.send(RecvOpcode.CHAR_SELECT, w -> w.writeInt(selectedId).writeString(MACS).writeString(HOST_STRING));
            PacketReader ip = login.expect(SendOpcode.SERVER_IP);
            check(ip.readShort() == 0, "SERVER_IP error code (account/IP/session coordinator refused the select)");
            byte[] addr = ip.readBytes(4);
            channelPort = ip.readShort();
            check(ip.readInt() == charId, "SERVER_IP returned a different character id");
            channelHost = InetAddress.getByAddress(addr).getHostAddress();
        }

        HeadlessClient game = new HeadlessClient(channelHost, channelPort, ctx.logger());
        try {
            game.send(RecvOpcode.PLAYER_LOGGEDIN, w -> w.writeInt(charId));
            PacketReader setField = game.expect(SendOpcode.SET_FIELD);
            ctx.log(label + ": SET_FIELD received (" + setField.available() + " bytes): " + charName + " is in the map");
            game.send(RecvOpcode.PLAYER_MAP_TRANSFER, w -> { }); // the real client acks every map load; clears the server-side "changing maps" flag
        } catch (Exception | Error e) {
            game.close();
            throw e;
        }
        return new GameSession(game, charId, charName, mapId, label);
    }

    @Override
    public void close() {
        client.close();
    }

    /** CHECK_CHAR_NAME then CREATE_CHAR a Beginner; returns the ADD_NEW_CHAR_ENTRY cursor at the entry start. */
    private static PacketReader createCharacter(HeadlessClient login, ScenarioContext ctx, String name, String label)
            throws Exception {
        login.send(RecvOpcode.CHECK_CHAR_NAME, w -> w.writeString(name));
        PacketReader resp = login.expect(SendOpcode.CHAR_NAME_RESPONSE);
        resp.readString();
        check(resp.readByte() == 0, "character name " + name + " is taken by another account");
        login.send(RecvOpcode.CREATE_CHAR, w -> w.writeString(name)
                .writeInt(1)        // 0 Cygnus, 1 Adventurer, 2 Aran
                .writeInt(20000)    // face
                .writeInt(30000)    // hair
                .writeInt(0)        // hair color
                .writeInt(0)        // skin
                .writeInt(1040002)  // top
                .writeInt(1060002)  // bottom
                .writeInt(1072001)  // shoes
                .writeInt(1302000)  // weapon
                .writeByte(0));     // gender
        PacketReader added = login.expect(SendOpcode.ADD_NEW_CHAR_ENTRY);
        check(added.readByte() == 0, "CREATE_CHAR failed");
        ctx.log(label + ": created character " + name);
        return added;
    }

    /**
     * Cursor is just after the 13-byte name. Reads CharacterStats up to the map id. Only valid for jobs without an SP
     * table (Beginner, Noblesse, Aran-as-beginner, all 1st-job and above except Evan); returns -1 otherwise.
     */
    private static int readMapId(PacketReader e) {
        try {
            e.skip(1 + 1 + 4 + 4 + 24); // gender, skin, face, hair, 3 pet ids
            e.skip(1);                  // level
            int job = e.readShort();
            if (job == 2001 || (job >= 2200 && job <= 2218)) {
                return -1;              // Evan uses the SP table layout
            }
            e.skip(4 * 2 + 4 * 2 + 2 + 2); // str dex int luk, hp maxhp mp maxmp, ap, sp
            e.skip(4 + 2 + 4);          // exp, fame, gacha exp
            return e.readInt();
        } catch (RuntimeException ex) {
            return -1;
        }
    }
}
