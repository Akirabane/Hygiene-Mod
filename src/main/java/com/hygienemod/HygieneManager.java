package com.hygienemod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HygieneManager {

    private static final Map<UUID, Long> nextDirtyTime = new HashMap<>();
    private static final Map<UUID, Integer> lastNotifiedLevel = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> proximityFirstSeen = new HashMap<>();

    // Par joueur sale : Set des joueurs proches qui ont DÉJÀ reçu le message ce cycle.
    // Reset à chaque bain → ils pourront recevoir le message au prochain cycle.
    private static final Map<UUID, Set<UUID>> level2Notified = new HashMap<>();
    private static final Map<UUID, Set<UUID>> level3Notified = new HashMap<>();
    private static final Map<UUID, Set<UUID>> level4Notified = new HashMap<>();

    public static final long DIRTY_DELAY_MS   =  30L * 1000;  // TEST: 30 s → level 1  (prod: 60 min)
    public static final long LEVEL_2_EXTRA_MS =  30L * 1000;  // TEST: +30 s → level 2  (prod: +30 min)
    public static final long LEVEL_3_EXTRA_MS =  90L * 1000;  // TEST: +90 s → level 3  (prod: +60 min)
    public static final long LEVEL_4_EXTRA_MS = 150L * 1000;  // TEST: +150 s → level 4 (prod: +120 min)
    public static final long SOAP_BONUS_MS    =  15L * 1000;  // TEST: +15 s            (prod: +30 min)

    public static void initPlayer(UUID uuid) {
        nextDirtyTime.computeIfAbsent(uuid, k -> System.currentTimeMillis() + DIRTY_DELAY_MS);
        lastNotifiedLevel.computeIfAbsent(uuid, k -> 0);
    }

    public static int getDirtLevel(UUID uuid) {
        long now = System.currentTimeMillis();
        long threshold = nextDirtyTime.getOrDefault(uuid, now + DIRTY_DELAY_MS);
        long elapsed = now - threshold;
        if (elapsed < 0)                  return 0;
        if (elapsed < LEVEL_2_EXTRA_MS)   return 1;
        if (elapsed < LEVEL_3_EXTRA_MS)   return 2;
        if (elapsed < LEVEL_4_EXTRA_MS)   return 3;
        return 4;
    }

    public static void resetBath(UUID uuid) {
        nextDirtyTime.put(uuid, System.currentTimeMillis() + DIRTY_DELAY_MS);
        lastNotifiedLevel.put(uuid, 0);
        proximityFirstSeen.remove(uuid);
        // Reset des notifications → les joueurs proches pourront recevoir les messages à nouveau
        level2Notified.remove(uuid);
        level3Notified.remove(uuid);
        level4Notified.remove(uuid);
    }

    public static void applySoapBonus(UUID uuid) {
        long current = nextDirtyTime.getOrDefault(uuid, System.currentTimeMillis() + DIRTY_DELAY_MS);
        nextDirtyTime.put(uuid, current + SOAP_BONUS_MS);
        lastNotifiedLevel.put(uuid, 0);
    }

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

    // Renvoie true si le joueur nearby n'a pas encore reçu ce message ce cycle
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
