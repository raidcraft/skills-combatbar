package de.raidcraft.hotbar.skills;

import de.raidcraft.RaidCraft;
import de.raidcraft.combatbar.HotbarManager;
import de.raidcraft.combatbar.api.Hotbar;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.combat.EffectType;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.skills.api.skill.Skill;
import de.raidcraft.skills.util.SkillUtil;
import fr.zcraft.zlib.components.gui.ExplorerGui;
import fr.zcraft.zlib.components.gui.GuiAction;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.Optional;

public class SkillsMenu extends ExplorerGui<Skill> {

    @Getter
    private final CharacterManager characterManager;

    public SkillsMenu() {
        this.characterManager = RaidCraft.getComponent(CharacterManager.class);
    }

    @Override
    protected void onUpdate() {
        setTitle("Verfügbare Skills");

        setData(RaidCraft.getComponent(CharacterManager.class).getHero(getPlayer())
                .getSkills().stream()
                .filter(Skill::isEnabled)
                .filter(skill -> !skill.isHidden())
                .sorted(Comparator.comparingInt(Skill::getRequiredLevel))
                .toArray(Skill[]::new));

        // Let's assume we want to add an action at the center of the bottom space
        // in an explorer GUI. As getSize() returns the size of the GUI, we can
        // substract slots from that to place our action at the end of the GUI.
        action("close", getSize() - 5, Material.BARRIER, "Menu schließen.");

        // ...BUT, with nothing else, the GUI will try to take all the place it can
        // get, and use the last line to put items if all of them can fit in one
        // page. There is an option to force the explorer GUI to keep this line
        // empty no matter what, placing items normally there in another page.
        // Here, we need this option because we use the space to place actions.
        setKeepHorizontalScrollingSpace(true);

        RaidCraft.getComponent(HotbarManager.class).getOrCreateHotbar(getPlayer())
                .fillEmptySlots(getHotbarFiller());
    }

    private ItemStack getHotbarFiller() {
        return new ItemStackBuilder(Material.END_CRYSTAL)
                .title(ChatColor.GREEN + "Freier Hotbar Slot")
                .longLore(ChatColor.YELLOW + "Ziehe einen Skill in diesen Slot um ihn in deiner Aktionsleiste zu speichern.", 60)
                .item();
    }

    @Override
    protected void onClose() {
        super.onClose();
        RaidCraft.getComponent(HotbarManager.class).getActiveHotbar(getPlayer())
                .ifPresent(hotbar -> {
                    for (Integer index : hotbar.getIndicies()) {
                        if (getHotbarFiller().isSimilar(getPlayer().getInventory().getItem(index))) {
                            getPlayer().getInventory().clear(index);
                        }
                    }
                    hotbar.fillEmptySlots();
                });
    }

    @Override
    protected ItemStack getViewItem(Skill skill) {
        ItemStackBuilder sb = new ItemStackBuilder();

        if (skill.isOfType(EffectType.PHYSICAL) && skill.isOfType(EffectType.DAMAGING)) {
            sb.material(Material.IRON_SWORD);
        } else if (skill.isOfType(EffectType.RANGE) && skill.isOfType(EffectType.PHYSICAL)) {
            sb.material(Material.BOW);
        } else if (skill.isOfType(EffectType.MAGICAL) && skill.isOfType(EffectType.DAMAGING)) {
            sb.material(Material.BLAZE_ROD);
        } else {
            sb.material(Material.BOOK);
        }

        sb.amount(skill.getRequiredLevel());

        if (skill.isUnlocked() && skill.isActive()) sb.glow();
        sb.hideAttributes()
                .title(SkillUtil.formatHeader(skill));

        for (String line : SkillUtil.formatBody(skill)) {
            sb.longLore(line, 60);
        }

        return sb.item();
    }

    @Override
    protected ItemStack getPickedUpItem(Skill skill) {
        if (!skill.isUnlocked() || !skill.isActive()) return null;

        return new ItemStackBuilder(getViewItem(skill))
                .amount(1)
                .title(skill.getName())
                .item();
    }

    @Override
    protected void onRightClick(Skill skill) {
        if (!skill.isUnlocked() || !skill.isActive()) return;

        RaidCraft.getComponent(HotbarManager.class).getActiveHotbar(getPlayer())
                .ifPresent(hotbar -> hotbar.addHotbarSlot(new SkillsHotbarSlot(skill)));
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        super.onClick(event);

        if (event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCurrentItem(null);
                event.setCancelled(true);
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        Optional<Hotbar> activeHotbar = RaidCraft.getComponent(HotbarManager.class).getActiveHotbar(getPlayer());

        if (cursor == null || cursor.getType() == Material.AIR) {
            activeHotbar.ifPresent(hotbar -> hotbar.removeHotbarSlot(event.getSlot()));
            event.setCancelled(true);
            return;
        }

        activeHotbar.filter(hotbar -> hotbar.getIndicies().contains(event.getSlot()))
                .ifPresent(hotbar -> {
                    try {
                        Skill skill = RaidCraft.getComponent(CharacterManager.class).getHero(getPlayer())
                            .getSkill(ChatColor.stripColor(cursor.getItemMeta().getDisplayName()));
                        hotbar.setHotbarSlot(event.getSlot(), new SkillsHotbarSlot(skill));
                        event.setCursor(new ItemStack(Material.AIR));
                        event.setCancelled(true);
                    } catch (UnknownSkillException e) {
                        getPlayer().sendMessage(ChatColor.RED + e.getMessage());
                        e.printStackTrace();
                    }
                });
    }

    @Override
    protected boolean onPutItem(ItemStack item) {
        return false;
    }

    @GuiAction("close")
    private void close_gui() {
        close();
    }
}
