package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.config.ClientConfig;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.retrotrack.openitempuller.ItemPuller.serverConfig;

public class BlockEntityUtil {

    public static BlockPos getConnectedChestPos(ServerPlayer player, BlockPos chestPos) {
        Level world = player.level();
        if (!(world.getBlockEntity(chestPos) instanceof ChestBlockEntity)) return null;
        if (!(world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) return null;

        ChestType chestType = world.getBlockState(chestPos).getValue(ChestBlock.TYPE);
        if (chestType.equals(ChestType.SINGLE)) return null;

        Direction facing = world.getBlockState(chestPos).getValue(ChestBlock.FACING);
        BlockPos connectedChestPos;
        switch (facing) {
            case NORTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.EAST) : chestPos.relative(Direction.WEST);
            case EAST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.SOUTH) : chestPos.relative(Direction.NORTH);
            case SOUTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.WEST) : chestPos.relative(Direction.EAST);
            case WEST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.NORTH) : chestPos.relative(Direction.SOUTH);
            default -> {
                return null;
            }
        }

        // The other half can sit in the neighbouring chunk: never load it just to look.
        if (!world.hasChunkAt(connectedChestPos)) return null;
        if (world.getBlockState(connectedChestPos).getBlock() instanceof ChestBlock) return connectedChestPos;
        return null;
    }

    public static ArrayList<DecodedBlockEntity> getChestsWithItem(ItemVariant variant, ArrayList<DecodedBlockEntity> decodedBlockEntities,
                                                                  ClientConfig.PriorityType priority) {
        ArrayList<DecodedBlockEntity> collect = decodedBlockEntities.stream()
                .filter(chest -> chest.items().containsKey(variant))
                .collect(Collectors.toCollection(ArrayList::new));
        if (priority == ClientConfig.PriorityType.ITEM_COUNT) return BlockEntitySearchUtil.sortByItemCount(collect, variant);
        return collect;
    }

    /**
     * @param moved          how many items were moved in total
     * @param targetRejected the requested target block failed validation; nothing was attempted
     * @param targetFallback a target was requested but could not be used, so the items went to the player
     */
    public record PullResult(long moved, boolean targetRejected, boolean targetFallback) {
        static final PullResult REJECTED = new PullResult(0, true, false);
    }

    /**
     * Handles one pull request: every entry (item variant) with its sources, into one target.
     * Messaging the player is left to the caller, so a click ends in exactly one message.
     */
    public static PullResult retrieveItemsBlockEntity(ServerPlayer player, PullItemsPayload payload) {
        // Validated against the server limit, never against what the client asked for.
        int radius = serverConfig().maxRadius();
        Level level = player.level();

        boolean fromBlock = payload.target().isPresent();
        BlockPos targetPos = payload.target().orElse(null);
        if (fromBlock) {
            // The target is client-supplied too: same radius / type / lock / permission rules as a source.
            // Rejected outright (not silently redirected) so a forged target can't be probed.
            if (!PullValidator.canAccess(player, targetPos, radius)) return PullResult.REJECTED;
        }

        // The other half of a double chest is the same inventory as the half at targetPos
        BlockPos targetPartner = fromBlock ? getConnectedChestPos(player, targetPos) : null;

        // Resolved once for the whole request, not per source
        Storage<ItemVariant> target = fromBlock ? storageAt(level, targetPos, null) : null;
        Container targetContainer = fromBlock ? HopperBlockEntity.getContainerAt(level, targetPos) : null;
        boolean fellBack = fromBlock && target == null;
        if (target == null) target = PlayerInventoryStorage.of(player);

        long total = 0;
        // both list lengths are already capped by the codec (PullItemsPayload.MAX_ENTRIES / MAX_SOURCES)
        for (PullItemsPayload.Entry entry : payload.entries()) {
            ItemVariant variant = entry.variant();
            if (variant.isBlank()) continue;

            for (PullItemsPayload.Source source : entry.sources()) {
                BlockPos sourcePos = source.pos();
                if (source.count() <= 0) continue;
                if (fromBlock && (sourcePos.equals(targetPos) || sourcePos.equals(targetPartner))) continue;
                // canAccess covers both halves of a double chest
                if (!PullValidator.canAccess(player, sourcePos, radius)) continue;

                total += transfer(level, variant, source.count(), sourcePos, target, targetContainer);
            }
        }
        return new PullResult(total, false, fellBack);
    }

    /**
     * Moves up to {@code count} of {@code variant} from the container at {@code sourcePos} into {@code target}.
     * <p>
     * Everything goes through the Fabric Transfer API on purpose: it runs inside a transaction, honours
     * {@code canPlaceItem} of the target and calls {@code setChanged()} on commit, so the chunk is marked
     * for saving and comparators / furnaces see the change. Mutating the stacks by hand skipped all of that.
     *
     * @param targetContainer the vanilla container behind the target block, if any, to refuse moving a container into itself
     */
    private static int transfer(Level level, ItemVariant variant, int count, BlockPos sourcePos,
                                Storage<ItemVariant> target, @Nullable Container targetContainer) {
        Storage<ItemVariant> source = storageAt(level, sourcePos, EXTRACT_SIDE);
        if (source == null) return 0;

        if (targetContainer != null) {
            // Both halves of a double chest resolve to the same inventory: nothing to move.
            Container sourceContainer = HopperBlockEntity.getContainerAt(level, sourcePos);
            if (sourceContainer != null && sourceContainer.equals(targetContainer)) return 0;
        }
        return transferFromStorage(source, target, variant, count);
    }

    /**
     * The side items are taken from. Same as a hopper under the block: a furnace gives up its output (and empty
     * buckets) but not its input or fuel, a brewing stand its potions, and so on. Vanilla containers that are
     * not sided (chests, barrels, ...) ignore it.
     */
    public static final Direction EXTRACT_SIDE = Direction.DOWN;

    /**
     * The item storage of the container at {@code pos}. Vanilla containers are looked up through
     * {@code getContainerAt} (so a double chest is one storage covering both halves, matching the counts shown
     * in the GUI); anything else falls back to the Transfer API lookup.
     *
     * @param side the face to access it from; {@code null} ignores sided slot rules and only honours
     *             {@code canPlaceItem}. Use {@link #EXTRACT_SIDE} when taking items out.
     */
    @Nullable
    public static Storage<ItemVariant> storageAt(Level level, BlockPos pos, @Nullable Direction side) {
        Container container = HopperBlockEntity.getContainerAt(level, pos);
        if (container != null) return ContainerStorage.of(container, side);
        return ItemStorage.SIDED.find(level, pos, side);
    }

    /**
     * What can actually be taken out of the container at {@code pos}, per item variant: the one source of the
     * counts shown in the GUI, so the list and {@link #transfer} agree (a furnace's input is not offered).
     *
     * @return {@code null} when there is no item storage at {@code pos}
     */
    @Nullable
    public static Map<ItemVariant, Long> extractableTotals(Level level, BlockPos pos) {
        // Sum in a Long so huge storages can't overflow while adding.
        // A variant is the item plus its components, so enchanted / renamed / filled stacks stay separate.
        Map<ItemVariant, Long> totals = new LinkedHashMap<>();

        Container container = HopperBlockEntity.getContainerAt(level, pos);
        if (container != null && !(container instanceof WorldlyContainer)) {
            // Unsided containers allow taking from every slot: no need for a transaction per slot
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                var stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                totals.merge(ItemVariant.of(stack), (long) stack.getCount(), Long::sum);
            }
            return totals;
        }

        if (container == null && !BlockEntitySearchUtil.isValidBlockEntity(pos, level)) return null;
        Storage<ItemVariant> storage = storageAt(level, pos, EXTRACT_SIDE);
        if (storage == null) return null;
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank()) continue;
            long amount = extractable(view);
            if (amount > 0) totals.merge(view.getResource(), amount, Long::sum);
        }
        return totals;
    }

    /** What {@code view} would give up, found by really extracting inside a transaction that is rolled back. */
    private static long extractable(StorageView<ItemVariant> view) {
        try (Transaction transaction = Transaction.openOuter()) {
            return view.extract(view.getResource(), view.getAmount(), transaction);
        } // never committed: nothing changes
    }

    /** Moves exactly this variant (item and components), so enchanted / renamed / filled stacks are never mixed with plain ones. */
    public static int transferFromStorage(Storage<ItemVariant> source, Storage<ItemVariant> target, ItemVariant variant, int maxAmount) {
        if (source == null || target == null) return 0;
        return (int) StorageUtil.move(source, target, variant::equals, maxAmount, null);
    }
}
