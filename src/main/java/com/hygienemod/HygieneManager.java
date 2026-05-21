package com.hygienemod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HygieneManager {

    // Epoch ms when the player reaches dirt level 1 (computed from last bath)
    private static final Map<UUID, Long> nextDirtyTime = new HashMap<>();
    // Highest dirt level already notified to the player
    private static final Map<UUID, Integer> lastNotifiedLevel = new HashMap<>();
    // dirtyUUID -> (nearbyUUID -> epoch ms when first detected in range)
    private static final Map<UUID, Map<UUID, Long>> proximityFirstSeen = new HashMap<>();
    // Per-dirty-player cooldown for level-2 messages sent to each nearby player
    private static final Map<UUID, Map<UUID, Long>> level2Cooldown = new HashMap<>();
    // Per-dirty-player cooldown for level-3 messages sent to each nearby player
    private static final Map<UUID, Map<UUID, Long>> level3Cooldown = new HashMap<>();

    public static final long DIRTY_DELAY_MS   = 30L * 1000;   // TEST: 30 s → level 1  (prod: 60 min)
    public static final long LEVEL_2_EXTRA_MS = 30L * 1000;   // TEST: +30 s → level 2  (prod: +30 min)
    public static final long LEVEL_3_EXTRA_MS = 90L * 1000;   // TEST: +90 s → level 3  (prod: +60 min)
    public static final long SOAP_BONUS_MS    = 15L * 1000;   // TEST: +15 s            (prod: +30 min)
    public static final long MSG_COOLDOWN_MS  = 10L * 1000;   // TEST: 10 s cooldown    (prod: 2 min)

    // Called on login or first tick for a player — idempotent
    public static void initPlayer(UUID uuid) {
        nextDirtyTime.computeIfAbsent(uuid, k -> System.currentTimeMillis() + DIRTY_DELAY_MS);
        lastNotifiedLevel.computeIfAbsent(uuid, k -> 0);
    }

    // 0 = propre, 1/2/3 = niveaux de saleté
    public static int getDirtLevel(UUID uuid) {
        long now = System.currentTimeMillis();
        long threshold = nextDirtyTime.getOrDefault(uuid, now + DIRTY_DELAY_MS);
        long elapsed = now - threshold;
        if (elapsed < 0)                  return 0;
        if (elapsed < LEVEL_2_EXTRA_MS)   return 1;
        if (elapsed < LEVEL_3_EXTRA_MS)   return 2;
        return 3;
    }

    // Bain dans l'eau : remet le compteur à zéro
    public static void resetBath(UUID uuid) {
        nextDirtyTime.put(uuid, System.currentTimeMillis() + DIRTY_DELAY_MS);
        lastNotifiedLevel.put(uuid, 0);
        proximityFirstSeen.remove(uuid);
    }

    // Savon utilisé dans l'eau : +30 min sur le prochain niveau de saleté
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

    // Retourne l'instant (epoch ms) auquel le joueur nearby a été détecté pour la 1re fois
    // près du joueur dirty. Initialise à `now` si pas encore vu.
    public static long getProximityFirstSeen(UUID dirtyUUID, UUID nearbyUUID, long now) {
        return proximityFirstSeen
                .computeIfAbsent(dirtyUUID, k -> new HashMap<>())
                .computeIfAbsent(nearbyUUID, k -> now);
    }

    // Supprime les joueurs plus présents dans la zone de proximité
    public static void retainProximity(UUID dirtyUUID, Set<UUID> presentUUIDs) {
        Map<UUID, Long> map = proximityFirstSeen.get(dirtyUUID);
        if (map != null) map.keySet().retainAll(presentUUIDs);
    }

    public static void clearProximity(UUID dirtyUUID) {
        proximityFirstSeen.remove(dirtyUUID);
    }

    // Renvoie true si le message niveau 2 peut être envoyé (cooldown expiré)
    public static boolean tryLevel2Message(UUID dirtyUUID, UUID nearbyUUID) {
        return tryCooldown(level2Cooldown, dirtyUUID, nearbyUUID);
    }

    // Renvoie true si le message niveau 3 peut être envoyé (cooldown expiré)
    public static boolean tryLevel3Message(UUID dirtyUUID, UUID nearbyUUID) {
        return tryCooldown(level3Cooldown, dirtyUUID, nearbyUUID);
    }

    private static boolean tryCooldown(Map<UUID, Map<UUID, Long>> map, UUID dirtyUUID, UUID nearbyUUID) {
        long now = System.currentTimeMillis();
        Map<UUID, Long> inner = map.computeIfAbsent(dirtyUUID, k -> new HashMap<>());
        Long last = inner.get(nearbyUUID);
        if (last == null || now - last > MSG_COOLDOWN_MS) {
            inner.put(nearbyUUID, now);
            return true;
        }
        return false;
    }
}
