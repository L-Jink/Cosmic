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
            default -> gm.yellowMessage("Syntax: !sim list|spawn <name>|remove <name>|wipe");
        }
    }
}
