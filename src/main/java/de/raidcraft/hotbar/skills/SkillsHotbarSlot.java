package de.raidcraft.hotbar.skills;

import de.raidcraft.RaidCraft;
import de.raidcraft.combatbar.api.Hotbar;
import de.raidcraft.combatbar.api.HotbarException;
import de.raidcraft.combatbar.api.HotbarSlot;
import de.raidcraft.combatbar.api.HotbarSlotName;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.skills.api.hero.Hero;
import de.raidcraft.skills.api.skill.Skill;
import de.raidcraft.skills.util.SkillUtil;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@HotbarSlotName("skill")
public class SkillsHotbarSlot extends HotbarSlot {

    @Getter
    private Skill skill;
    private String skillName;

    public SkillsHotbarSlot() {
        setSaveItem(false);
        setName("skill");
    }

    public SkillsHotbarSlot(Skill skill) {
        this.skill = skill;
        this.skillName = skill.getName();
        setSaveItem(false);
        setName("skill");
    }

    @Override
    public void load(ConfigurationSection config) {
        this.skillName = config.getString("skill");
    }

    @Override
    public void saveData(ConfigurationSection config) {
        config.set("skill", skillName);
    }

    @Override
    public ItemStack getItem() {
        return buildDisplayItem();
    }

    @Override
    public void onAttach(Hotbar hotbar) throws HotbarException {
        try {
            Hero hero = RaidCraft.getComponent(CharacterManager.class).getHero(hotbar.getPlayer());
            this.skill = hero.getSkill(this.skillName);
        } catch (UnknownSkillException e) {
            throw new HotbarException(e);
        }
    }

    private ItemStack buildDisplayItem() {
        if (skill == null) return new ItemStack(Material.BARRIER);

        ItemStackBuilder sb = new ItemStackBuilder(Material.BOOK)
                .title(SkillUtil.formatHeader(skill));

        for (String line : SkillUtil.formatBody(skill)) {
            sb.longLore(line, 60);
        }

        return sb.item();
    }

    @Override
    public void onSelect(Player player) {
        if (skill == null) {
            player.sendMessage("Something went wrong...");
            return;
        }
        skill.use();
    }
}
