package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.networking.VariantCodec;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import com.retrotrack.openitempuller.util.BlockEntityUtil.PullResult;
import com.retrotrack.openitempuller.util.RateLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

/**
 * Serverbound: one click on "Pull" = one packet. It carries every selected item variant
 * (item + components), each with the containers to take it from and how many.
 * <p>
 * The serverbound payload limit is 32 KiB. Typical entries are a few dozen bytes, but a variant with large
 * components may be up to 8 KiB (the server lists nothing bigger), so {@link #split} cuts the entries into as
 * few packets as fit under {@link #MAX_PACKET_BYTES}. Normally that is exactly one.
 * <p>
 * The channel id carries a version: a client and server that disagree on the format see "no receiver"
 * (a friendly message) instead of a decode error that disconnects the player.
 * Lists longer than {@link #MAX_SOURCES} / {@link #MAX_ENTRIES} fail to decode.
 *
 * @param target where to put the items, or empty for the player's inventory
 */
public record PullItemsPayload(Optional<BlockPos> target, List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_SOURCES = 64;
    public static final int MAX_ENTRIES = 64;
    /** Stay clear of the 32 KiB limit. */
    public static final int MAX_PACKET_BYTES = 30 * 1024;
    /** Generous allowance for the channel id, the target and the list length that come with every packet. */
    private static final int HEADER_BYTES = 64;

    public record Source(BlockPos pos, int count) {
        public static final StreamCodec<ByteBuf, Source> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Source::pos,
                ByteBufCodecs.VAR_INT, Source::count,
                Source::new);
    }

    /** One item variant and where to take it from. */
    public record Entry(ItemVariant variant, List<Source> sources) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                VariantCodec.CODEC, Entry::variant,
                Source.CODEC.apply(ByteBufCodecs.list(MAX_SOURCES)), Entry::sources,
                Entry::new);
    }

    public static final CustomPacketPayload.Type<PullItemsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "pull_items_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PullItemsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PullItemsPayload::target,
            Entry.CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), PullItemsPayload::entries,
            PullItemsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Packs {@code entries} into as few payloads as stay under the size limit (normally one).
     * An entry that cannot be encoded at all is left out.
     */
    public static List<PullItemsPayload> split(Optional<BlockPos> target, List<Entry> entries, RegistryAccess registryAccess) {
        List<PullItemsPayload> packets = new ArrayList<>();
        List<Entry> current = new ArrayList<>();
        int bytes = HEADER_BYTES;
        for (Entry entry : entries) {
            int size = encodedSize(entry, registryAccess);
            if (size < 0 || size + HEADER_BYTES > MAX_PACKET_BYTES) continue;
            if (!current.isEmpty() && (bytes + size > MAX_PACKET_BYTES || current.size() >= MAX_ENTRIES)) {
                packets.add(new PullItemsPayload(target, List.copyOf(current)));
                current = new ArrayList<>();
                bytes = HEADER_BYTES;
            }
            current.add(entry);
            bytes += size;
        }
        if (!current.isEmpty()) packets.add(new PullItemsPayload(target, List.copyOf(current)));
        return packets;
    }

    private static int encodedSize(Entry entry, RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            Entry.CODEC.encode(buffer, entry);
            return buffer.readableBytes();
        } catch (RuntimeException e) {
            return -1;
        } finally {
            buffer.release();
        }
    }

    public void receiveServer(ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!RateLimiter.PULL.tryAcquire(player)) {
            player.sendSystemMessage(
                    Component.translatable("openitempuller.message.rate_limited").withStyle(ChatFormatting.RED), true);
            return;
        }
        try {
            PullResult result = BlockEntityUtil.retrieveItemsBlockEntity(player, this);
            // One message per click, whatever the outcome (second argument true = action bar)
            if (result.targetRejected()) {
                player.sendSystemMessage(Component.translatable("openitempuller.message.pull_invalid")
                        .withStyle(ChatFormatting.RED), true);
            } else if (result.moved() <= 0) {
                player.sendSystemMessage(Component.translatable("openitempuller.message.pull_nothing")
                        .withStyle(ChatFormatting.YELLOW), true);
            } else if (result.targetFallback()) {
                player.sendSystemMessage(Component.translatable("openitempuller.message.pull_to_target.fallback", result.moved())
                        .withStyle(ChatFormatting.GOLD), true);
            } else {
                player.sendSystemMessage(Component.translatable("openitempuller.message.pull_to_target.success", result.moved())
                        .withStyle(ChatFormatting.GREEN), true);
            }
        } catch (Exception e) {
            ItemPuller.LOGGER.error("Pull request from {} failed", player.getName().getString(), e);
            player.sendSystemMessage(
                    Component.translatable("openitempuller.message.internal_error").withStyle(ChatFormatting.RED), true);
        }
    }
}
