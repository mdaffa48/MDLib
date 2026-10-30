package com.muhammaddaffa.mdlib.worldguards.events;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

/**
 * event that is triggered after a player left a WorldGuard region
 */
public class RegionLeftEvent extends RegionEvent {
    /**
     * creates a new RegionLeftEvent
     *
     * @param region   the region the player has left
     * @param entity   the entity who triggered the event
     * @param movement the type of movement how the player left the region
     */
    public RegionLeftEvent(ProtectedRegion region, LivingEntity entity, MovementWay movement) {
        super(region, entity, movement);
    }

}
