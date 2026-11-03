package sim;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * The parts of the sim framework that touch the live server and the database. Split out so
 * {@link SimManager} logic can be unit tested without a running server.
 */
public interface SimBackend {
    /**
     * Brings the bot online in its home map (creating its DB row, flagged is_sim, if missing).
     *
     * @return a handle for the online bot
     * @throws Exception when the bot cannot enter (bad channel, name taken by a real player, ...)
     */
    SimHandle enter(SimBotConfig bot) throws Exception;

    /**
     * Deletes every is_sim character (and their inventory rows) from the database.
     *
     * @return number of characters deleted
     */
    int wipeDatabase();

    /** Clears a pending party invite addressed to a sim that has no live handle (e.g. sims were switched off). */
    default void declineStrayPartyInvite(int characterId, int partyId) {
    }

    /** A spot on a foothold: where a bot can stand. */
    record Ground(int x, int y, int fh) {
    }

    /** An online bot. */
    interface SimHandle {
        int characterId();

        int mapId();

        void leave();

        String name();

        boolean isInParty();

        /** Joins the party the pending invite is for. @return true if the bot is now a member. */
        boolean acceptPartyInvite(int partyId);

        /** Turns a pending invite down (the inviter is told). */
        void declinePartyInvite(int partyId);

        /** Says something in party chat. @return false if the bot has no party. */
        boolean partySay(String text);

        /** Invites an online player, creating a party for the bot if needed. */
        SimParty.InviteOutcome invite(String playerName);

        void leaveParty();

        // --- idle routine (defaults make test fakes and old handles do nothing) ---

        /** Where the bot stands, or null if unknown. */
        default Point position() {
            return null;
        }

        /** The map's walkable area; a bot never walks outside it. */
        default Rectangle walkBounds() {
            return null;
        }

        /** Foothold surface at x looking down from y, or null if there is none. */
        default Ground groundBelow(int x, int y) {
            return null;
        }

        /** Walks (visibly to everyone in the map) to a spot on a foothold. */
        default void walkTo(Ground to, boolean right, int durationMs) {
        }

        /** Sits on a chair item; false if it could not. */
        default boolean sit(int chairItemId) {
            return false;
        }

        default void stand() {
        }

        default boolean isSitting() {
            return false;
        }

        /** Says something in the map's general chat. */
        default void say(String text) {
        }
    }
}
