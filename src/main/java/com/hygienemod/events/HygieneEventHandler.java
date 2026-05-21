package com.hygienemod.events;

import com.hygienemod.HygieneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class HygieneEventHandler {

    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;       // toutes les secondes
    private static final long PROXIMITY_MS  = 5_000L;  // 5 secondes avant nausée

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HygieneManager.initPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter % CHECK_INTERVAL != 0) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long now = System.currentTimeMillis();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            HygieneManager.initPlayer(uuid);

            // --- Créatif : immunité totale ---
            if (player.isCreative()) {
                HygieneManager.resetBath(uuid);
                continue;
            }

            // --- Détection bain ---
            if (isInQualifyingWater(player)) {
                int prevLevel = HygieneManager.getDirtLevel(uuid);
                HygieneManager.resetBath(uuid);
                if (prevLevel > 0) {
                    player.sendSystemMessage(Component.literal("§8§oL'eau froide emporte l'odeur. Vous vous sentez propre."));
                }
                continue;
            }

            int dirtLevel   = HygieneManager.getDirtLevel(uuid);
            int lastNotified = HygieneManager.getLastNotifiedLevel(uuid);

            // --- Transitions : message au joueur lui-même à chaque nouveau niveau ---
            if (dirtLevel >= 1 && lastNotified < 1) {
                player.sendSystemMessage(Component.literal("§8§oVous commencez à sentir mauvais..."));
                HygieneManager.setLastNotifiedLevel(uuid, 1);
                lastNotified = 1;
            }
            if (dirtLevel >= 2 && lastNotified < 2) {
                player.sendSystemMessage(Component.literal("§8§oL'odeur se fait plus prononcée... Vous feriez mieux de vous laver."));
                HygieneManager.setLastNotifiedLevel(uuid, 2);
                lastNotified = 2;
            }
            if (dirtLevel >= 3 && lastNotified < 3) {
                player.sendSystemMessage(Component.literal("§8§oVous sentez vraiment très mauvais. Les gens autour de vous s'éloignent..."));
                HygieneManager.setLastNotifiedLevel(uuid, 3);
                lastNotified = 3;
            }
            if (dirtLevel >= 4 && lastNotified < 4) {
                player.sendSystemMessage(Component.literal("§8§oVous êtes nauséabond. Votre propre odeur vous donne envie de vomir."));
                HygieneManager.setLastNotifiedLevel(uuid, 4);
                lastNotified = 4;
            }

            // --- Niveau 2 : message aux joueurs proches (< 3 blocs) ---
            if (dirtLevel >= 2) {
                for (ServerPlayer nearby : getNearbyPlayers(player, server, 3.0)) {
                    if (HygieneManager.tryLevel2Message(uuid, nearby.getUUID())) {
                        nearby.sendSystemMessage(Component.literal("§8§oUne personne proche semble dégager une odeur inconfortable..."));
                    }
                }
            }

            // --- Niveau 3 : nausée aux joueurs proches après 5 s (< 5 blocs) ---
            if (dirtLevel >= 3) {
                List<ServerPlayer> nearbyPlayers = getNearbyPlayers(player, server, 5.0);
                Set<UUID> nearbyUUIDs = new HashSet<>();

                for (ServerPlayer nearby : nearbyPlayers) {
                    UUID nearbyUUID = nearby.getUUID();
                    nearbyUUIDs.add(nearbyUUID);

                    long firstSeen = HygieneManager.getProximityFirstSeen(uuid, nearbyUUID, now);
                    if (now - firstSeen >= PROXIMITY_MS) {
                        nearby.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
                        if (HygieneManager.tryLevel3Message(uuid, nearbyUUID)) {
                            nearby.sendSystemMessage(Component.literal("§8§oLa puanteur d'une personne se fait ressentir, vous avez envie de gerber..."));
                        }
                    }
                }

                HygieneManager.retainProximity(uuid, nearbyUUIDs);
            } else {
                HygieneManager.clearProximity(uuid);
            }

            // --- Niveau 4 : nausée sur soi + sur tous les joueurs < 7 blocs ---
            if (dirtLevel >= 4) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
                for (ServerPlayer nearby : getNearbyPlayers(player, server, 7.0)) {
                    nearby.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
                    if (HygieneManager.tryLevel4Message(uuid, nearby.getUUID())) {
                        nearby.sendSystemMessage(Component.literal("§8§oUne odeur pestilentielle vous envahit... vous n'arrivez plus à respirer."));
                    }
                }
            }
        }
    }

    // Vérifie que le joueur est dans un volume d'eau 2x2x2 minimum
    private boolean isInQualifyingWater(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        if (!isWater(level, feet) || !isWater(level, feet.above())) return false;

        int x = feet.getX(), y = feet.getY(), z = feet.getZ();
        int[][] anchors = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}};
        for (int[] a : anchors) {
            if (is2x2x2Water(level, x + a[0], y, z + a[1])) return true;
        }
        return false;
    }

    private boolean is2x2x2Water(Level level, int x, int y, int z) {
        for (int dx = 0; dx <= 1; dx++)
            for (int dy = 0; dy <= 1; dy++)
                for (int dz = 0; dz <= 1; dz++)
                    if (!isWater(level, new BlockPos(x + dx, y + dy, z + dz))) return false;
        return true;
    }

    private boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(Fluids.WATER);
    }

    private List<ServerPlayer> getNearbyPlayers(ServerPlayer source, MinecraftServer server, double range) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(source.getUUID())) continue;
            if (!other.level().dimension().equals(source.level().dimension())) continue;
            if (source.distanceTo(other) <= range) result.add(other);
        }
        return result;
    }
}
