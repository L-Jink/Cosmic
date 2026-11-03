package client;

import net.packet.Packet;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import server.TimerManager;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketlessClientTest {
    private static final int WORLD = 0;
    private static final int CHANNEL = 1;
    private static final int MAP_ID = 100000000;

    private MockedStatic<Server> serverStatic;

    @BeforeEach
    void setUp() {
        Server server = mock(Server.class);
        World world = mock(World.class);
        Channel channel = mock(Channel.class);
        when(server.getWorld(WORLD)).thenReturn(world);
        when(server.getChannel(WORLD, CHANNEL)).thenReturn(channel);
        serverStatic = Mockito.mockStatic(Server.class);
        serverStatic.when(Server::getInstance).thenReturn(server);
        TimerManager.getInstance().start(); // map item monitor needs the timer
    }

    @AfterEach
    void tearDown() {
        TimerManager.getInstance().stop();
        serverStatic.close();
    }

    @Test
    void sendPacketAndSessionCallsDoNotThrow() {
        Client c = Client.createSocketless(-1, WORLD, CHANNEL);

        assertTrue(c.isSocketless());
        assertFalse(c.isLoggedIn());
        assertDoesNotThrow(() -> c.sendPacket(PacketCreator.enableActions()));
        assertDoesNotThrow(c::closeSession);
        assertDoesNotThrow(c::disconnectSession);
        assertDoesNotThrow(() -> c.checkIfIdle(null));
        assertDoesNotThrow(c::forceDisconnect);
    }

    @Test
    void socketlessCharacterCanEnterAndLeaveMapWhileOtherClientReceivesBroadcasts() {
        MapleMap map = spy(new MapleMap(MAP_ID, WORLD, CHANNEL, MAP_ID, 1.0f));
        map.setOnFirstUserEnter("");
        map.setOnUserEnter("");
        // The spawn packet embeds equipment data from ItemInformationProvider (needs the DB); it is not
        // what this test is about, so skip building it.
        doNothing().when(map).broadcastSpawnPlayerMapObjectMessage(any(), any(), anyBoolean());

        Client simClient = Client.createSocketless(-1, WORLD, CHANNEL);
        Character sim = Character.getDefault(simClient);
        sim.setName("SimOne");
        sim.setPosition(new Point(0, 0));
        simClient.setPlayer(sim);

        Client otherClient = mock(Client.class);
        Character other = mock(Character.class);
        when(other.getClient()).thenReturn(otherClient);
        when(other.getId()).thenReturn(2);
        when(other.getType()).thenReturn(server.maps.MapObjectType.PLAYER);
        when(other.getPets()).thenReturn(new client.inventory.Pet[3]);
        when(other.getPosition()).thenReturn(new Point(0, 0));
        map.addPlayer(other);

        assertDoesNotThrow(() -> map.addPlayer(sim));
        assertEquals(2, map.getCharacters().size());
        assertDoesNotThrow(() -> map.broadcastMessage(PacketCreator.enableActions()));
        verify(other, atLeastOnce()).sendPacket(any(Packet.class));

        assertDoesNotThrow(() -> map.removePlayer(sim));
        assertEquals(1, map.getCharacters().size());
    }
}
