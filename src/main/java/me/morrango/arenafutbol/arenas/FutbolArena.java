package me.morrango.arenafutbol.arenas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mc.alk.arena.BattleArena;
import mc.alk.arena.competition.match.Match;
import mc.alk.arena.events.matches.MatchMessageEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.EventPriority;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import me.morrango.arenafutbol.FutbolPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
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
public class FutbolArena extends Arena {

    private final FutbolPlugin plugin;
    private final Map<Entity, Player> kickedBy = new HashMap<Entity, Player>();
    private final Map<Entity, Match> kickedBalls = new HashMap<Entity, Match>();
    private final Map<Match, Entity> cleanUpList = new HashMap<Match, Entity>();
    private final Map<ArenaTeam, Integer> ballTimers = new HashMap<ArenaTeam, Integer>();
    private final Set<ArenaTeam> canKick = new HashSet<ArenaTeam>();
    private final Random random = new Random();

    public FutbolArena() {
        this.plugin = (FutbolPlugin) Bukkit.getPluginManager().getPlugin("ArenaFutbol");
    }

    public FutbolArena(FutbolPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onOpen() {
        Set<ArenaPlayer> set = getMatch().getPlayers();
        List<ArenaTeam> teamsList = getTeams();
        String teamOne = ((ArenaTeam) teamsList.get(0)).getDisplayName();
        String teamTwo = ((ArenaTeam) teamsList.get(1)).getDisplayName();
        /*
         SScoreboard scoreboard = getMatch().getScoreboard();
         SObjective objective = scoreboard.registerNewObjective("futbolObjective", "totalKillCount", "&6Time", SAPIDisplaySlot.SIDEBAR);

         Iterable<SObjective> objectives = scoreboard.getObjectives();
         for (SObjective sObjective : objectives) {
         sObjective.setDisplayPlayers(false);
         sObjective.setDisplayTeams(false);
         }
         SEntry team1 = scoreboard.createEntry(teamOne, "&4" + teamOne);
         SEntry team2 = scoreboard.createEntry(teamTwo, "&4" + teamTwo);

         objective.addEntry(team1, 0);
         objective.addEntry(team2, 0);
         for (ArenaPlayer arenaPlayer : set) {
         scoreboard.setScoreboard(arenaPlayer.getPlayer());
         } */
    }

    @Override
    public void onStart() {
        List<ArenaTeam> teamsList = this.match.getArena().getTeams();
        SpawnLocation loc = getSpawn(2, false);
        Location location = loc.getLocation();
        World world = location.getWorld();
        ItemStack is = this.plugin.getConfig().getItemStack("ball");

        Location center = fixCenter(world, location);
        world.dropItem(center, is);
        for (ArenaTeam t : teamsList) {
            this.canKick.add(t);
        }
    }
    
    @Override
    public void onVictory(MatchResult result) {
        onCancel();
    }

    @Override
    public void onCancel() {
        removeBalls(getMatch());
        removeArenaTeams(getMatch());
    }

    @ArenaEventHandler
    public void matchMessages(MatchMessageEvent event) {
        MatchState state = event.getState();
        if (!state.equals(MatchState.ONMATCHINTERVAL)) {
            event.setMatchMessage("");
        }
    }

    @ArenaEventHandler
    public void onArenaPlayerPickupItem(PlayerPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        event.setCancelled(true);
    }

    @ArenaEventHandler(needsPlayer = false)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (this.kickedBalls.containsKey(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @ArenaEventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @ArenaEventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = getAP(player);
        ArenaTeam kickersTeam = getTeam(arenaPlayer);
        List<Entity> entities = player.getNearbyEntities(1.0D, 1.0D, 1.0D);
        for (Entity entity : entities) {
            if (((entity instanceof Item)) && (this.canKick.contains(kickersTeam))) {
                Location location = player.getLocation();
                World world = player.getWorld();
                Vector kickVector = kickVector(player);
                entity.setVelocity(kickVector);
                FutbolPlugin.balls.add(entity);
                world.playEffect(location, Effect.STEP_SOUND, 10);
                this.kickedBy.put(entity, player);
                this.kickedBalls.put(entity, getMatch());
                this.cleanUpList.put(getMatch(), entity);

                for (ArenaTeam team : getTeams()) {
                    if (!canKick.contains(team)) {
                        canKick.add(team);
                        cancelBallTimer(team);
                    }
                }
            }
        }
    }

    public Vector kickVector(Player player) {
        float configAdjPitch = this.plugin.getConfig().getInt("pitch") * -1;
        float configMaxPitch = this.plugin.getConfig().getInt("maxpitch") * -1;
        double configPower = this.plugin.getConfig().getDouble("power");
        Location loc = player.getEyeLocation();
        if (player.getEquipment().getBoots() != null) {
            ItemStack boots = player.getEquipment().getBoots();
            if (boots.isSimilar(new ItemStack(Material.DIAMOND_BOOTS))) {
                configPower += 0.5D;
            }
            if (boots.isSimilar(new ItemStack(Material.IRON_BOOTS))) {
                configPower += 0.4D;
            }
            if (boots.isSimilar(new ItemStack(Material.GOLD_BOOTS))) {
                configPower += 0.3D;
            }
            if (boots.isSimilar(new ItemStack(Material.CHAINMAIL_BOOTS))) {
                configPower += 0.2D;
            }
            if (boots.isSimilar(new ItemStack(Material.LEATHER_BOOTS))) {
                configPower += 0.1D;
            }
        }
        float pitch = loc.getPitch();
        pitch = pitch + configAdjPitch;
        if (pitch > 0.0F) {
            pitch = 0.0F;
        }
        if (pitch < configMaxPitch) {
            pitch = 0.0F + configMaxPitch;
        }
        loc.setPitch(pitch);
        Vector vector = loc.getDirection();
        vector = vector.multiply(configPower);
        return vector;
    }

    private void startBallTimer(final ArenaTeam team) {
        cancelBallTimer(team);
        int ballTimer = this.plugin.getConfig().getInt("balltimer");
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin,
                new Runnable() {
                    public void run() {
                        FutbolArena.this.canKick.add(team);
                    }
                }, ballTimer * 20 + 60);
        this.ballTimers.put(team, Integer.valueOf(task.getTaskId()));
    }

    private void cancelBallTimer(ArenaTeam team) {
        Integer timerid = (Integer) this.ballTimers.get(team);
        if (timerid != null) {
            Bukkit.getScheduler().cancelTask(timerid.intValue());
        }
    }

    @ArenaEventHandler(priority = EventPriority.HIGHEST, needsPlayer = false)
    public void onGoalScored(EntityInteractEvent event) {
        // SObjective objective = getMatch().getScoreboard().getObjective("futbolObjective");
        Entity ent = event.getEntity();
        if (((ent instanceof Item)) && (this.kickedBalls.containsKey(ent))
                && (this.kickedBy.get(ent) != null)) {
            World world = ent.getWorld();
            Location loc = event.getEntity().getLocation();
            Location center = fixCenter(world, getSpawn(2, false).getLocation());

            Block block = loc.getBlock().getRelative(BlockFace.DOWN);
            Material type = block.getType();
            event.setCancelled(true);
            Match thisMatch = (Match) this.kickedBalls.get(ent);
            List<ArenaTeam> teamsList = thisMatch.getArena().getTeams();
            ArenaTeam teamOne = (ArenaTeam) teamsList.get(0);
            ArenaTeam teamTwo = (ArenaTeam) teamsList.get(1);
            ArenaTeam scoringTeam = null;
            if ((!type.equals(Material.STONE)) && (!type.equals(Material.COBBLESTONE))) {
                this.plugin.log(ChatColor.RED + "Set blocks for goals.");
                return;
            }
            if (type.equals(Material.STONE)) {
                scoringTeam = (ArenaTeam) teamsList.get(0);
                // objective.setPoints(teamOne.getDisplayName(), teamOne.getNKills());
                createFireWork(center, Color.RED, teamOne.getNKills());
            }
            if (type.equals(Material.COBBLESTONE)) {
                scoringTeam = (ArenaTeam) teamsList.get(1);
                createFireWork(center, Color.BLUE, teamTwo.getNKills());
            }
            ArenaPlayer scoringPlayer = getAP((Player) this.kickedBy.get(ent));
            // Add kill and send message
            scoringTeam.addKill(scoringPlayer);
            // objective.setPoints(scoringTeam.getDisplayName(), scoringTeam.getNKills());
            this.canKick.remove(scoringTeam);
            startBallTimer(scoringTeam);
            this.kickedBy.put(ent, null);

            FutbolPlugin.balls.remove(ent);
            ent.remove();

            // Send ball to center
            ItemStack is = this.plugin.getConfig().getItemStack("ball");
            world.dropItem(center, is);

            // Return players to team spawn
            Set<Player> setOne = teamOne.getBukkitPlayers();
            Set<Player> setTwo = teamTwo.getBukkitPlayers();
            tpArenaTeams(setOne, setTwo, thisMatch);
        }
    }
    
    public void createFireWork(Location loc, Color teamColor, int i) {
        if ((loc != null) && (teamColor != null)) {
            World w = loc.getWorld();
            for (int j = 0; j <= i; j++) {
                Entity firework = w.spawnEntity(
                        new Location(w, loc.getX() + this.random.nextGaussian() * 3.0D, loc.getY(), loc.getZ() + this.random.nextGaussian() * 3.0D),
                        EntityType.FIREWORK);
                Firework fw = (Firework) firework;
                FireworkMeta meta = fw.getFireworkMeta();
                FireworkEffect.Builder builder = FireworkEffect.builder();

                builder.withColor(teamColor);
                switch (this.random.nextInt(3)) {
                    case 0:
                        builder.with(FireworkEffect.Type.BALL);
                        break;
                    case 1:
                        builder.with(FireworkEffect.Type.BURST);
                        break;
                    case 2:
                        builder.with(FireworkEffect.Type.BALL_LARGE);
                        break;
                    default:
                        builder.with(FireworkEffect.Type.CREEPER);
                }
                builder.trail(false);
                meta.addEffect(builder.build());
                meta.setPower(0);
                fw.setFireworkMeta(meta);
            }
        }
    }

    public ArenaPlayer getAP(Player player) {
        return BattleArena.toArenaPlayer(player);
    }

    public Location fixCenter(World world, Location origin) {
        Location center = new Location(world,
                origin.getX(),
                origin.getY() + 1.0,
                origin.getZ());
        Chunk chunk = center.getChunk();
        if (!chunk.isLoaded()) {
            center.getWorld().loadChunk(chunk);
        }
        return center;
    }

    public void tpArenaTeams(Set<Player> setOne, Set<Player> setTwo, Match match) {
        for (Player player : setOne) {
            player.teleport(match.getArena().getSpawn(0, false).getLocation());
        }
        for (Player player : setTwo) {
            player.teleport(match.getArena().getSpawn(1, false).getLocation());
        }
    }

    public void removeBalls(Match match) {
        Entity ball = (Entity) this.cleanUpList.get(match);
        if (ball != null) {
            FutbolPlugin.balls.remove(ball);
            this.kickedBalls.remove(ball);
            this.kickedBy.remove(ball);
            ball.remove();
        }
    }

    public void removeArenaTeams(Match match) {
        List<ArenaTeam> teamsList = match.getArena().getTeams();
        for (ArenaTeam t : teamsList) {
            if (this.canKick.contains(t)) {
                this.canKick.remove(t);
            }
        }
    }

}
