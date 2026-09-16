package Economy;

import Economy.Inventory;
import Entities.Artifacts.Artifact;
import Entities.Artifacts.ArtifactSubstat;
import Entities.Items.*;
import Entities.StatType;
import Entities.Weapons.Weapon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * InventoryPanel — full-screen 1280×720 inventory UI.
 *
 * Layout (centred on the panel):
 *   Info box   : left,  320×540  — item sprite (upper) + name/description (lower)
 *   Item box   : right, 704×540  — 5-column scrollable item grid
 *   Tab bar    : above item box  — WEAPON / ARTIFACT / ASCENSION / MISC
 *   Exit button: top-right corner of the full panel
 *
 * Visual design matches CharacterLevelUpScreen (golden fantasy palette,
 * Georgia font, rounded golden box borders).
 */
public class InventoryPanel extends JPanel {

    // ── Dimensions ────────────────────────────────────────────────────────────
    private static final int W = 1280, H = 720;

    private static final int INFO_W = 320, INFO_H = 540;
    private static final int ITEM_W = 704, ITEM_H = 540;
    private static final int TAB_H  = 44;
    private static final int GAP    = 16;   // gap between info and item box

    // Horizontal centering: total occupied = INFO_W + GAP + ITEM_W = 1040
    private static final int LEFT_X  = (W - INFO_W - GAP - ITEM_W) / 2;   // 120
    private static final int INFO_X  = LEFT_X;
    private static final int ITEM_X  = LEFT_X + INFO_W + GAP;              // 456

    // Vertical centering: total occupied = TAB_H + GAP + INFO_H
    private static final int TOTAL_H = TAB_H + 4 + INFO_H;
    private static final int TOP_Y   = (H - TOTAL_H) / 2;                  // ~66
    private static final int TAB_Y   = TOP_Y;
    private static final int BOX_Y   = TOP_Y + TAB_H + 4;

    // Item grid
    private static final int GRID_COLS  = 5;
    private static final int CELL_SIZE  = 124;
    private static final int CELL_GAP   = 8;
    private static final int GRID_PAD   = 12;

    // ── Golden fantasy palette (same as CharacterLevelUpScreen) ──────────────
    private static final Color BG_MAIN      = new Color(185, 155, 80);
    private static final Color BG_BOX       = new Color(210, 185, 115);
    private static final Color BG_BOX_INNER = new Color(228, 205, 140);
    private static final Color BORDER_DARK  = new Color(140, 108, 40);
    private static final Color TEXT_DARK    = new Color(45, 28, 5);
    private static final Color TEXT_MID     = new Color(90, 60, 15);
    private static final Color TEXT_LABEL   = new Color(70, 48, 10);
    private static final Color TEXT_DIM     = new Color(130, 100, 40);
    private static final Color BTN_GOLD     = new Color(222, 178, 48);
    private static final Color BTN_HOV      = new Color(240, 200, 70);
    private static final Color BTN_DARK_BDR = new Color(160, 120, 25);
    private static final Color TAB_ACTIVE   = new Color(200, 160, 45);
    private static final Color TAB_INACTIVE = new Color(170, 140, 70);
    private static final Color CELL_BG      = new Color(220, 195, 130);
    private static final Color CELL_SEL     = new Color(240, 210, 80);
    private static final Color CELL_HOV     = new Color(230, 200, 100);
    private static final Color BOX_ARC_C    = new Color(0, 0, 0, 0);

    private static final int ARC = 14;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Inventory      inventory;
    private final Runnable       onExit;
    private       ItemType       activeTab    = ItemType.WEAPON;
    private       InventoryItem  selectedItem = null;

    // ── Child components ──────────────────────────────────────────────────────
    private JPanel     infoBox;
    private JPanel     itemGridPanel;
    private JScrollPane itemScroll;
    private final Map<InventoryItem, JPanel> cellMap = new LinkedHashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public InventoryPanel(Inventory inventory, Runnable onExit) {
        this.inventory = inventory;
        this.onExit    = onExit;

        setLayout(null);
        setPreferredSize(new Dimension(W, H));
        setOpaque(true);

        buildUI();
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sandy golden background
        g2.setColor(BG_MAIN);
        g2.fillRect(0, 0, W, H);

        // Info box background
        drawBox(g2, INFO_X, BOX_Y, INFO_W, INFO_H);

        // Item box background
        drawBox(g2, ITEM_X, BOX_Y, ITEM_W, ITEM_H);
    }

    private void drawBox(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(BG_BOX);
        g2.fillRoundRect(x, y, w, h, ARC, ARC);
        g2.setColor(BORDER_DARK);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, ARC, ARC);
        g2.setStroke(new BasicStroke(1));
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        removeAll();

        // ── Exit button ───────────────────────────────────────────────────────
        JButton exitBtn = goldenButton("Exit");
        exitBtn.setBounds(W - 110, 14, 96, 38);
        exitBtn.addActionListener(e -> onExit.run());
        add(exitBtn);

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("INVENTORY", JLabel.LEFT);
        title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setForeground(new Color(255, 235, 150));
        title.setBounds(LEFT_X, TOP_Y - 52, 300, 30);
        add(title);

        JLabel capacity = new JLabel("Capacity: " + inventory.getUsedSlots() + " / " + Inventory.MAX_CAPACITY, JLabel.LEFT);
        capacity.setFont(new Font("Georgia", Font.PLAIN, 13));
        capacity.setForeground(new Color(220, 195, 130));
        capacity.setBounds(LEFT_X, TOP_Y - 26, 300, 20);
        add(capacity);

        // ── Tab buttons ───────────────────────────────────────────────────────
        ItemType[] tabs = { ItemType.WEAPON, ItemType.ARTIFACT, ItemType.ASCENSION, ItemType.MISC };
        String[] tabLabels = { "Weapons", "Artifacts", "Ascension", "Misc" };
        int tabW = ITEM_W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final ItemType tab = tabs[i];
            JButton tabBtn = buildTabButton(tabLabels[i], tab == activeTab);
            tabBtn.setBounds(ITEM_X + i * tabW, TAB_Y, tabW - 2, TAB_H);
            tabBtn.addActionListener(e -> switchTab(tab));
            add(tabBtn);
        }

        // ── Info box ──────────────────────────────────────────────────────────
        infoBox = buildInfoBox();
        infoBox.setBounds(INFO_X, BOX_Y, INFO_W, INFO_H);
        add(infoBox);

        // ── Item grid (scroll) ────────────────────────────────────────────────
        rebuildItemGrid();

        revalidate();
        repaint();
    }

    // ── Info box ──────────────────────────────────────────────────────────────

    private JPanel buildInfoBox() {
        JPanel panel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                // box drawn in parent paintComponent; just paint content
            }
        };
        panel.setOpaque(false);

        refreshInfoBox(panel);
        return panel;
    }

    private void refreshInfoBox(JPanel panel) {
        panel.removeAll();

        int spriteH = INFO_H / 2;
        int descH   = INFO_H - spriteH;
        int pad     = 12;

        // ── Sprite area ───────────────────────────────────────────────────────
        JPanel spriteArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Inner rounded box
                g2.setColor(BG_BOX_INNER);
                g2.fillRoundRect(pad, pad, getWidth() - pad*2, getHeight() - pad*2, 10, 10);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(pad, pad, getWidth() - pad*2, getHeight() - pad*2, 10, 10);

                if (selectedItem == null) {
                    g2.setFont(new Font("Georgia", Font.ITALIC, 15));
                    g2.setColor(TEXT_DIM);
                    String hint = "Select an item";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2,
                            getHeight() / 2 + fm.getAscent() / 2);
                } else {
                    // Placeholder sprite — colored block with item type initial
                    Color c = itemColor(selectedItem.getItemType());
                    int bx = pad + 20, by = pad + 20;
                    int bw = getWidth() - bx - pad - 20;
                    int bh = getHeight() - by - pad - 20;
                    g2.setColor(c);
                    g2.fillRoundRect(bx, by, bw, bh, 12, 12);
                    g2.setColor(c.darker());
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(bx, by, bw, bh, 12, 12);
                    g2.setFont(new Font("Georgia", Font.BOLD, 36));
                    g2.setColor(new Color(255, 255, 255, 180));
                    String ini = selectedItem.getItemType().name().substring(0, 1);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ini, bx + (bw - fm.stringWidth(ini)) / 2,
                            by + bh / 2 + fm.getAscent() / 2 - 4);
                }
            }
        };
        spriteArea.setOpaque(false);
        spriteArea.setBounds(0, 0, INFO_W, spriteH);
        panel.add(spriteArea);

        // ── Divider ───────────────────────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_DARK);
        sep.setBackground(BORDER_DARK);
        sep.setBounds(pad, spriteH, INFO_W - pad * 2, 2);
        panel.add(sep);

        // ── Description area ──────────────────────────────────────────────────
        JPanel descArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (selectedItem == null) return;

                int x   = pad;
                int rw  = getWidth() - pad * 2;   // usable row width
                int y   = 18;

                // ── Item name ─────────────────────────────────────────────
                g2.setFont(new Font("Georgia", Font.BOLD, 16));
                g2.setColor(TEXT_DARK);
                g2.drawString(selectedItem.getName(), x, y);
                y += 22;

                // ── Type label ────────────────────────────────────────────
                g2.setFont(new Font("Georgia", Font.ITALIC, 12));
                g2.setColor(TEXT_MID);
                g2.drawString(selectedItem.getItemType().name(), x, y);
                y += 16;

                // ── Divider ───────────────────────────────────────────────
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(x, y, x + rw, y);
                y += 14;

                // ── Per-type stat rows ─────────────────────────────────────
                g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();

                if (selectedItem instanceof OwnedWeapon ow) {
                    Entities.Weapons.Weapon wp = ow.getWeapon();

                    // Stat rows: name left, value right
                    String[][] rows = buildWeaponRows(wp);
                    for (String[] row : rows) {
                        g2.setColor(TEXT_LABEL);
                        g2.drawString(row[0], x, y);
                        g2.setColor(TEXT_DARK);
                        g2.drawString(row[1], x + rw - fm.stringWidth(row[1]), y);
                        y += 20;
                    }

                    // Equip status — centered at bottom
                    String equip = ow.isEquipped()
                            ? "Equipped by " + ow.getEquippedBy().getName()
                            : "Unequipped";
                    drawCenteredStatus(g2, equip, ow.isEquipped(), x, rw, getHeight() - 16);

                } else if (selectedItem instanceof OwnedArtifact oa) {
                    Entities.Artifacts.Artifact art = oa.getArtifact();

                    // One row per substat
                    for (Entities.Artifacts.ArtifactSubstat s : art.getSubstats()) {
                        String label = formatStatName(s.type());
                        String value = formatStatValue(s.type(), s.value());
                        g2.setColor(TEXT_LABEL);
                        g2.drawString(label, x, y);
                        g2.setColor(TEXT_DARK);
                        g2.drawString(value, x + rw - fm.stringWidth(value), y);
                        y += 20;
                    }

                    // Equip status — centered at bottom
                    String equip = oa.isEquipped()
                            ? "Equipped by " + oa.getEquippedBy().getName()
                            : "Unequipped";
                    drawCenteredStatus(g2, equip, oa.isEquipped(), x, rw, getHeight() - 16);

                } else if (selectedItem instanceof AscensionCrystal ac) {
                    // Description sentence
                    g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                    g2.setColor(TEXT_LABEL);
                    String acDesc = "Ascension material for " + ac.getCharacterName()
                            + " to break through the next phase.";
                    for (String line : wrapText(acDesc, fm, rw)) {
                        g2.drawString(line, x, y);
                        y += 18;
                    }

                    // Quantity — centered at bottom
                    g2.setFont(new Font("Georgia", Font.BOLD, 14));
                    fm = g2.getFontMetrics();
                    String qtyStr = "Quantity:  " + ac.getQuantity();
                    g2.setColor(TEXT_DARK);
                    g2.drawString(qtyStr, x + (rw - fm.stringWidth(qtyStr)) / 2, getHeight() - 16);

                } else {
                    // Misc: description word-wrapped
                    g2.setFont(new Font("Georgia", Font.PLAIN, 13));
                    fm = g2.getFontMetrics();
                    g2.setColor(TEXT_LABEL);
                    for (String line : wrapText(selectedItem.getDescription(), fm, rw)) {
                        g2.drawString(line, x, y);
                        y += 18;
                    }
                }
            }
        };
        descArea.setOpaque(false);
        descArea.setBounds(0, spriteH + 4, INFO_W, descH - 4);
        panel.add(descArea);

        panel.revalidate();
        panel.repaint();
    }

    // ── Item grid ─────────────────────────────────────────────────────────────

    private void rebuildItemGrid() {
        if (itemScroll != null) remove(itemScroll);
        cellMap.clear();

        List<InventoryItem> items = getItemsForTab(activeTab);

        // Grid panel
        int cols     = GRID_COLS;
        int rows     = Math.max(1, (int) Math.ceil((double) items.size() / cols));
        int gridW    = cols * CELL_SIZE + (cols - 1) * CELL_GAP + GRID_PAD * 2;
        int gridH    = rows * CELL_SIZE + (rows - 1) * CELL_GAP + GRID_PAD * 2;

        itemGridPanel = new JPanel(null);
        itemGridPanel.setOpaque(false);
        itemGridPanel.setPreferredSize(new Dimension(
                Math.max(gridW, ITEM_W - 4), Math.max(gridH, ITEM_H - 4)));

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx  = GRID_PAD + col * (CELL_SIZE + CELL_GAP);
            int cy  = GRID_PAD + row * (CELL_SIZE + CELL_GAP);

            JPanel cell = buildItemCell(item);
            cell.setBounds(cx, cy, CELL_SIZE, CELL_SIZE);
            itemGridPanel.add(cell);
            cellMap.put(item, cell);
        }

        // Empty state message
        if (items.isEmpty()) {
            JLabel empty = new JLabel("No items", JLabel.CENTER);
            empty.setFont(new Font("Georgia", Font.ITALIC, 16));
            empty.setForeground(TEXT_DIM);
            empty.setBounds(0, ITEM_H / 2 - 20, ITEM_W - 4, 40);
            itemGridPanel.add(empty);
        }

        itemScroll = new JScrollPane(itemGridPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Inset further so the scroll pane never overlaps the painted rounded corners
        itemScroll.setBounds(ITEM_X + ARC, BOX_Y + ARC, ITEM_W - ARC * 2 - 4, ITEM_H - ARC * 2);
        itemScroll.setBorder(BorderFactory.createEmptyBorder());
        // Make scroll pane and viewport transparent — the rounded box is
        // already painted by InventoryPanel.paintComponent, so the scroll
        // pane just needs to show through without covering the corners
        itemScroll.setOpaque(false);
        itemScroll.getViewport().setOpaque(false);
        itemGridPanel.setOpaque(false);
        styleScrollBar(itemScroll.getVerticalScrollBar());
        add(itemScroll);

        revalidate();
        repaint();
    }

    private JPanel buildItemCell(InventoryItem item) {
        JPanel cell = new JPanel(null) {
            boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { selectItem(item); }
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean selected = item == selectedItem;
                Color bg = selected ? CELL_SEL : hovered ? CELL_HOV : CELL_BG;

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, CELL_SIZE, CELL_SIZE, 10, 10);
                g2.setColor(selected ? BTN_DARK_BDR : BORDER_DARK);
                g2.setStroke(new BasicStroke(selected ? 2.5f : 1.5f));
                g2.drawRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 10, 10);
                g2.setStroke(new BasicStroke(1));

                // Item color block (placeholder sprite)
                Color ic = itemColor(item.getItemType());
                int bx = 12, by = 12, bw = CELL_SIZE - 24, bh = CELL_SIZE - 36;
                g2.setColor(ic);
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);
                g2.setColor(ic.darker());
                g2.drawRoundRect(bx, by, bw, bh, 8, 8);

                // Type initial
                g2.setFont(new Font("Georgia", Font.BOLD, 28));
                g2.setColor(new Color(255, 255, 255, 200));
                String ini = item.getItemType().name().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, bx + (bw - fm.stringWidth(ini)) / 2,
                        by + bh / 2 + fm.getAscent() / 2 - 4);

                // Quantity badge (top right) — only for stackable items
                String qty = getQuantityLabel(item);
                if (qty != null) {
                    g2.setFont(new Font("Georgia", Font.BOLD, 12));
                    FontMetrics fmQ = g2.getFontMetrics();
                    int qw = fmQ.stringWidth(qty) + 8;
                    int qx = CELL_SIZE - qw - 4, qy = 4;
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRoundRect(qx, qy, qw, 16, 6, 6);
                    g2.setColor(Color.WHITE);
                    g2.drawString(qty, qx + 4, qy + 13);
                }

                // Short name label at bottom
                g2.setFont(new Font("Georgia", Font.PLAIN, 10));
                g2.setColor(TEXT_DARK);
                String shortName = truncate(item.getName(), g2.getFontMetrics(), CELL_SIZE - 8);
                FontMetrics fmN = g2.getFontMetrics();
                g2.drawString(shortName, (CELL_SIZE - fmN.stringWidth(shortName)) / 2,
                        CELL_SIZE - 5);
            }
        };
        cell.setOpaque(false);
        return cell;
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private void switchTab(ItemType tab) {
        activeTab    = tab;
        selectedItem = null;
        buildUI();
    }

    // ── Item selection ────────────────────────────────────────────────────────

    private void selectItem(InventoryItem item) {
        selectedItem = item;
        refreshInfoBox(infoBox);
        // Repaint all cells so selection highlight updates
        cellMap.values().forEach(Component::repaint);
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private List<InventoryItem> getItemsForTab(ItemType tab) {
        List<InventoryItem> list = new ArrayList<>();
        switch (tab) {
            case WEAPON    -> list.addAll(inventory.getWeapons());
            case ARTIFACT  -> list.addAll(inventory.getArtifacts());
            case ASCENSION -> list.addAll(inventory.getAllCrystals());
            case MISC      -> list.addAll(inventory.getMiscItems());
        }
        return list;
    }

    private String getQuantityLabel(InventoryItem item) {
        if (item instanceof AscensionCrystal c) return "x" + c.getQuantity();
        return null;   // unique items have no quantity badge
    }

    private Color itemColor(ItemType type) {
        return switch (type) {
            case WEAPON    -> new Color(180, 80,  50);
            case ARTIFACT  -> new Color(60,  100, 180);
            case ASCENSION -> new Color(80,  160, 80);
            case MISC      -> new Color(130, 90,  160);
        };
    }

    // ── Button builders ───────────────────────────────────────────────────────

    private JButton goldenButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isPressed() ? BTN_GOLD.darker()
                        : getModel().isRollover() ? BTN_HOV : BTN_GOLD;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BTN_DARK_BDR);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, 14));
        btn.setForeground(TEXT_DARK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildTabButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = active ? TAB_ACTIVE
                        : getModel().isRollover() ? new Color(185, 155, 80) : TAB_INACTIVE;
                g2.setColor(fill);
                // Rounded top only
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + ARC, ARC, ARC);
                g2.setColor(active ? BTN_DARK_BDR : BORDER_DARK);
                g2.setStroke(new BasicStroke(active ? 2f : 1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight() + ARC - 2, ARC, ARC);
                // Hide bottom border by drawing over it
                g2.setColor(fill);
                g2.fillRect(1, getHeight() - 4, getWidth() - 2, 8);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, active ? 15 : 13));
        btn.setForeground(active ? TEXT_DARK : TEXT_MID);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Scroll bar styling ────────────────────────────────────────────────────

    private void styleScrollBar(JScrollBar bar) {
        bar.setPreferredSize(new Dimension(6, 0));   // thin bar
        bar.setBackground(new Color(0, 0, 0, 0));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BTN_GOLD;
                trackColor = new Color(0, 0, 0, 0);
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BTN_GOLD);
                g2.fillRoundRect(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 6, 6);
                g2.setColor(BTN_DARK_BDR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 6, 6);
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                // transparent track — no-op
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    // ── Stat formatting helpers ───────────────────────────────────────────────

    private String[][] buildWeaponRows(Entities.Weapons.Weapon wp) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{ "Base Attack", String.valueOf(wp.getCurrentAtk()) });
        if (wp.hasSecondaryStat()) {
            rows.add(new String[]{
                    formatStatName(wp.getSecondaryStatType()),
                    formatStatValue(wp.getSecondaryStatType(), wp.getCurrentSecondaryValue())
            });
        }
        return rows.toArray(new String[0][]);
    }

    private static String formatStatName(Entities.StatType type) {
        return switch (type) {
            case ATK          -> "Flat ATK";
            case ATK_PERCENT  -> "ATK %";
            case DEF          -> "Flat DEF";
            case DEF_PERCENT  -> "DEF %";
            case HP           -> "Flat HP";
            case HP_PERCENT   -> "HP %";
            case CRIT_RATE    -> "Crit Rate";
            case CRIT_DAMAGE  -> "Crit Damage";
            case ACCURACY     -> "Accuracy";
            case DAMAGE_BONUS -> "DMG Bonus";
        };
    }

    private static String formatStatValue(Entities.StatType type, double value) {
        return switch (type) {
            case ATK, DEF, HP -> String.valueOf((int) value);
            default            -> (int) Math.round(value * 100) + "%";
        };
    }

    private void drawCenteredStatus(Graphics2D g2, String text, boolean equipped,
                                    int x, int rw, int y) {
        g2.setFont(new Font("Georgia", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (rw - fm.stringWidth(text)) / 2;
        // Subtle background pill
        int pw = fm.stringWidth(text) + 16, ph = 18;
        int px = x + (rw - pw) / 2;
        g2.setColor(equipped ? new Color(60, 120, 200, 60) : new Color(0, 0, 0, 40));
        g2.fillRoundRect(px, y - 14, pw, ph, 8, 8);
        g2.setColor(equipped ? new Color(50, 110, 210) : TEXT_MID);
        g2.drawString(text, tx, y);
    }

    // ── Text utilities ────────────────────────────────────────────────────────

    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        for (String word : text.split(" ")) {
            if (lines.isEmpty()) { lines.add(word); continue; }
            String last = lines.get(lines.size() - 1);
            String test = last + " " + word;
            if (fm.stringWidth(test) <= maxWidth) lines.set(lines.size() - 1, test);
            else lines.add(word);
        }
        return lines;
    }

    private static String truncate(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) return text;
        while (text.length() > 1 && fm.stringWidth(text + "…") > maxWidth)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }
}