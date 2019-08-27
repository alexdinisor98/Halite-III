// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.ArrayList;
import java.util.Random;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        game.ready("MyJavaBot");
        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

      
         // remembering the previous ship position to avoid going in the same spot       
        ArrayList<Position> lastPos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lastPos.add(i, new Position(-Integer.MAX_VALUE, -Integer.MAX_VALUE));
        }

        /**
         * Each ship will have a status: "collecting",
         * "depositing", "IWillDrop" or "dropping",
         * Initilizing all ships on collecting.
         */
        String[] shipStates = new String[100];
        for (int i = 0; i < 100; i++) {
            shipStates[i] = "collecting";
        }

        /**
         * DropOff position on the spot with maximum halite on the map
         * or in the south of the map.
         */
        SubMap div = new SubMap(0, 0, 0, 0, 0);
        ArrayList<SubMap> HALITE = new ArrayList<>();

        HALITE = div.divideMap(game.gameMap);
        Position posDrop = null;
        if (game.gameMap.width == 32 || game.gameMap.width == 56) {
            posDrop = Strategy.setPosDrop(HALITE);
        } else {
            if (game.gameMap.width == 40 || game.gameMap.width == 48) {
                posDrop = new Position(0, game.gameMap.height - 8);
            }
        }

        int okay = 0;
        int weHaveDrop = 0;
        int round = 0;
        double cargoPercentage;

        for (; ; ) {
            round++;
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;
            Position p = new Position(me.shipyard.position.x + 1, me.shipyard.position.y);
            final ArrayList<Command> commandQueue = new ArrayList<>();
            
            // possible cardinal points, including stay still.
            ArrayList<Direction> directionOrder = new ArrayList<>();
            directionOrder.add(Direction.NORTH);
            directionOrder.add(Direction.SOUTH);
            directionOrder.add(Direction.EAST);
            directionOrder.add(Direction.WEST);
            directionOrder.add(Direction.STILL);

            for (final Ship ship : me.ships.values()) {
                 // avoiding an enemy entering our shipyard
                if (gameMap.width == 32) {
                    if (round >= 50 && me.halite <= 100) {
                        if (ship.position.equals(p)) {
                            commandQueue.add(ship.move(Direction.WEST));
                            continue;
                        }
                    }
                }
                // possible coords of a ship to progress in another direction
                ArrayList<Position> positionOptions = new ArrayList<>();
                positionOptions = ship.position.getSurroundingCardinals();
                positionOptions.add(ship.position);
                
                // list with maximum halite of size = 5: N, S, V, E, current location.
                ArrayList<Integer> haliteAmountList = new ArrayList<>();
                for (int i = 0; i < directionOrder.size(); i++) {
                    haliteAmountList.add(i, -1);
                }
                // check if each position is occupied or not by another ship.
                // encoding: 0 - occupied.
                // It should not go on the same spot twice
                for (int i = 0; i < directionOrder.size(); i++) {
                    if ((!gameMap.at(positionOptions.get(i)).isOccupied())
                            && (!lastPos.get(ship.id.id).equals(positionOptions.get(i)))) {

                        haliteAmountList.add(i, gameMap.at(positionOptions.get(i)).halite);
                    } else {
                        haliteAmountList.add(i, 0);
                    }
                }

                // The corresponding index of the max halite value to go into the same spot
                int maxHalite = java.util.Collections.max(haliteAmountList);
                int index = -1;
                for (int i = 0; i < directionOrder.size(); i++) {
                    if (haliteAmountList.get(i) == maxHalite) {
                        index = i;
                    }
                }
                
                // Check ship status and update it.
                if (shipStates[ship.id.id].equals("dropping")) {
                    commandQueue.add(ship.move(gameMap.naiveNavigate(ship, posDrop)));
                    if (ship.position.equals(posDrop)) {
                        shipStates[ship.id.id] = "IWillDrop";
                    }
                    continue;
                }

                if ((shipStates[ship.id.id].equals("IWillDrop") && me.halite >= 4000)) {
                    commandQueue.add(ship.makeDropoff());
                    weHaveDrop = 1;
                    shipStates[ship.id.id] = "NULL";
                    continue;
                }

                // Ships enter the shipyard in the final round to gathe final collected halite.
                // Next rounds are for collecting in dropOff or shipyard.
                if (shipStates[ship.id.id].equals("depositing")) {
                    if (weHaveDrop == 0) {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position)));
                        if (ship.position.equals(me.shipyard.position)) {
                            shipStates[ship.id.id] = "collecting";
                        }
                    } else {
                        if (gameMap.width == 32) {
                            Strategy.depositInDropOff(game, ship, posDrop, commandQueue,
                                    shipStates, Strategy.COLLISION_TURN32Map);
                        }
                        if (gameMap.width == 40 || gameMap.width == 48) {
                            Strategy.depositInDropOff(game, ship, posDrop, commandQueue,
                                    shipStates, Strategy.COLLISION_TURN40Map);
                        }
                        if (gameMap.width == 56) {
                            Strategy.depositInDropOff(game, ship, posDrop, commandQueue,
                                    shipStates, Strategy.COLLISION_TURN56Map);
                        }
                    }
                    /**
                     * colecteaza halite si merge in dropOff in functie de cantitatea colectata.
                     */
                } else if (shipStates[ship.id.id].equals("collecting")) {

                    if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10) {
                        lastPos.set(ship.id.id, ship.position);
                        gameMap.at(positionOptions.get(index)).markUnsafe(ship);
                        commandQueue.add(ship.move(directionOrder.get(index)));
                    }

                    if (gameMap.width == 32) {
                        cargoPercentage = 0.6;
                        Strategy.goToDepo(ship, shipStates, game, Strategy.COLLISION_TURN32Map, cargoPercentage);
                    }

                    if (gameMap.width == 40 || gameMap.width == 48) {
                        cargoPercentage = 0.4;
                        Strategy.goToDepo(ship, shipStates, game, Strategy.COLLISION_TURN40Map, cargoPercentage);
                    }

                    if (gameMap.width == 56) {
                        cargoPercentage = 0.6;
                        Strategy.goToDepo(ship, shipStates, game, Strategy.COLLISION_TURN56Map, cargoPercentage);
                    }
                }
            }

            // Ship spawn depending on the map dimension.
            if (game.turnNumber <= 200 && me.halite >= Constants.SHIP_COST && !gameMap.at(me.shipyard).isOccupied()) {
                if (weHaveDrop == 1) {
                    Strategy.spawnMapSize(gameMap, me, commandQueue);
                }
                if (okay == 0) {
                    if (me.ships.values().size() < 8) {
                        commandQueue.add(me.shipyard.spawn());
                    } else {
                        if (me.halite >= 5001) {
                            okay = 1;
                            shipStates[1] = "dropping";
                        }
                    }
                }
            }
            game.endTurn(commandQueue);
        }
    }
}
