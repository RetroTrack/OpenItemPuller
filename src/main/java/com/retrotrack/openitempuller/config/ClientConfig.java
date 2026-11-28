package com.retrotrack.openitempuller.config;

import com.google.gson.annotations.SerializedName;

/**
 * Per-player settings (config/openitempuller/client.json). Never read by the server: the radius
 * here is only what this player <em>asks for</em>; the server clamps it to {@link ServerConfig#maxRadius()}.
 */
public record ClientConfig(
        @SerializedName("radius") int radius,
        @SerializedName("priority_type") PriorityType priorityType,
        @SerializedName("display_mode") DisplayMode displayMode,
        @SerializedName("sorting_mode") SortingMode sortingMode) {

    public static final int DEFAULT_RADIUS = 16;
    public static final ClientConfig DEFAULT =
            new ClientConfig(DEFAULT_RADIUS, PriorityType.RANGE, DisplayMode.ALL, SortingMode.DESCENDING);

    public ClientConfig {
        // Missing or unknown values arrive as 0 / null and fall back to the defaults.
        if (radius < 1) radius = DEFAULT_RADIUS;
        if (priorityType == null) priorityType = PriorityType.RANGE;
        if (displayMode == null) displayMode = DisplayMode.ALL;
        if (sortingMode == null) sortingMode = SortingMode.DESCENDING;
    }

    /** How the chest list is ordered. The ordinal is used in the lang keys ({@code ...option_1.mode_<n>}). */
    public enum PriorityType {
        @SerializedName("range") RANGE,
        @SerializedName("item_count") ITEM_COUNT;

        public PriorityType next() { return values()[(ordinal() + 1) % values().length]; }
    }

    /** Which items the item list shows. The ordinal is used in the lang keys ({@code ...option_2.mode_<n>}). */
    public enum DisplayMode {
        @SerializedName("all") ALL,
        @SerializedName("available") AVAILABLE;

        public DisplayMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public enum SortingMode {
        @SerializedName("descending") DESCENDING,
        @SerializedName("ascending") ASCENDING;

        public SortingMode toggled() { return this == ASCENDING ? DESCENDING : ASCENDING; }
    }
}
