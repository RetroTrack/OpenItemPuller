package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.networking.VariantCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

/**
 * Clientbound chest list. The server caps it (see the MAX_* constants and the byte budget in
 * {@code NbtCoder}) so that even the worst case stays below the 1 MiB clientbound payload limit; anything
 * beyond the caps is dropped, nearest containers first.
 * <p>
 * Every distinct item variant (item + data components) is sent once in {@code variants}; the chests refer to
 * them by index, so a stack with large components (a filled shulker box, a long book) costs its size once.
 * {@code serverMaxRadius} is the server's limit (shown in the settings tooltip), not the radius used for this list.
 */
public record OpenPullItemScreenPayload(List<ItemVariant> variants, List<ChestEntry> chests, int serverMaxRadius)
        implements CustomPacketPayload {
    public static final int MAX_VARIANTS = 4096;
    public static final int MAX_CHESTS = 300;
    public static final int MAX_ITEMS_PER_CHEST = 256;
    public static final int MAX_NAME_LENGTH = 128;

    /** @param variant index into {@link OpenPullItemScreenPayload#variants()} */
    public record ItemEntry(int variant, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ItemEntry::variant,
                ByteBufCodecs.VAR_INT, ItemEntry::count,
                ItemEntry::new);
    }

    /**
     * @param name         a translation key when {@code translatable} is true (the client translates it in
     *                     its own language), otherwise the literal name (a custom name, shown as typed)
     * @param translatable separate flag instead of a marker inside the name, so a custom name can never
     *                     be mistaken for a translation key
     */
    public record ChestEntry(String name, boolean translatable, BlockPos pos, List<ItemEntry> items) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), ChestEntry::name,
                ByteBufCodecs.BOOL, ChestEntry::translatable,
                BlockPos.STREAM_CODEC, ChestEntry::pos,
                ItemEntry.CODEC.apply(ByteBufCodecs.list(MAX_ITEMS_PER_CHEST)), ChestEntry::items,
                ChestEntry::new);
    }

    public static final CustomPacketPayload.Type<OpenPullItemScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "open_pull_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPullItemScreenPayload> CODEC = StreamCodec.composite(
            VariantCodec.CODEC.apply(ByteBufCodecs.list(MAX_VARIANTS)), OpenPullItemScreenPayload::variants,
            ChestEntry.CODEC.apply(ByteBufCodecs.list(MAX_CHESTS)), OpenPullItemScreenPayload::chests,
            ByteBufCodecs.VAR_INT, OpenPullItemScreenPayload::serverMaxRadius,
            OpenPullItemScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
