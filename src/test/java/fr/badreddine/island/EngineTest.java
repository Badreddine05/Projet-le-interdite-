package fr.badreddine.island;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class EngineTest {
  static int at(Engine e, int x, int y) {
    return e.tiles.stream().filter(t -> t.x == x && t.y == y).findFirst().orElseThrow().id;
  }

  static Engine fixture(Engine.Role role) {
    Engine e = new Engine(42, 2, 1);
    for (Engine.Tile t : e.tiles) t.state = 0;
    e.players.clear();
    e.players.add(new Engine.Player(role, at(e, 2, 2)));
    e.players.add(new Engine.Player(Engine.Role.ENGINEER, at(e, 3, 2)));
    e.treasureDeck.clear();
    for (int i = 0; i < 20; i++) e.treasureDeck.add(i % 4);
    e.treasureDiscard.clear();
    e.floodDeck.clear();
    e.floodDiscard.clear();
    for (Engine.Tile t : e.tiles)
      if (t.id != e.landing && t.id != e.players.get(0).tile && t.id != e.players.get(1).tile)
        e.floodDeck.add(t.id);
    return e;
  }

  @Test
  void validSetupAndSeed() {
    for (int seed = 0; seed < 100; seed++) {
      Engine a = new Engine(seed, 4, 1);
      assertEquals(24, a.tiles.size());
      assertEquals(6, a.tiles.stream().filter(t -> t.state == 1).count());
      assertEquals(6, a.floodDiscard.size());
      assertEquals(4, a.players.stream().map(p -> p.role).distinct().count());
      assertEquals(
          28, a.treasureDeck.size() + a.players.stream().mapToInt(p -> p.hand.size()).sum());
      assertEquals(a.json(), new Engine(seed, 4, 1).json());
      for (int t = 0; t < 4; t++) {
        final int kind = t;
        assertEquals(2, a.tiles.stream().filter(tile -> tile.treasure == kind).count());
      }
    }
  }

  @Test
  void invalidConfiguration() {
    assertThrows(IllegalArgumentException.class, () -> new Engine(0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new Engine(0, 5, 1));
    assertThrows(IllegalArgumentException.class, () -> new Engine(0, 2, 0));
  }

  @Test
  void movingCostsOneAndNeverEntersSunkTile() {
    Engine e = fixture(Engine.Role.MESSENGER);
    int target = at(e, 2, 1);
    assertTrue(e.command("move:" + target));
    assertEquals(2, e.actions);
    assertEquals(target, e.players.get(0).tile);
    int sunk = at(e, 3, 1);
    e.tiles.get(sunk).state = 2;
    assertFalse(e.command("move:" + sunk));
    assertEquals(2, e.actions);
  }

  @Test
  void playersMayShareTiles() {
    Engine e = fixture(Engine.Role.MESSENGER);
    assertTrue(e.command("move:" + e.players.get(1).tile));
    assertEquals(e.players.get(0).tile, e.players.get(1).tile);
  }

  @Test
  void shoreCostsActionAndWorksAtNorthSeaBoundary() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).tile = at(e, 2, 0);
    int south = at(e, 2, 1);
    e.tiles.get(south).state = 1;
    assertTrue(e.command("shore:" + south));
    assertEquals(0, e.tiles.get(south).state);
    assertEquals(2, e.actions);
    e.actions = 0;
    e.tiles.get(south).state = 1;
    assertFalse(e.command("shore:" + south));
    assertEquals(1, e.tiles.get(south).state);
  }

  @Test
  void captureConsumesCardsActionAndCannotRepeat() {
    Engine e = fixture(Engine.Role.MESSENGER);
    Engine.Tile t = e.tiles.stream().filter(x -> x.treasure == 0).findFirst().orElseThrow();
    e.players.get(0).tile = t.id;
    t.state = 1;
    e.players.get(0).hand.addAll(List.of(0, 0, 0, 0));
    assertTrue(e.command("capture"));
    assertTrue(e.treasures[0]);
    assertEquals(0, e.players.get(0).hand.size());
    assertEquals(4, e.treasureDiscard.size());
    assertEquals(2, e.actions);
    assertFalse(e.command("capture"));
  }

  @Test
  void engineerMayShoreTwoForOneIncludingFinalBonus() {
    Engine e = fixture(Engine.Role.ENGINEER);
    int a = e.players.get(0).tile, b = at(e, 2, 1);
    e.tiles.get(a).state = 1;
    e.tiles.get(b).state = 1;
    e.actions = 1;
    assertTrue(e.command("shore:" + a));
    assertEquals(0, e.actions);
    assertTrue(e.command("shore:" + b));
    assertEquals(0, e.actions);
    assertFalse(e.engineerBonus);
  }

  @Test
  void explorerMovesAndShoresDiagonally() {
    Engine e = fixture(Engine.Role.EXPLORER);
    int t = at(e, 3, 3);
    e.tiles.get(t).state = 1;
    assertTrue(e.command("shore:" + t));
    assertTrue(e.command("move:" + t));
    assertEquals(1, e.actions);
  }

  @Test
  void pilotFlightOnlyOncePerTurn() {
    Engine e = fixture(Engine.Role.PILOT);
    assertTrue(e.command("move:" + at(e, 3, 5)));
    assertTrue(e.pilotUsed);
    assertFalse(e.command("move:" + at(e, 2, 0)));
    assertEquals(2, e.actions);
  }

  @Test
  void diverCrossesFloodedOrSunkButNotDryIntermediates() {
    Engine e = fixture(Engine.Role.DIVER);
    e.players.get(0).tile = at(e, 0, 2);
    int target = at(e, 2, 2);
    assertFalse(e.canMove(target));
    e.tiles.get(at(e, 1, 2)).state = 2;
    assertTrue(e.command("move:" + target));
    assertEquals(2, e.actions);
  }

  @Test
  void navigatorMovesOtherPlayerUpToTwoSteps() {
    Engine e = fixture(Engine.Role.NAVIGATOR);
    int target = at(e, 4, 3);
    assertTrue(e.command("nav:1:" + target));
    assertEquals(target, e.players.get(1).tile);
    assertEquals(2, e.actions);
    assertFalse(e.command("nav:1:" + at(e, 1, 2)));
  }

  @Test
  void messengerGivesAtDistanceAndHandOverflowBlocksTurn() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).hand.add(0);
    e.players.get(1).hand.addAll(List.of(1, 1, 2, 2, 3));
    assertTrue(e.command("give:1:0"));
    assertEquals(Engine.Phase.DISCARD, e.phase);
    assertEquals(1, e.pendingPlayer);
    assertFalse(e.command("end"));
    assertFalse(e.command("discard:0:0"));
    assertTrue(e.command("discard:1:3"));
    assertEquals(Engine.Phase.ACTION, e.phase);
    assertEquals(2, e.actions);
  }

  @Test
  void normalRoleCannotGiveAcrossIsland() {
    Engine e = fixture(Engine.Role.DIVER);
    e.players.get(0).hand.add(0);
    assertFalse(e.command("give:1:0"));
    assertEquals(3, e.actions);
  }

  @Test
  void turnDrawsTwoThenFloodsAndChangesPlayer() {
    Engine e = fixture(Engine.Role.MESSENGER);
    assertTrue(e.command("end"));
    assertEquals(2, e.players.get(0).hand.size());
    assertEquals(2, e.tiles.stream().filter(t -> t.state == 1).count());
    assertEquals(1, e.active);
    assertEquals(2, e.turn);
    assertEquals(3, e.actions);
  }

  @Test
  void discardInterruptsAndResumesDrawPhaseExactlyOnce() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).hand.addAll(List.of(0, 0, 1, 1, 2));
    assertTrue(e.command("end"));
    assertEquals(Engine.Phase.DISCARD, e.phase);
    assertEquals(1, e.drawRemaining);
    assertTrue(e.command("discard:0:2"));
    assertEquals(Engine.Phase.DISCARD, e.phase);
    assertEquals(0, e.drawRemaining);
    assertTrue(e.command("discard:0:1"));
    assertEquals(1, e.active);
    assertEquals(5, e.players.get(0).hand.size());
    assertEquals(2, e.tiles.stream().filter(t -> t.state == 1).count());
  }

  @Test
  void watersRiseRecyclesDiscardAndRaisesFloodRate() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.water = 2;
    e.treasureDeck.add(0, Engine.RISE);
    int t = e.floodDeck.remove(0);
    e.tiles.get(t).state = 1;
    e.floodDiscard.add(t);
    e.command("end");
    assertEquals(3, e.water);
    assertEquals(3, e.floodRate());
    assertEquals(2, e.tiles.get(t).state);
    assertFalse(e.floodDeck.contains(t));
    assertFalse(e.floodDiscard.contains(t));
  }

  @Test
  void sunkTileNeverReturnsInLongRun() {
    Engine e = fixture(Engine.Role.MESSENGER);
    int t = e.floodDeck.get(0);
    e.tiles.get(t).state = 1;
    e.command("end");
    assertEquals(2, e.tiles.get(t).state);
    for (int i = 0; i < 20 && e.phase == Engine.Phase.ACTION; i++) e.command("end");
    assertEquals(2, e.tiles.get(t).state);
  }

  @Test
  void specialCardsAreFreeButNeverRecoverSunkTiles() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(1).hand.add(Engine.SANDBAGS);
    int t = at(e, 2, 5);
    e.tiles.get(t).state = 2;
    assertFalse(e.command("sand:1:" + t));
    assertEquals(1, e.players.get(1).hand.size());
    e.tiles.get(t).state = 1;
    assertTrue(e.command("sand:1:" + t));
    assertEquals(0, e.tiles.get(t).state);
    assertEquals(3, e.actions);
  }

  @Test
  void helicopterValidatesEntirePassengerGroupBeforeMutation() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).hand.add(Engine.HELICOPTER);
    int target = at(e, 2, 5);
    assertFalse(e.command("heli:0:" + target + ":3"));
    assertEquals(1, e.players.get(0).hand.size());
    e.players.get(1).tile = e.players.get(0).tile;
    assertTrue(e.command("heli:0:" + target + ":3"));
    assertEquals(target, e.players.get(1).tile);
    assertEquals(target, e.players.get(0).tile);
    assertEquals(3, e.actions);
  }

  @Test
  void victoryRequiresFourTreasuresAllPlayersAndHelicopter() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).hand.add(4);
    assertFalse(e.command("escape:0"));
    Arrays.fill(e.treasures, true);
    assertFalse(e.command("escape:0"));
    for (Engine.Player p : e.players) p.tile = e.landing;
    assertTrue(e.command("escape:0"));
    assertEquals(Engine.Phase.WON, e.phase);
    assertFalse(e.command("end"));
  }

  @Test
  void lossWhenLandingSinks() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.tiles.get(e.landing).state = 1;
    e.floodDeck.add(0, e.landing);
    e.command("end");
    assertEquals(Engine.Phase.LOST, e.phase);
    assertTrue(e.message().contains("héliport"));
  }

  @Test
  void lossWhenBothUnclaimedTreasureSitesSink() {
    Engine e = fixture(Engine.Role.MESSENGER);
    List<Engine.Tile> sites = e.tiles.stream().filter(t -> t.treasure == 0).toList();
    sites.get(0).state = 2;
    sites.get(1).state = 1;
    e.floodDeck.remove(Integer.valueOf(sites.get(1).id));
    e.floodDeck.add(0, sites.get(1).id);
    e.command("end");
    assertEquals(Engine.Phase.LOST, e.phase);
    assertTrue(e.message().contains("deux sites"));
  }

  @Test
  void lossWhenWaterReachesTen() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.water = 9;
    e.treasureDeck.add(0, Engine.RISE);
    e.command("end");
    assertEquals(Engine.Phase.LOST, e.phase);
    assertEquals(10, e.water);
  }

  @Test
  void emergencySwimResolvesBeforeNextFlood() {
    Engine e = fixture(Engine.Role.MESSENGER);
    Arrays.fill(e.treasures, true);
    int origin = e.players.get(0).tile;
    e.tiles.get(origin).state = 1;
    e.floodDeck.add(0, origin);
    e.command("end");
    assertEquals(Engine.Phase.RESCUE, e.phase);
    assertEquals(0, e.active);
    int target = at(e, 2, 1);
    assertTrue(e.command("rescue:" + target));
    assertEquals(target, e.players.get(0).tile);
    assertEquals(1, e.active);
  }

  @Test
  void isolatedSwimmerLoses() {
    Engine e = fixture(Engine.Role.MESSENGER);
    Arrays.fill(e.treasures, true);
    int origin = e.players.get(0).tile;
    e.players.get(1).tile = at(e, 3, 5);
    e.tiles.get(origin).state = 1;
    for (Engine.Tile t : e.tiles) if (Math.abs(t.x - 2) + Math.abs(t.y - 2) == 1) t.state = 2;
    e.floodDeck.add(0, origin);
    e.command("end");
    assertEquals(Engine.Phase.LOST, e.phase);
    assertTrue(e.message().contains("ne peut plus"));
  }

  @Test
  void diverRescueUsesNearestSurvivor() {
    Engine e = fixture(Engine.Role.DIVER);
    e.players.get(0).tile = at(e, 0, 2);
    e.tiles.get(at(e, 0, 2)).state = 2;
    e.tiles.get(at(e, 1, 2)).state = 2;
    e.tiles.get(at(e, 0, 3)).state = 2;
    e.phase = Engine.Phase.RESCUE;
    e.pendingPlayer = 0;
    assertTrue(e.canRescue(at(e, 2, 2)));
    assertFalse(e.canRescue(at(e, 3, 2)));
  }

  @Test
  void pauseAndMalformedInputNeverConsumeActions() {
    Engine e = fixture(Engine.Role.MESSENGER);
    assertFalse(e.command("move:garbage"));
    assertFalse(e.command("give:99:0"));
    assertFalse(e.command("nav"));
    assertTrue(e.command("pause"));
    assertFalse(e.command("end"));
    assertEquals(3, e.actions);
    assertEquals(1, e.turn);
    assertTrue(e.command("pause"));
  }

  @Test
  void treasureDeckRefillsImmediatelyBeforeHandOverflowChoice() {
    Engine e = fixture(Engine.Role.MESSENGER);
    e.players.get(0).hand.addAll(List.of(0, 0, 1, 1, 2));
    e.treasureDeck.clear();
    e.treasureDeck.add(3);
    e.treasureDiscard.addAll(List.of(2, 2, 2));
    e.command("end");
    assertEquals(Engine.Phase.DISCARD, e.phase);
    assertEquals(3, e.treasureDeck.size());
    assertTrue(e.treasureDiscard.isEmpty());
    e.command("discard:0:3");
    assertTrue(e.treasureDiscard.contains(3));
    assertFalse(e.treasureDeck.contains(3));
  }

  @Test
  void floodDeckRefillsOnFinalDrawWithoutResurrectingSunkTiles() {
    Engine e = fixture(Engine.Role.MESSENGER);
    int first = e.floodDeck.get(0), last = e.floodDeck.get(1);
    int recycled = e.floodDeck.get(2);
    e.floodDeck.clear();
    e.floodDeck.addAll(List.of(first, last));
    e.tiles.get(last).state = 1;
    e.tiles.get(recycled).state = 1;
    e.floodDiscard.add(recycled);
    e.command("end");
    assertEquals(2, e.tiles.get(last).state);
    assertTrue(e.floodDiscard.isEmpty());
    assertEquals(2, e.floodDeck.size());
    assertFalse(e.floodDeck.contains(last));
    assertTrue(e.floodDeck.contains(first));
    assertTrue(e.floodDeck.contains(recycled));
  }

  @Test
  void secondRescueResumesSameFloodPhaseWithoutDrawingTreasureAgain() {
    Engine e = fixture(Engine.Role.MESSENGER);
    Arrays.fill(e.treasures, true);
    int origin = e.players.get(0).tile;
    e.players.get(1).tile = origin;
    e.tiles.get(origin).state = 1;
    e.floodDeck.add(0, origin);
    e.command("end");
    assertEquals(0, e.pendingPlayer);
    int treasureRemaining = e.treasureDeck.size();
    e.command("rescue:" + at(e, 2, 1));
    assertEquals(Engine.Phase.RESCUE, e.phase);
    assertEquals(1, e.pendingPlayer);
    e.command("rescue:" + at(e, 3, 2));
    assertEquals(treasureRemaining, e.treasureDeck.size());
    assertEquals(1, e.active);
    assertEquals(2, e.turn);
  }

  @Test
  void manySeededGamesMaintainInvariants() {
    for (int seed = 1; seed <= 100; seed++) {
      Engine e = new Engine(seed, 2 + seed % 3, 1 + seed % 4);
      Random r = new Random(seed);
      for (int turn = 0;
          turn < 300 && e.phase != Engine.Phase.WON && e.phase != Engine.Phase.LOST;
          turn++) {
        if (e.phase == Engine.Phase.DISCARD)
          e.command(
              "discard:" + e.pendingPlayer + ":" + e.players.get(e.pendingPlayer).hand.get(0));
        else if (e.phase == Engine.Phase.RESCUE) {
          int t = e.tiles.stream().filter(x -> e.canRescue(x.id)).findFirst().orElseThrow().id;
          e.command("rescue:" + t);
        } else {
          List<Integer> moves =
              e.tiles.stream().filter(t -> e.canMove(t.id)).map(t -> t.id).toList();
          if (e.actions > 0 && !moves.isEmpty() && r.nextBoolean())
            e.command("move:" + moves.get(r.nextInt(moves.size())));
          else e.command("end");
        }
        assertTrue(e.actions >= 0 && e.actions <= 3);
        assertTrue(e.water >= 1 && e.water <= 10);
        for (Engine.Tile t : e.tiles) assertTrue(t.state >= 0 && t.state <= 2);
        for (int id : e.floodDeck) assertNotEquals(2, e.tiles.get(id).state);
        for (int id : e.floodDiscard) assertNotEquals(2, e.tiles.get(id).state);
        int cards =
            e.treasureDeck.size()
                + e.treasureDiscard.size()
                + e.players.stream().mapToInt(p -> p.hand.size()).sum();
        assertEquals(28, cards);
      }
    }
  }
}
