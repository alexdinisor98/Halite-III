package hlt;

import java.util.ArrayList;

public class Garbage {
    public final static int COLLISION_TURN32Map = 350;
    public final static int COLLISION_TURN40Map = 360;
    public final static int COLLISION_TURN56Map = 430;
    public final static int SPAWN_32Map = 15;
    public final static int SPAWN_40Map = 19;
    public final static int SPAWN_56Map = 22;

	/**
	 * Set the position to drop.
	 * 
	 * @param HALITE List of Halite
	 * @return the position to drop
	 */
    public static Position setPosDrop(ArrayList<SubMap> HALITE) {
        return new Position((HALITE.get(0).endCol + HALITE.get(0).startCol) / 2,
                (HALITE.get(0).startRow + HALITE.get(0).endRow) / 2);
    }

	/**
	 * Get the ships to collide in dropOff in final rounds.
	 * 
	 * @param ship         The current ship
	 * @param commandQueue The queue of commandQueue
	 * @param posDrop      The position to drop
	 */
    public static void collideInDropOff(Ship ship, ArrayList<Command> commandQueue, Position posDrop) {
        if (ship.position.x == posDrop.x && ship.position.y - 1 == posDrop.y) {
            commandQueue.add(ship.move(Direction.NORTH));
        }
        if (ship.position.x == posDrop.x && ship.position.y + 1 == posDrop.y) {
            commandQueue.add(ship.move(Direction.SOUTH));
        }
        if (ship.position.x - 1 == posDrop.x && ship.position.y == posDrop.y) {
            commandQueue.add(ship.move(Direction.WEST));
        }
        if (ship.position.x + 1 == posDrop.x && ship.position.y == posDrop.y) {
            commandQueue.add(ship.move(Direction.EAST));
        }
    }

	/**
	 * Ship changes its status to depositing.
	 * 
	 * @param ship            The current ship
	 * @param shipStates      The shipStates
	 * @param game            The game
	 * @param COLLISION_TURN  The turn for collision
	 * @param cargoPercentage The cargoPercentag
	 */
	public static void goToDepo(Ship ship, String[] shipStates, Game game, int COLLISION_TURN, double cargoPercentage) {
        if (ship.halite > Constants.MAX_HALITE * 0.90 && game.turnNumber <= COLLISION_TURN) {
            shipStates[ship.id.id] = "depositing";
        } else {
            if (ship.halite > Constants.MAX_HALITE * cargoPercentage && game.turnNumber > COLLISION_TURN) {
                shipStates[ship.id.id] = "depositing";
            }
        }
    }

	/**
	 * Decide how many ships to spawn depending on map size.
	 * 
	 * @param gameMap      The gameMap
	 * @param me           Player "me"
	 * @param commandQueue The queue of commands.
	 */
    public static void spawnMapSize(GameMap gameMap, Player me, ArrayList<Command> commandQueue) {
        if (gameMap.width == 32) {
            if (me.ships.values().size() < SPAWN_32Map) {
                commandQueue.add(me.shipyard.spawn());
            }
        } else {
            if (gameMap.width == 40 || gameMap.width == 48) {
                if (me.ships.values().size() < SPAWN_40Map) {
                    commandQueue.add(me.shipyard.spawn());
                }
            } else {
                if (me.ships.values().size() < SPAWN_56Map) {
                    commandQueue.add(me.shipyard.spawn());
                }
            }
        }
    }

    /**
	 * Decide to deposit in dropOff.
	 * 
	 * @param game           The game
	 * @param ship           The current ship
	 * @param posDrop        The position to drop
	 * @param commandQueue   The queue of commands
	 * @param shipStates     The shipStates
	 * @param COLLISION_TURN The turn for collision
	 */
	public static void depositInDropOff(Game game, Ship ship, Position posDrop, ArrayList<Command> commandQueue,
                       String[] shipStates, int COLLISION_TURN) {
        if (game.turnNumber > COLLISION_TURN
                && game.gameMap.calculateDistance(ship.position, posDrop) == 1) {
            Strategy.collideInDropOff(ship, commandQueue, posDrop);
        } else {
            if (game.turnNumber > COLLISION_TURN
                    && game.gameMap.calculateDistance(ship.position, posDrop) == 0) {
                commandQueue.add(ship.move(Direction.STILL));
            } else {
                commandQueue.add(ship.move(game.gameMap.naiveNavigate(ship, posDrop)));
                if (ship.position.equals(posDrop)) {
                    shipStates[ship.id.id] = "collecting";
                }
            }
        }
    }

}
