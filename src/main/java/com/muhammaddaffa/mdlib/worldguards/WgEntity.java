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
import org.bukkit.event.player.PlayerEvent;

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
        if(!toRegions.getRegions().isEmpty()) {
            for(ProtectedRegion region : toRegions) {
                if(!regions.contains(region)) {
                    final RegionEnterEvent enter = new RegionEnterEvent(region, entity, way);
                    Bukkit.getPluginManager().callEvent(enter);
                    if(enter.isCancelled()) {
                        return true;
                    }
                    regions.add(region);
                    Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionEnteredEvent(region, entity, way)));
                }

            }

            final Set<ProtectedRegion> toRemove = new HashSet<>();

            for(ProtectedRegion oldRegion : fromRegions) {
                if(!toRegions.getRegions().contains(oldRegion)) {
                    final RegionLeaveEvent leave = new RegionLeaveEvent(oldRegion, entity, way);
                    Bukkit.getPluginManager().callEvent(leave);
                    if(leave.isCancelled()) {
                        return true;
                    }
                    Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionLeftEvent(oldRegion, entity, way)));
                    toRemove.add(oldRegion);
                }
            }
            regions.removeAll(toRemove);

        } else {
            for(ProtectedRegion region : regions) {
                final RegionLeaveEvent leave = new RegionLeaveEvent(region, entity, way);
                Bukkit.getPluginManager().callEvent(leave);
                if(leave.isCancelled()) {
                    return true;
                }
                Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionLeftEvent(region, entity, way)));
            }
            regions.clear();
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
