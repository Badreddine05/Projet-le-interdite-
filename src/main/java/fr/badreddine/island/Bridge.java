package fr.badreddine.island;

import org.teavm.jso.JSExport;

/** Thin browser boundary. All game decisions are made by Engine. */
public final class Bridge {
  private static Engine game = new Engine(2024, 2, 1);

  private Bridge() {}

  @JSExport
  public static String reset(int seed, int players, int difficulty) {
    game = new Engine(seed, players, difficulty);
    return game.json();
  }

  @JSExport
  public static String command(String input) {
    game.command(input);
    return game.json();
  }

  @JSExport
  public static String state() {
    return game.json();
  }
}
