package testclient;

import net.opcodes.SendOpcode;

import static testclient.ScenarioContext.check;

/**
 * Scenarios for in-map actions: chat, move, change map. Characters are brand new Beginners on throwaway accounts
 * (unique per run, auto-registered), so every run starts in Mushroom Town (10000) at the spawn point no matter what
 * earlier runs did - changing map is not reversible there, so reusing a character would drift.
 */
public final class ActionScenarios {
    static final int START_MAP = 10000;
    // Mushroom Town portal "out00" -> map 20000. wz/Map.wz/Map/Map0/000010000.img.xml: x=1077 y=480 tm=20000.
    static final String PORTAL = "out00";
    static final int PORTAL_X = 1077;
    static final int PORTAL_Y = 480;
    static final int PORTAL_TARGET = 20000;

    private ActionScenarios() {
    }

    /** Two clients in one map; waits until A can see B so later broadcasts cannot race B's map join. */
    private static final class Pair implements AutoCloseable {
        final GameSession a;
        final GameSession b;

        Pair(ScenarioContext ctx) throws Exception {
            String run = ScenarioContext.uniqueSuffix();
            GameSession first = GameSession.enterMap(ctx, "qa" + run + "a", "qa", "Qa" + run + "A");
            GameSession second = null;
            try {
                second = GameSession.enterMap(ctx, "qa" + run + "b", "qa", "Qa" + run + "B");
                check(first.listedMapId == START_MAP && second.listedMapId == START_MAP,
                        "new characters should start in " + START_MAP + " but are in " + first.listedMapId + "/" + second.listedMapId);
                // A is already in the map, so B's arrival is announced to A (SPAWN_PLAYER, first int = charId)
                final int bId = second.charId;
                first.client.expect(SendOpcode.SPAWN_PLAYER, r -> r.readInt() == bId);
                ctx.log("both characters are in map " + START_MAP + " and see each other");
            } catch (Exception | Error e) {
                first.close();
                if (second != null) {
                    second.close();
                }
                throw e;
            }
            a = first;
            b = second;
        }

        @Override
        public void close() {
            a.close();
            b.close();
        }
    }

    /** A says something; both B and A must receive the CHATTEXT broadcast (the sender gets its own line too). */
    public static final class Chat implements Scenario {
        @Override
        public String run(ScenarioContext ctx) throws Exception {
            String text = "hello from the test client";
            try (Pair p = new Pair(ctx)) {
                Actions.chat(p.a.client, text);
                int heardByB = Actions.expectChat(p.b.client, text);
                check(heardByB == p.a.charId, "B heard the chat from char " + heardByB + ", expected " + p.a.charId);
                int heardByA = Actions.expectChat(p.a.client, text);
                check(heardByA == p.a.charId, "A did not get its own chat line");
                return "sender=" + p.a.charName + " heardBy=" + p.b.charName + " text='" + text + "'";
            }
        }
    }

    /** A walks; B must receive MOVE_PLAYER for A with the same coordinates. */
    public static final class Move implements Scenario {
        @Override
        public String run(ScenarioContext ctx) throws Exception {
            try (Pair p = new Pair(ctx)) {
                Actions.walkTo(p.a.client, 123, 456);
                int[] xy = Actions.expectMove(p.b.client, p.a.charId);
                check(xy[0] == 123 && xy[1] == 456, "B saw A at " + xy[0] + "," + xy[1] + ", expected 123,456");
                return "mover=" + p.a.charName + " seenBy=" + p.b.charName + " at=" + xy[0] + "," + xy[1];
            }
        }
    }

    /** One character walks next to a portal and uses it; the reply is SET_FIELD for the target map. */
    public static final class ChangeMap implements Scenario {
        @Override
        public String run(ScenarioContext ctx) throws Exception {
            String run = ScenarioContext.uniqueSuffix();
            try (GameSession s = GameSession.enterMap(ctx, "qa" + run + "a", "qa", "Qa" + run + "A")) {
                check(s.listedMapId == START_MAP, "new character should start in " + START_MAP + " but is in " + s.listedMapId);
                Actions.walkTo(s.client, PORTAL_X - 10, PORTAL_Y);
                Actions.usePortal(s.client, PORTAL);
                int mapId = Actions.expectWarp(s.client);
                check(mapId == PORTAL_TARGET, "portal " + PORTAL + " led to map " + mapId + ", expected " + PORTAL_TARGET);
                return "character=" + s.charName + " " + START_MAP + " -> " + mapId + " via " + PORTAL;
            }
        }
    }
}
