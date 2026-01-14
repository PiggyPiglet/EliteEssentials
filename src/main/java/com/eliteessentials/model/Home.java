package com.eliteessentials.model;

import java.time.Instant;

/**
 * Represents a player's saved home location.
 */
public class Home {

    private String name;
    private Location location;
    private long createdAt;

    public Home() {
        // For Gson deserialization
    }

    public Home(String name, Location location) {
        this.name = name;
        this.location = location;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("Home{name='%s', location=%s}", name, location);
    }
}
