package sim;

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
    }
}
