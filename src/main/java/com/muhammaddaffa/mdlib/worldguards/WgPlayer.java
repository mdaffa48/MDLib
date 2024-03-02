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
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import java.util.*;

public class WgPlayer {

    private final Player player;
    private final List<ProtectedRegion> regions = new ArrayList<>();

    public WgPlayer(Player player) {
        this.player = player;
    }

    public boolean updateRegions(MovementWay way, Location to, Location from, PlayerEvent parent) {
        Objects.requireNonNull(way, "MovementWay 'way' can not be null.");
        Objects.requireNonNull(to, "Location 'to' can not be null.");
        Objects.requireNonNull(from, "Location 'from' can not be null.");

        final ApplicableRegionSet toRegions = SimpleWorldGuardAPI.getRegions(to);
        final ApplicableRegionSet fromRegions = SimpleWorldGuardAPI.getRegions(from);
        if(!toRegions.getRegions().isEmpty()) {
            for(ProtectedRegion region : toRegions) {
                if(!regions.contains(region)) {
                    final RegionEnterEvent enter = new RegionEnterEvent(region, player, way, parent);
                    Bukkit.getPluginManager().callEvent(enter);
                    if(enter.isCancelled()) {
                        return true;
                    }
                    regions.add(region);
                    Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionEnteredEvent(region, player, way, parent)));
                }

            }

            final Set<ProtectedRegion> toRemove = new HashSet<>();

            for(ProtectedRegion oldRegion : fromRegions) {
                if(!toRegions.getRegions().contains(oldRegion)) {
                    final RegionLeaveEvent leave = new RegionLeaveEvent(oldRegion, player, way, parent);
                    Bukkit.getPluginManager().callEvent(leave);
                    if(leave.isCancelled()) {
                        return true;
                    }
                    Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionLeftEvent(oldRegion, player, way, parent)));
                    toRemove.add(oldRegion);
                }
            }
            regions.removeAll(toRemove);

        } else {
            for(ProtectedRegion region : regions) {
                final RegionLeaveEvent leave = new RegionLeaveEvent(region, player, way, parent);
                Bukkit.getPluginManager().callEvent(leave);
                if(leave.isCancelled()) {
                    return true;
                }
                Executor.syncLater(1L, () -> Bukkit.getPluginManager().callEvent(new RegionLeftEvent(region, player, way, parent)));
            }
            regions.clear();
        }

        return false;
    }

    public List<ProtectedRegion> getRegions() {
        return regions;
    }

    public Player getPlayer() {
        return player;
    }

    // --------------------------------------
    // --------------------------------------
    // --------------------------------------

    private static final HashMap<UUID, WgPlayer> playerCache = new HashMap<>();

    public static HashMap<UUID, WgPlayer> getPlayerCache() {
        return playerCache;
    }

    public static WgPlayer get(UUID uuid) {
        return playerCache.get(uuid);
    }

}
