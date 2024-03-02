package com.muhammaddaffa.mdlib.worldguards.events;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.*;

public abstract class RegionEvent extends PlayerEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final ProtectedRegion region;
    private final MovementWay movement;
    public PlayerEvent parentEvent;

    public RegionEvent(ProtectedRegion region, Player player, MovementWay movement, PlayerEvent parent) {
        super(player);
        this.region = region;
        this.movement = movement;
        this.parentEvent = parent;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public MovementWay getMovementWay() {
        return this.movement;
    }

    /**
     * retrieves the event that has been used to create this event
     *
     * @return
     * @see PlayerMoveEvent
     * @see PlayerTeleportEvent
     * @see PlayerQuitEvent
     * @see PlayerKickEvent
     * @see PlayerJoinEvent
     * @see PlayerRespawnEvent
     */
    public PlayerEvent getParentEvent() {
        return parentEvent;
    }
}
