package sim;

import client.Character;
import client.Client;
import client.command.Command;

/** {@code !sim list|spawn <name>|remove <name>|wipe} */
public class SimCommand extends Command {
    {
        setDescription("Manage simulated players: !sim list|spawn <name>|remove <name>|wipe");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character gm = c.getPlayer();
        SimManager sims = SimManager.getInstance();
        String sub = params.length > 0 ? params[0] : "";

        switch (sub) {
            case "list" -> {
                gm.yellowMessage("Sim players " + (sims.isEnabled() ? "ENABLED" : "disabled (SIM_PLAYERS_ENABLED=false)")
                        + ", " + sims.onlineCount() + "/" + sims.getConfigured().size() + " online");
                sims.describe().forEach(gm::yellowMessage);
            }
            case "spawn" -> {
                if (params.length < 2) {
                    gm.yellowMessage("Syntax: !sim spawn <name>");
                    return;
                }
                switch (sims.spawn(params[1])) {
                    case SPAWNED -> gm.yellowMessage("Spawned " + params[1] + ".");
                    case DISABLED -> gm.yellowMessage("Sim players are disabled (SIM_PLAYERS_ENABLED=false).");
                    case UNKNOWN -> gm.yellowMessage(params[1] + " is not in SIM_BOTS.");
                    case ALREADY_ONLINE -> gm.yellowMessage(params[1] + " is already online.");
                    case FAILED -> gm.yellowMessage("Could not spawn " + params[1] + " (see server log).");
                }
            }
            case "remove" -> {
                if (params.length < 2) {
                    gm.yellowMessage("Syntax: !sim remove <name>");
                    return;
                }
                gm.yellowMessage(sims.remove(params[1]) ? "Removed " + params[1] + " from the world."
                        : params[1] + " is not an online sim.");
            }
            case "wipe" -> gm.yellowMessage("Removed all sims; deleted " + sims.wipe() + " sim character(s) from the database.");
            case "invite" -> {
                if (params.length < 3) {
                    gm.yellowMessage("Syntax: !sim invite <bot> <player>");
                    return;
                }
                SimParty.InviteOutcome o = sims.invite(params[1], params[2]);
                gm.yellowMessage(o == null ? params[1] + " is not an online sim." : params[1] + " invite to " + params[2] + ": " + o);
            }
            case "say" -> {
                if (params.length < 3) {
                    gm.yellowMessage("Syntax: !sim say <bot> <text>");
                    return;
                }
                String text = String.join(" ", java.util.Arrays.copyOfRange(params, 2, params.length));
                gm.yellowMessage(sims.partySay(params[1], text) ? "Sent." : params[1] + " is not an online sim in a party.");
            }
            case "leaveparty" -> {
                if (params.length < 2) {
                    gm.yellowMessage("Syntax: !sim leaveparty <bot>");
                    return;
                }
                gm.yellowMessage(sims.leaveParty(params[1]) ? params[1] + " left the party." : params[1] + " is not an online sim in a party.");
            }
            default -> gm.yellowMessage("Syntax: !sim list|spawn <name>|remove <name>|wipe|invite <bot> <player>|say <bot> <text>|leaveparty <bot>");
        }
    }
}
