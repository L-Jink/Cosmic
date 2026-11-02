package testclient;

/**
 * Login server: password login, world list, character list (creates "QaBot" if the account has none), character
 * select. Channel server: PLAYER_LOGGEDIN, wait for SET_FIELD (the map-entry packet), then close the socket.
 */
public final class LoginEnterMapScenario implements Scenario {
    static final String BOT_NAME = "QaBot";

    @Override
    public String run(ScenarioContext ctx) throws Exception {
        try (GameSession s = GameSession.enterMap(ctx, ctx.account, ctx.password, BOT_NAME)) {
            return "character=" + s.charName + " id=" + s.charId + " map=" + s.listedMapId + " channel=" + (ctx.channel + 1);
        } // close == clean logout
    }
}
