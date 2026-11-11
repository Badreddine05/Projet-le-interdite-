package fr.badreddine.island;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Real Swing tree exercised offscreen. Run with a desktop or xvfb, never in headless mode. */
public final class DesktopSmoke {
  static Object field(Desktop app, String name) throws Exception {
    Field f = Desktop.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.get(app);
  }

  static List<JButton> buttons(Container c) {
    List<JButton> all = new ArrayList<>();
    for (Component child : c.getComponents()) {
      if (child instanceof JButton b) all.add(b);
      if (child instanceof Container nested) all.addAll(buttons(nested));
    }
    return all;
  }

  static JButton button(JFrame frame, String name) {
    return buttons(frame).stream().filter(b -> b.getText().equals(name)).findFirst().orElseThrow();
  }

  public static void main(String[] args) throws Exception {
    final Throwable[] failure = {null};
    SwingUtilities.invokeAndWait(
        () -> {
          JFrame frame = null;
          try {
            Desktop app = new Desktop();
            app.open(false);
            frame = (JFrame) field(app, "frame");
            Engine game = (Engine) field(app, "game");
            if (button(frame, "Déplacer").isEnabled())
              throw new AssertionError("Move must require a selection");
            Engine.Tile target =
                game.tiles().stream().filter(t -> game.canMove(t.id)).findFirst().orElseThrow();
            JButton tileButton =
                buttons(frame).stream()
                    .filter(b -> b.getText().contains(target.name))
                    .findFirst()
                    .orElseThrow();
            tileButton.doClick();
            if (!button(frame, "Déplacer").isEnabled())
              throw new AssertionError("Legal move not enabled");
            button(frame, "Déplacer").doClick();
            if (game.actions() != 2 || game.players().get(game.active()).tile() != target.id)
              throw new AssertionError("Move did not reach engine");
            button(frame, "Pause / reprendre").doClick();
            if (!game.paused() || button(frame, "Fin du tour").isEnabled())
              throw new AssertionError("Pause did not disable actions");
            button(frame, "Pause / reprendre").doClick();
            if (game.paused()) throw new AssertionError("Resume failed");
            frame.validate();
            BufferedImage image =
                new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            frame.getContentPane().printAll(g);
            g.dispose();
            if ((image.getRGB(40, 40) & 0xffffff) == 0)
              throw new AssertionError("Native rendering is blank");
            if (args.length > 0) ImageIO.write(image, "png", new File(args[0]));
            System.out.println(
                "Swing smoke PASS: legal move, action count, selection, pause/resume, rendered"
                    + " frame "
                    + frame.getWidth()
                    + "x"
                    + frame.getHeight());
          } catch (Throwable e) {
            failure[0] = e;
          } finally {
            if (frame != null) frame.dispose();
          }
        });
    if (failure[0] != null) throw new AssertionError(failure[0]);
  }
}
