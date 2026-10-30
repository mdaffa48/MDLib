package com.muhammaddaffa.mdlib.core.text;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NumbersTest {

    // Same pattern and default locale as Numbers, so the expected strings hold on any machine.
    private final DecimalFormat format = new DecimalFormat("###,###,###,###,###.##");

    @Test
    void formatShortensWithDefaultSuffixes() {
        assertEquals(format.format(999.5), Numbers.format(999.5));
        assertEquals(format.format(1.5) + "K", Numbers.format(1_500));
        assertEquals(format.format(2.25) + "M", Numbers.format(2_250_000));
        assertEquals(format.format(1) + "B", Numbers.format(1_000_000_000));
        assertEquals(format.format(3) + "Sp", Numbers.format(3e24));
    }

    @Test
    void eachThresholdStartsItsSuffixExactly() {
        assertEquals(format.format(1) + "K", Numbers.format(1_000));
        assertEquals(format.format(1) + "M", Numbers.format(1_000_000));
        assertEquals(format.format(1) + "T", Numbers.format(1e12));
        assertEquals(format.format(1) + "Qa", Numbers.format(1e15));
        assertEquals(format.format(1) + "Qi", Numbers.format(1e18));
        assertEquals(format.format(1) + "Sx", Numbers.format(1e21));
        assertEquals(format.format(999.99), Numbers.format(999.99));
    }

    @Test
    void notationIsAskedWithTheConfigPathOfEachSuffix() {
        List<String> asked = new java.util.ArrayList<>();
        for (double number : new double[]{1e3, 1e6, 1e9, 1e12, 1e15, 1e18, 1e21, 1e24}) {
            Numbers.format(number, (path, def) -> {
                asked.add(path + "=" + def);
                return def;
            });
        }
        assertEquals(List.of("currency-notation.thousand=K", "currency-notation.million=M", "currency-notation.billion=B",
                "currency-notation.trillion=T", "currency-notation.quadrillion=Qa", "currency-notation.quintillion=Qi",
                "currency-notation.sextillion=Sx", "currency-notation.septillion=Sp"), asked);
    }

    @Test
    void formatAsksTheNotationForEachSuffix() {
        Map<String, String> config = Map.of("currency-notation.thousand", "k");
        assertEquals(format.format(1.5) + "k", Numbers.format(1_500, (path, def) -> config.getOrDefault(path, def)));
        assertEquals(format.format(2) + "M", Numbers.format(2_000_000, (path, def) -> config.getOrDefault(path, def)));
    }

    @Test
    void digitsAndChecks() {
        assertEquals(format.format(1234567.891), Numbers.digits(1234567.891));
        assertTrue(Numbers.isInt("42"));
        assertFalse(Numbers.isInt("4.2"));
        assertTrue(Numbers.isDouble("4.2"));
        assertFalse(Numbers.isDouble("abc"));
        assertTrue(Numbers.isValid(List.of(1, 2), 1));
        assertFalse(Numbers.isValid(List.of(1, 2), 2));
        assertFalse(Numbers.isValid(List.of(1, 2), -1));
    }

    @Test
    void randomNumbersStayInRange() {
        for (int i = 0; i < 100; i++) {
            int value = Numbers.getRandomNumberBetween(3, 6);
            assertTrue(value >= 3 && value < 6);
            double decimal = Numbers.getRandomNumberBetween(1.5, 2.5);
            assertTrue(decimal >= 1.5 && decimal < 2.5);
        }
    }
}
