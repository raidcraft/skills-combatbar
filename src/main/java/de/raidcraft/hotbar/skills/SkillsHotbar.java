package de.raidcraft.hotbar.skills;

import de.raidcraft.combatbar.api.Hotbar;
import de.raidcraft.combatbar.api.HotbarHolder;
import de.raidcraft.combatbar.api.HotbarName;

import java.util.ArrayList;
import java.util.Arrays;

@HotbarName("skills")
public class SkillsHotbar extends Hotbar {

    public SkillsHotbar(HotbarHolder holder) {
        super(holder);
        setFillEmptySlots(false);
        setIndicies(Arrays.asList(2,3,4,5,6,7,8));
    }
}
