package me.morrango.arenafutbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mc.alk.arena.BattleArena;
import me.morrango.arenafutbol.commands.FutbolExecutor;
import me.morrango.arenafutbol.arenas.FutbolArena;
import me.morrango.arenafutbol.tasks.PhysicsHandler;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * ArenaFutbol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArenaFutbol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ArenaFutbol.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Morrango, Europia79
 */
public class FutbolPlugin extends JavaPlugin {
    
    public static Set<Entity> balls = new HashSet<Entity>();
    public Map<UUID, Vector> vectors = new HashMap<UUID, Vector>();
    private boolean particles = false;
    private Effect particleEffect;

    @Override
    public void onEnable() {
        loadConfig();

        BattleArena.registerCompetition(this, "Futbol", "fb", FutbolArena.class, new FutbolExecutor(this));
        BattleArena.registerCompetition(this, "WorldCup", "wc", FutbolArena.class, new FutbolExecutor(this));

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new PhysicsHandler(this), 1L, 1L);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelAllTasks();
    }

    public void log(String message) {
        getLogger().info(message);
    }

    public void loadConfig() {
        getConfig().addDefault("particles", Boolean.valueOf(false));
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        saveConfig();
        if (getConfig().getBoolean("particles")) {
            this.particles = true;
            // enableParticles();
        }
        String temp = getConfig().getString("particleEffect", "POTION_BREAK");
        try {
            this.particleEffect = Effect.valueOf(temp);
        } catch (IllegalArgumentException ex) {
            this.particleEffect = Effect.POTION_BREAK;
        }
    }

    public void doBallPhysics() {
        for (Entity ball : balls) {
            UUID uuid = ball.getUniqueId();
            Vector velocity = ball.getVelocity();
            if (this.vectors.containsKey(uuid)) {
                velocity = (Vector) this.vectors.get(uuid);
            }
            Vector newVector = ball.getVelocity();
            if (newVector.getX() == 0.0D) {
                newVector.setX(-velocity.getX() * 0.9D);
            } else if (Math.abs(velocity.getX() - newVector.getX()) < 0.15D) {
                newVector.setX(velocity.getX() * 0.975D);
            }
            if ((newVector.getY() == 0.0D) && (velocity.getY() < -0.1D)) {
                newVector.setY(-velocity.getY() * 0.9D);
            }
            if (newVector.getZ() == 0.0D) {
                newVector.setZ(-velocity.getZ() * 0.9D);
            } else if (Math.abs(velocity.getZ() - newVector.getZ()) < 0.15D) {
                newVector.setZ(velocity.getZ() * 0.975D);
            }
            ball.setVelocity(newVector);
            this.vectors.put(uuid, newVector);
            if (this.particles) {
                showEffect(ball);
            }
        }
    }

    public void showEffect(Entity entity) {
        Location location = entity.getLocation();
        World world = entity.getWorld();
        world.playEffect(location, this.particleEffect, 0, 128);
        // playParticleEffect(location, "fireworksSpark", 0.1F, 0.1F, 0.01F, 1, 64, 0.2F);
    }
    
    public void setParticleVisibility(boolean visible) {
        this.particles = visible;
    }
    
    /*
    public void enableParticles() {
        try {
            this.packet63Fields[0] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("a");
            this.packet63Fields[1] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("b");
            this.packet63Fields[2] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("c");
            this.packet63Fields[3] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("d");
            this.packet63Fields[4] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("e");
            this.packet63Fields[5] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("f");
            this.packet63Fields[6] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("g");
            this.packet63Fields[7] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("h");
            this.packet63Fields[8] = PacketPlayOutWorldParticles.class
                    .getDeclaredField("i");
            for (int i = 0; i <= 8; i++) {
                this.packet63Fields[i].setAccessible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles();
        try {
            this.packet63Fields[0].set(packet, name);
            this.packet63Fields[1].setFloat(packet, (float) location.getX());
            this.packet63Fields[2].setFloat(packet, (float) location.getY()
                    + yOffset);
            this.packet63Fields[3].setFloat(packet, (float) location.getZ());
            this.packet63Fields[4].setFloat(packet, spreadHoriz);
            this.packet63Fields[5].setFloat(packet, spreadVert);
            this.packet63Fields[6].setFloat(packet, spreadHoriz);
            this.packet63Fields[7].setFloat(packet, speed);
            this.packet63Fields[8].setInt(packet, count);

            int rSq = radius * radius;
            for (Player player : location.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(location) <= rSq) {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
