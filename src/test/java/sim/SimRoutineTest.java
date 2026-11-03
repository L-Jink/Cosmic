package sim;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimRoutineTest {
    // ---- population curve

    @Test
    void curveGivesTargetCountPerHour() {
        SimPopulation pop = new SimPopulation(SimPopulation.DEFAULT_CURVE, 12, 30);
        assertEquals(1, pop.targetCount(3, 5));   // night: 10% of 5 rounds to 1 (never 0 while the curve is > 0)
        assertEquals(5, pop.targetCount(20, 5));  // evening peak: everyone
        assertEquals(3, pop.targetCount(12, 5));  // noon: 65% of 5 = 3.25 -> 3
        assertEquals(0, pop.targetCount(3, 0));
        assertTrue(pop.targetCount(20, 5) > pop.targetCount(4, 5));
        assertEquals(pop.percentAt(20), pop.percentAt(44)); // hour wraps
    }

    @Test
    void zeroPercentMeansNobody() {
        int[] curve = new int[24];
        assertEquals(0, new SimPopulation(curve, 12, 30).targetCount(5, 10));
    }

    @Test
    void capsLimitBotsPerMapAndPerChannel() {
        List<SimBotConfig> bots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            bots.add(new SimBotConfig("Henebot" + i, 0, 10, 100000000, 1, 0));
        }
        bots.add(new SimBotConfig("Ellibot1", 0, 10, 101000000, 1, 0));
        bots.add(new SimBotConfig("Ellibot2", 0, 10, 101000000, 1, 0));
        bots.add(new SimBotConfig("Otherch1", 0, 10, 100000000, 2, 0));

        // 4 per map: 4 Henesys ch1 + 2 Ellinia ch1 + 1 Henesys ch2 = 7; channel 1 capped at 5 -> 4 Henesys + 1 Ellinia
        SimPopulation pop = new SimPopulation(new int[]{100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100}, 4, 5);
        List<SimBotConfig> admitted = pop.desired(bots, 20);
        assertEquals(6, admitted.size());
        assertEquals(4, admitted.stream().filter(b -> b.channel() == 1 && b.homeMap() == 100000000).count());
        assertEquals(1, admitted.stream().filter(b -> b.homeMap() == 101000000).count());
        assertEquals(1, admitted.stream().filter(b -> b.channel() == 2).count());
    }

    @Test
    void badCurveFallsBackToDefault() {
        assertArrayEquals(SimPopulation.DEFAULT_CURVE, SimPopulation.parseCurve(List.of(1, 2, 3)));
        assertArrayEquals(SimPopulation.DEFAULT_CURVE, SimPopulation.parseCurve(null));
        List<Object> withText = new ArrayList<>(java.util.Collections.nCopies(24, "50"));
        withText.set(7, "lots");
        assertArrayEquals(SimPopulation.DEFAULT_CURVE, SimPopulation.parseCurve(withText));
        List<Object> ok = new ArrayList<>(java.util.Collections.nCopies(24, "50"));
        ok.set(0, "250");
        ok.set(1, "-4");
        int[] parsed = SimPopulation.parseCurve(ok);
        assertEquals(100, parsed[0]);
        assertEquals(0, parsed[1]);
        assertEquals(50, parsed[2]);
    }

    @Test
    void shippedConfigCurveIsValid() throws Exception {
        // see SimConfigParsingTest.shippedConfigHasValidRoutineSettings
        assertEquals(24, SimPopulation.DEFAULT_CURVE.length);
    }

    // ---- phrase bank

    @Test
    void shippedPhraseBankHasEnoughV83StyleLines() {
        SimPhrases phrases = SimPhrases.load("config/sim_phrases.yaml");
        assertTrue(phrases.size() >= 30, "need at least 30 lines, got " + phrases.size());
        for (String line : phrases.all()) {
            assertEquals(line.toLowerCase(), line, "lines are lowercase: " + line);
            assertTrue(line.length() <= SimPhrases.MAX_LENGTH, line);
        }
    }

    @Test
    void pickerNeverRepeatsPreviousLine() {
        SimPhrases phrases = new SimPhrases(List.of("a", "b", "c"));
        Random random = new Random(1);
        String prev = null;
        for (int i = 0; i < 200; i++) {
            String s = phrases.pick(random, prev);
            assertNotNull(s);
            assertNotEquals(prev, s);
            prev = s;
        }
    }

    @Test
    void emptyOrBlankBankFallsBack() {
        assertFalse(new SimPhrases(List.of(" ", "")).all().isEmpty());
        assertEquals("only", new SimPhrases(List.of("only")).pick(new Random(), "only"));
    }

    @Test
    void missingPhraseFileFallsBack() {
        assertFalse(SimPhrases.load("config/does-not-exist.yaml").all().isEmpty());
    }

    // ---- move packet

    @Test
    void walkPacketBytes() {
        net.packet.Packet p = SimPackets.walk(0x01020304, 100, -50, 7, true, 800);
        byte[] expected = {
                (byte) net.opcodes.SendOpcode.MOVE_PLAYER.getValue(), 0,
                4, 3, 2, 1,             // chr id
                0, 0, 0, 0,             // int 0
                2,                      // 2 commands
                0, 100, 0, (byte) 0xCE, (byte) 0xFF, 0, 0, 0, 0, 7, 0, 2, (byte) 0x20, 3,   // walk right to (100,-50), 800ms
                0, 100, 0, (byte) 0xCE, (byte) 0xFF, 0, 0, 0, 0, 7, 0, 4, 0, 0};          // stand right
        assertArrayEquals(expected, p.getBytes());
    }

    // ---- routine

    /** A bot standing on one flat platform (-300..300, y = 0) inside map bounds (-400..400); nothing exists elsewhere. */
    static class FakeBot implements SimBackend.SimHandle {
        final String name;
        final int id;
        Point pos = new Point(0, 0);
        boolean sitting;
        boolean inParty;
        final List<Point> visited = new ArrayList<>();
        final List<String> said = new ArrayList<>();
        int sits;
        boolean left;

        FakeBot(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public int characterId() { return id; }
        public int mapId() { return 100000000; }
        public void leave() { left = true; }
        public String name() { return name; }
        public boolean isInParty() { return inParty; }
        public boolean acceptPartyInvite(int partyId) { return false; }
        public void declinePartyInvite(int partyId) { }
        public boolean partySay(String text) { return false; }
        public SimParty.InviteOutcome invite(String playerName) { return SimParty.InviteOutcome.NO_SUCH_PLAYER; }
        public void leaveParty() { }

        @Override public Point position() { return pos; }
        @Override public Rectangle walkBounds() { return new Rectangle(-400, -200, 800, 400); }
        @Override public SimBackend.Ground groundBelow(int x, int y) {
            return x >= -300 && x <= 300 ? new SimBackend.Ground(x, 0, 1) : null;
        }
        @Override public void walkTo(SimBackend.Ground to, boolean right, int durationMs) {
            assertTrue(durationMs > 0);
            pos = new Point(to.x(), to.y());
            visited.add(pos);
        }
        @Override public boolean sit(int chairItemId) { sitting = true; sits++; return true; }
        @Override public void stand() { sitting = false; }
        @Override public boolean isSitting() { return sitting; }
        @Override public void say(String text) { said.add(text); }
    }

    static class FakeWorld implements SimBackend {
        final List<FakeBot> bots = new ArrayList<>();

        @Override public SimHandle enter(SimBotConfig bot) {
            FakeBot b = new FakeBot(bot.name(), 500 + bots.size());
            bots.add(b);
            return b;
        }

        @Override public int wipeDatabase() { return 0; }

        FakeBot bot(String name) {
            return bots.stream().filter(b -> b.name.equals(name) && !b.left).findFirst().orElse(null);
        }

        long onlineNow() {
            return bots.stream().filter(b -> !b.left).count();
        }
    }

    private static List<SimBotConfig> five() {
        List<SimBotConfig> l = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            l.add(new SimBotConfig("Resident" + i, 0, 10, 100000000, 1, 0));
        }
        return l;
    }

    private static SimManager manager(FakeWorld world, int[] curve, Random random) {
        return new SimManager(world, true, five(), (task, delay) -> task.run(), random,
                new SimPopulation(curve, 12, 30), new SimPhrases(List.of("lf party kpq", "hi all", "s> ilbi 7m")));
    }

    @Test
    void planWalkStaysInsideBoundsAndOnGround() {
        Rectangle bounds = new Rectangle(-400, -200, 800, 400);
        SimRoutine.GroundLookup lookup = (x, y) -> x >= -300 && x <= 300 ? new SimBackend.Ground(x, 0, 1) : null;
        Random random = new Random(42);
        Point pos = new Point(290, 0); // right next to the platform edge
        for (int i = 0; i < 2000; i++) {
            SimRoutine.Move m = SimRoutine.planWalk(pos, bounds, lookup, random);
            if (m == null) {
                continue;
            }
            assertTrue(bounds.contains(m.to().x(), m.to().y()), "left the map: " + m);
            assertTrue(m.to().x() >= -300 && m.to().x() <= 300, "left the platform: " + m);
            assertTrue(Math.abs(m.to().x() - pos.x) >= 1);
            assertEquals(Math.abs(m.to().x() - pos.x) * SimRoutine.MS_PER_PIXEL, m.durationMs());
            pos = new Point(m.to().x(), m.to().y());
        }
    }

    @Test
    void planWalkGivesUpWhenThereIsNoGround() {
        assertEquals(null, SimRoutine.planWalk(new Point(0, 0), new Rectangle(-400, -200, 800, 400), (x, y) -> null, new Random(3)));
    }

    @Test
    void tickingBotsNeverLeaveTheMapAndEventuallyWalkSitAndChat() {
        FakeWorld world = new FakeWorld();
        int[] full = new int[24];
        java.util.Arrays.fill(full, 100);
        SimManager sims = manager(world, full, new Random(7));
        sims.spawnInitial(20);
        assertEquals(5, world.onlineNow());

        long now = 1_000_000;
        for (int i = 0; i < 3 * 3600; i++) { // three simulated hours at 1 tick/s
            sims.tick(now, 20);
            now += SimManager.TICK_MS;
        }
        Rectangle bounds = new FakeBot("x", 0).walkBounds();
        int walks = 0;
        int chats = 0;
        int sits = 0;
        for (FakeBot b : world.bots) {
            for (Point p : b.visited) {
                assertTrue(bounds.contains(p), "bot walked outside map bounds: " + p);
                assertTrue(p.x >= -300 && p.x <= 300);
            }
            walks += b.visited.size();
            chats += b.said.size();
            sits += b.sits;
        }
        assertTrue(walks > 100, "bots should walk, got " + walks);
        assertTrue(chats > 20, "bots should chat now and then, got " + chats);
        assertTrue(chats < 5 * 3 * 3600 / 60, "but not spam, got " + chats);
        assertTrue(sits > 0, "bots should sit sometimes");
    }

    @Test
    void populationFollowsCurveGraduallyOneBotAtATime() {
        FakeWorld world = new FakeWorld();
        int[] curve = new int[24];
        java.util.Arrays.fill(curve, 100);
        curve[3] = 20; // 1 of 5
        SimManager sims = manager(world, curve, new Random(5));
        sims.spawnInitial(20);
        assertEquals(5, world.onlineNow());

        long now = 0;
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 400; i++) { // night: thin out, one change per 20-40s
            sims.tick(now, 3);
            now += SimManager.TICK_MS;
            counts.add(world.onlineNow());
        }
        assertEquals(1, world.onlineNow());
        for (int i = 1; i < counts.size(); i++) {
            assertTrue(Math.abs(counts.get(i) - counts.get(i - 1)) <= 1);
        }
        // the first configured bot (the "regular") is the one that stays
        assertNotNull(world.bot("Resident1"));

        for (int i = 0; i < 400; i++) { // evening: fill up again
            sims.tick(now, 20);
            now += SimManager.TICK_MS;
        }
        assertEquals(5, world.onlineNow());
        assertEquals(5, sims.onlineCount());
    }

    @Test
    void gmRemovedBotStaysOffUntilSpawnedAgain() {
        FakeWorld world = new FakeWorld();
        int[] full = new int[24];
        java.util.Arrays.fill(full, 100);
        SimManager sims = manager(world, full, new Random(9));
        sims.spawnInitial(20);
        assertTrue(sims.remove("Resident2"));
        long now = 0;
        for (int i = 0; i < 300; i++) {
            sims.tick(now, 20);
            now += SimManager.TICK_MS;
        }
        assertFalse(sims.isOnline("Resident2"));
        assertEquals(SimManager.SpawnResult.SPAWNED, sims.spawn("Resident2"));
        assertTrue(sims.isOnline("Resident2"));
    }

    @Test
    void partiedBotsAreLeftAlone() {
        FakeWorld world = new FakeWorld();
        int[] full = new int[24];
        java.util.Arrays.fill(full, 100);
        SimManager sims = manager(world, full, new Random(11));
        sims.spawnInitial(20);
        world.bot("Resident1").inParty = true;
        long now = 0;
        for (int i = 0; i < 1800; i++) {
            sims.tick(now, 20);
            now += SimManager.TICK_MS;
        }
        assertTrue(world.bot("Resident1").visited.isEmpty());
        assertTrue(world.bot("Resident1").said.isEmpty());
        assertFalse(world.bot("Resident2").visited.isEmpty());
    }

    @Test
    void routineIsOffWithoutPhrases() {
        FakeWorld world = new FakeWorld();
        SimManager sims = new SimManager(world, true, five(), (task, delay) -> task.run(), new Random(), null, null);
        sims.spawn("Resident1");
        for (int i = 0; i < 100; i++) {
            sims.tick(i * 1000L, 20);
        }
        assertTrue(world.bot("Resident1").visited.isEmpty());
    }
}
