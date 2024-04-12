package com.retrotrack.itempuller.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ItemListUtil implements ArgumentType<ItemStackArgument> {
    private final RegistryWrapper<Item> registryWrapper;

    public ItemListUtil(CommandRegistryAccess commandRegistryAccess) {
        this.registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.ITEM);
    }

    public static ItemStackArgumentType itemStack(CommandRegistryAccess commandRegistryAccess) {
        return new ItemStackArgumentType(commandRegistryAccess);
    }

    public ItemStackArgument parse(StringReader stringReader) throws CommandSyntaxException {
        ItemStringReader.ItemResult itemResult = ItemStringReader.item(this.registryWrapper, stringReader);
        return new ItemStackArgument(itemResult.item(), itemResult.nbt());
    }

    public static <S> ItemStackArgument getItemStackArgument(CommandContext<S> context, String name) {
        return context.getArgument(name, ItemStackArgument.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ItemStringReader.getSuggestions(this.registryWrapper, builder, false);
    }
}
