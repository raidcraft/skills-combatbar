package de.raidcraft.hotbar.skills;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.combatbar.HotbarManager;
import fr.zcraft.zlib.components.gui.Gui;
import fr.zcraft.zlib.components.gui.GuiBase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RCSkillsHotbar extends BasePlugin {

    @Override
    public void enable() {
        RaidCraft.getComponent(HotbarManager.class)
                .registerHotbarSlotType(this, SkillsHotbarSlot.class);
        registerCommands(HotbarCommand.class);
    }

    @Override
    public void loadDependencyConfigs() {
        loadComponents(Gui.class);
        RaidCraft.getComponent(HotbarManager.class).addHotbarMenuAction(holder -> {
            GuiBase openGui = Gui.getOpenGui(holder.getPlayer());
            if (openGui != null && openGui.isOpen()) return;
            Gui.open(holder.getPlayer(), new SkillsMenu());
        });
    }

    @Override
    public void disable() {
    }

    public class HotbarCommand {

        @Command(
                aliases = "hotbar",
                desc = "Opens the Skills hotbar menu."
        )
        public void hotbar(CommandContext args, CommandSender sender) {
            Gui.open((Player) sender, new SkillsMenu());
        }
    }
}
