package sim;

import client.Character;
import client.Client;
import config.YamlConfig;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import net.server.world.Party;
import net.server.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import server.TimerManager;
import server.maps.MapleMap;
import sim.SimManager.SimState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Party behaviour of simulated players against a real World/Party with socketless characters:
 * invite -> (delay) -> accept -> member list, bot-invites-player, party chat, leave, and the decline cases.
 */
class SimPartyTest {
    private static final int WORLD = 0;
    private static final int CHANNEL = 1;
    private static final int MAP_ID = 100000000;

    private MockedStatic<Server> serverStatic;
    private World world;
    private MapleMap map;
    private boolean familySystem;

    /** Backend whose bots are real socketless characters in the test world. */
    private class TestBackend implements SimBackend {
        int declinedStray;

        @Override
        public SimHandle enter(SimBotConfig bot) {
            Character chr = socketlessChar(bot.name().hashCode() & 0xffff, bot.name());
            return new SimHandle() {
                public int characterId() { return chr.getId(); }
                public int mapId() { return MAP_ID; }
                public void leave() { }
                public String name() { return chr.getName(); }
                public boolean isInParty() { return chr.getParty() != null; }
                public boolean acceptPartyInvite(int partyId) { return SimParty.accept(chr, partyId); }
                public void declinePartyInvite(int partyId) { SimParty.decline(chr, partyId); }
                public boolean partySay(String text) { return SimParty.say(chr, text); }
                public SimParty.InviteOutcome invite(String playerName) { return SimParty.invite(chr, playerName); }
                public void leaveParty() { SimParty.leave(chr); }
            };
        }

        @Override
        public int wipeDatabase() {
            return 0;
        }

        @Override
        public void declineStrayPartyInvite(int characterId, int partyId) {
            declinedStray++;
        }
    }

    private final List<Runnable> pending = new ArrayList<>();
    private final List<Long> delays = new ArrayList<>();

    private void runPending() {
        List<Runnable> now = new ArrayList<>(pending);
        pending.clear();
        now.forEach(Runnable::run);
    }

    private SimManager manager(TestBackend backend, boolean enabled) {
        return new SimManager(backend, enabled,
                SimBotConfig.parse(List.of(
                        java.util.Map.of("name", "Mapleaf", "job", 110, "level", 35, "homeMap", MAP_ID, "channel", 1),
                        java.util.Map.of("name", "Moonpie", "job", 310, "level", 40, "homeMap", MAP_ID, "channel", 1))),
                (task, delay) -> {
                    pending.add(task);
                    delays.add(delay);
                }, new Random(7));
    }

    @BeforeEach
    void setUp() {
        Server server = mock(Server.class);
        Channel channel = mock(Channel.class);
        TimerManager.getInstance().start();
        when(server.getCurrentTime()).thenReturn(0L);
        serverStatic = Mockito.mockStatic(Server.class);
        serverStatic.when(Server::getInstance).thenReturn(server);
        familySystem = YamlConfig.config.server.USE_FAMILY_SYSTEM;
        YamlConfig.config.server.USE_FAMILY_SYSTEM = false; // World ctor would hit the DB
        world = new World(WORLD, 0, "", 1, 1, 1, 1, 1, 1, 1);
        when(server.getWorld(WORLD)).thenReturn(world);
        when(server.getChannel(WORLD, CHANNEL)).thenReturn(channel);
        map = new MapleMap(MAP_ID, WORLD, CHANNEL, MAP_ID, 1.0f);
    }

    @AfterEach
    void tearDown() {
        YamlConfig.config.server.USE_FAMILY_SYSTEM = familySystem;
        TimerManager.getInstance().stop();
        serverStatic.close();
    }

    private Character socketlessChar(int id, String name) {
        Client client = Client.createSocketless(-1, WORLD, CHANNEL);
        Character chr = spy(Character.getDefault(client));
        chr.setName(name);
        chr.setLevel(30);
        doReturn(id).when(chr).getId();
        doReturn(map).when(chr).getMap();
        chr.setLoggedIn(true);
        client.setPlayer(chr);
        world.getPlayerStorage().addPlayer(chr);
        return chr;
    }

    private static int invitePlayerToBot(Character player, Character bot) {
        assertTrue(Party.createParty(player, true) || player.getParty() != null);
        Party party = player.getParty();
        assertTrue(InviteCoordinator.createInvite(InviteType.PARTY, player, party.getId(), bot.getId()));
        return party.getId();
    }

    @Test
    void invitedBotAcceptsAfterADelayAndAppearsInPartyThenLeaves() {
        TestBackend backend = new TestBackend();
        SimManager sims = manager(backend, true);
        assertEquals(SimManager.SpawnResult.SPAWNED, sims.spawn("Mapleaf"));
        Character bot = world.getPlayerStorage().getCharacterByName("Mapleaf");
        Character player = socketlessChar(1, "RealGuy");

        int partyId = invitePlayerToBot(player, bot);
        sims.onPartyInvite(bot.getId(), partyId);

        // not in the party yet: it is "thinking" for a human-like moment
        assertEquals(SimState.RESPONDING, sims.stateOf("Mapleaf"));
        assertNull(bot.getParty());
        assertEquals(1, delays.size());
        assertTrue(delays.get(0) >= 1500 && delays.get(0) < 4000, "accept delay " + delays.get(0));

        runPending(); // accept fires, greeting gets scheduled
        assertEquals(SimState.IDLE, sims.stateOf("Mapleaf"));
        Party party = player.getParty();
        assertEquals(party, bot.getParty());
        assertEquals(2, party.getMembers().size());
        assertTrue(party.getMembers().stream().anyMatch(m -> m.getName().equals("Mapleaf")), "member list contains the bot");

        assertEquals(1, pending.size(), "greeting scheduled");
        runPending(); // greeting is sent through party chat without throwing

        assertTrue(sims.partySay("Mapleaf", "lf kpq"));
        assertTrue(sims.leaveParty("Mapleaf"));
        assertNull(bot.getParty());
        assertEquals(1, party.getMembers().size());
        assertFalse(sims.leaveParty("Mapleaf"), "no party left to leave");
        assertFalse(sims.partySay("Mapleaf", "hello?"));
    }

    @Test
    void botAlreadyInAPartyDeclines() {
        TestBackend backend = new TestBackend();
        SimManager sims = manager(backend, true);
        sims.spawn("Mapleaf");
        Character bot = world.getPlayerStorage().getCharacterByName("Mapleaf");
        Character first = socketlessChar(1, "FirstGuy");
        Character second = socketlessChar(2, "SecondGuy");

        sims.onPartyInvite(bot.getId(), invitePlayerToBot(first, bot));
        runPending();
        runPending(); // greeting
        assertEquals(first.getParty(), bot.getParty());

        int otherParty = invitePlayerToBot(second, bot);
        sims.onPartyInvite(bot.getId(), otherParty);
        assertTrue(pending.isEmpty(), "no second accept queued");
        assertEquals(first.getParty(), bot.getParty(), "still in the first party");
        assertFalse(InviteCoordinator.hasInvite(InviteType.PARTY, bot.getId()), "invite was answered (declined)");
        assertEquals(1, second.getParty().getMembers().size(), "second leader's party has only the leader");
    }

    @Test
    void secondInviteWhileStillThinkingIsDeclined() {
        TestBackend backend = new TestBackend();
        SimManager sims = manager(backend, true);
        sims.spawn("Mapleaf");
        Character bot = world.getPlayerStorage().getCharacterByName("Mapleaf");
        Character first = socketlessChar(1, "FirstGuy");

        int p1 = invitePlayerToBot(first, bot);
        sims.onPartyInvite(bot.getId(), p1);
        assertEquals(SimState.RESPONDING, sims.stateOf("Mapleaf"));
        // a stale second answer for the same bot is declined and does not queue another accept
        sims.onPartyInvite(bot.getId(), p1);
        assertEquals(1, delays.size());
    }

    @Test
    void simsOffDeclinesWithoutJoining() {
        TestBackend backend = new TestBackend();
        SimManager sims = manager(backend, false);
        sims.onPartyInvite(12345, 99);
        assertEquals(1, backend.declinedStray);
        assertTrue(delays.isEmpty());
        assertEquals(SimManager.SpawnResult.DISABLED, sims.spawn("Mapleaf"));
    }

    @Test
    void botCanInviteARealPlayerWhoAccepts() {
        TestBackend backend = new TestBackend();
        SimManager sims = manager(backend, true);
        sims.spawn("Moonpie");
        Character bot = world.getPlayerStorage().getCharacterByName("Moonpie");
        Character player = socketlessChar(1, "RealGuy");

        assertEquals(SimParty.InviteOutcome.NO_SUCH_PLAYER, sims.invite("Moonpie", "Nobody"));
        assertNull(sims.invite("Ghost", "RealGuy"), "bot not online");
        assertEquals(SimParty.InviteOutcome.SENT, sims.invite("Moonpie", "RealGuy"));
        Party party = bot.getParty();
        assertNotNull(party, "bot created a party to invite from");
        assertTrue(InviteCoordinator.hasInvite(InviteType.PARTY, player.getId()));
        assertEquals(SimParty.InviteOutcome.ALREADY_INVITED, sims.invite("Moonpie", "RealGuy"));

        assertTrue(player.acceptPartyInvite(party.getId()));
        assertEquals(2, party.getMembers().size());
        assertTrue(sims.partySay("Moonpie", "hi"));
        assertEquals(SimParty.InviteOutcome.TARGET_IN_PARTY, sims.invite("Moonpie", "RealGuy"));
    }
}
