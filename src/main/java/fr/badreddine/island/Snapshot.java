package fr.badreddine.island;

/** Stable UI contract, built without a JSON dependency so the JVM and browser share it. */
final class Snapshot {
  private Snapshot() {}

  static String quote(String text) {
    StringBuilder b = new StringBuilder("\"");
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '"' || c == '\\') b.append('\\').append(c);
      else if (c == '\n') b.append("\\n");
      else if (c == '\r') b.append("\\r");
      else if (c == '\t') b.append("\\t");
      else if (c < 32) b.append(' ');
      else b.append(c);
    }
    return b.append('"').toString();
  }

  static String json(Engine e, int seed, int difficulty) {
    StringBuilder b =
        new StringBuilder("{\"version\":2,\"seed\":")
            .append(seed)
            .append(",\"difficulty\":")
            .append(difficulty);
    b.append(",\"phase\":")
        .append(quote(e.phase.name()))
        .append(",\"active\":")
        .append(e.active)
        .append(",\"actions\":")
        .append(e.actions)
        .append(",\"turn\":")
        .append(e.turn)
        .append(",\"water\":")
        .append(e.water)
        .append(",\"floodRate\":")
        .append(e.floodRate())
        .append(",\"landing\":")
        .append(e.landing)
        .append(",\"pendingPlayer\":")
        .append(e.pendingPlayer)
        .append(",\"paused\":")
        .append(e.paused)
        .append(",\"accepted\":")
        .append(e.accepted())
        .append(",\"message\":")
        .append(quote(e.message()))
        .append(",\"engineerBonus\":")
        .append(e.engineerBonus)
        .append(",\"pilotUsed\":")
        .append(e.pilotUsed);
    b.append(",\"treasures\":[");
    for (int i = 0; i < 4; i++) {
      if (i > 0) b.append(',');
      b.append(e.treasures[i]);
    }
    b.append("]");
    b.append(",\"tiles\":[");
    for (Engine.Tile t : e.tiles) {
      if (t.id > 0) b.append(',');
      b.append("{\"id\":")
          .append(t.id)
          .append(",\"x\":")
          .append(t.x)
          .append(",\"y\":")
          .append(t.y)
          .append(",\"state\":")
          .append(t.state)
          .append(",\"treasure\":")
          .append(t.treasure)
          .append(",\"name\":")
          .append(quote(t.name))
          .append(",\"move\":")
          .append(e.canMove(t.id))
          .append(",\"shore\":")
          .append(e.canShore(t.id))
          .append(",\"rescue\":")
          .append(e.canRescue(t.id))
          .append('}');
    }
    b.append(']');
    b.append(",\"players\":[");
    for (int i = 0; i < e.players.size(); i++) {
      if (i > 0) b.append(',');
      Engine.Player p = e.players.get(i);
      b.append("{\"role\":")
          .append(quote(p.role.name()))
          .append(",\"name\":")
          .append(quote(Engine.ROLE_NAMES[p.role.ordinal()]))
          .append(",\"tile\":")
          .append(p.tile)
          .append(",\"hand\":")
          .append(p.hand)
          .append(",\"navigate\":[");
      boolean first = true;
      for (Engine.Tile t : e.tiles)
        if (e.canNavigate(i, t.id)) {
          if (!first) b.append(',');
          first = false;
          b.append(t.id);
        }
      b.append("]}");
    }
    b.append(']');
    b.append(",\"canCapture\":")
        .append(e.canCapture())
        .append(",\"decks\":{\"treasure\":")
        .append(e.treasureDeck.size())
        .append(",\"flood\":")
        .append(e.floodDeck.size())
        .append(",\"floodDiscard\":")
        .append(e.floodDiscard)
        .append(",\"treasureDiscard\":")
        .append(e.treasureDiscard)
        .append("},\"journal\":[");
    for (int i = 0; i < e.journal.size(); i++) {
      if (i > 0) b.append(',');
      b.append(quote(e.journal.get(i)));
    }
    return b.append("]}").toString();
  }
}
