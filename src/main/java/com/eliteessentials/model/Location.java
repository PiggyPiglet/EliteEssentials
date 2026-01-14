package com.eliteessentials.model;

import java.util.Objects;

/**
 * Represents a location in the world.
 * This is a plugin-internal representation that can be converted to/from Hytale's location type.
 */
public class Location {

    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Location() {
        // For Gson deserialization
    }

    public Location(String world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }

    public Location(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public Location clone() {
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(location.x, x) == 0 &&
               Double.compare(location.y, y) == 0 &&
               Double.compare(location.z, z) == 0 &&
               Float.compare(location.yaw, yaw) == 0 &&
               Float.compare(location.pitch, pitch) == 0 &&
               Objects.equals(world, location.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return String.format("Location{world='%s', x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}",
                world, x, y, z, yaw, pitch);
    }
}
