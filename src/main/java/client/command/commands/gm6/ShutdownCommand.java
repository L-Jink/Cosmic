/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.TimerManager;
import server.liveops.Maintenance;

import java.time.Duration;

public class ShutdownCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(ShutdownCommand.class);

    {
        setDescription("Shut down the server (maintenance countdown with world notices).");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !shutdown [<minutes>|NOW]");
            return;
        }

        if (params[0].equalsIgnoreCase("now")) {
            log.info("{} ran !shutdown now", player.getName());
            TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), 1);
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Syntax: !shutdown [<minutes>|NOW]");
            return;
        }
        if (minutes < 1) {
            player.yellowMessage("Minutes must be at least 1.");
            return;
        }

        // Countdown flow: world notices (T-10/5/1 min, 30s, 10s), logins blocked at T-0, then clean shutdown.
        log.info("{} started a {} minute maintenance countdown via !shutdown.", player.getName(), minutes);
        if (!Maintenance.start(Duration.ofMinutes(minutes), player.getName())) {
            player.yellowMessage("A maintenance countdown is already running.");
        }
    }
}
