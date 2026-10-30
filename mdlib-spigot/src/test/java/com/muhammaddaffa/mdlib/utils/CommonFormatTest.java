package com.muhammaddaffa.mdlib.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.*;

class CommonFormatTest {

    private final DecimalFormat format = new DecimalFormat("###,###,###,###,###.##");

    @Test
    void configNotationReplacesOnlyTheSuffixesItDefines() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("currency-notation.thousand", "k");
        config.set("currency-notation.million", " mil");

        assertEquals(format.format(1.5) + "k", Common.format(config, 1_500));
        assertEquals(format.format(2) + " mil", Common.format(config, 2_000_000));
        assertEquals(format.format(3) + "B", Common.format(config, 3_000_000_000.0));
        assertEquals(format.format(12), Common.format(config, 12));
    }

    @Test
    void numberHelpersKeepTheirOldResults() {
        assertEquals(format.format(1.5) + "K", Common.format(1_500));
        assertEquals(format.format(1234.5), Common.digits(1234.5));
        assertTrue(Common.isInt("7"));
        assertFalse(Common.isDouble("x"));
        assertTrue(Common.isValid(java.util.List.of(1), 0));
    }
}
