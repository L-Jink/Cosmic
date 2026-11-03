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

    /** An online bot. */
    interface SimHandle {
        int characterId();

        int mapId();

        void leave();
    }
}
