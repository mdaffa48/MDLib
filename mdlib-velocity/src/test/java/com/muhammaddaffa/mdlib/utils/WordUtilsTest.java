package com.muhammaddaffa.mdlib.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordUtilsTest {

    @Test
    void helpersMatchSpigotResults() {
        assertEquals("'s", WordUtils.getGenitiveSuffix("§amfnalex"));
        assertEquals("'", WordUtils.getGenitiveSuffix("Jesus§r"));
        assertEquals("'", WordUtils.getGenitiveSuffix("Boss§X"));
        assertEquals("Armor Stand", WordUtils.getNiceName("ARMOR_STAND"));
        assertEquals("Hello", WordUtils.upperCaseFirstLetterOnly("hELLO"));
        assertEquals("B c", WordUtils.buildString(new String[]{"a", "B", "c"}, 1));
        assertEquals(2, WordUtils.countChar("a.b.c", '.'));
    }

}
