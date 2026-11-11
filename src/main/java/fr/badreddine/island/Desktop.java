package fr.badreddine.island;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Native Swing client of the same engine exported to the web. */
public final class Desktop {
  private static final Color NAVY = new Color(18, 42, 52),
      SEA = new Color(217, 234, 232),
      GOLD = new Color(223, 174, 79);
  private final JFrame frame = new JFrame("L’Île interdite · Expédition");
  private final JPanel board = new JPanel(new GridLayout(6, 6, 6, 6)),
      side = new JPanel(),
      header = new JPanel(new BorderLayout(20, 0));
  private final JLabel title = new JLabel(), status = new JLabel();
  private Engine game = new Engine(2024, 2, 1);
  private int selected = -1;

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new Desktop().open(true));
  }

  void open(boolean visible) {
    UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 13));
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JPanel root = new JPanel(new BorderLayout(20, 20));
    root.setBorder(new EmptyBorder(22, 22, 22, 22));
    root.setBackground(SEA);
    frame.setContentPane(root);
    title.setFont(new Font("Serif", Font.BOLD, 29));
    title.setForeground(NAVY);
    header.setOpaque(false);
    header.add(title, BorderLayout.CENTER);
    JPanel menus = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    menus.setOpaque(false);
    menus.add(button("Nouvelle expédition", this::newGame));
    menus.add(button("Aide", this::help));
    menus.add(button("Pause / reprendre", () -> act("pause")));
    header.add(menus, BorderLayout.EAST);
    root.add(header, BorderLayout.NORTH);
    board.setPreferredSize(new Dimension(630, 630));
    board.setBackground(SEA);
    root.add(board, BorderLayout.CENTER);
    side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
    side.setBackground(Color.WHITE);
    side.setBorder(new EmptyBorder(15, 15, 15, 15));
    JScrollPane scroll = new JScrollPane(side);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setPreferredSize(new Dimension(340, 600));
    root.add(scroll, BorderLayout.EAST);
    status.setBorder(new EmptyBorder(12, 0, 0, 0));
    status.setFont(new Font("SansSerif", Font.BOLD, 14));
    root.add(status, BorderLayout.SOUTH);
    frame.setMinimumSize(new Dimension(900, 700));
    frame.pack();
    frame.setLocationByPlatform(true);
    refresh();
    frame.setVisible(visible);
  }

  private JButton button(String label, Runnable action) {
    JButton b = new JButton(label);
    b.setFocusPainted(true);
    b.addActionListener(e -> action.run());
    return b;
  }

  private void act(String command) {
    game.command(command);
    refresh();
  }

  private void newGame() {
    if (JOptionPane.showConfirmDialog(
            frame,
            "Commencer une nouvelle expédition ? La partie actuelle sera remplacée.",
            "Nouvelle expédition",
            JOptionPane.OK_CANCEL_OPTION)
        != JOptionPane.OK_OPTION) return;
    Integer count =
        (Integer)
            JOptionPane.showInputDialog(
                frame,
                "Nombre d’aventuriers",
                "Équipe",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Integer[] {2, 3, 4},
                2);
    if (count == null) return;
    Integer level =
        (Integer)
            JOptionPane.showInputDialog(
                frame,
                "Niveau initial des eaux",
                "Difficulté",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Integer[] {1, 2, 3, 4},
                1);
    if (level == null) return;
    game = new Engine((int) System.currentTimeMillis(), count, level);
    selected = -1;
    refresh();
  }

  private void help() {
    JOptionPane.showMessageDialog(
        frame,
        "<html><div style='width:520px'><h2>Quatre reliques. Une seule équipe.</h2><p>À votre tour"
            + " : jusqu’à 3 actions, puis 2 cartes trésor et des inondations. Cliquez une tuile,"
            + " puis Déplacer ou Assécher. Une relique exige 4 cartes correspondantes sur l’un de"
            + " ses deux sites.</p><br><p>Les sacs de sable et hélicoptères ne coûtent pas"
            + " d’action. Donnez les cartes relique à un équipier sur votre tuile. La Messagère"
            + " donne à distance. Revenez tous à l’héliport avec les 4 reliques puis jouez un"
            + " hélicoptère pour gagner.</p><br><p>Exploratrice : diagonales. Ingénieur : 2"
            + " assèchements consécutifs pour 1 action. Pilote : 1 vol par tour. Plongeur :"
            + " traversée des tuiles inondées/englouties. Navigateur : déplace un équipier de 2"
            + " tuiles.</p><br><p>Défaite si l’héliport disparaît, si les 2 sites d’une relique"
            + " manquante disparaissent, si un aventurier ne peut pas nager, ou si l’eau atteint"
            + " 10. Lors d’un sauvetage, cliquez une destination indiquée.</p><br><p>Variante"
            + " pédagogique : résolvez immédiatement les sauvetages ; les cartes spéciales sont"
            + " bloquées pendant ce choix. Pas de jeu réseau.</p></div></html>",
        "Carnet d’expédition",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private void refresh() {
    title.setText("L’Île interdite · Tour " + game.turn());
    status.setText("<html>" + game.message() + "</html>");
    board.removeAll();
    for (int y = 0; y < 6; y++)
      for (int x = 0; x < 6; x++) {
        Engine.Tile tile = null;
        for (Engine.Tile t : game.tiles()) if (t.x == x && t.y == y) tile = t;
        if (tile == null) {
          JPanel gap = new JPanel();
          gap.setOpaque(false);
          board.add(gap);
          continue;
        }
        final Engine.Tile t = tile;
        StringBuilder pawns = new StringBuilder();
        for (int i = 0; i < game.players().size(); i++)
          if (game.players().get(i).tile() == t.id) pawns.append(" J").append(i + 1);
        String icon =
            t.id == game.landing()
                ? "H"
                : t.treasure >= 0 ? new String[] {"◆", "◇", "∪", "△"}[t.treasure] : "·";
        JButton b =
            button(
                "<html><center><b>"
                    + icon
                    + " "
                    + pawns
                    + "</b><br><small>"
                    + t.name
                    + "</small><br>"
                    + new String[] {"Sèche", "Inondée", "Engloutie"}[t.state()]
                    + "</center></html>",
                () -> {
                  selected = t.id;
                  if (game.phase() == Engine.Phase.RESCUE) act("rescue:" + t.id);
                  else refresh();
                });
        b.setOpaque(true);
        b.setBackground(
            t.state() == 2
                ? new Color(82, 118, 126)
                : t.state() == 1 ? new Color(159, 215, 224) : new Color(248, 242, 222));
        b.setForeground(NAVY);
        b.setBorder(
            BorderFactory.createLineBorder(
                selected == t.id ? GOLD : game.canRescue(t.id) ? new Color(39, 145, 112) : SEA,
                selected == t.id ? 4 : 2));
        b.setToolTipText(
            t.name + " · " + new String[] {"Sèche", "Inondée", "Engloutie"}[t.state()]);
        board.add(b);
      }
    side.removeAll();
    Engine.Player active = game.players().get(game.active());
    label("Eau " + game.water() + " / 10 · " + game.floodRate() + " inondations");
    label("J" + (game.active() + 1) + " · " + Engine.ROLE_NAMES[active.role.ordinal()]);
    label(
        game.actions() + " actions restantes" + (game.engineerBonus ? " · bonus assèchement" : ""));
    int relics = 0;
    for (int i = 0; i < 4; i++) if (game.treasure(i)) relics++;
    label("Reliques sauvées : " + relics + " / 4");
    side.add(Box.createVerticalStrut(12));
    label(selected < 0 ? "Choisissez une tuile." : game.tiles().get(selected).name);
    addAction("Déplacer", () -> act("move:" + selected), selected >= 0 && game.canMove(selected));
    addAction("Assécher", () -> act("shore:" + selected), selected >= 0 && game.canShore(selected));
    addAction("Récupérer la relique", () -> act("capture"), game.canCapture());
    addAction(
        "Donner une carte", this::give, game.phase() == Engine.Phase.ACTION && !game.paused());
    addAction(
        "Guider un équipier",
        this::navigate,
        active.role == Engine.Role.NAVIGATOR && selected >= 0 && !game.paused());
    addAction(
        "Fin du tour", () -> act("end"), game.phase() == Engine.Phase.ACTION && !game.paused());
    for (int i = 0; i < game.players().size(); i++) {
      final int player = i;
      Engine.Player p = game.players().get(i);
      side.add(Box.createVerticalStrut(14));
      label(
          "J"
              + (i + 1)
              + " · "
              + Engine.ROLE_NAMES[p.role.ordinal()]
              + " ("
              + p.hand().size()
              + "/5)");
      for (int card : p.hand()) {
        String suffix =
            game.phase() == Engine.Phase.DISCARD && game.pendingPlayer() == i
                ? " — défausser"
                : card == 4 || card == 5 ? " — jouer" : "";
        JButton c = button(Engine.CARD_NAMES[card] + suffix, () -> card(player, card));
        side.add(c);
      }
    }
    side.add(Box.createVerticalStrut(12));
    label("Journal");
    for (String line : game.journal().subList(0, Math.min(6, game.journal().size())))
      label("• " + line);
    board.revalidate();
    board.repaint();
    side.revalidate();
    side.repaint();
  }

  private void label(String text) {
    JLabel label = new JLabel("<html><div style='width:200px'>" + text + "</div></html>");
    label.setBorder(new EmptyBorder(5, 0, 5, 0));
    label.setForeground(NAVY);
    side.add(label);
  }

  private void addAction(String text, Runnable action, boolean enabled) {
    JButton b = button(text, action);
    b.setEnabled(enabled);
    b.setAlignmentX(Component.LEFT_ALIGNMENT);
    side.add(b);
    side.add(Box.createVerticalStrut(5));
  }

  private Integer choosePlayer(String title) {
    String[] choices = new String[game.players().size()];
    for (int i = 0; i < choices.length; i++)
      choices[i] = "J" + (i + 1) + " · " + Engine.ROLE_NAMES[game.players().get(i).role.ordinal()];
    String s =
        (String)
            JOptionPane.showInputDialog(
                frame, title, "Équipier", JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
    if (s == null) return null;
    for (int i = 0; i < choices.length; i++) if (choices[i].equals(s)) return i;
    return null;
  }

  private void give() {
    Integer receiver = choosePlayer("À qui donner une carte ?");
    if (receiver == null) return;
    List<Integer> cards = new ArrayList<>();
    for (int c : game.players().get(game.active()).hand())
      if (c < 4 && !cards.contains(c)) cards.add(c);
    if (cards.isEmpty()) {
      JOptionPane.showMessageDialog(frame, "Aucune carte relique à donner.");
      return;
    }
    String[] names = cards.stream().map(c -> Engine.CARD_NAMES[c]).toArray(String[]::new);
    String chosen =
        (String)
            JOptionPane.showInputDialog(
                frame,
                "Quelle carte ?",
                "Donner",
                JOptionPane.QUESTION_MESSAGE,
                null,
                names,
                names[0]);
    if (chosen != null)
      for (int c : cards)
        if (Engine.CARD_NAMES[c].equals(chosen)) {
          act("give:" + receiver + ":" + c);
          break;
        }
  }

  private void navigate() {
    Integer p = choosePlayer("Qui guider vers la tuile sélectionnée ?");
    if (p != null) act("nav:" + p + ":" + selected);
  }

  private void card(int owner, int card) {
    if (game.phase() == Engine.Phase.DISCARD && game.pendingPlayer() == owner) {
      if (card >= 4) {
        int choice =
            JOptionPane.showOptionDialog(
                frame,
                "Jouer ou défausser cette carte ?",
                "Main pleine",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[] {"Jouer", "Défausser", "Annuler"},
                "Jouer");
        if (choice == 1) {
          act("discard:" + owner + ":" + card);
          return;
        }
        if (choice != 0) return;
      } else {
        act("discard:" + owner + ":" + card);
        return;
      }
    }
    if (card < 4) {
      JOptionPane.showMessageDialog(
          frame,
          "Réunissez 4 cartes « "
              + Engine.CARD_NAMES[card]
              + " » sur un site correspondant pour récupérer cette relique.\n"
              + "Pour transmettre une carte, utilisez « Donner une carte » pendant votre tour.",
          Engine.CARD_NAMES[card],
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    if (card == 5) {
      act("sand:" + owner + ":" + selected);
      return;
    }
    if (card == 4) {
      int choice =
          JOptionPane.showOptionDialog(
              frame,
              "Transporter vers la tuile sélectionnée ou évacuer l’île ?",
              "Hélicoptère",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              new String[] {"Transporter", "Évacuer", "Annuler"},
              "Transporter");
      if (choice == 1) {
        act("escape:" + owner);
        return;
      }
      if (choice != 0) return;
      JPanel passengers = new JPanel(new GridLayout(0, 1));
      List<JCheckBox> boxes = new ArrayList<>();
      for (int i = 0; i < game.players().size(); i++) {
        JCheckBox box =
            new JCheckBox(
                "J" + (i + 1) + " " + Engine.ROLE_NAMES[game.players().get(i).role.ordinal()]);
        boxes.add(box);
        passengers.add(box);
      }
      if (JOptionPane.showConfirmDialog(
              frame, passengers, "Passagers sur une même tuile", JOptionPane.OK_CANCEL_OPTION)
          == JOptionPane.OK_OPTION) {
        int mask = 0;
        for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isSelected()) mask |= 1 << i;
        act("heli:" + owner + ":" + selected + ":" + mask);
      }
    }
  }
}
