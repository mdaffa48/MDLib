package com.muhammaddaffa.mdlib.worldguards.events;

import com.muhammaddaffa.mdlib.worldguards.MovementWay;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

/**
 * event that is triggered before a player enters a WorldGuard region, can be cancelled sometimes
 */
public class RegionEnterEvent extends RegionEvent implements Cancellable {
    private boolean cancelled, cancellable;

    /**
     * creates a new RegionEnterEvent
     *
     * @param region   the region the player entered
     * @param entity   the entity who triggered the event
     * @param movement the type of movement how the player entered the region
     */
    public RegionEnterEvent(ProtectedRegion region, LivingEntity entity, MovementWay movement) {
        super(region, entity, movement);
        cancelled = false;
        cancellable = true;

        if (movement == MovementWay.SPAWN
                || movement == MovementWay.DISCONNECT) {
            cancellable = false;
        }
    }

    /**
     * sets whether this event should be cancelled
     * when the event is cancelled the player will not be able to move into the region
     *
     * @param cancelled true if the player should be stopped from moving into the region
     */
    @Override
    public void setCancelled(boolean cancelled) {
        if (!this.cancellable) {
            return;
        }

        this.cancelled = cancelled;
    }

    /**
     * retrieves whether this event will be cancelled/has been cancelled by any plugin
     *
     * @return true if this event will be cancelled and the player will be stopped from moving
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }


    /**
     * sometimes you can not cancel an event, i.e. if a player entered a region by spawning inside of it
     *
     * @return true, if you can cancel this event
     */
    public boolean isCancellable() {
        return this.cancellable;
    }

    protected void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;

        if (!this.cancellable) {
            this.cancelled = false;
        }
    }

}
