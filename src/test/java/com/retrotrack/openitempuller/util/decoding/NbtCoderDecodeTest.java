package com.retrotrack.openitempuller.util.decoding;

import com.retrotrack.openitempuller.MinecraftTestBootstrap;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload.ChestEntry;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload.ItemEntry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the client makes of the chest list the server sent. */
class NbtCoderDecodeTest {
    // Must run before the fields below: they touch Items, which needs the registries to exist already.
    // (A @BeforeAll is too late: static fields are initialised first.)
    static {
        MinecraftTestBootstrap.init();
    }

    private static final ItemVariant STONE = ItemVariant.of(Items.STONE);
    private static final ItemVariant DIAMOND = ItemVariant.of(Items.DIAMOND);

    @Test
    void countsAreLinkedToTheVariantTheyPointAt() {
        ChestEntry chest = new ChestEntry("container.chest", true, new BlockPos(1, 2, 3),
                List.of(new ItemEntry(0, 5), new ItemEntry(1, 9)));
        List<DecodedBlockEntity> decoded = NbtCoder.decode(List.of(STONE, DIAMOND), List.of(chest));

        assertEquals(1, decoded.size());
        assertEquals(new BlockPos(1, 2, 3), decoded.getFirst().pos());
        assertEquals(5, decoded.getFirst().items().get(STONE));
        assertEquals(9, decoded.getFirst().items().get(DIAMOND));
    }

    @Test
    void entriesWithABadVariantIndexAreSkippedInsteadOfCrashing() {
        ChestEntry chest = new ChestEntry("Box", false, BlockPos.ZERO,
                List.of(new ItemEntry(-1, 1), new ItemEntry(0, 5), new ItemEntry(7, 3)));
        List<DecodedBlockEntity> decoded = NbtCoder.decode(List.of(STONE), List.of(chest));

        assertEquals(1, decoded.getFirst().items().size());
        assertEquals(5, decoded.getFirst().items().get(STONE));
    }

    @Test
    void aDefaultNameStaysTranslatableAndACustomNameStaysLiteral() {
        ChestEntry plain = new ChestEntry("container.chest", true, BlockPos.ZERO, List.of());
        ChestEntry custom = new ChestEntry("container.chest", false, BlockPos.ZERO, List.of());
        List<DecodedBlockEntity> decoded = NbtCoder.decode(List.of(), List.of(plain, custom));

        // the same text, but a custom name named "container.chest" must not be translated
        assertTrue(decoded.get(0).name().getContents() instanceof TranslatableContents);
        assertTrue(!(decoded.get(1).name().getContents() instanceof TranslatableContents));
        assertEquals("container.chest", decoded.get(1).name().getString());
    }

    @Test
    void noChestsGivesAnEmptyList() {
        assertEquals(0, NbtCoder.decode(List.of(), null).size());
        assertEquals(0, NbtCoder.decode(List.of(), List.of()).size());
    }
}
