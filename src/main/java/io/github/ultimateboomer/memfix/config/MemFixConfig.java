package io.github.ultimateboomer.memfix.config;

import io.github.ultimateboomer.memfix.MemFix;
import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;

@Config(name = MemFix.MOD_ID)
public class MemFixConfig implements ConfigData {
    public int poolSize = 4096;
}
