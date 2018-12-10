package de.raidcraft.hotbar.skills;

import de.raidcraft.RaidCraft;
import de.raidcraft.combatbar.HotbarManager;
import de.raidcraft.combatbar.api.Hotbar;
import de.raidcraft.combatbar.api.HotbarSlot;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.combat.EffectType;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.skills.api.hero.Hero;
import de.raidcraft.skills.api.skill.Active;
import de.raidcraft.skills.api.skill.Skill;
import de.raidcraft.skills.util.SkillUtil;
import fr.zcraft.zlib.components.gui.ExplorerGui;
import fr.zcraft.zlib.components.gui.GuiAction;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
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

        RaidCraft.getComponent(HotbarManager.class).getOrCreateHotbar(getPlayer(), SkillsHotbar.class);
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

        // TODO: zLib with 1.13 currently broken
//        if (skill.isUnlocked() && skill.isActive()) sb.glow();
        sb.hideAttributes()
                .title(SkillUtil.formatHeader(skill));

        for (String line : SkillUtil.formatBody(skill)) {
            sb.longLore(line, 30);
        }

        return sb.item();
    }

    @Override
    protected ItemStack getPickedUpItem(Skill skill) {
        if (!skill.isUnlocked() || !skill.isActive() || !(skill instanceof Active)) return null;

        return new ItemStackBuilder(getViewItem(skill))
                .amount(1)
                .title(skill.getName())
                .item();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        super.onClick(event);

        if (event.getClick() != ClickType.LEFT) {
            event.setCancelled(true);
            return;
        }

        Optional<Hotbar> activeHotbar = RaidCraft.getComponent(HotbarManager.class)
                .getActiveHotbar(getPlayer())
                .filter(SkillsHotbar.class::isInstance);

        Boolean isHotbarSlot = event.getSlotType() == InventoryType.SlotType.QUICKBAR
                && activeHotbar.map(hotbar -> hotbar.getHotbarSlot(event.getSlot()).isPresent()).orElse(false);

        if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
            if (isHotbarSlot) {
                // remove active hotbar slots to make place for items
                activeHotbar.ifPresent(hotbar -> {
                    hotbar.removeHotbarSlot(event.getSlot())
                            .filter(slot -> slot instanceof SkillsHotbarSlot)
                            .map(slot -> (SkillsHotbarSlot) slot)
                            .filter(slot -> slot.getSkill() != null)
                            .map(SkillsHotbarSlot::getSkill)
                            .map(this::getPickedUpItem)
                            .ifPresent(event::setCursor);
                });
            }
            // the player is trying to pick up something, which is handled elsewhere
            return;
        }

        try {
            Skill skill = RaidCraft.getComponent(CharacterManager.class).getHero(getPlayer())
                    .getSkill(ChatColor.stripColor(event.getCursor().getItemMeta().getDisplayName()));
            // the player is holding a hotbar skill
            if (event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
                if (event.getClickedInventory() == null) {
                    // handle clicking outside of inventory
                    event.setCancelled(true);
                    return;
                }
                if (event.getClickedInventory().equals(getInventory())) {
                    event.setCursor(null);
                    event.setCancelled(true);
                    return;
                }
                getPlayer().sendMessage(ChatColor.RED + "Du kannst Skills nur in Deiner Aktionsleiste platzieren.");
                event.setCancelled(true);
                return;
            }
            // the player is placing a skill in the wrong hotbarslots
            if (event.getSlot() < 2) {
                getPlayer().sendMessage(ChatColor.RED + "Du kannst Skills nur in den Slots der Aktionsleiste " + ChatColor.AQUA + "drei" + ChatColor.RED + " bis " + ChatColor.AQUA + "neun" + ChatColor.RED + " platzieren.");
                getPlayer().sendMessage(ChatColor.GRAY + "Die ersten beiden Slots sind für Waffen und Gegenstände reserviert.");
                event.setCancelled(true);
                return;
            }
            event.setCursor(event.getCurrentItem());
            event.setCurrentItem(null);
            activeHotbar.ifPresent(hotbar -> {
                Optional<HotbarSlot> removedSlot = hotbar.setHotbarSlot(event.getSlot(), new SkillsHotbarSlot(skill));
                removedSlot.filter(slot -> slot instanceof SkillsHotbarSlot)
                        .map(slot -> ((SkillsHotbarSlot) slot).getSkill())
                        .map(this::getPickedUpItem)
                        .ifPresent(event::setCursor);
            });
            event.setCancelled(true);
        } catch (UnknownSkillException e) {
            // ignore unknown skills - we are probably handling a normal item
            if (isHotbarSlot) {
                // make place for the item in hand
                activeHotbar.ifPresent(hotbar -> {
                    hotbar.removeHotbarSlot(event.getSlot())
                            .filter(slot -> slot instanceof SkillsHotbarSlot)
                            .map(slot -> (SkillsHotbarSlot) slot)
                            .filter(slot -> slot.getSkill() != null)
                            .ifPresent(slot -> getPlayer().sendMessage(ChatColor.GRAY + slot.getSkill().getFriendlyName() + " wurde aus deiner Hotbar entfernt."));
                });
            }
        }
    }

    @Override
    protected boolean onPutItem(ItemStack item) {
        return false;
    }

    @Override
    protected void onClose() {
        super.onClose();
        try {
            ItemStack itemOnCursor = getPlayer().getItemOnCursor();
            if (itemOnCursor != null && itemOnCursor.getType() != Material.AIR && itemOnCursor.getItemMeta() != null) {
                Hero hero = RaidCraft.getComponent(CharacterManager.class).getHero(getPlayer());
                if (hero != null) {
                    hero.getSkill(ChatColor.stripColor(itemOnCursor.getItemMeta().getDisplayName()));
                }
                getPlayer().setItemOnCursor(new ItemStack(Material.AIR));
            }
        } catch (UnknownSkillException e) {
            // ignored
        }
    }

    @GuiAction("close")
    private void close_gui() {
        close();
    }
}
