package net.server.world;

import client.Character;
import client.Client;
import config.YamlConfig;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import server.TimerManager;
import server.maps.MapleMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
 * Party operations (create / invite+accept / chat / HP bar / leave / expel / disband) with members that sit on a
 * socketless Client, as used by simulated players.
 */
class SocketlessPartyTest {
    private static final int WORLD = 0;
    private static final int CHANNEL = 1;
    private static final int MAP_ID = 100000000;

    private MockedStatic<Server> serverStatic;
    private World world;
    private MapleMap map;
    private boolean familySystem;

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

    @Test
    void createInviteAcceptChatLeaveAndDisbandDoNotThrow() {
        Character leader = socketlessChar(1, "Leader");
        Character sim = socketlessChar(2, "SimOne");
        Character sim2 = socketlessChar(3, "SimTwo");

        assertTrue(Party.createParty(leader, false));
        Party party = leader.getParty();
        assertNotNull(party);

        // invite + accept
        assertTrue(InviteCoordinator.createInvite(InviteType.PARTY, leader, party.getId(), sim.getId()));
        assertTrue(assertDoesNotThrow(() -> sim.acceptPartyInvite(party.getId())));
        assertEquals(party, sim.getParty());
        assertEquals(2, party.getMembers().size());

        // an invite that does not exist is refused without throwing
        assertFalse(assertDoesNotThrow(() -> sim2.acceptPartyInvite(party.getId())));
        assertNull(sim2.getParty());

        // party chat, HP bar
        assertDoesNotThrow(() -> world.partyChat(party, "hello", "SimOne"));
        assertDoesNotThrow(sim::updatePartyMemberHP);
        assertDoesNotThrow(leader::receivePartyMemberHP);

        // third member joins, then is expelled by the leader
        assertTrue(InviteCoordinator.createInvite(InviteType.PARTY, leader, party.getId(), sim2.getId()));
        assertTrue(sim2.acceptPartyInvite(party.getId()));
        assertEquals(3, party.getMembers().size());
        assertDoesNotThrow(() -> Party.expelFromParty(party, leader.getClient(), sim2.getId()));
        assertNull(sim2.getParty());
        assertEquals(2, party.getMembers().size());

        // member leaves
        assertDoesNotThrow(() -> Party.leaveParty(party, sim.getClient()));
        assertNull(sim.getParty());
        assertEquals(1, party.getMembers().size());

        // leader changes with a socketless member present, then leader leaves = disband
        assertTrue(InviteCoordinator.createInvite(InviteType.PARTY, leader, party.getId(), sim.getId()));
        assertTrue(sim.acceptPartyInvite(party.getId()));
        assertDoesNotThrow(() -> world.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, sim.getMPC()));
        assertEquals(sim.getId(), party.getLeaderId());
        assertDoesNotThrow(() -> Party.leaveParty(party, sim.getClient()));
        assertNull(world.getParty(party.getId()));
        assertNull(leader.getParty());
        assertNull(sim.getParty());
    }
}
