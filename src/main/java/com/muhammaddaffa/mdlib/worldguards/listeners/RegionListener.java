package com.muhammaddaffa.mdlib.worldguards.listeners;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.muhammaddaffa.mdlib.worldguards.SimpleWorldGuardAPI;
import com.muhammaddaffa.mdlib.worldguards.WgPlayer;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class RegionListener implements Listener {

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
    )
    public void onLogin(PlayerLoginEvent e) {
        if(e.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        WgPlayer.getPlayerCache().remove(e.getPlayer().getUniqueId());
        WgPlayer.getPlayerCache().put(e.getPlayer().getUniqueId(), new WgPlayer(e.getPlayer()));
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onJoin(PlayerJoinEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        wp.updateRegions(MovementWay.SPAWN, e.getPlayer().getLocation(), e.getPlayer().getLocation(), e);

    }

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
    )
    public void onKick(PlayerKickEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        for (ProtectedRegion region : wp.getRegions()) {
            final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementWay.DISCONNECT, e);
            final RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementWay.DISCONNECT, e);
            Bukkit.getPluginManager().callEvent(leaveEvent);
            Bukkit.getPluginManager().callEvent(leftEvent);
        }
        SimpleWorldGuardAPI.isInRegion(e.getPlayer().getLocation(), "name");


        wp.getRegions().clear();
        WgPlayer.getPlayerCache().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onQuit(PlayerQuitEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        for (ProtectedRegion region : wp.getRegions()) {
            final RegionLeaveEvent leaveEvent = new RegionLeaveEvent(region, e.getPlayer(), MovementWay.DISCONNECT, e);
            final RegionLeftEvent leftEvent = new RegionLeftEvent(region, e.getPlayer(), MovementWay.DISCONNECT, e);
            Bukkit.getPluginManager().callEvent(leaveEvent);
            Bukkit.getPluginManager().callEvent(leftEvent);

        }
        wp.getRegions().clear();
        WgPlayer.getPlayerCache().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
    )
    public void onMove(PlayerMoveEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        e.setCancelled(wp.updateRegions(MovementWay.MOVE, e.getTo(), e.getFrom(), e));
    }

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
    )
    public void onMove(PlayerTeleportEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        e.setCancelled(wp.updateRegions(MovementWay.TELEPORT, e.getTo(), e.getFrom(), e));
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onRespawn(PlayerRespawnEvent e) {
        final WgPlayer wp = WgPlayer.get(e.getPlayer().getUniqueId());
        if(wp == null) return;

        wp.updateRegions(MovementWay.SPAWN, e.getRespawnLocation(), e.getPlayer().getLocation(), e);
    }

}
