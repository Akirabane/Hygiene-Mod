package com.hygienemod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HygieneManager {

    public enum BathType { TUB, RIVER }

    // --- Timers selon type de bain ---
    // Baquet : bain complet
    public static final long DIRTY_DELAY_TUB    = 60L * 60 * 1000;  // 1h
    public static final long LEVEL_2_EXTRA_TUB  = 30L * 60 * 1000;  // +30min (1h30 total)
    public static final long LEVEL_3_EXTRA_TUB  = 60L * 60 * 1000;  // +60min (2h total)
    public static final long LEVEL_4_EXTRA_TUB  = 90L * 60 * 1000;  // +90min (2h30 total)
    public static final long SOAP_BONUS_TUB     = 30L * 60 * 1000;  // +30min

    // Eau naturelle : bain moins efficace
    public static final long DIRTY_DELAY_RIVER   = 30L * 60 * 1000; // 30min
    public static final long LEVEL_2_EXTRA_RIVER = 30L * 60 * 1000; // +30min (1h total)
    public static final long LEVEL_3_EXTRA_RIVER = 45L * 60 * 1000; // +45min (1h15 total)
    public static final long LEVEL_4_EXTRA_RIVER = 60L * 60 * 1000; // +60min (1h30 total)
    public static final long SOAP_BONUS_RIVER    = 15L * 60 * 1000; // +15min

    // Durées de trempage
    public static final long TUB_SOAK_MS   =  60L * 1000; // 1 min
    public static final long RIVER_SOAK_MS = 120L * 1000; // 2 min

    // --- État par joueur ---
    private static final Map<UUID, Long>    nextDirtyTime      = new HashMap<>();
    private static final Map<UUID, BathType> lastBathType      = new HashMap<>();
    private static final Map<UUID, Integer> lastNotifiedLevel  = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> proximityFirstSeen = new HashMap<>();

    private static final Map<UUID, Set<UUID>> level2Notified = new HashMap<>();
    private static final Map<UUID, Set<UUID>> level3Notified = new HashMap<>();
    private static final Map<UUID, Set<UUID>> level4Notified = new HashMap<>();

    // Trempage en cours
    private static final Map<UUID, Long>     soakingStartTime    = new HashMap<>();
    private static final Map<UUID, BathType> soakingType         = new HashMap<>();
    private static final Map<UUID, Boolean>  soakMidMessageSent  = new HashMap<>();

    public static void initPlayer(UUID uuid) {
        lastBathType.computeIfAbsent(uuid, k -> BathType.TUB);
        nextDirtyTime.computeIfAbsent(uuid, k -> System.currentTimeMillis() + DIRTY_DELAY_TUB);
        lastNotifiedLevel.computeIfAbsent(uuid, k -> 0);
    }

    public static int getDirtLevel(UUID uuid) {
        long now = System.currentTimeMillis();
        BathType type = lastBathType.getOrDefault(uuid, BathType.TUB);
        long delay  = (type == BathType.TUB) ? DIRTY_DELAY_TUB  : DIRTY_DELAY_RIVER;
        long extra2 = (type == BathType.TUB) ? LEVEL_2_EXTRA_TUB : LEVEL_2_EXTRA_RIVER;
        long extra3 = (type == BathType.TUB) ? LEVEL_3_EXTRA_TUB : LEVEL_3_EXTRA_RIVER;
        long extra4 = (type == BathType.TUB) ? LEVEL_4_EXTRA_TUB : LEVEL_4_EXTRA_RIVER;
        long threshold = nextDirtyTime.getOrDefault(uuid, now + delay);
        long elapsed   = now - threshold;
        if (elapsed < 0)       return 0;
        if (elapsed < extra2)  return 1;
        if (elapsed < extra3)  return 2;
        if (elapsed < extra4)  return 3;
        return 4;
    }

    public static void resetBath(UUID uuid, BathType type) {
        long delay = (type == BathType.TUB) ? DIRTY_DELAY_TUB : DIRTY_DELAY_RIVER;
        nextDirtyTime.put(uuid, System.currentTimeMillis() + delay);
        lastBathType.put(uuid, type);
        lastNotifiedLevel.put(uuid, 0);
        proximityFirstSeen.remove(uuid);
        level2Notified.remove(uuid);
        level3Notified.remove(uuid);
        level4Notified.remove(uuid);
        clearSoaking(uuid);
    }

    public static void applySoapBonus(UUID uuid, BathType context) {
        long bonus = (context == BathType.TUB) ? SOAP_BONUS_TUB : SOAP_BONUS_RIVER;
        BathType current = lastBathType.getOrDefault(uuid, BathType.TUB);
        long delay = (current == BathType.TUB) ? DIRTY_DELAY_TUB : DIRTY_DELAY_RIVER;
        long now = System.currentTimeMillis();
        long base = nextDirtyTime.getOrDefault(uuid, now + delay);
        nextDirtyTime.put(uuid, base + bonus);
        lastNotifiedLevel.put(uuid, 0);
    }

    // --- Trempage ---

    public static void startSoaking(UUID uuid, BathType type) {
        soakingStartTime.put(uuid, System.currentTimeMillis());
        soakingType.put(uuid, type);
        soakMidMessageSent.put(uuid, false);
    }

    public static void clearSoaking(UUID uuid) {
        soakingStartTime.remove(uuid);
        soakingType.remove(uuid);
        soakMidMessageSent.remove(uuid);
    }

    public static boolean isSoaking(UUID uuid) {
        return soakingStartTime.containsKey(uuid);
    }

    public static BathType getSoakingType(UUID uuid) {
        return soakingType.getOrDefault(uuid, BathType.TUB);
    }

    public static long getSoakElapsed(UUID uuid) {
        Long start = soakingStartTime.get(uuid);
        return (start != null) ? System.currentTimeMillis() - start : 0L;
    }

    public static boolean isSoakMidMessageSent(UUID uuid) {
        return soakMidMessageSent.getOrDefault(uuid, false);
    }

    public static void setSoakMidMessageSent(UUID uuid) {
        soakMidMessageSent.put(uuid, true);
    }

    // --- Notifications de proximité ---

    public static int getLastNotifiedLevel(UUID uuid) {
        return lastNotifiedLevel.getOrDefault(uuid, 0);
    }

    public static void setLastNotifiedLevel(UUID uuid, int level) {
        lastNotifiedLevel.put(uuid, level);
    }

    public static long getProximityFirstSeen(UUID dirtyUUID, UUID nearbyUUID, long now) {
        return proximityFirstSeen
                .computeIfAbsent(dirtyUUID, k -> new HashMap<>())
                .computeIfAbsent(nearbyUUID, k -> now);
    }

    public static void retainProximity(UUID dirtyUUID, Set<UUID> presentUUIDs) {
        Map<UUID, Long> map = proximityFirstSeen.get(dirtyUUID);
        if (map != null) map.keySet().retainAll(presentUUIDs);
    }

    public static void clearProximity(UUID dirtyUUID) {
        proximityFirstSeen.remove(dirtyUUID);
    }

    public static boolean tryLevel2Message(UUID dirtyUUID, UUID nearbyUUID) {
        return level2Notified.computeIfAbsent(dirtyUUID, k -> new HashSet<>()).add(nearbyUUID);
    }

    public static boolean tryLevel3Message(UUID dirtyUUID, UUID nearbyUUID) {
        return level3Notified.computeIfAbsent(dirtyUUID, k -> new HashSet<>()).add(nearbyUUID);
    }

    public static boolean tryLevel4Message(UUID dirtyUUID, UUID nearbyUUID) {
        return level4Notified.computeIfAbsent(dirtyUUID, k -> new HashSet<>()).add(nearbyUUID);
    }
}
