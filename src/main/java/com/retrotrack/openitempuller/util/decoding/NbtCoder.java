package com.retrotrack.openitempuller.util.decoding;

import com.retrotrack.openitempuller.networking.VariantCodec;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload.ChestEntry;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload.ItemEntry;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts between block entities and the compact wire records of {@link OpenPullItemScreenPayload}. */
public class NbtCoder {
    /** A variant whose encoded form is larger than this (a shulker box full of written books, say) is not listed. */
    private static final int MAX_VARIANT_BYTES = 8 * 1024;
    /** Budget for the whole variant table. The chest part adds at most ~400 KiB, so the total stays below 1 MiB. */
    private static final int MAX_TOTAL_VARIANT_BYTES = 400 * 1024;

    /** @param variants every distinct variant once; the chests refer to them by index */
    public record Encoded(List<ItemVariant> variants, List<ChestEntry> chests) {}

    /** The deduplicated variant table, with the size limits applied while it is filled. */
    private static final class VariantTable {
        private final List<ItemVariant> variants = new ArrayList<>();
        private final Map<ItemVariant, Integer> indices = new HashMap<>();
        private final Set<ItemVariant> rejected = new HashSet<>();
        private final RegistryAccess registryAccess;
        private int bytes;

        VariantTable(RegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
        }

        /** @return the index of {@code variant} in the table, or -1 if it does not fit within the limits */
        int indexOf(ItemVariant variant) {
            Integer known = indices.get(variant);
            if (known != null) return known;
            if (rejected.contains(variant)) return -1;

            int size = variants.size() >= OpenPullItemScreenPayload.MAX_VARIANTS ? Integer.MAX_VALUE : encodedSize(variant);
            if (size > MAX_VARIANT_BYTES || bytes + size > MAX_TOTAL_VARIANT_BYTES) {
                rejected.add(variant);
                return -1;
            }
            bytes += size;
            indices.put(variant, variants.size());
            variants.add(variant);
            return variants.size() - 1;
        }

        private int encodedSize(ItemVariant variant) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                VariantCodec.CODEC.encode(buffer, variant);
                return buffer.readableBytes();
            } catch (RuntimeException e) {
                return Integer.MAX_VALUE; // cannot be encoded, so it cannot be listed
            } finally {
                buffer.release();
            }
        }
    }

    public static Encoded encode(List<BlockEntity> blockEntityList, ServerPlayer player) {
        List<ChestEntry> chests = new ArrayList<>();
        VariantTable table = new VariantTable(player.level().registryAccess());

        for (BlockEntity blockEntity : blockEntityList) {
            if (chests.size() >= OpenPullItemScreenPayload.MAX_CHESTS) break; // list is sorted nearest-first
            BlockPos blockPos = blockEntity.getBlockPos();

            // One rule for every kind of storage: only what can really be taken out is listed
            Map<ItemVariant, Long> totals = BlockEntityUtil.extractableTotals(player.level(), blockPos);
            if (totals == null) continue; // not an item storage
            if (totals.isEmpty()) continue; // nothing to pull from here

            List<ItemEntry> items = new ArrayList<>();
            for (Map.Entry<ItemVariant, Long> e : totals.entrySet()) {
                if (items.size() >= OpenPullItemScreenPayload.MAX_ITEMS_PER_CHEST) break;
                int index = table.indexOf(e.getKey());
                if (index < 0) continue; // too large (or too many variants) to list
                items.add(new ItemEntry(index, (int) Math.min(e.getValue(), Integer.MAX_VALUE)));
            }
            if (items.isEmpty()) continue;

            Component name = displayName(player, blockEntity);
            String text;
            boolean translatable;
            if (name.getContents() instanceof TranslatableContents contents
                    && contents.getArgs().length == 0 && name.getSiblings().isEmpty()
                    && contents.getKey().length() <= OpenPullItemScreenPayload.MAX_NAME_LENGTH) {
                // A plain default name ("container.chest"): send the key so the client translates it
                text = contents.getKey();
                translatable = true;
            } else {
                text = truncate(name.getString());
                translatable = false;
            }
            chests.add(new ChestEntry(text, translatable, blockPos.immutable(), items));
        }
        return new Encoded(List.copyOf(table.variants), chests);
    }

    private static Component displayName(ServerPlayer player, BlockEntity blockEntity) {
        if (blockEntity instanceof Nameable nameable) return nameable.getName();
        return Component.translatable(player.level().getBlockState(blockEntity.getBlockPos()).getBlock().getDescriptionId());
    }

    /** Must stay within the codec's string limit, or the whole packet fails to encode. */
    private static String truncate(String name) {
        return name.length() > OpenPullItemScreenPayload.MAX_NAME_LENGTH
                ? name.substring(0, OpenPullItemScreenPayload.MAX_NAME_LENGTH) : name;
    }

    public static ArrayList<DecodedBlockEntity> decode(List<ItemVariant> variants, List<ChestEntry> chests) {
        ArrayList<DecodedBlockEntity> decoded = new ArrayList<>();
        if (chests == null) return decoded;
        for (ChestEntry chest : chests) {
            HashMap<ItemVariant, Integer> map = new HashMap<>();
            for (ItemEntry entry : chest.items()) {
                if (entry.variant() < 0 || entry.variant() >= variants.size()) continue; // defensive: bad index
                map.put(variants.get(entry.variant()), entry.count());
            }
            Component name = chest.translatable() ? Component.translatable(chest.name()) : Component.literal(chest.name());
            decoded.add(new DecodedBlockEntity(name, chest.pos(), map));
        }
        return decoded;
    }
}
