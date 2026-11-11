package fr.badreddine.island;

/** JVM reference trace consumed by tests/bridge-parity.test.mjs. */
public final class ParityTrace {
  public static void main(String[] args) {
    for (int seed : new int[] {1, 42, 2024, 2000000000}) {
      int players = 2 + seed % 3, difficulty = 1 + seed % 4;
      Engine game = new Engine(seed, players, difficulty);
      System.out.println(
          "{\"reset\":["
              + seed
              + ","
              + players
              + ","
              + difficulty
              + "],\"state\":"
              + game.json()
              + "}");
      emit(game, "move:99");
      emit(game, "pause");
      emit(game, "end");
      emit(game, "pause");
      for (int step = 0;
          step < 150 && game.phase() != Engine.Phase.WON && game.phase() != Engine.Phase.LOST;
          step++) {
        String cmd = "end";
        if (game.phase() == Engine.Phase.DISCARD)
          cmd =
              "discard:"
                  + game.pendingPlayer()
                  + ":"
                  + game.players().get(game.pendingPlayer()).hand().get(0);
        else if (game.phase() == Engine.Phase.RESCUE) {
          for (Engine.Tile t : game.tiles())
            if (game.canRescue(t.id)) {
              cmd = "rescue:" + t.id;
              break;
            }
        } else if (game.canCapture()) cmd = "capture";
        else {
          boolean found = false;
          for (Engine.Tile t : game.tiles())
            if (game.canShore(t.id)) {
              cmd = "shore:" + t.id;
              found = true;
              break;
            }
          if (!found)
            for (Engine.Tile t : game.tiles())
              if (game.canMove(t.id)) {
                cmd = "move:" + t.id;
                break;
              }
        }
        emit(game, cmd);
      }
    }
  }

  private static void emit(Engine game, String cmd) {
    game.command(cmd);
    System.out.println("{\"command\":" + Snapshot.quote(cmd) + ",\"state\":" + game.json() + "}");
  }
}
