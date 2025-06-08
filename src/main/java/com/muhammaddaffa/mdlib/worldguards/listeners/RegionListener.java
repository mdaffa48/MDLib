package com.muhammaddaffa.mdlib.worldguards.listeners;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.muhammaddaffa.mdlib.worldguards.SimpleWorldGuardAPI;
import com.muhammaddaffa.mdlib.worldguards.WgEntity;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class RegionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLogin(PlayerLoginEvent e) {
        if(e.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        WgEntity.getPlayerCache().remove(e.getPlayer().getUniqueId());
        WgEntity.getPlayerCache().put(e.getPlayer().getUniqueId(), new WgEntity(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        final WgEntity we = WgEntity.get(e.getPlayer().getUniqueId());
        if(we != null) {
            we.updateRegions(MovementWay.SPAWN, e.getPlayer().getLocation(), e.getPlayer().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        final WgEntity we = WgEntity.get(e.getPlayer().getUniqueId());
        if (we != null) {
            for (ProtectedRegion region : we.getRegions()) {
                final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementWay.DISCONNECT);
                final RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementWay.DISCONNECT);
                Bukkit.getPluginManager().callEvent(leaveEvent);
                Bukkit.getPluginManager().callEvent(leftEvent);
            }
            we.getRegions().clear();
            WgEntity.getPlayerCache().remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        final WgEntity we = WgEntity.get(e.getPlayer().getUniqueId());
        if (we != null) {
            for (ProtectedRegion region : we.getRegions()) {
                final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementWay.DISCONNECT);
                final RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementWay.DISCONNECT);
                Bukkit.getPluginManager().callEvent(leaveEvent);
                Bukkit.getPluginManager().callEvent(leftEvent);

            }
            we.getRegions().clear();
            WgEntity.getPlayerCache().remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        final WgEntity we = WgEntity.get(e.getPlayer().getUniqueId());
        if (we != null) {
            we.updateRegions(MovementWay.SPAWN, e.getRespawnLocation(), e.getPlayer().getLocation());
        }
    }

}
