package GameMain;

import Economy.Inventory;
import Entities.Artifacts.*;
import Entities.Items.*;
import Entities.StatType;
import Entities.Weapons.Attack.*;
import Entities.Weapons.CritRate.*;
import Entities.Weapons.CritDmg.*;
import Entities.Weapons.Hp.*;
import Entities.Weapons.Defense.*;

import javax.swing.*;
import java.util.List;

/**
 * InventoryTester — temporary standalone tester for InventoryPanel.
 *
 * Run main() directly to open the inventory UI pre-filled with sample items.
 *
 * ── To customize ─────────────────────────────────────────────────────────
 * Edit the "SAMPLE DATA" section below to add/remove items.
 */
public class InventoryTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            Inventory inventory = new Inventory();

            // ═══════════════════════════════════════════════════════════════
            //  SAMPLE DATA — edit freely
            // ═══════════════════════════════════════════════════════════════

            // ── Weapons ───────────────────────────────────────────────────
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());
            inventory.addWeapon(new Spear());           // second copy
            inventory.addWeapon(new Saber());
            inventory.addWeapon(new Gauntlet());
            inventory.addWeapon(new Katana());
            inventory.addWeapon(new Greatsword());
            inventory.addWeapon(new Spear());

            // ── Artifacts ─────────────────────────────────────────────────
            inventory.addArtifact(Artifact.generateRandom());
            inventory.addArtifact(Artifact.generateRandom());
            inventory.addArtifact(Artifact.generateRandom());

            // ── Ascension crystals ────────────────────────────────────────
            inventory.addCrystals("Xyniz",      12);
            inventory.addCrystals("Kindle",      4);
            inventory.addCrystals("Zenzenkoi",  20);
            inventory.addCrystals("Kouzen",      8);
            inventory.addCrystals("Zayir",      16);

            // ── Misc items ────────────────────────────────────────────────
            inventory.addMiscItem("Event Token",  "Collected during the Summer Festival event.");
            inventory.addMiscItem("Quest Scroll", "A mysterious scroll from the first story quest.");

            // ═══════════════════════════════════════════════════════════════

            JFrame frame = new JFrame("Inventory Tester");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            Economy.InventoryPanel panel = new Economy.InventoryPanel(inventory, () -> {
                System.out.println("Exit clicked — closing tester.");
                frame.dispose();
                System.exit(0);
            });

            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("Inventory loaded: " + inventory);
        });
    }
}