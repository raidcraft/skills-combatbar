package de.raidcraft.hotbar.skills;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.combatbar.HotbarManager;
import de.raidcraft.skills.api.skill.PlayerUnlockSkillEvent;
import de.raidcraft.skills.util.SkillUtil;
import de.raidcraft.util.CommandUtil;
import fr.zcraft.zlib.components.gui.Gui;
import fr.zcraft.zlib.components.gui.GuiBase;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RCSkillsHotbar extends BasePlugin implements Listener {

    @Getter
    private HotbarManager hotbarManager;

    @Override
    public void enable() {
        hotbarManager = RaidCraft.getComponent(HotbarManager.class);
        hotbarManager.registerHotbarType(this, SkillsHotbar.class);
        hotbarManager.registerHotbarSlotType(SkillsHotbarSlot.class);
        registerCommands(HotbarCommand.class);
    }

    @Override
    public void loadDependencyConfigs() {
        loadComponents(Gui.class);
        registerEvents(this);
        RaidCraft.getComponent(HotbarManager.class).addHotbarMenuAction(holder -> {
            GuiBase openGui = Gui.getOpenGui(holder.getPlayer());
            if (openGui != null && openGui.isOpen()) return;
            Gui.open(holder.getPlayer(), new SkillsMenu());
        });
    }

    public void openSkillMenu(Player player) {
        Gui.open(player, new SkillsMenu());
    }

    @Override
    public void disable() {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onUnlockSkill(PlayerUnlockSkillEvent event) {

        if (event.getSkill().isHidden()
                || !event.getSkill().isActive()
                || !event.getSkill().isEnabled()
                || (event.getHero().getVirtualProfession() != null && event.getHero().getVirtualProfession().equals(event.getSkill().getProfession()))) {
            return;
        }

        getHotbarManager().getHotbarHolder(event.getPlayer())
                .getActiveHotbar()
                .filter(hotbar -> hotbar instanceof SkillsHotbar)
                .ifPresent(hotbar -> {
                    BaseComponent[] skillTooltip = SkillUtil.getSkillTooltip(event.getSkill(), true);
                    if (hotbar.addHotbarSlot(new SkillsHotbarSlot(event.getSkill()))) {
                        ComponentBuilder text = new ComponentBuilder("Dein neuer Skill ").color(ChatColor.GRAY)
                                .append("[").color(ChatColor.GOLD)
                                .append(event.getSkill().getFriendlyName()).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, skillTooltip)).color(ChatColor.AQUA)
                                .append("]").color(ChatColor.GOLD)
                                .append(" wurde automatisch in deine Aktionsleiste gelegt.").color(ChatColor.GRAY);
                        event.getPlayer().spigot().sendMessage(text.create());
                    } else {
                        event.getPlayer().spigot().sendMessage(new ComponentBuilder("Dein neuer Skill ").color(ChatColor.GRAY)
                        .append("[").color(ChatColor.GOLD)
                        .append(event.getSkill().getFriendlyName()).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, skillTooltip)).color(ChatColor.AQUA)
                        .append("]").color(ChatColor.GOLD)
                        .append(" konnte leider ").color(ChatColor.GRAY)
                        .append("nicht").color(ChatColor.RED)
                        .append(" in deine Aktionsleiste eingefügt werden. ").color(ChatColor.GRAY)
                        .append("Klicke hier um deine Skills zu verwalten.").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hotbar")).color(ChatColor.GRAY).underlined(true).create());
                    }
                });
    }

    public class HotbarCommand {

        @Command(
                aliases = "hotbar",
                desc = "Opens the Skills hotbar menu."
        )
        public void hotbar(CommandContext args, CommandSender sender) {
            openSkillMenu((Player) sender);
        }
    }
}
