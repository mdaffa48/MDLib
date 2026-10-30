package com.muhammaddaffa.mdlib.worldguards;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.worldguards.events.RegionEnterEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionEnteredEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.*;

public class WgEntity {

    private final LivingEntity entity;
    private final List<ProtectedRegion> regions = new ArrayList<>();

    public WgEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public boolean updateRegions(MovementWay way, Location to, Location from) {
        Objects.requireNonNull(way, "MovementWay 'way' can not be null.");
        Objects.requireNonNull(to, "Location 'to' can not be null.");
        Objects.requireNonNull(from, "Location 'from' can not be null.");

        final ApplicableRegionSet toRegions = SimpleWorldGuardAPI.getRegions(to);
        final ApplicableRegionSet fromRegions = SimpleWorldGuardAPI.getRegions(from);
        final List<ProtectedRegion> entered = new ArrayList<>();
        final List<ProtectedRegion> left = new ArrayList<>();
        for (ProtectedRegion region : toRegions) {
            if (!regions.contains(region)) {
                entered.add(region);
            }
        }
        final Iterable<ProtectedRegion> previous = toRegions.getRegions().isEmpty() ? regions : fromRegions;
        for (ProtectedRegion region : previous) {
            if (!toRegions.getRegions().contains(region)) {
                left.add(region);
            }
        }

        for (ProtectedRegion region : entered) {
            final RegionEnterEvent enter = new RegionEnterEvent(region, entity, way);
            Bukkit.getPluginManager().callEvent(enter);
            if (enter.isCancelled()) {
                return true;
            }
        }
        for (ProtectedRegion region : left) {
            final RegionLeaveEvent leave = new RegionLeaveEvent(region, entity, way);
            Bukkit.getPluginManager().callEvent(leave);
            if (leave.isCancelled()) {
                return true;
            }
        }

        regions.addAll(entered);
        regions.removeAll(left);
        for (ProtectedRegion region : entered) {
            Executor.entityLater(entity, 1L, () -> Bukkit.getPluginManager().callEvent(new RegionEnteredEvent(region, entity, way)));
        }
        for (ProtectedRegion region : left) {
            Executor.entityLater(entity, 1L, () -> Bukkit.getPluginManager().callEvent(new RegionLeftEvent(region, entity, way)));
        }
        return false;
    }

    public List<ProtectedRegion> getRegions() {
        return regions;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    // --------------------------------------
    // --------------------------------------
    // --------------------------------------

    private static final Map<UUID, WgEntity> playerCache = new HashMap<>();

    public static Map<UUID, WgEntity> getPlayerCache() {
        return playerCache;
    }

    public static WgEntity get(UUID uuid) {
        return playerCache.get(uuid);
    }

    public static WgEntity get(LivingEntity entity) {
        return playerCache.computeIfAbsent(entity.getUniqueId(), k -> new WgEntity(entity));
    }

}
