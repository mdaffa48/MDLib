package com.muhammaddaffa.mdlib.worldguards.listeners;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.muhammaddaffa.mdlib.worldguards.WgEntity;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public class EntityRegionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityMove(EntityMoveEvent event) {
        final WgEntity we = WgEntity.get(event.getEntity());
        if (we != null) {
            event.setCancelled(we.updateRegions(MovementWay.MOVE, event.getTo(), event.getFrom()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity))
            return;

        final WgEntity we = WgEntity.get(entity);
        if (we != null) {
            event.setCancelled(we.updateRegions(MovementWay.TELEPORT, event.getTo(), event.getFrom()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity))
            return;

        final WgEntity we = WgEntity.get(entity);
        if(we != null) {
            we.updateRegions(MovementWay.SPAWN, entity.getLocation(), entity.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntitySpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        final WgEntity we = WgEntity.get(entity);
        if(we != null) {
            we.updateRegions(MovementWay.SPAWN, entity.getLocation(), entity.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity))
            return;

        final WgEntity we = WgEntity.get(entity.getUniqueId());
        if (we != null) {
            for (ProtectedRegion region : we.getRegions()) {
                final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, entity, MovementWay.DISCONNECT);
                final RegionLeftEvent leftEvent = new RegionLeftEvent(region, entity, MovementWay.DISCONNECT);
                Bukkit.getPluginManager().callEvent(leaveEvent);
                Bukkit.getPluginManager().callEvent(leftEvent);
            }
            we.getRegions().clear();
            WgEntity.getPlayerCache().remove(entity.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity))
            return;

        final WgEntity we = WgEntity.get(entity.getUniqueId());
        if (we != null) {
            for (ProtectedRegion region : we.getRegions()) {
                final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, entity, MovementWay.DISCONNECT);
                final RegionLeftEvent leftEvent = new RegionLeftEvent(region, entity, MovementWay.DISCONNECT);
                Bukkit.getPluginManager().callEvent(leaveEvent);
                Bukkit.getPluginManager().callEvent(leftEvent);
            }
            we.getRegions().clear();
            WgEntity.getPlayerCache().remove(entity.getUniqueId());
        }
    }

}
