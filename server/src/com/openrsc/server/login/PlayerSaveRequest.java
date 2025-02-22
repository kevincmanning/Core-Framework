package com.openrsc.server.login;

import com.openrsc.server.Server;
import com.openrsc.server.database.GameDatabaseException;
import com.openrsc.server.model.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used to verify save players on the Login thread
 */
public class PlayerSaveRequest extends LoginExecutorProcess {
	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	private final Server server;
	private final Player player;
	private final boolean logout;

	public PlayerSaveRequest(final Server server, final Player player, boolean logout) {
		this.server = server;
		this.player = player;
		this.logout = logout;
	}

	public final Player getPlayer() {
		return player;
	}

	public final Server getServer() {
		return server;
	}

	protected void processInternal() {
		//LOGGER.info("Saved player " + playerToSave.getUsername() + "");
		try {
			boolean success = getServer().getDatabase().savePlayer(getPlayer());
			if (success && this.logout)
				logoutSaveSuccess();
		} catch (final GameDatabaseException ex) {
			LOGGER.catching(ex);
		}
	}

	public void logoutSaveSuccess() {

		getPlayer().setLoggedIn(false);

		/* IP Tracking in wilderness removal */
		/*if(player.getLocation().inWilderness())
		{
			wildernessIPTracker.remove(player.getCurrentIP());
		}*/

		for (Player other : getPlayer().getWorld().getPlayers()) {
			other.getSocial().alertOfLogout(getPlayer());
		}

		getPlayer().getWorld().getClanManager().checkAndUnattachFromClan(getPlayer());
		getPlayer().getWorld().getPartyManager().checkAndUnattachFromParty(getPlayer());

		getServer().getPacketFilter().removeLoggedInPlayer(getPlayer().getCurrentIP());

		getPlayer().remove();
		getServer().getWorld().getPlayers().remove(getPlayer());
		LOGGER.info("Removed player " + getPlayer().getUsername());
	}

}
