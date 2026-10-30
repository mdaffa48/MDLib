package com.muhammaddaffa.mdlib.core.slots;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotParserTest {

    private final List<String> invalid = new ArrayList<>();

    private List<Integer> parse(Object value, int size) {
        return SlotParser.parse(value, size, invalid::add);
    }

    @Test
    void singleNumbersAndNumericStrings() {
        assertEquals(List.of(13), parse(13, 27));
        assertEquals(List.of(13), parse("13", 27));
        assertEquals(List.of(0), parse(0, 9));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void inclusiveRangesAndMixedLists() {
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), parse("10-16", 27));
        assertEquals(List.of(10, 11, 12, 22), parse(List.of("10-12", 22), 27));
        assertEquals(List.of(0, 1, 2), parse(List.of(0, 1, 2), 27));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void parseKeepsDuplicatesAndOrder() {
        assertEquals(List.of(5, 3, 4, 5, 5), parse(List.of(5, "3-5", "5"), 27));
    }

    @Test
    void whitespaceAroundNumbersAndDashIsAllowed() {
        assertEquals(List.of(1, 2, 3), parse("  1 - 3 ", 9));
        assertEquals(List.of(4), parse(" 4\t", 9));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void singleSlotRangeAndLastSlot() {
        assertEquals(List.of(7), parse("7-7", 9));
        assertEquals(List.of(8), parse(8, 9));
        assertEquals(List.of(), parse(List.of(), 9));
        assertEquals(List.of(), parse(null, 9));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void invalidEntriesAreReportedTrimmedAndSkipped() {
        assertEquals(List.of(1),
                parse(Arrays.asList("abc", " 5-3 ", "9", "-1", 1, "1-2-3", "", null, 2.5, "1234567890"), 9));
        assertEquals(List.of("abc", "5-3", "9", "-1", "1-2-3", "", "null", "2.5", "1234567890"), invalid);
    }

    @Test
    void rangesThatRunPastTheMenuAreDroppedWhole() {
        assertEquals(List.of(), parse("7-9", 9));
        assertEquals(List.of("7-9"), invalid);
        assertEquals(List.of(), parse(0, 0));
        assertEquals(List.of("7-9", "0"), invalid);
    }

    @Test
    void nonListValuesAreReadAsText() {
        assertEquals(List.of(), parse(true, 9));
        assertEquals(List.of("true"), invalid);
    }

    @Test
    void itemSlotsMergeSlotThenSlotsWithoutDuplicates() {
        assertEquals(List.of(4, 0, 1, 2, 3), SlotParser.getItemSlots(4, List.of("0-4", 2), 9, invalid::add));
        assertEquals(List.of(0, 1), SlotParser.getItemSlots(null, "0-1", 9, invalid::add));
        assertEquals(List.of(), SlotParser.getItemSlots(null, null, 9, invalid::add));
        assertEquals(List.of(1), SlotParser.getItemSlots("x", List.of(1, 99), 9, invalid::add));
        assertEquals(List.of("x", "99"), invalid);
    }

    @Test
    void resultsAreMutableCopies() {
        List<Integer> slots = parse("1-2", 9);
        slots.add(3);
        List<Integer> merged = SlotParser.getItemSlots(1, null, 9, invalid::add);
        merged.add(2);
        assertEquals(List.of(1, 2), merged);
    }
}
