package alphaciv.domain;

/**
 * Skeleton implementation of HotCiv.
 * 
 * This source code is from the book "Flexible, Reliable Software: Using
 * Patterns and Agile Development" published 2010 by CRC Press. Author: Henrik B
 * Christensen Computer Science Department Aarhus University
 * 
 * This source code is provided WITHOUT ANY WARRANTY either expressed or
 * implied. You may study, use, modify, and distribute it for non-commercial
 * purposes. For any commercial use, see http://www.baerbak.com/
 */

public class GameImpl implements Game {
	private Tile[][] tiles = new Tile[GameConstants.WORLDSIZE][GameConstants.WORLDSIZE];
	private Unit[][] units = new Unit[GameConstants.WORLDSIZE][GameConstants.WORLDSIZE];
	private City[][] cities = new City[GameConstants.WORLDSIZE][GameConstants.WORLDSIZE];

	private Player playerInTurn;
	private int age = GameConstants.STARTAGE;

	private AgingStrategy agingStrategy;
	private WinningStrategy winningStrategy;
	private UnitActionStrategy unitActionStrategy;
	private UnitMovementStrategy unitMovementStrategy;
	private WorldCreationStrategy worldCreationStrategy;

	public GameImpl(AgingStrategy agingStrategy, WinningStrategy winningStrategy, UnitActionStrategy unitActionStrategy,
			UnitMovementStrategy unitMovementStrategy, WorldCreationStrategy worldCreationStrategy) {
		playerInTurn = Player.RED;
		this.agingStrategy = agingStrategy;
		this.winningStrategy = winningStrategy;
		this.unitActionStrategy = unitActionStrategy;
		this.unitMovementStrategy = unitMovementStrategy;
		this.worldCreationStrategy = worldCreationStrategy;

		this.worldCreationStrategy.createWorld(tiles, units, cities);
	}

	public Tile getTileAt(Position p) {
		Tile tile = tiles[p.getRow()][p.getColumn()];
		if (tile != null)
			return tile;
		else
			return new TileImpl(GameConstants.PLAINS);
	}

	public Unit getUnitAt(Position p) {
		return units[p.getRow()][p.getColumn()];
	}

	public City getCityAt(Position p) {
		return cities[p.getRow()][p.getColumn()];
	}

	public Player getPlayerInTurn() {
		return playerInTurn;
	}

	public Player getWinner() {
		return winningStrategy.getWinner(age, cities);

	}

	public int getAge() {
		return age;
	}

	public boolean moveUnit(Position from, Position to) {
		Unit unitAtFromPosition = getUnitAt(from);
		if (unitAtFromPosition.getOwner() != playerInTurn)
			return false;
		if (!unitMovementStrategy.canMoveUnit(unitAtFromPosition))
			return false;

		Tile tileAtToPosition = getTileAt(to);
		if (tileAtToPosition.getTypeString().equals(GameConstants.OCEANS)
				|| tileAtToPosition.getTypeString().equals(GameConstants.MOUNTAINS))
			return false;
		if (Math.abs(to.getRow() - from.getRow()) <= 1 && Math.abs(to.getColumn() - from.getColumn()) <= 1) {
			units[to.getRow()][to.getColumn()] = unitAtFromPosition;
			units[from.getRow()][from.getColumn()] = null;
			if (cities[to.getRow()][to.getColumn()] != null) {
				((CityImpl) cities[to.getRow()][to.getColumn()]).setOwner(unitAtFromPosition.getOwner());
			}
			return true;
		}
		return false;
	}

	public void endOfTurn() {
		if (playerInTurn == Player.RED) {
			playerInTurn = Player.BLUE;
		} else {
			playerInTurn = Player.RED;
			age = agingStrategy.getNewAge(age);
			gatherAllCitiesResources();
			produceAllCitiesUnits();
		}
	}

	private void gatherAllCitiesResources() {
		for (int i = 0; i < cities.length; i++) {
			for (int j = 0; j < cities[i].length; j++) {
				CityImpl c = (CityImpl) cities[i][j];
				if (c != null) {
					c.addResources(GameConstants.PRODPERROUND);
				}
			}
		}
	}

	private void produceAllCitiesUnits() {
		for (int i = 0; i < cities.length; i++) {
			for (int j = 0; j < cities[i].length; j++) {
				CityImpl c = (CityImpl) cities[i][j];
				if (c != null) {
					tryToPlaceUnitsAtPosition(new Position(i, j), c);
					tryToPlaceUnitsAtPosition(new Position(i - 1, j), c);
					tryToPlaceUnitsAtPosition(new Position(i - 1, j + 1), c);
					tryToPlaceUnitsAtPosition(new Position(i, j + 1), c);
					tryToPlaceUnitsAtPosition(new Position(i + 1, j + 1), c);
					tryToPlaceUnitsAtPosition(new Position(i + 1, j), c);
					tryToPlaceUnitsAtPosition(new Position(i + 1, j - 1), c);
					tryToPlaceUnitsAtPosition(new Position(i, j - 1), c);
					tryToPlaceUnitsAtPosition(new Position(i - 1, j - 1), c);
				}
			}
		}
	}

	private void tryToPlaceUnitsAtPosition(Position position, CityImpl c) {
		if (getUnitAt(position) != null) {
			return;
		}
		if (getTileAt(position).getTypeString().equals(GameConstants.MOUNTAINS)
				|| getTileAt(position).getTypeString().equals(GameConstants.OCEANS)) {
			return;
		}
		if (!c.canProduceUnit()) {
			return;
		}
		units[position.getRow()][position.getColumn()] = new UnitImpl(c.getProduction(), c.getOwner());
		c.removeResources(GameConstants.getCostForUnit(c.getProduction()));
	}

	public void changeWorkForceFocusInCityAt(Position p, String balance) {
	}

	public void changeProductionInCityAt(Position p, String unitType) {
		CityImpl c = (CityImpl) getCityAt(p);
		if (c != null && c.getOwner() == playerInTurn) {
			c.setProductionType(unitType);
		}
	}

	public void performUnitActionAt(Position p) {
		if (playerInTurn == getUnitAt(p).getOwner()) {
			unitActionStrategy.performUnitAction(p, new UnitActionRequirementsImpl(cities, units));
		}
	}
}
