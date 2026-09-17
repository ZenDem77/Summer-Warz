package GameMain;

import Economy.Currency;
import Economy.Wallet;
import Entities.Characters.Xyniz;
import Entities.Characters.Kindle;
import Entities.Characters.Zenzenkoi;
import Entities.Characters.Kouzen;
import Entities.Character;

import javax.swing.*;

/**
 * LevelUpScreenTester — temporary standalone tester for CharacterLevelUpScreen.
 *
 * Run main() directly to open the level-up dialog for a chosen character.
 *
 * ── To customize ─────────────────────────────────────────────────────────
 * Change `character` to any character you want to test.
 * Change `startingElixir` to test different wallet states.
 * ─────────────────────────────────────────────────────────────────────────
 */
public class LevelUpScreenTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // ═══════════════════════════════════════════════════════════════
            //  EDIT THESE TO CUSTOMIZE THE TEST
            // ═══════════════════════════════════════════════════════════════
            Character character    = new Xyniz();
            int       startingElixir = 100000;
            // ═══════════════════════════════════════════════════════════════

            Wallet wallet = new Wallet();
            wallet.add(Currency.ELIXIR, startingElixir);

            // Use a dummy hidden JFrame as the parent so the dialog centres properly
            JFrame dummyParent = new JFrame();
            dummyParent.setUndecorated(true);
            dummyParent.setVisible(true);

            CharacterLevelUpScreen screen =
                    new CharacterLevelUpScreen(dummyParent, character, wallet);
            screen.setVisible(true);

            // Clean up after the dialog is closed
            dummyParent.dispose();
            System.out.println("Dialog closed. Final level: " + character.getLevel()
                    + " | Elixir remaining: " + wallet.getBalance(Currency.ELIXIR));
            System.exit(0);
        });
    }
}