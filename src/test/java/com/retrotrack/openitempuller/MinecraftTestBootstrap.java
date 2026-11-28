package com.retrotrack.openitempuller;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Item, component and registry classes need Minecraft's registries to exist before a test can use them.
 * Call {@link #init()} from a {@code @BeforeAll} of every test that touches items.
 * <p>
 * If a Minecraft update changes how the game is bootstrapped, this is the one place to adjust.
 */
public final class MinecraftTestBootstrap {
    private static boolean done;

    private MinecraftTestBootstrap() {}

    public static synchronized void init() {
        if (done) return;
        try {
            // Sets the game version; looked up by name so a Minecraft version that no longer has it still compiles
            Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // not needed (or not available) in this version
        }
        Bootstrap.bootStrap();
        bindItemComponents();
        done = true;
    }

    /** True once an ItemStack can be created, i.e. the items' default components (max stack size etc.) are bound. */
    private static boolean componentsBound() {
        try {
            new ItemStack(Items.STONE);
            return true;
        } catch (NullPointerException notBoundYet) {
            return false;
        }
    }

    /**
     * Bootstrap.bootStrap() registers the items, but since the data component initializers were introduced the
     * default components of every item are only bound once the game loads its registries (what a server does on
     * start-up). Without that, "new ItemStack(...)" throws "Components not bound yet".
     * Done by reflection so a rename in a later Minecraft version gives a readable error instead of a compile error.
     */
    private static void bindItemComponents() {
        if (componentsBound()) return;

        List<String> log = new ArrayList<>();
        try {
            Object initializers = null;
            for (Field field : BuiltInRegistries.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType().getSimpleName().equals("DataComponentInitializers")) {
                    field.setAccessible(true);
                    initializers = field.get(null);
                }
            }
            if (initializers == null) {
                throw new IllegalStateException("no DataComponentInitializers field found in BuiltInRegistries");
            }

            // Everything that could be the registry context the initializers want; the most complete one first
            List<Object> contexts = new ArrayList<>();
            try {
                Class<?> vanilla = Class.forName("net.minecraft.data.registries.VanillaRegistries");
                for (Method method : vanilla.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                            && method.getReturnType().getSimpleName().equals("Provider")) {
                        method.setAccessible(true);
                        try {
                            contexts.add(method.invoke(null));
                            log.add("context from VanillaRegistries." + method.getName() + "()");
                        } catch (ReflectiveOperationException | RuntimeException e) {
                            log.add("VanillaRegistries." + method.getName() + "() failed: " + e);
                        }
                    }
                }
                if (contexts.isEmpty()) {
                    log.add("VanillaRegistries has: " + describe(vanilla.getDeclaredMethods()));
                }
            } catch (ClassNotFoundException e) {
                log.add("no VanillaRegistries class");
            }
            try {
                contexts.add(Class.forName("net.minecraft.core.RegistryAccess")
                        .getMethod("fromRegistryOfRegistries", Class.forName("net.minecraft.core.Registry"))
                        .invoke(null, BuiltInRegistries.REGISTRY));
                log.add("context from RegistryAccess.fromRegistryOfRegistries");
            } catch (ReflectiveOperationException | RuntimeException e) {
                log.add("RegistryAccess context failed: " + e);
            }

            Method build = null;
            for (Method method : initializers.getClass().getMethods()) {
                if (method.getName().equals("build") && method.getParameterCount() == 1) build = method;
            }
            if (build == null) {
                throw new IllegalStateException("no build(context) method; available: " + describe(initializers.getClass().getMethods()));
            }

            for (Object context : contexts) {
                if (!build.getParameterTypes()[0].isInstance(context)) {
                    log.add("build() wants " + build.getParameterTypes()[0].getName() + ", skipped " + context.getClass().getName());
                    continue;
                }
                try {
                    Object pending = build.invoke(initializers, context);
                    if (pending instanceof Iterable<?> iterable) {
                        for (Object entry : iterable) {
                            Method apply = null;
                            for (Method method : entry.getClass().getDeclaredMethods()) {
                                if (method.getParameterCount() == 0 && method.getName().equals("apply")) apply = method;
                            }
                            if (apply == null) {
                                throw new IllegalStateException("no apply() on " + entry.getClass() + "; available: " + describe(entry.getClass().getDeclaredMethods()));
                            }
                            apply.setAccessible(true);
                            apply.invoke(entry);
                        }
                    } else {
                        log.add("build() returned " + (pending == null ? "null" : pending.getClass().getName()));
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    log.add("binding with " + context.getClass().getName() + " failed: " + e + " / cause " + e.getCause());
                    continue;
                }
                if (componentsBound()) return;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Could not bind the item components for tests. " + log, e);
        }

        throw new IllegalStateException("Could not bind the item components for tests. " + log);
    }

    private static String describe(Method[] methods) {
        StringBuilder out = new StringBuilder();
        for (Method method : methods) out.append("\n  ").append(method);
        return out.toString();
    }
}