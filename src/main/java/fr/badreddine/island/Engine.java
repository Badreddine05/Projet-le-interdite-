package fr.badreddine.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic cooperative game. No graphics, I/O, threads or wall-clock dependency. */
public final class Engine {
  public enum Role {
    EXPLORER,
    ENGINEER,
    PILOT,
    DIVER,
    MESSENGER,
    NAVIGATOR
  }

  public enum Phase {
    ACTION,
    DISCARD,
    RESCUE,
    WON,
    LOST
  }

  public static final String[] ROLE_NAMES = {
    "Exploratrice", "Ingénieur", "Pilote", "Plongeur", "Messagère", "Navigateur"
  };
  public static final String[] CARD_NAMES = {
    "Cristal du feu",
    "Statue du vent",
    "Calice des marées",
    "Pierre de la terre",
    "Hélicoptère",
    "Sacs de sable",
    "Montée des eaux"
  };
  public static final int HELICOPTER = 4, SANDBAGS = 5, RISE = 6;

  public static final class Tile {
    public final int id, x, y, treasure;
    public final String name;
    int state; // 0 dry, 1 flooded, 2 sunk. Sinking is irreversible.

    Tile(int id, int x, int y, int treasure, String name) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.treasure = treasure;
      this.name = name;
    }

    public int state() {
      return state;
    }
  }

  public static final class Player {
    public final Role role;
    int tile;
    final List<Integer> hand = new ArrayList<>();

    Player(Role role, int tile) {
      this.role = role;
      this.tile = tile;
    }

    public int tile() {
      return tile;
    }

    public List<Integer> hand() {
      return Collections.unmodifiableList(hand);
    }
  }

  final List<Tile> tiles = new ArrayList<>();
  final List<Player> players = new ArrayList<>();
  final List<Integer> floodDeck = new ArrayList<>(),
      floodDiscard = new ArrayList<>(),
      treasureDeck = new ArrayList<>(),
      treasureDiscard = new ArrayList<>();
  final boolean[] treasures = new boolean[4];
  final List<String> journal = new ArrayList<>();
  Phase phase = Phase.ACTION;
  int active = 0,
      actions = 3,
      water,
      turn = 1,
      landing,
      drawRemaining = 0,
      floodRemaining = 0,
      pendingPlayer = -1;
  boolean pilotUsed = false, engineerBonus = false, ending = false, paused = false;
  private int rng;
  private final int seed, difficulty;
  private String message = "L’expédition commence. Choisissez une tuile pour agir.";
  private boolean accepted = true;

  public Engine(int seed, int count, int difficulty) {
    if (count < 2 || count > 4 || difficulty < 1 || difficulty > 4)
      throw new IllegalArgumentException("2–4 joueurs, difficulté 1–4.");
    this.seed = seed;
    this.difficulty = difficulty;
    this.rng = seed == 0 ? 0x6d2b79f5 : seed;
    water = difficulty;
    List<Integer> layout = new ArrayList<>();
    for (int i = 0; i < 24; i++) layout.add(i);
    shuffle(layout);
    String[] names = {
      "Temple des braises",
      "Sanctuaire du feu",
      "Palais des vents",
      "Tour des nuages",
      "Jardin des marées",
      "Palais de corail",
      "Caverne d’émeraude",
      "Rocher des racines",
      "Héliport",
      "Porte de cuivre",
      "Porte d’ivoire",
      "Porte d’ambre",
      "Porte de jade",
      "Porte d’azur",
      "Observatoire",
      "Pont suspendu",
      "Lagon oublié",
      "Forêt des brumes",
      "Ruines du soleil",
      "Grotte lunaire",
      "Cour des échos",
      "Dunes d’argent",
      "Falaises du nord",
      "Place des anciens"
    };
    int n = 0;
    for (int y = 0; y < 6; y++)
      for (int x = 0; x < 6; x++)
        if (validPosition(x, y)) {
          int kind = layout.get(n);
          tiles.add(new Tile(n, x, y, kind < 8 ? kind / 2 : -1, names[kind]));
          if (kind == 8) landing = n;
          n++;
        }
    List<Integer> roles = new ArrayList<>();
    for (int i = 0; i < 6; i++) roles.add(i);
    shuffle(roles);
    for (int i = 0; i < count; i++) {
      Role role = Role.values()[roles.get(i)];
      int kind = role == Role.PILOT ? 8 : 9 + role.ordinal() - (role.ordinal() > 2 ? 1 : 0);
      players.add(new Player(role, layout.indexOf(kind)));
    }
    for (int i = 0; i < 24; i++) floodDeck.add(i);
    shuffle(floodDeck);
    for (int i = 0; i < 6; i++) {
      int id = floodDeck.remove(0);
      tiles.get(id).state = 1;
      floodDiscard.add(id);
    }
    for (int t = 0; t < 4; t++) for (int i = 0; i < 5; i++) treasureDeck.add(t);
    for (int i = 0; i < 3; i++) treasureDeck.add(HELICOPTER);
    for (int i = 0; i < 2; i++) treasureDeck.add(SANDBAGS);
    shuffle(treasureDeck);
    for (Player p : players) for (int i = 0; i < 2; i++) p.hand.add(treasureDeck.remove(0));
    for (int i = 0; i < 3; i++) treasureDeck.add(RISE);
    shuffle(treasureDeck);
    log("Expédition " + seed + " · " + count + " aventuriers · niveau " + difficulty + ".");
  }

  private static boolean validPosition(int x, int y) {
    return x >= 0 && x < 6 && y >= 0 && y < 6 && Math.abs(x - 2.5) + Math.abs(y - 2.5) <= 3;
  }

  private int random(int bound) {
    rng ^= rng << 13;
    rng ^= rng >>> 17;
    rng ^= rng << 5;
    return (rng & 0x7fffffff) % bound;
  }

  private void shuffle(List<Integer> list) {
    for (int i = list.size() - 1; i > 0; i--) {
      int j = random(i + 1), v = list.get(i);
      list.set(i, list.get(j));
      list.set(j, v);
    }
  }

  public List<Tile> tiles() {
    return Collections.unmodifiableList(tiles);
  }

  public List<Player> players() {
    return Collections.unmodifiableList(players);
  }

  public Phase phase() {
    return phase;
  }

  public int active() {
    return active;
  }

  public int actions() {
    return actions;
  }

  public int water() {
    return water;
  }

  public int turn() {
    return turn;
  }

  public int landing() {
    return landing;
  }

  public int pendingPlayer() {
    return pendingPlayer;
  }

  public boolean paused() {
    return paused;
  }

  public boolean accepted() {
    return accepted;
  }

  public String message() {
    return message;
  }

  public boolean treasure(int n) {
    return n >= 0 && n < 4 && treasures[n];
  }

  public List<String> journal() {
    return Collections.unmodifiableList(journal);
  }

  public int floodRate() {
    return water <= 2 ? 2 : water <= 5 ? 3 : water <= 7 ? 4 : 5;
  }

  private boolean live() {
    return phase != Phase.WON && phase != Phase.LOST;
  }

  private boolean tileExists(int id) {
    return id >= 0 && id < tiles.size();
  }

  private boolean onIsland(int id) {
    return tileExists(id) && tiles.get(id).state < 2;
  }

  private boolean playerExists(int id) {
    return id >= 0 && id < players.size();
  }

  private void log(String text) {
    journal.add(0, text);
    if (journal.size() > 30) journal.remove(30);
    message = text;
  }

  private boolean reject(String text) {
    message = text;
    accepted = false;
    return false;
  }

  private void fail(String text) {
    phase = Phase.LOST;
    log(text);
  }

  private boolean adjacent(int a, int b, boolean diagonal) {
    Tile t = tiles.get(a), u = tiles.get(b);
    int dx = Math.abs(t.x - u.x), dy = Math.abs(t.y - u.y);
    return a != b && (diagonal ? Math.max(dx, dy) == 1 : dx + dy == 1);
  }

  private boolean actionAvailable() {
    return live() && !paused && phase == Phase.ACTION && actions > 0;
  }

  private void spend() {
    actions--;
    engineerBonus = false;
  }

  public boolean canMove(int target) {
    return actionAvailable() && canTravel(players.get(active), target, false);
  }

  private boolean canTravel(Player p, int target, boolean rescue) {
    if (!onIsland(target) || target == p.tile) return false;
    if (p.role == Role.PILOT && (rescue || !pilotUsed)) return true;
    if (adjacent(p.tile, target, p.role == Role.EXPLORER)) return true;
    if (p.role != Role.DIVER) return false;
    if (rescue) {
      int[] distance = new int[24];
      java.util.Arrays.fill(distance, -1);
      distance[p.tile] = 0;
      List<Integer> search = new ArrayList<>();
      search.add(p.tile);
      int nearest = 100;
      for (int q = 0; q < search.size(); q++) {
        int from = search.get(q);
        for (Tile t : tiles)
          if (distance[t.id] < 0 && adjacent(from, t.id, false)) {
            distance[t.id] = distance[from] + 1;
            if (t.state < 2) nearest = Math.min(nearest, distance[t.id]);
            else search.add(t.id);
          }
      }
      return distance[target] == nearest;
    }
    // During a normal dive, only intermediate flooded/missing tiles may be crossed.
    // For emergency swimming, choose any nearest surviving tile reachable through missing tiles.
    boolean[] seen = new boolean[24];
    List<Integer> queue = new ArrayList<>();
    queue.add(p.tile);
    seen[p.tile] = true;
    for (int q = 0; q < queue.size(); q++) {
      int current = queue.get(q);
      for (Tile t : tiles)
        if (!seen[t.id] && adjacent(current, t.id, false)) {
          if (t.id == target) return true;
          if (t.state >= (rescue ? 2 : 1)) {
            seen[t.id] = true;
            queue.add(t.id);
          }
        }
    }
    return false;
  }

  public boolean canShore(int target) {
    if (!live()
        || paused
        || phase != Phase.ACTION
        || (!actionAvailable() && !engineerBonus)
        || !tileExists(target)
        || tiles.get(target).state != 1) return false;
    Player p = players.get(active);
    return target == p.tile || adjacent(p.tile, target, p.role == Role.EXPLORER);
  }

  public boolean canNavigate(int player, int target) {
    if (!actionAvailable()
        || players.get(active).role != Role.NAVIGATOR
        || !playerExists(player)
        || player == active
        || !onIsland(target)
        || players.get(player).tile == target) return false;
    Player p = players.get(player);
    boolean diagonal = p.role == Role.EXPLORER;
    if (adjacent(p.tile, target, diagonal)) return true;
    for (Tile via : tiles)
      if (via.state < 2 && adjacent(p.tile, via.id, diagonal) && adjacent(via.id, target, diagonal))
        return true;
    return false;
  }

  public boolean canRescue(int target) {
    return !paused && phase == Phase.RESCUE && canTravel(players.get(pendingPlayer), target, true);
  }

  public boolean canCapture() {
    if (!actionAvailable()) return false;
    Player p = players.get(active);
    Tile tile = tiles.get(p.tile);
    return tile.state < 2
        && tile.treasure >= 0
        && !treasures[tile.treasure]
        && Collections.frequency(p.hand, tile.treasure) >= 4;
  }

  /**
   * All interfaces use this protocol. Invalid commands never consume an action or mutate game
   * state.
   */
  public boolean command(String input) {
    accepted = true;
    if (input == null || input.length() > 100) return reject("Commande inconnue.");
    String[] args = input.split(":");
    String op = args[0];
    if (op.equals("pause")) {
      if (!live()) return reject("La partie est terminée.");
      paused = !paused;
      message = paused ? "Expédition en pause." : "Expédition reprise.";
      return true;
    }
    if (paused) return reject("Reprenez l’expédition pour jouer.");
    if (!live()) return reject("La partie est terminée. Lancez une nouvelle expédition.");
    try {
      switch (op) {
        case "move":
          {
            int t = arg(args, 1);
            if (!canMove(t)) return reject("Déplacement impossible ou aucune action restante.");
            Player p = players.get(active);
            boolean distant = !adjacent(p.tile, t, p.role == Role.EXPLORER);
            if (p.role == Role.PILOT && distant) pilotUsed = true;
            p.tile = t;
            spend();
            log(ROLE_NAMES[p.role.ordinal()] + " rejoint " + tiles.get(t).name + ".");
            break;
          }
        case "shore":
          {
            int t = arg(args, 1);
            if (!canShore(t)) return reject("Choisissez une tuile inondée à portée.");
            tiles.get(t).state = 0;
            Player p = players.get(active);
            if (p.role == Role.ENGINEER) {
              if (engineerBonus) engineerBonus = false;
              else {
                spend();
                engineerBonus = true;
              }
            } else spend();
            log(tiles.get(t).name + " a été asséchée.");
            break;
          }
        case "capture":
          if (!canCapture())
            return reject("Il faut 4 cartes identiques sur le site de la relique.");
          else {
            Player p = players.get(active);
            int t = tiles.get(p.tile).treasure;
            for (int i = 0; i < 4; i++) {
              p.hand.remove(Integer.valueOf(t));
              treasureDiscard.add(t);
            }
            treasures[t] = true;
            spend();
            log(CARD_NAMES[t] + " récupéré : une relique est sauvée !");
          }
          break;
        case "give":
          {
            int receiver = arg(args, 1), card = arg(args, 2);
            Player p = players.get(active);
            if (!actionAvailable()
                || !playerExists(receiver)
                || receiver == active
                || card < 0
                || card > 3
                || !p.hand.contains(card)
                || (p.role != Role.MESSENGER && p.tile != players.get(receiver).tile))
              return reject(
                  "Donnez une carte relique à un équipier sur votre tuile (Messagère : partout).");
            p.hand.remove(Integer.valueOf(card));
            players.get(receiver).hand.add(card);
            spend();
            log("Carte transmise à " + ROLE_NAMES[players.get(receiver).role.ordinal()] + ".");
            checkHand(receiver);
            break;
          }
        case "nav":
          {
            int p = arg(args, 1), t = arg(args, 2);
            if (!canNavigate(p, t))
              return reject("Le Navigateur déplace un équipier de 1 ou 2 tuiles accessibles.");
            players.get(p).tile = t;
            spend();
            log("Le Navigateur guide " + ROLE_NAMES[players.get(p).role.ordinal()] + ".");
            break;
          }
        case "end":
          if (phase != Phase.ACTION) return reject("Résolvez d’abord le choix en cours.");
          else {
            ending = true;
            engineerBonus = false;
            drawRemaining = 2;
            advanceTurn();
          }
          break;
        case "discard":
          {
            int p = arg(args, 1), card = arg(args, 2);
            if (phase != Phase.DISCARD || p != pendingPlayer || !players.get(p).hand.contains(card))
              return reject("Choisissez une carte de la main qui dépasse 5 cartes.");
            players.get(p).hand.remove(Integer.valueOf(card));
            treasureDiscard.add(card);
            log("Une carte a été défaussée.");
            resumeAfterHand();
            break;
          }
        case "rescue":
          {
            int t = arg(args, 1);
            if (!canRescue(t)) return reject("Choisissez une tuile de sauvetage accessible.");
            players.get(pendingPlayer).tile = t;
            log("Un aventurier a rejoint une tuile sûre.");
            phase = Phase.ACTION;
            pendingPlayer = -1;
            advanceFlood();
            break;
          }
        case "sand":
          {
            int owner = arg(args, 1), t = arg(args, 2);
            if (!specialAllowed(owner, SANDBAGS) || !tileExists(t) || tiles.get(t).state != 1)
              return reject(
                  "Un sac de sable assèche une tuile inondée, jamais une tuile engloutie.");
            consume(owner, SANDBAGS);
            tiles.get(t).state = 0;
            log("Sacs de sable : " + tiles.get(t).name + " est asséchée.");
            afterSpecial();
            break;
          }
        case "heli":
          {
            int owner = arg(args, 1), t = arg(args, 2), mask = arg(args, 3);
            if (!specialAllowed(owner, HELICOPTER)
                || !onIsland(t)
                || mask <= 0
                || mask >= (1 << players.size()))
              return reject("Choisissez une destination et au moins un passager.");
            int origin = -1;
            for (int i = 0; i < players.size(); i++)
              if ((mask & (1 << i)) != 0) {
                if (origin < 0) origin = players.get(i).tile;
                else if (origin != players.get(i).tile)
                  return reject("Les passagers doivent partir de la même tuile.");
              }
            if (origin == t) return reject("Choisissez une autre destination.");
            consume(owner, HELICOPTER);
            for (int i = 0; i < players.size(); i++)
              if ((mask & (1 << i)) != 0) players.get(i).tile = t;
            log("L’hélicoptère transporte l’équipe sélectionnée.");
            afterSpecial();
            break;
          }
        case "escape":
          {
            int owner = arg(args, 1);
            if (!specialAllowed(owner, HELICOPTER))
              return reject("Une carte Hélicoptère est nécessaire.");
            for (boolean t : treasures) if (!t) return reject("Il manque encore des reliques.");
            for (Player p : players)
              if (p.tile != landing)
                return reject("Tous les aventuriers doivent rejoindre l’héliport.");
            if (!onIsland(landing)) return reject("L’héliport est englouti.");
            consume(owner, HELICOPTER);
            phase = Phase.WON;
            log("Mission accomplie ! Les quatre reliques et toute l’équipe sont sauvées.");
            break;
          }
        default:
          return reject("Commande inconnue.");
      }
      return true;
    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      return reject("Commande incomplète ou invalide.");
    }
  }

  private static int arg(String[] args, int n) {
    return Integer.parseInt(args[n]);
  }

  private boolean specialAllowed(int owner, int card) {
    return playerExists(owner) && players.get(owner).hand.contains(card) && phase != Phase.RESCUE;
  }

  private void consume(int owner, int card) {
    players.get(owner).hand.remove(Integer.valueOf(card));
    treasureDiscard.add(card);
  }

  private void afterSpecial() {
    if (phase == Phase.DISCARD) resumeAfterHand();
  }

  private boolean checkHand(int p) {
    if (players.get(p).hand.size() > 5) {
      phase = Phase.DISCARD;
      pendingPlayer = p;
      message =
          "Main pleine : "
              + ROLE_NAMES[players.get(p).role.ordinal()]
              + " doit revenir à 5 cartes.";
      return true;
    }
    return false;
  }

  private void resumeAfterHand() {
    if (players.get(pendingPlayer).hand.size() > 5) return;
    phase = Phase.ACTION;
    pendingPlayer = -1;
    if (ending) advanceTurn();
  }

  private void advanceTurn() {
    while (drawRemaining > 0 && live()) {
      refillTreasure();
      if (treasureDeck.isEmpty()) {
        fail("La pioche ne peut plus être reconstituée.");
        return;
      }
      int card = treasureDeck.remove(0);
      drawRemaining--;
      // Rebuild immediately when the last card leaves the deck, before a later hand choice.
      refillTreasure();
      if (card == RISE) {
        treasureDiscard.add(card);
        water++;
        log("Montée des eaux ! Niveau " + water + ".");
        if (water >= 10) {
          fail("Le niveau des eaux a atteint le seuil fatal.");
          return;
        }
        shuffle(floodDiscard);
        floodDeck.addAll(0, floodDiscard);
        floodDiscard.clear();
      } else {
        players.get(active).hand.add(card);
        log(ROLE_NAMES[players.get(active).role.ordinal()] + " pioche : " + CARD_NAMES[card] + ".");
        if (checkHand(active)) return;
      }
    }
    floodRemaining = floodRate();
    advanceFlood();
  }

  private void refillTreasure() {
    if (treasureDeck.isEmpty() && !treasureDiscard.isEmpty()) {
      treasureDeck.addAll(treasureDiscard);
      treasureDiscard.clear();
      shuffle(treasureDeck);
    }
  }

  private void refillFlood() {
    if (floodDeck.isEmpty() && !floodDiscard.isEmpty()) {
      floodDeck.addAll(floodDiscard);
      floodDiscard.clear();
      shuffle(floodDeck);
    }
  }

  private void advanceFlood() {
    if (!checkRescue()) return;
    while (floodRemaining > 0 && live()) {
      refillFlood();
      if (floodDeck.isEmpty()) {
        fail("L’île a entièrement disparu.");
        return;
      }
      int id = floodDeck.remove(0);
      floodRemaining--;
      Tile t = tiles.get(id);
      if (t.state == 0) {
        t.state = 1;
        floodDiscard.add(id);
        refillFlood();
        log(t.name + " est inondée.");
      } else if (t.state == 1) {
        t.state = 2;
        refillFlood();
        log(t.name + " est engloutie.");
        checkLoss();
        if (!live() || !checkRescue()) return;
      }
    }
    if (live()) {
      ending = false;
      active = (active + 1) % players.size();
      turn++;
      actions = 3;
      pilotUsed = false;
      engineerBonus = false;
      phase = Phase.ACTION;
      log(
          "Tour "
              + turn
              + " · "
              + ROLE_NAMES[players.get(active).role.ordinal()]
              + " prend la main.");
    }
  }

  private boolean checkRescue() {
    for (int i = 0; i < players.size(); i++)
      if (tiles.get(players.get(i).tile).state == 2) {
        pendingPlayer = i;
        phase = Phase.RESCUE;
        boolean possible = false;
        for (Tile t : tiles)
          if (canTravel(players.get(i), t.id, true)) {
            possible = true;
            break;
          }
        if (!possible) {
          fail(
              ROLE_NAMES[players.get(i).role.ordinal()]
                  + " ne peut plus rejoindre l’île. L’expédition est perdue.");
          return false;
        }
        message =
            "Sauvetage urgent : choisissez une tuile pour "
                + ROLE_NAMES[players.get(i).role.ordinal()]
                + ".";
        return false;
      }
    return true;
  }

  private void checkLoss() {
    if (tiles.get(landing).state == 2) {
      fail("L’héliport a été englouti. L’expédition ne peut plus repartir.");
      return;
    }
    for (int kind = 0; kind < 4; kind++)
      if (!treasures[kind]) {
        boolean exists = false;
        for (Tile t : tiles) if (t.treasure == kind && t.state < 2) exists = true;
        if (!exists) {
          fail("Les deux sites du " + CARD_NAMES[kind] + " ont disparu avant sa récupération.");
          return;
        }
      }
  }

  public String json() {
    return Snapshot.json(this, seed, difficulty);
  }
}
