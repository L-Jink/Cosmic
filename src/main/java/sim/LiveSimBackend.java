package sim;

import client.Character;
import client.Client;
import client.Job;
import client.SkinColor;
import client.creator.CharacterFactoryRecipe;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real implementation of {@link SimBackend}: bots are ordinary {@link Character}s owned by one hidden
 * account ({@value #SIM_ACCOUNT}), attached to a socketless {@link Client}, with characters.is_sim = 1.
 */
public class LiveSimBackend implements SimBackend {
    private static final Logger log = LoggerFactory.getLogger(LiveSimBackend.class);

    /** Account that owns every sim character. Its password is not a valid hash, so nobody can log in as it. */
    public static final String SIM_ACCOUNT = "sim_residents";
    private static final int WORLD = 0;
    private static final AtomicLong SESSION_IDS = new AtomicLong(-1000);

    @Override
    public SimHandle enter(SimBotConfig bot) throws Exception {
        Server server = Server.getInstance();
        World world = server.getWorld(WORLD);
        Channel channel = world == null ? null : world.getChannel(bot.channel());
        if (channel == null) {
            throw new IllegalStateException("channel " + bot.channel() + " does not exist");
        }

        int accountId = ensureAccount();
        int charId = ensureCharacter(bot, accountId);

        Client client = Client.createSocketless(SESSION_IDS.getAndDecrement(), WORLD, bot.channel());
        client.setAccID(accountId);
        server.loadAccountCharacters(client); // registers the account's character view, which saveCharToDB updates
        Character chr = Character.loadCharFromDB(charId, client, true);
        client.setPlayer(chr);

        channel.addPlayer(chr);
        world.addPlayer(chr);
        chr.setEnteredChannelWorld();
        chr.getMap().addPlayer(chr);

        log.info("Sim {} entered map {} on channel {}", bot.name(), chr.getMapId(), bot.channel());
        return new LiveHandle(client, chr);
    }

    @Override
    public void declineStrayPartyInvite(int characterId, int partyId) {
        net.server.coordinator.world.InviteCoordinator.InviteResult res = net.server.coordinator.world.InviteCoordinator
                .answerInvite(net.server.coordinator.world.InviteCoordinator.InviteType.PARTY, characterId, partyId, false);
        if (res.from != null) {
            res.from.sendPacket(tools.PacketCreator.partyStatusMessage(23, "that player"));
        }
    }

    @Override
    public int wipeDatabase() {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                exec(con, "DELETE ie FROM inventoryequipment ie JOIN inventoryitems ii ON ie.inventoryitemid = ii.inventoryitemid "
                        + "JOIN characters c ON ii.characterid = c.id WHERE c.is_sim = 1");
                exec(con, "DELETE ii FROM inventoryitems ii JOIN characters c ON ii.characterid = c.id WHERE c.is_sim = 1");
                int deleted = exec(con, "DELETE FROM characters WHERE is_sim = 1");
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM accounts WHERE name = ?")) {
                    ps.setString(1, SIM_ACCOUNT);
                    ps.executeUpdate();
                }
                con.commit();
                return deleted;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Sim wipe failed", e);
            return 0;
        }
    }

    private static int exec(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    private static int ensureAccount() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?")) {
                ps.setString(1, SIM_ACCOUNT);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO accounts (name, password, characterslots, tos) VALUES (?, '!', 127, 1)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, SIM_ACCOUNT);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    /** @return the character id for this bot, creating the (is_sim) row on first use. */
    private static int ensureCharacter(SimBotConfig bot, int accountId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, is_sim FROM characters WHERE name = ?")) {
                ps.setString(1, bot.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt("is_sim") != 1) {
                            throw new IllegalStateException("a real character is already named " + bot.name());
                        }
                        int id = rs.getInt("id");
                        try (PreparedStatement up = con.prepareStatement("UPDATE characters SET map = ?, spawnpoint = 0 WHERE id = ?")) {
                            up.setInt(1, bot.homeMap());
                            up.setInt(2, id);
                            up.executeUpdate();
                        }
                        return id;
                    }
                }
            }
        }

        Client creator = Client.createSocketless(SESSION_IDS.getAndDecrement(), WORLD, bot.channel());
        creator.setAccID(accountId);
        Character chr = Character.getDefault(creator);
        chr.setWorld(WORLD);
        chr.setSkinColor(SkinColor.LIGHT);
        chr.setGender(bot.gender());
        chr.setName(bot.name());
        chr.setHair(bot.gender() == 0 ? 30000 : 31000);
        chr.setFace(bot.gender() == 0 ? 20000 : 21000);
        chr.setLevel(bot.level());
        Job job = Job.getById(bot.job());
        if (job == null) {
            throw new IllegalStateException("unknown job id " + bot.job());
        }
        chr.setJob(job);
        chr.setMapId(bot.homeMap());

        int top = bot.gender() == 0 ? 1040002 : 1041002;
        int bottom = bot.gender() == 0 ? 1060002 : 1061002;
        int shoes = 1072001;
        int weapon = weaponFor(bot.job());

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Inventory equipped = chr.getInventory(InventoryType.EQUIPPED);
        int[][] gear = {{top, -5}, {bottom, -6}, {shoes, -7}, {weapon, -11}};
        for (int[] g : gear) {
            Item eq = ii.getEquipById(g[0]);
            eq.setPosition((byte) g[1]);
            equipped.addItemFromDB(eq);
        }

        CharacterFactoryRecipe recipe = new CharacterFactoryRecipe(chr.getJob(), bot.level(), bot.homeMap(), top, bottom, shoes, weapon);
        recipe.setMaxHp(100 + 20 * bot.level());
        recipe.setMaxMp(50 + 10 * bot.level());
        recipe.setMeso(0);
        if (!chr.insertNewChar(recipe)) {
            throw new IllegalStateException("could not insert character row for " + bot.name());
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET is_sim = 1 WHERE id = ?")) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
        log.info("Created sim character {} (id {})", bot.name(), chr.getId());
        return chr.getId();
    }

    private static int weaponFor(int job) {
        return switch (job / 100) {
            case 1 -> 1302000;  // sword
            case 2 -> 1372005;  // wand
            case 3 -> 1452002;  // bow
            case 4 -> 1332007;  // dagger
            case 5 -> 1482000;  // knuckler
            default -> 1302000;
        };
    }

    private static final class LiveHandle implements SimHandle {
        private final Client client;
        private final Character chr;

        LiveHandle(Client client, Character chr) {
            this.client = client;
            this.chr = chr;
        }

        @Override
        public int characterId() {
            return chr.getId();
        }

        @Override
        public int mapId() {
            return chr.getMapId();
        }

        @Override
        public void leave() {
            client.forceDisconnect();
        }

        @Override
        public String name() {
            return chr.getName();
        }

        @Override
        public boolean isInParty() {
            return chr.getParty() != null;
        }

        @Override
        public boolean acceptPartyInvite(int partyId) {
            return SimParty.accept(chr, partyId);
        }

        @Override
        public void declinePartyInvite(int partyId) {
            SimParty.decline(chr, partyId);
        }

        @Override
        public boolean partySay(String text) {
            return SimParty.say(chr, text);
        }

        @Override
        public SimParty.InviteOutcome invite(String playerName) {
            return SimParty.invite(chr, playerName);
        }

        @Override
        public void leaveParty() {
            SimParty.leave(chr);
        }
    }
}
