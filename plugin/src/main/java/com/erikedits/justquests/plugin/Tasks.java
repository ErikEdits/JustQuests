package com.erikedits.justquests.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Scheduling and the one lock of the plugin's quest data (0.6.1), so the same code runs on every server
 * type, Folia included.
 * <ul>
 * <li>Folia has no Bukkit scheduler: repeating and global tasks go to its global region scheduler, a task
 *     about a player to that player's own scheduler (the thread of the region it stands in). Reached by
 *     reflection, so the plugin still builds against the plain Spigot API.</li>
 * <li>On Folia, events of different regions run at the same time on different threads. Every way into the
 *     plugin's logic (events, commands, timers, placeholders) therefore runs under {@link #LOCK}: one at a
 *     time, as on a normal server. Elsewhere the lock is never contended and costs nothing.</li>
 * </ul>
 */
public final class Tasks {
    /** The lock every event, command, timer and placeholder of the plugin takes. Reentrant (synchronized). */
    public static final Object LOCK = new Object();
    public static final boolean FOLIA = has("io.papermc.paper.threadedregions.RegionizedServer");

    private Tasks() {}

    // ---- the lock

    public static void locked(Runnable r) {
        synchronized (LOCK) {
            r.run();
        }
    }

    public static <T> T locked(Supplier<T> s) {
        synchronized (LOCK) {
            return s.get();
        }
    }

    /**
     * Registers every @EventHandler of the listener, each running under the lock (instead of
     * PluginManager.registerEvents, which would call them unlocked).
     */
    @SuppressWarnings("unchecked")
    public static void register(Plugin plugin, Listener listener) {
        for (Method m : listener.getClass().getMethods()) {
            EventHandler h = m.getAnnotation(EventHandler.class);
            if (h == null || m.getParameterCount() != 1 || !Event.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            Class<? extends Event> type = (Class<? extends Event>) m.getParameterTypes()[0];
            m.setAccessible(true);
            EventExecutor executor = (l, event) -> {
                if (!type.isInstance(event)) return;
                try {
                    synchronized (LOCK) {
                        m.invoke(l, event);
                    }
                } catch (InvocationTargetException e) {
                    throw new EventException(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new EventException(e);
                }
            };
            Bukkit.getPluginManager().registerEvent(type, listener, h.priority(), executor, plugin, h.ignoreCancelled());
        }
    }

    // ---- scheduling

    /** A repeating task (on Folia on the global region), under the lock. */
    public static void timer(Plugin plugin, Runnable r, long delay, long period) {
        Runnable locked = () -> locked(r);
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskTimer(plugin, locked, delay, period);
            return;
        }
        Object global = call(Bukkit.class, null, "getGlobalRegionScheduler");
        call(global.getClass(), global, "runAtFixedRate", plugin, (Consumer<Object>) t -> locked.run(), Math.max(1, delay), Math.max(1, period));
    }

    /** Next tick, not tied to a place (on Folia on the global region), under the lock. */
    public static void global(Plugin plugin, Runnable r) {
        Runnable locked = () -> locked(r);
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, locked);
            return;
        }
        Object global = call(Bukkit.class, null, "getGlobalRegionScheduler");
        call(global.getClass(), global, "run", plugin, (Consumer<Object>) t -> locked.run());
    }

    /** Next tick on the thread of the entity's region (on Folia; the main thread elsewhere), under the lock. */
    public static void at(Plugin plugin, Entity entity, Runnable r) {
        Runnable locked = () -> locked(r);
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, locked);
            return;
        }
        Object scheduler = call(Entity.class, entity, "getScheduler");
        call(scheduler.getClass(), scheduler, "run", plugin, (Consumer<Object>) t -> locked.run(), (Runnable) null);
    }

    /** Next tick in the region of a place (a block; on Folia; the main thread elsewhere), under the lock. */
    public static void at(Plugin plugin, org.bukkit.Location where, Runnable r) {
        Runnable locked = () -> locked(r);
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, locked);
            return;
        }
        Object scheduler = call(Bukkit.class, null, "getRegionScheduler");
        call(scheduler.getClass(), scheduler, "run", plugin, where, (Consumer<Object>) t -> locked.run());
    }

    /** Right now when the entity's region is this thread, else on its thread (Folia); right now elsewhere. */
    public static void on(Plugin plugin, Entity entity, Runnable r) {
        if (!FOLIA || owns(entity)) {
            locked(r);
        } else {
            at(plugin, entity, r);
        }
    }

    /** A console command; on Folia it has to run on the global region. */
    public static void console(Plugin plugin, String command) {
        if (!FOLIA) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            Object global = call(Bukkit.class, null, "getGlobalRegionScheduler");
            call(global.getClass(), global, "run", plugin, (Consumer<Object>) t -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }

    private static boolean owns(Entity entity) {
        Object r = call(Bukkit.class, null, "isOwnedByCurrentRegion", entity);
        return Boolean.TRUE.equals(r);
    }

    /** Calls a public method by name and argument count (the Folia API is reached this way). */
    private static Object call(Class<?> type, Object target, String name, Object... args) {
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] p = m.getParameterTypes();
            boolean fits = true;
            for (int i = 0; i < p.length && fits; i++) {
                fits = args[i] == null ? !p[i].isPrimitive() : wrap(p[i]).isInstance(args[i]);
            }
            if (!fits) continue;
            try {
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Folia: " + name + " failed", e);
            }
        }
        throw new IllegalStateException("Folia: no method " + name + " with " + args.length + " argument(s)");
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == long.class) return Long.class;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        return c;
    }

    private static boolean has(String cls) {
        try {
            Class.forName(cls, false, Tasks.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
