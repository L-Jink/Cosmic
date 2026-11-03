package sim;

import net.opcodes.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;

/** Packets a sim player sends to its map. Layouts mirror what the client sends/receives for a real player. */
public final class SimPackets {
    private SimPackets() {
    }

    // v83 move actions: even = facing right, odd = facing left (see AbstractAnimatedMapObject.isFacingLeft).
    public static final int STANCE_WALK_RIGHT = 2;
    public static final int STANCE_WALK_LEFT = 3;
    public static final int STANCE_STAND_RIGHT = 4;
    public static final int STANCE_STAND_LEFT = 5;

    public static int walkStance(boolean right) {
        return right ? STANCE_WALK_RIGHT : STANCE_WALK_LEFT;
    }

    public static int standStance(boolean right) {
        return right ? STANCE_STAND_RIGHT : STANCE_STAND_LEFT;
    }

    /**
     * MOVE_PLAYER: walk in a straight line to (x, y) taking {@code durationMs}, then stand there.
     * Same shape as {@code PacketCreator.movePlayer}: chr id, int 0, then the movement list of two absolute commands.
     */
    public static Packet walk(int chrId, int x, int y, int footholdId, boolean right, int durationMs) {
        OutPacket p = OutPacket.create(SendOpcode.MOVE_PLAYER);
        p.writeInt(chrId);
        p.writeInt(0);
        p.writeByte(2);
        absolute(p, x, y, footholdId, walkStance(right), durationMs);
        absolute(p, x, y, footholdId, standStance(right), 0);
        return p;
    }

    private static void absolute(OutPacket p, int x, int y, int fh, int stance, int duration) {
        p.writeByte(0);        // command: normal (absolute) move
        p.writeShort(x);
        p.writeShort(y);
        p.writeShort(0);       // x velocity
        p.writeShort(0);       // y velocity
        p.writeShort(fh);
        p.writeByte(stance);
        p.writeShort(duration);
    }
}
