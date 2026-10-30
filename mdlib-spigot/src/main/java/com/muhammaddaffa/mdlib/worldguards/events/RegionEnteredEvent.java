package com.muhammaddaffa.mdlib.worldguards.events;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

/**
 * event that is triggered after a player entered a WorldGuard region
 */
public class RegionEnteredEvent extends RegionEvent {
    /**
     * creates a new RegionEnteredEvent
     *
     * @param region   the region the player entered
     * @param entity   the entity who triggered the event
     * @param movement the type of movement how the player entered the region
     */
    public RegionEnteredEvent(ProtectedRegion region, LivingEntity entity, MovementWay movement) {
        super(region, entity, movement);
    }

}
