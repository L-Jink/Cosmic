package sim;

import client.Character;
import config.YamlConfig;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteResult;
import net.server.coordinator.world.InviteCoordinator.InviteResultType;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import net.server.world.Party;
import tools.PacketCreator;

/**
 * Party actions of a simulated player, done directly on its {@link Character} (a bot has no client to send
 * packets). Mirrors what PartyOperationHandler / DenyPartyRequestHandler do for a real player.
 */
public final class SimParty {
    private SimParty() {
    }

    public enum InviteOutcome { SENT, NO_SUCH_PLAYER, REQUIREMENTS, TARGET_IN_PARTY, PARTY_FULL, ALREADY_INVITED, CANNOT_CREATE }

    public static boolean accept(Character bot, int partyId) {
        return bot.acceptPartyInvite(partyId);
    }

    public static void decline(Character bot, int partyId) {
        InviteResult res = InviteCoordinator.answerInvite(InviteType.PARTY, bot.getId(), partyId, false);
        if (res.result == InviteResultType.DENIED && res.from != null) {
            res.from.sendPacket(PacketCreator.partyStatusMessage(23, bot.getName()));
        }
    }

    public static boolean say(Character bot, String text) {
        Party party = bot.getParty();
        if (party == null) {
            return false;
        }
        bot.getWorldServer().partyChat(party, text, bot.getName());
        return true;
    }

    public static void leave(Character bot) {
        bot.leaveParty();
    }

    public static InviteOutcome invite(Character bot, String targetName) {
        Character invited = bot.getWorldServer().getPlayerStorage().getCharacterByName(targetName);
        if (invited == null) {
            return InviteOutcome.NO_SUCH_PLAYER;
        }
        boolean starters = YamlConfig.config.server.USE_PARTY_FOR_STARTERS;
        if (invited.getLevel() < 10 && (!starters || bot.getLevel() >= 10)) {
            return InviteOutcome.REQUIREMENTS;
        }
        if (starters && invited.getLevel() >= 10 && bot.getLevel() < 10) {
            return InviteOutcome.REQUIREMENTS;
        }
        if (invited.getParty() != null) {
            return InviteOutcome.TARGET_IN_PARTY;
        }
        if (bot.getParty() == null && !Party.createParty(bot, true)) {
            return InviteOutcome.CANNOT_CREATE;
        }
        Party party = bot.getParty();
        if (party.getMembers().size() >= 6) {
            return InviteOutcome.PARTY_FULL;
        }
        if (!InviteCoordinator.createInvite(InviteType.PARTY, bot, party.getId(), invited.getId())) {
            return InviteOutcome.ALREADY_INVITED;
        }
        invited.sendPacket(PacketCreator.partyInvite(bot));
        return InviteOutcome.SENT;
    }
}
