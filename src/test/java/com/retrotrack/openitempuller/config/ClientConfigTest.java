package com.retrotrack.openitempuller.config;

import com.google.gson.Gson;
import com.retrotrack.openitempuller.config.ClientConfig.DisplayMode;
import com.retrotrack.openitempuller.config.ClientConfig.PriorityType;
import com.retrotrack.openitempuller.config.ClientConfig.SortingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void emptyFileGivesTheDefaults() {
        assertEquals(ClientConfig.DEFAULT, GSON.fromJson("{}", ClientConfig.class));
    }

    @Test
    void validValuesAreKept() {
        ClientConfig config = GSON.fromJson(
                "{\"radius\": 40, \"priority_type\": \"item_count\", \"display_mode\": \"available\", \"sorting_mode\": \"ascending\"}",
                ClientConfig.class);
        assertEquals(new ClientConfig(40, PriorityType.ITEM_COUNT, DisplayMode.AVAILABLE, SortingMode.ASCENDING), config);
    }

    @Test
    void unknownEnumValuesFallBackToTheDefaults() {
        ClientConfig config = GSON.fromJson(
                "{\"priority_type\": \"bogus\", \"display_mode\": \"???\", \"sorting_mode\": \"sideways\"}", ClientConfig.class);
        assertEquals(PriorityType.RANGE, config.priorityType());
        assertEquals(DisplayMode.ALL, config.displayMode());
        assertEquals(SortingMode.DESCENDING, config.sortingMode());
    }

    @Test
    void radiusBelowOneFallsBackToTheDefault() {
        assertEquals(ClientConfig.DEFAULT_RADIUS, GSON.fromJson("{\"radius\": -3}", ClientConfig.class).radius());
    }

    @Test
    void settingsScreenCyclesWrapAround() {
        assertEquals(PriorityType.ITEM_COUNT, PriorityType.RANGE.next());
        assertEquals(PriorityType.RANGE, PriorityType.ITEM_COUNT.next());
        assertEquals(DisplayMode.AVAILABLE, DisplayMode.ALL.next());
        assertEquals(DisplayMode.ALL, DisplayMode.AVAILABLE.next());
        assertEquals(SortingMode.ASCENDING, SortingMode.DESCENDING.toggled());
        assertEquals(SortingMode.DESCENDING, SortingMode.ASCENDING.toggled());
    }

    @Test
    void enumOrdinalsMatchTheLangKeys() {
        // en_us.json uses option_1.mode_<ordinal> and option_2.mode_<ordinal>: changing the order breaks the labels
        assertEquals(0, PriorityType.RANGE.ordinal());
        assertEquals(1, PriorityType.ITEM_COUNT.ordinal());
        assertEquals(0, DisplayMode.ALL.ordinal());
        assertEquals(1, DisplayMode.AVAILABLE.ordinal());
    }
}
