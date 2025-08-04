package com.muhammaddaffa.mdlib.worldguards.listeners.entity;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.muhammaddaffa.mdlib.worldguards.WgEntity;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;

public class EntityRemoveListenerV1_20_4 implements Listener {

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

}
